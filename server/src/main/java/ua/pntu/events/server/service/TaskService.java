package ua.pntu.events.server.service;

import ua.pntu.events.server.dao.EventDAO;
import ua.pntu.events.server.dao.PersonDAO;
import ua.pntu.events.server.dao.ResourceDAO;
import ua.pntu.events.server.dao.TaskDAO;
import ua.pntu.events.server.dao.TaskDependencyDAO;
import ua.pntu.events.server.dao.TaskResourceDAO;
import ua.pntu.events.server.model.Resource;
import ua.pntu.events.server.model.Task;
import ua.pntu.events.server.model.TaskResource;
import ua.pntu.events.server.model.TaskStatus;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Предметна логіка завдань: валідація, чотиристанний життєвий цикл,
 * залежності, ресурси та інтегральний показник пріоритету.
 */
public class TaskService extends BaseService {

    private static final Logger LOG = Logger.getLogger(TaskService.class.getName());

    private final TaskDAO taskDao;
    private final EventDAO eventDao;
    private final PersonDAO personDao;
    private final ResourceDAO resourceDao;
    private final TaskDependencyDAO dependencyDao;
    private final TaskResourceDAO taskResourceDao;
    private final Clock clock;

    public TaskService() {
        this(new TaskDAO(), new EventDAO(), new PersonDAO(), new ResourceDAO(),
                new TaskDependencyDAO(), new TaskResourceDAO(), Clock.systemDefaultZone());
    }

    public TaskService(TaskDAO taskDao, EventDAO eventDao, PersonDAO personDao, ResourceDAO resourceDao,
                       TaskDependencyDAO dependencyDao, TaskResourceDAO taskResourceDao, Clock clock) {
        this.taskDao = taskDao;
        this.eventDao = eventDao;
        this.personDao = personDao;
        this.resourceDao = resourceDao;
        this.dependencyDao = dependencyDao;
        this.taskResourceDao = taskResourceDao;
        this.clock = clock;
    }

    // =================== Життєвий цикл ===================

    /**
     * Єдине місце, де визначено допустиму послідовність переходів:
     * CREATED → ASSIGNED → IN_PROGRESS → DONE. Інші переходи (пропуск, повернення назад) заборонені.
     */
    public static boolean isAllowedTransition(TaskStatus from, TaskStatus to) {
        return (from == TaskStatus.CREATED && to == TaskStatus.ASSIGNED)
                || (from == TaskStatus.ASSIGNED && to == TaskStatus.IN_PROGRESS)
                || (from == TaskStatus.IN_PROGRESS && to == TaskStatus.DONE);
    }

    /** Змінює стан завдання після перевірки допустимості переходу та передумов. */
    public void changeStatus(int taskId, TaskStatus target) {
        executeVoid(LOG, "зміна стану завдання", () -> {
            Task task = require(taskId);
            TaskStatus from = task.getStatus();
            if (target == null || !isAllowedTransition(from, target)) {
                throw new IllegalStateException("Перехід " + from + " → " + target + " заборонений (завдання id=" + taskId + ")");
            }
            if (target == TaskStatus.ASSIGNED && task.getAssigneeId() == null) {
                throw new IllegalStateException("Для переходу в ПРИЗНАЧЕНО спочатку призначте виконавця (assign)");
            }
            if (target == TaskStatus.IN_PROGRESS) {
                for (int depId : dependencyDao.getDependsOnIds(taskId)) {
                    Optional<Task> dep = taskDao.getById(depId);
                    if (dep.isPresent() && dep.get().getStatus() != TaskStatus.DONE) {
                        throw new IllegalStateException("Не можна почати виконання: завдання id=" + depId
                                + " ще не виконано");
                    }
                }
            }
            taskDao.updateStatus(taskId, target);
            if (target == TaskStatus.DONE) {
                taskDao.updatePriorityIndex(taskId, null);
            }
            LOG.info("Завдання id=" + taskId + ": стан " + from + " → " + target);
            return null;
        });
    }

    /** Призначає виконавця: у стані CREATED виконує перехід у ASSIGNED, у стані ASSIGNED — перепризначення. */
    public void assign(int taskId, int personId) {
        executeVoid(LOG, "призначення виконавця", () -> {
            Task task = require(taskId);
            if (personDao.getById(personId).isEmpty()) {
                throw new ValidationException("Відповідальної особи з id=" + personId + " не існує");
            }
            if (task.getStatus() == TaskStatus.CREATED) {
                if (!isAllowedTransition(TaskStatus.CREATED, TaskStatus.ASSIGNED)) {
                    throw new IllegalStateException("Перехід заборонений");
                }
            } else if (task.getStatus() != TaskStatus.ASSIGNED) {
                throw new IllegalStateException("У стані " + task.getStatus() + " змінювати виконавця не можна");
            }
            taskDao.updateAssigneeAndStatus(taskId, personId, TaskStatus.ASSIGNED);
            LOG.info("Завдання id=" + taskId + ": призначено особу id=" + personId + ", стан → ASSIGNED");
            return null;
        });
    }

    // =================== CRUD із валідацією ===================

    /** Створює завдання. Початковий стан CREATED задає сервер; виконавця призначають через assign(). */
    public int add(Task t) {
        return execute(LOG, "створення завдання", () -> {
            validateData(t);
            if (t.getAssigneeId() != null) {
                throw new ValidationException("Виконавця призначають окремою операцією assign()");
            }
            t.setStatus(TaskStatus.CREATED);
            t.setPriorityIndex(computeIndex(t, 0));
            int id = taskDao.add(t);
            LOG.info("Створено завдання id=" + id + " (стан CREATED, I=" + t.getPriorityIndex() + ")");
            return id;
        });
    }

    /**
     * Створює завдання й одразу призначає виконавця (CREATED → ASSIGNED).
     * Наявність особи перевіряється до створення, щоб не лишати «напівстворених» записів.
     */
    public int addWithAssignee(Task t, int personId) {
        return execute(LOG, "створення завдання з виконавцем", () -> {
            if (personDao.getById(personId).isEmpty()) {
                throw new ValidationException("Відповідальної особи з id=" + personId + " не існує");
            }
            int id = add(t);
            assign(id, personId);
            return id;
        });
    }

    public List<Task> getAll() {
        return execute(LOG, "список завдань", taskDao::getAll);
    }

    public List<Task> getByEvent(int eventId) {
        return execute(LOG, "завдання події", () -> taskDao.getByEventId(eventId));
    }

    /** Активні завдання, відсортовані за спаданням інтегрального показника; виконані — в кінці. */
    public List<Task> getAllByPriority() {
        return execute(LOG, "список завдань за пріоритетом", () -> {
            List<Task> list = taskDao.getAll();
            list.sort(Comparator.comparing(Task::getPriorityIndex,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return list;
        });
    }

    public Optional<Task> getById(int id) {
        return execute(LOG, "отримання завдання", () -> taskDao.getById(id));
    }

    /** Редагує дані завдання. Стан і виконавець не змінюються (для цього є changeStatus/assign). */
    public void update(Task t) {
        executeVoid(LOG, "редагування завдання", () -> {
            Task existing = require(t.getId());
            if (existing.getStatus() == TaskStatus.DONE) {
                throw new IllegalStateException("Виконане завдання редагувати не можна");
            }
            validateData(t);
            taskDao.update(t);
            recalculate(t.getId());
            LOG.info("Відредаговано завдання id=" + t.getId());
            return null;
        });
    }

    public void delete(int id) {
        executeVoid(LOG, "видалення завдання", () -> {
            require(id);
            List<Integer> prerequisites = dependencyDao.getDependsOnIds(id);
            taskDao.delete(id);
            for (int depId : prerequisites) {
                recalculate(depId);
            }
            LOG.info("Видалено завдання id=" + id);
            return null;
        });
    }

    // =================== Залежності ===================

    public void addDependency(int taskId, int dependsOnId) {
        executeVoid(LOG, "додавання залежності", () -> {
            Task task = require(taskId);
            Task prerequisite = require(dependsOnId);
            if (taskId == dependsOnId) {
                throw new ValidationException("Завдання не може залежати саме від себе");
            }
            if (task.getEventId() != prerequisite.getEventId()) {
                throw new ValidationException("Залежності допускаються лише між завданнями однієї події");
            }
            if (dependencyDao.exists(taskId, dependsOnId)) {
                throw new ValidationException("Така залежність уже існує");
            }
            if (reaches(dependsOnId, taskId)) {
                throw new ValidationException("Залежність створює цикл між завданнями");
            }
            dependencyDao.add(taskId, dependsOnId);
            recalculate(dependsOnId);
            LOG.info("Додано залежність: завдання id=" + taskId + " залежить від id=" + dependsOnId);
            return null;
        });
    }

    public void removeDependency(int taskId, int dependsOnId) {
        executeVoid(LOG, "видалення залежності", () -> {
            if (!dependencyDao.delete(taskId, dependsOnId)) {
                throw new ValidationException("Такої залежності не існує");
            }
            recalculate(dependsOnId);
            LOG.info("Видалено залежність: завдання id=" + taskId + " від id=" + dependsOnId);
            return null;
        });
    }

    public List<Integer> getDependsOn(int taskId) {
        return execute(LOG, "залежності завдання", () -> dependencyDao.getDependsOnIds(taskId));
    }

    // =================== Ресурси ===================

    public void addResource(int taskId, int resourceId, int quantity) {
        executeVoid(LOG, "прив'язка ресурсу", () -> {
            require(taskId);
            Resource resource = resourceDao.getById(resourceId)
                    .orElseThrow(() -> new ValidationException("Ресурсу з id=" + resourceId + " не існує"));
            if (quantity <= 0) {
                throw new ValidationException("Потрібна кількість ресурсу має бути більшою за 0");
            }
            if (quantity > resource.getTotalQuantity()) {
                throw new ValidationException("Потрібна кількість (" + quantity + ") перевищує наявну ("
                        + resource.getTotalQuantity() + ")");
            }
            if (taskResourceDao.getById(taskId, resourceId).isPresent()) {
                throw new ValidationException("Ресурс уже прив'язано до цього завдання");
            }
            taskResourceDao.add(new TaskResource(taskId, resourceId, quantity));
            LOG.info("Завдання id=" + taskId + ": прив'язано ресурс id=" + resourceId + " × " + quantity);
            return null;
        });
    }

    public void removeResource(int taskId, int resourceId) {
        executeVoid(LOG, "відв'язування ресурсу", () -> {
            if (!taskResourceDao.delete(taskId, resourceId)) {
                throw new ValidationException("Такої прив'язки ресурсу не існує");
            }
            LOG.info("Завдання id=" + taskId + ": відв'язано ресурс id=" + resourceId);
            return null;
        });
    }

    public List<TaskResource> getResources(int taskId) {
        return execute(LOG, "ресурси завдання", () -> taskResourceDao.getByTaskId(taskId));
    }

    // =================== Інтегральний показник ===================

    /** Перераховує та зберігає показник I завдання; для виконаних завдань показник очищується (null). */
    public Double recalculatePriority(int taskId) {
        return execute(LOG, "перерахунок показника", () -> recalculate(taskId));
    }

    /** Перераховує показник усіх активних завдань (час спливає, тому F1 змінюється). */
    public int refreshAllPriorities() {
        return execute(LOG, "перерахунок усіх показників", () -> {
            int n = 0;
            for (Task t : taskDao.getAll()) {
                if (t.getStatus() != TaskStatus.DONE) {
                    recalculate(t.getId());
                    n++;
                }
            }
            LOG.info("Перераховано інтегральний показник для " + n + " завдань");
            return n;
        });
    }

    // =================== Внутрішні методи ===================

    private Double recalculate(int taskId) throws SQLException {
        Task task = require(taskId);
        Double value = null;
        if (task.getStatus() != TaskStatus.DONE) {
            value = computeIndex(task, dependencyDao.countDependents(taskId));
        }
        taskDao.updatePriorityIndex(taskId, value);
        return value;
    }

    private double computeIndex(Task t, int dependents) {
        double f1 = PriorityCalculator.factor1(t.getDeadline(), LocalDateTime.now(clock));
        double f2 = PriorityCalculator.factor2(t.getCriticality());
        double f3 = PriorityCalculator.factor3(dependents);
        return PriorityCalculator.calculate(f1, f2, f3);
    }

    private Task require(int id) throws SQLException {
        return taskDao.getById(id)
                .orElseThrow(() -> new ValidationException("Завдання з id=" + id + " не знайдено"));
    }

    private void validateData(Task t) throws SQLException {
        if (t == null) {
            throw new ValidationException("Завдання не задане");
        }
        t.setTitle(requireText(t.getTitle(), "Назва завдання", 200));
        t.setDescription(optionalText(t.getDescription(), "Опис", 5000));
        if (t.getDeadline() == null) {
            throw new ValidationException("Дедлайн є обов'язковим");
        }
        if (t.getCriticality() < PriorityCalculator.MIN_CRITICALITY
                || t.getCriticality() > PriorityCalculator.MAX_CRITICALITY) {
            throw new ValidationException("Критичність має бути від 1 до 5");
        }
        if (eventDao.getById(t.getEventId()).isEmpty()) {
            throw new ValidationException("Події з id=" + t.getEventId() + " не існує");
        }
    }

    /** Чи досяжне завдання target, рухаючись за залежностями від from (пошук вшир). */
    private boolean reaches(int from, int target) throws SQLException {
        Set<Integer> seen = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(from);
        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (current == target) {
                return true;
            }
            if (seen.add(current)) {
                queue.addAll(dependencyDao.getDependsOnIds(current));
            }
        }
        return false;
    }
}
