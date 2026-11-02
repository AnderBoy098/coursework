package ua.pntu.events.client.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import ua.pntu.events.client.api.HttpEventManagerClient;
import ua.pntu.events.client.csv.CsvImporter;
import ua.pntu.events.client.csv.SamplePaths;
import ua.pntu.events.client.model.Resource;

import java.util.List;
import java.util.function.Consumer;

/** Вкладка «Ресурси» (з початком CSV-імпорту ресурсів). */
class ResourcesTab extends CrudTab<Resource> {

    private final TextField name = new TextField();
    private final TextField type = new TextField();
    private final TextField quantity = new TextField();
    private final TextField unit = new TextField();
    private final Button importButton = new Button("Імпортувати CSV");

    ResourcesTab(HttpEventManagerClient api, Consumer<String> status) {
        super("Ресурси", api, status);
        init();
        importButton.setOnAction(e -> Ui.runImport(importButton,
                () -> new CsvImporter(api).importResources(SamplePaths.resolve("resources.csv")),
                this::refresh, this::report));
        buttons.getChildren().add(importButton);
    }

    @Override
    protected List<TableColumn<Resource, ?>> columns() {
        return List.of(
                Ui.column("ID", 50, (Resource r) -> Ui.text(r.getId())),
                Ui.column("Назва", 260, (Resource r) -> r.getName()),
                Ui.column("Тип", 150, (Resource r) -> r.getResourceType()),
                Ui.column("Кількість", 100, (Resource r) -> Ui.text(r.getTotalQuantity())),
                Ui.column("Одиниця", 100, (Resource r) -> r.getUnit()));
    }

    @Override
    protected Node buildForm() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(6);
        type.setPromptText("Обладнання / Приміщення / …");
        unit.setPromptText("шт.");
        g.addRow(0, new Label("Назва"), name, new Label("Тип"), type);
        g.addRow(1, new Label("Кількість"), quantity, new Label("Одиниця"), unit);
        return g;
    }

    private Resource read(int id) {
        Resource r = new Resource();
        r.setId(id);
        r.setName(name.getText());
        r.setResourceType(type.getText());
        r.setTotalQuantity(Ui.parseInt(quantity.getText(), "Кількість"));
        r.setUnit(unit.getText());
        return r;
    }

    @Override
    protected void fillForm(Resource r) {
        name.setText(Ui.text(r.getName()));
        type.setText(Ui.text(r.getResourceType()));
        quantity.setText(Ui.text(r.getTotalQuantity()));
        unit.setText(Ui.text(r.getUnit()));
    }

    @Override
    protected void clearForm() {
        name.clear();
        type.clear();
        quantity.clear();
        unit.clear();
    }

    @Override
    protected List<Resource> load() {
        return api.getResources();
    }

    @Override
    protected List<Resource> doAdd() {
        return api.addResource(read(0));
    }

    @Override
    protected List<Resource> doUpdate(Resource selected) {
        return api.updateResource(read(selected.getId()));
    }

    @Override
    protected List<Resource> doDelete(Resource selected) {
        return api.deleteResource(selected.getId());
    }
}
