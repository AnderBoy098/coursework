package ua.pntu.events.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.pntu.events.server.model.Resource;
import ua.pntu.events.server.model.Task;
import ua.pntu.events.server.model.TaskStatus;
import ua.pntu.events.server.support.Fakes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Предметна логіка завдань: валідація, життєвий цикл, залежності, ресурси, показник I. */
class TaskServiceTest {

    private Fakes f;
    private int person;
    private int event;

    @BeforeEach
    void setUp() {
        f = new Fakes();
        person = f.newPerson("worker@example.com");
        event = f.newEvent(person);
    }

    private Task stored(int id) {
        return f.tasks.get(id);
    }

    private void finish(int id) {
        f.taskService.assign(id, person);
        f.taskService.changeStatus(id, TaskStatus.IN_PROGRESS);
        f.taskService.changeStatus(id, TaskStatus.DONE);
    }

    // ---------- створення та валідація ----------

    @Test
    @DisplayName("Нове завдання створюється у стані СТВОРЕНО з показником I (F1=80, F2=80, F3=0 → 60)")
    void addCreatesTaskInCreatedState() {
        int id = f.newTask(event, "Підготувати зал", 144, 4);
        assertEquals(TaskStatus.CREATED, stored(id).getStatus());
        assertEquals(60.0, stored(id).getPriorityIndex(), 1e-9);
        assertNull(stored(id).getAssigneeId());
    }

    @Test
    @DisplayName("Критичність поза діапазоном 1–5 відхиляється")
    void rejectsCriticalityOutOfRange() {
        assertThrows(ValidationException.class, () -> f.taskService.add(f.task(event, "А", 10, 0)));
        assertThrows(ValidationException.class, () -> f.taskService.add(f.task(event, "Б", 10, 6)));
        assertTrue(f.tasks.isEmpty());
    }

    @Test
    @DisplayName("Порожня назва, відсутній дедлайн і неіснуюча подія відхиляються")
    void rejectsMissingDataAndUnknownEvent() {
        assertThrows(ValidationException.class, () -> f.taskService.add(f.task(event, "  ", 10, 3)));
        Task t = f.task(event, "Без дедлайну", 10, 3);
        t.setDeadline(null);
        assertThrows(ValidationException.class, () -> f.taskService.add(t));
        assertThrows(ValidationException.class, () -> f.taskService.add(f.task(999, "Завдання", 10, 3)));
    }

    @Test
    @DisplayName("Дублікат назви в межах події відхиляється, в іншій події — дозволений")
    void duplicateTitleRules() {
        f.newTask(event, "Розіслати запрошення", 48, 3);
        assertThrows(ValidationException.class, () -> f.taskService.add(f.task(event, "РОЗІСЛАТИ ЗАПРОШЕННЯ", 48, 3)));
        f.newTask(f.newEvent(person), "Розіслати запрошення", 48, 3);
        assertEquals(2, f.tasks.size());
    }

    @Test
    @DisplayName("Виконавця під час створення задають лише через assign() або addWithAssignee()")
    void assigneeAtCreation() {
        Task t = f.task(event, "Завдання", 10, 3);
        t.setAssigneeId(person);
        assertThrows(ValidationException.class, () -> f.taskService.add(t));
        int id = f.taskService.addWithAssignee(f.task(event, "З виконавцем", 10, 3), person);
        assertEquals(TaskStatus.ASSIGNED, stored(id).getStatus());
    }

    @Test
    @DisplayName("Створення з неіснуючим виконавцем не залишає напівстворених записів")
    void addWithUnknownAssigneeCreatesNothing() {
        assertThrows(ValidationException.class, () -> f.taskService.addWithAssignee(f.task(event, "Завдання", 10, 3), 999));
        assertTrue(f.tasks.isEmpty());
    }

    // ---------- життєвий цикл ----------

    @Test
    @DisplayName("assign() переводить СТВОРЕНО → ПРИЗНАЧЕНО; неіснуюча особа відхиляється")
    void assignRules() {
        int id = f.newTask(event, "Завдання", 48, 3);
        assertThrows(ValidationException.class, () -> f.taskService.assign(id, 999));
        assertEquals(TaskStatus.CREATED, stored(id).getStatus());
        f.taskService.assign(id, person);
        assertEquals(TaskStatus.ASSIGNED, stored(id).getStatus());
        assertEquals(Integer.valueOf(person), stored(id).getAssigneeId());
    }

    @Test
    @DisplayName("Заборонені переходи відхиляються та не змінюють стан")
    void forbiddenTransitionsRejected() {
        int id = f.newTask(event, "Завдання", 48, 3);
        assertThrows(IllegalStateException.class, () -> f.taskService.changeStatus(id, TaskStatus.DONE));
        assertThrows(IllegalStateException.class, () -> f.taskService.changeStatus(id, TaskStatus.IN_PROGRESS));
        assertThrows(IllegalStateException.class, () -> f.taskService.changeStatus(id, TaskStatus.ASSIGNED));
        assertEquals(TaskStatus.CREATED, stored(id).getStatus());
        f.taskService.assign(id, person);
        assertThrows(IllegalStateException.class, () -> f.taskService.changeStatus(id, TaskStatus.DONE));
        assertEquals(TaskStatus.ASSIGNED, stored(id).getStatus());
    }

    @Test
    @DisplayName("Повний цикл: показник очищується для ВИКОНАНО; повернення й повторне призначення заборонені")
    void fullLifecycle() {
        int id = f.newTask(event, "Завдання", 48, 3);
        finish(id);
        assertEquals(TaskStatus.DONE, stored(id).getStatus());
        assertNull(stored(id).getPriorityIndex());
        assertThrows(IllegalStateException.class, () -> f.taskService.changeStatus(id, TaskStatus.IN_PROGRESS));
        assertThrows(IllegalStateException.class, () -> f.taskService.assign(id, person));
    }

    @Test
    @DisplayName("Старт заблоковано, доки не виконано завдання-передумову")
    void startBlockedByUnfinishedDependency() {
        int a = f.newTask(event, "Передумова", 48, 3);
        int b = f.newTask(event, "Залежне", 48, 3);
        f.taskService.addDependency(b, a);
        f.taskService.assign(b, person);
        assertThrows(IllegalStateException.class, () -> f.taskService.changeStatus(b, TaskStatus.IN_PROGRESS));
        finish(a);
        f.taskService.changeStatus(b, TaskStatus.IN_PROGRESS);
        assertEquals(TaskStatus.IN_PROGRESS, stored(b).getStatus());
    }

    @Test
    @DisplayName("Виконане завдання редагувати не можна")
    void updateDoneRejected() {
        int id = f.newTask(event, "Завдання", 48, 3);
        finish(id);
        Task upd = f.task(event, "Нова назва", 10, 2);
        upd.setId(id);
        assertThrows(IllegalStateException.class, () -> f.taskService.update(upd));
    }

    @Test
    @DisplayName("Редагування змінює дані, але не стан; показник перераховується (60 → 67)")
    void updateKeepsStatusAndRecalculates() {
        int id = f.newTask(event, "Завдання", 144, 4);
        f.taskService.assign(id, person);
        Task upd = f.task(event, "Змінена назва", 144, 5);
        upd.setId(id);
        f.taskService.update(upd);
        assertEquals(TaskStatus.ASSIGNED, stored(id).getStatus());
        assertEquals("Змінена назва", stored(id).getTitle());
        assertEquals(67.0, stored(id).getPriorityIndex(), 1e-9);
    }

    // ---------- залежності ----------

    @Test
    @DisplayName("Залежність підвищує показник I передумови (F3): 60 → 65; видалення залежності повертає 60")
    void dependencyChangesPrerequisiteIndex() {
        int a = f.newTask(event, "Передумова", 144, 4);
        int b = f.newTask(event, "Залежне", 144, 4);
        f.taskService.addDependency(b, a);
        assertEquals(65.0, stored(a).getPriorityIndex(), 1e-9);
        f.taskService.removeDependency(b, a);
        assertEquals(60.0, stored(a).getPriorityIndex(), 1e-9);
    }

    @Test
    @DisplayName("Самозалежність, дублікат, цикл і залежність між різними подіями відхиляються")
    void invalidDependenciesRejected() {
        int a = f.newTask(event, "А", 48, 3);
        int b = f.newTask(event, "Б", 48, 3);
        int c = f.newTask(event, "В", 48, 3);
        int other = f.newTask(f.newEvent(person), "Інша подія", 48, 3);
        assertThrows(ValidationException.class, () -> f.taskService.addDependency(a, a));
        f.taskService.addDependency(b, a);
        assertThrows(ValidationException.class, () -> f.taskService.addDependency(b, a));
        f.taskService.addDependency(c, b);
        assertThrows(ValidationException.class, () -> f.taskService.addDependency(a, c));
        assertThrows(ValidationException.class, () -> f.taskService.addDependency(other, a));
    }

    @Test
    @DisplayName("Видалення залежного завдання повертає показник передумови")
    void deleteRecalculatesPrerequisite() {
        int a = f.newTask(event, "Передумова", 144, 4);
        int b = f.newTask(event, "Залежне", 144, 4);
        f.taskService.addDependency(b, a);
        f.taskService.delete(b);
        assertEquals(60.0, stored(a).getPriorityIndex(), 1e-9);
    }

    // ---------- ресурси ----------

    @Test
    @DisplayName("Ресурс прив'язується в межах наявної кількості; некоректні прив'язки відхиляються")
    void resourceQuantityRules() {
        int rid = f.resourceService.add(new Resource(0, "Проєктор", "Обладнання", 3, "шт."));
        int id = f.newTask(event, "Завдання", 48, 3);
        int id2 = f.newTask(event, "Завдання 2", 48, 3);
        f.taskService.addResource(id, rid, 2);
        assertEquals(1, f.taskService.getResources(id).size());
        assertThrows(ValidationException.class, () -> f.taskService.addResource(id, rid, 1));
        assertThrows(ValidationException.class, () -> f.taskService.addResource(id2, rid, 5));
        assertThrows(ValidationException.class, () -> f.taskService.addResource(id2, rid, 0));
        assertThrows(ValidationException.class, () -> f.taskService.addResource(id2, 777, 1));
    }

    // ---------- показник I та впорядкування ----------

    @Test
    @DisplayName("refreshAllPriorities перераховує лише активні завдання")
    void refreshCountsOnlyActive() {
        f.newTask(event, "Активне", 48, 3);
        finish(f.newTask(event, "Виконане", 48, 3));
        assertEquals(1, f.taskService.refreshAllPriorities());
    }

    @Test
    @DisplayName("Список за пріоритетом: спадання I, виконані — в кінці")
    void sortedByPriorityDescendingDoneLast() {
        int low = f.newTask(event, "Низький", 700, 1);
        int high = f.newTask(event, "Високий", 10, 5);
        int done = f.newTask(event, "Виконане", 10, 5);
        finish(done);
        List<Task> sorted = f.taskService.getAllByPriority();
        assertEquals(high, sorted.get(0).getId());
        assertEquals(low, sorted.get(1).getId());
        assertEquals(done, sorted.get(2).getId());
        assertFalse(sorted.get(0).getPriorityIndex() < sorted.get(1).getPriorityIndex());
    }
}
