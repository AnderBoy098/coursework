package ua.pntu.events.client.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ua.pntu.events.client.api.HttpEventManagerClient;
import ua.pntu.events.client.csv.CsvImporter;
import ua.pntu.events.client.csv.SamplePaths;
import ua.pntu.events.client.model.Event;
import ua.pntu.events.client.model.Person;
import ua.pntu.events.client.model.Resource;
import ua.pntu.events.client.model.Task;
import ua.pntu.events.client.model.TaskLinks;
import ua.pntu.events.client.model.TaskResourceLink;
import ua.pntu.events.client.model.TaskStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Вкладка «Завдання»: CRUD, призначення виконавця, зміна стану життєвого циклу,
 * залежності та ресурси, показник I. Правила життєвого циклу й розрахунок I виконує сервер;
 * клієнт лише надсилає вибраний стан і відображає результат (або повідомлення про відмову).
 */
class TasksTab extends CrudTab<Task> {

    private final ComboBox<Event> eventBox = new ComboBox<>();
    private final TextField title = new TextField();
    private final TextField description = new TextField();
    private final TextField deadline = new TextField();
    private final ComboBox<Integer> criticality = new ComboBox<>();

    private final ComboBox<Person> assigneeBox = new ComboBox<>();
    private final ComboBox<TaskStatus> statusBox = new ComboBox<>();

    private final TextArea linksArea = new TextArea();
    private final TextField dependsOnId = new TextField();
    private final ComboBox<Resource> resourceBox = new ComboBox<>();
    private final TextField resourceQty = new TextField();

    private final Button importButton = new Button("Імпортувати CSV");
    private final Map<Integer, String> personNames = new HashMap<>();
    private final Map<Integer, String> eventTitles = new HashMap<>();
    private final Map<Integer, String> resourceNames = new HashMap<>();

    TasksTab(HttpEventManagerClient api, Consumer<String> status) {
        super("Завдання", api, status);
        init();
        importButton.setOnAction(e -> Ui.runImport(importButton,
                () -> new CsvImporter(api).importTasks(SamplePaths.resolve("tasks.csv")),
                this::refresh, this::report));
        buttons.getChildren().add(importButton);
    }

    // ===================== довідники для ComboBox =====================

    @Override
    protected void beforeLoad() {
        List<Person> persons = api.getPersons();
        List<Event> events = api.getEvents();
        List<Resource> resources = api.getResources();
        personNames.clear();
        eventTitles.clear();
        resourceNames.clear();
        persons.forEach(p -> personNames.put(p.getId(), p.getFullName()));
        events.forEach(e -> eventTitles.put(e.getId(), e.getTitle()));
        resources.forEach(r -> resourceNames.put(r.getId(), r.getName()));
        assigneeBox.getItems().setAll(persons);
        eventBox.getItems().setAll(events);
        resourceBox.getItems().setAll(resources);
    }

    // ===================== таблиця =====================

    @Override
    protected List<TableColumn<Task, ?>> columns() {
        return List.of(
                Ui.column("ID", 45, (Task t) -> Ui.text(t.getId())),
                Ui.column("Подія", 170, (Task t) -> eventTitles.getOrDefault(t.getEventId(), "#" + t.getEventId())),
                Ui.column("Назва", 220, (Task t) -> t.getTitle()),
                Ui.column("Дедлайн", 120, (Task t) -> Ui.format(t.getDeadline())),
                Ui.column("Критичність", 85, (Task t) -> Ui.text(t.getCriticality())),
                Ui.column("Стан", 120, (Task t) -> t.getStatus() == null ? "" : t.getStatus().getLabel()),
                Ui.column("Виконавець", 170, (Task t) -> t.getAssigneeId() == null ? "—"
                        : personNames.getOrDefault(t.getAssigneeId(), "#" + t.getAssigneeId())),
                Ui.column("Показник I", 80, (Task t) -> t.getPriorityIndex() == null ? "—"
                        : String.format("%.2f", t.getPriorityIndex())));
    }

    // ===================== форма =====================

    @Override
    protected Node buildForm() {
        criticality.getItems().setAll(1, 2, 3, 4, 5);
        criticality.setValue(3);
        statusBox.getItems().setAll(TaskStatus.values());
        deadline.setPromptText("2026-11-20 18:00");
        dependsOnId.setPromptText("ID передумови");
        dependsOnId.setPrefWidth(110);
        resourceQty.setPromptText("к-сть");
        resourceQty.setPrefWidth(70);
        linksArea.setEditable(false);
        linksArea.setPrefRowCount(3);
        linksArea.setPromptText("Виберіть завдання, щоб побачити його залежності й ресурси");

        GridPane data = new GridPane();
        data.setHgap(8);
        data.setVgap(6);
        data.addRow(0, new Label("Подія"), eventBox, new Label("Назва"), title);
        data.addRow(1, new Label("Дедлайн"), deadline, new Label("Критичність (1–5)"), criticality);
        data.addRow(2, new Label("Опис"), description);

        Button assign = new Button("Призначити виконавця");
        assign.setOnAction(e -> withSelection(sel -> {
            Person p = assigneeBox.getValue();
            if (p == null) {
                Ui.error("Виберіть відповідальну особу");
                return;
            }
            apply("Виконавця призначено", () -> api.assignTask(sel.getId(), p.getId()));
        }));
        Button changeState = new Button("Змінити стан");
        changeState.setOnAction(e -> withSelection(sel -> {
            TaskStatus target = statusBox.getValue();
            if (target == null) {
                Ui.error("Виберіть новий стан");
                return;
            }
            apply("Стан змінено на " + target.getLabel(), () -> api.changeTaskStatus(sel.getId(), target));
        }));
        HBox lifecycle = new HBox(8, new Label("Виконавець:"), assigneeBox, assign,
                new Label("Стан:"), statusBox, changeState);

        Button addDep = new Button("Додати залежність");
        addDep.setOnAction(e -> withSelection(sel -> linkAction(sel,
                () -> api.addDependency(sel.getId(), Ui.parseInt(dependsOnId.getText(), "ID передумови")),
                "Залежність додано")));
        Button removeDep = new Button("Видалити залежність");
        removeDep.setOnAction(e -> withSelection(sel -> linkAction(sel,
                () -> api.removeDependency(sel.getId(), Ui.parseInt(dependsOnId.getText(), "ID передумови")),
                "Залежність видалено")));
        Button addRes = new Button("Додати ресурс");
        addRes.setOnAction(e -> withSelection(sel -> linkAction(sel, () -> {
            Resource r = resourceBox.getValue();
            if (r == null) {
                throw new IllegalArgumentException("Виберіть ресурс");
            }
            return api.addTaskResource(sel.getId(), r.getId(), Ui.parseInt(resourceQty.getText(), "Кількість"));
        }, "Ресурс прив'язано")));
        Button removeRes = new Button("Відв'язати ресурс");
        removeRes.setOnAction(e -> withSelection(sel -> linkAction(sel, () -> {
            Resource r = resourceBox.getValue();
            if (r == null) {
                throw new IllegalArgumentException("Виберіть ресурс");
            }
            return api.removeTaskResource(sel.getId(), r.getId());
        }, "Ресурс відв'язано")));
        HBox deps = new HBox(8, new Label("Залежить від:"), dependsOnId, addDep, removeDep);
        HBox res = new HBox(8, new Label("Ресурс:"), resourceBox, resourceQty, addRes, removeRes);

        return new VBox(8, data, lifecycle, deps, res, linksArea);
    }

    private Task read(int id) {
        Event ev = eventBox.getValue();
        if (ev == null) {
            throw new IllegalArgumentException("Виберіть подію");
        }
        Integer crit = criticality.getValue();
        Task t = new Task();
        t.setId(id);
        t.setEventId(ev.getId());
        t.setTitle(title.getText());
        t.setDescription(description.getText());
        t.setDeadline(Ui.parseDateTime(deadline.getText(), "Дедлайн"));
        t.setCriticality(crit == null ? 0 : crit);
        return t;
    }

    @Override
    protected void fillForm(Task t) {
        title.setText(Ui.text(t.getTitle()));
        description.setText(Ui.text(t.getDescription()));
        deadline.setText(Ui.format(t.getDeadline()));
        criticality.setValue(t.getCriticality());
        for (Event ev : eventBox.getItems()) {
            if (ev.getId() == t.getEventId()) {
                eventBox.setValue(ev);
            }
        }
        for (Person p : assigneeBox.getItems()) {
            if (t.getAssigneeId() != null && p.getId() == t.getAssigneeId()) {
                assigneeBox.setValue(p);
            }
        }
        statusBox.setValue(t.getStatus());
        try {
            showLinks(api.getTaskLinks(t.getId()));
        } catch (RuntimeException e) {
            linksArea.setText("Не вдалося завантажити зв'язки: " + e.getMessage());
        }
    }

    @Override
    protected void clearForm() {
        title.clear();
        description.clear();
        deadline.clear();
        criticality.setValue(3);
        eventBox.setValue(null);
        linksArea.clear();
    }

    // ===================== зв'язки завдання =====================

    private void linkAction(Task selected, Supplier<TaskLinks> action, String okMessage) {
        try {
            TaskLinks links = action.get();
            report(okMessage);
            refresh();                       // показник I інших завдань міг змінитися
            reselect(selected.getId());
            showLinks(links);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    private void reselect(int taskId) {
        for (Task t : table.getItems()) {
            if (t.getId() == taskId) {
                table.getSelectionModel().select(t);
                return;
            }
        }
    }

    private void showLinks(TaskLinks links) {
        StringBuilder sb = new StringBuilder("Залежить від завдань: ");
        sb.append(links.getDependsOn().isEmpty() ? "—" : links.getDependsOn().toString()).append('\n');
        sb.append("Ресурси: ");
        if (links.getResources().isEmpty()) {
            sb.append('—');
        }
        for (TaskResourceLink l : links.getResources()) {
            sb.append("\n • ").append(resourceNames.getOrDefault(l.getResourceId(), "#" + l.getResourceId()))
              .append(" × ").append(l.getQuantityNeeded());
        }
        linksArea.setText(sb.toString());
    }

    // ===================== обмін із сервером =====================

    /** Перед показом просимо сервер перерахувати показник I (час спливає, F1 змінюється). */
    @Override
    protected List<Task> load() {
        return api.refreshPriorities();
    }

    @Override
    protected List<Task> doAdd() {
        return api.addTask(read(0), null);
    }

    @Override
    protected List<Task> doUpdate(Task selected) {
        return api.updateTask(read(selected.getId()));
    }

    @Override
    protected List<Task> doDelete(Task selected) {
        return api.deleteTask(selected.getId());
    }
}
