package ua.pntu.events.client.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import ua.pntu.events.client.api.HttpEventManagerClient;
import ua.pntu.events.client.model.Person;

import java.util.List;
import java.util.function.Consumer;

/** Вкладка «Відповідальні особи». */
class PersonsTab extends CrudTab<Person> {

    private final TextField fullName = new TextField();
    private final TextField email = new TextField();
    private final TextField phone = new TextField();
    private final TextField jobTitle = new TextField();

    PersonsTab(HttpEventManagerClient api, Consumer<String> status) {
        super("Відповідальні особи", api, status);
        init();
    }

    @Override
    protected List<TableColumn<Person, ?>> columns() {
        return List.of(
                Ui.column("ID", 50, (Person p) -> Ui.text(p.getId())),
                Ui.column("ПІБ", 240, (Person p) -> p.getFullName()),
                Ui.column("Електронна пошта", 220, (Person p) -> p.getEmail()),
                Ui.column("Телефон", 140, (Person p) -> p.getPhone()),
                Ui.column("Посада", 200, (Person p) -> p.getJobTitle()));
    }

    @Override
    protected Node buildForm() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(6);
        fullName.setPromptText("Прізвище Ім'я По батькові");
        email.setPromptText("name@example.com");
        phone.setPromptText("+380...");
        g.addRow(0, new Label("ПІБ"), fullName, new Label("Пошта"), email);
        g.addRow(1, new Label("Телефон"), phone, new Label("Посада"), jobTitle);
        return g;
    }

    private Person read(int id) {
        Person p = new Person();
        p.setId(id);
        p.setFullName(fullName.getText());
        p.setEmail(email.getText());
        p.setPhone(phone.getText());
        p.setJobTitle(jobTitle.getText());
        return p;
    }

    @Override
    protected void fillForm(Person p) {
        fullName.setText(Ui.text(p.getFullName()));
        email.setText(Ui.text(p.getEmail()));
        phone.setText(Ui.text(p.getPhone()));
        jobTitle.setText(Ui.text(p.getJobTitle()));
    }

    @Override
    protected void clearForm() {
        fullName.clear();
        email.clear();
        phone.clear();
        jobTitle.clear();
    }

    @Override
    protected List<Person> load() {
        return api.getPersons();
    }

    @Override
    protected List<Person> doAdd() {
        return api.addPerson(read(0));
    }

    @Override
    protected List<Person> doUpdate(Person selected) {
        return api.updatePerson(read(selected.getId()));
    }

    @Override
    protected List<Person> doDelete(Person selected) {
        return api.deletePerson(selected.getId());
    }
}
