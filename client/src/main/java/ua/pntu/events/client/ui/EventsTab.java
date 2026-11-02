package ua.pntu.events.client.ui;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import ua.pntu.events.client.api.HttpEventManagerClient;
import ua.pntu.events.client.model.Event;
import ua.pntu.events.client.model.Person;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Вкладка «Події». */
class EventsTab extends CrudTab<Event> {

    private final TextField title = new TextField();
    private final TextField description = new TextField();
    private final TextField location = new TextField();
    private final TextField start = new TextField();
    private final TextField end = new TextField();
    private final ComboBox<Person> organizer = new ComboBox<>();
    private final Map<Integer, String> personNames = new HashMap<>();

    EventsTab(HttpEventManagerClient api, Consumer<String> status) {
        super("Події", api, status);
        init();
    }

    @Override
    protected void beforeLoad() {
        List<Person> persons = api.getPersons();
        personNames.clear();
        for (Person p : persons) {
            personNames.put(p.getId(), p.getFullName());
        }
        organizer.getItems().setAll(persons);
    }

    @Override
    protected List<TableColumn<Event, ?>> columns() {
        return List.of(
                Ui.column("ID", 50, (Event e) -> Ui.text(e.getId())),
                Ui.column("Назва", 240, (Event e) -> e.getTitle()),
                Ui.column("Місце", 200, (Event e) -> e.getLocation()),
                Ui.column("Початок", 130, (Event e) -> Ui.format(e.getStartDate())),
                Ui.column("Завершення", 130, (Event e) -> Ui.format(e.getEndDate())),
                Ui.column("Організатор", 200, (Event e) -> personNames.getOrDefault(e.getOrganizerId(), "#" + e.getOrganizerId())));
    }

    @Override
    protected Node buildForm() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(6);
        start.setPromptText("2026-11-20 10:00");
        end.setPromptText("2026-11-20 17:00");
        g.addRow(0, new Label("Назва"), title, new Label("Місце"), location);
        g.addRow(1, new Label("Початок"), start, new Label("Завершення"), end);
        g.addRow(2, new Label("Опис"), description, new Label("Організатор"), organizer);
        return g;
    }

    private Event read(int id) {
        Person org = organizer.getValue();
        if (org == null) {
            throw new IllegalArgumentException("Виберіть організатора події");
        }
        Event e = new Event();
        e.setId(id);
        e.setTitle(title.getText());
        e.setDescription(description.getText());
        e.setLocation(location.getText());
        e.setStartDate(Ui.parseDateTime(start.getText(), "Початок"));
        e.setEndDate(Ui.parseDateTime(end.getText(), "Завершення"));
        e.setOrganizerId(org.getId());
        return e;
    }

    @Override
    protected void fillForm(Event e) {
        title.setText(Ui.text(e.getTitle()));
        description.setText(Ui.text(e.getDescription()));
        location.setText(Ui.text(e.getLocation()));
        start.setText(Ui.format(e.getStartDate()));
        end.setText(Ui.format(e.getEndDate()));
        for (Person p : organizer.getItems()) {
            if (p.getId() == e.getOrganizerId()) {
                organizer.setValue(p);
            }
        }
    }

    @Override
    protected void clearForm() {
        title.clear();
        description.clear();
        location.clear();
        start.clear();
        end.clear();
        organizer.setValue(null);
    }

    @Override
    protected List<Event> load() {
        return api.getEvents();
    }

    @Override
    protected List<Event> doAdd() {
        return api.addEvent(read(0));
    }

    @Override
    protected List<Event> doUpdate(Event selected) {
        return api.updateEvent(read(selected.getId()));
    }

    @Override
    protected List<Event> doDelete(Event selected) {
        return api.deleteEvent(selected.getId());
    }
}
