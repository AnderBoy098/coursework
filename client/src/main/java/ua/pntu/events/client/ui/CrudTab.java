package ua.pntu.events.client.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ua.pntu.events.client.api.HttpEventManagerClient;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Вкладка з TableView, формою та кнопками «Додати / Змінити / Видалити / Оновити».
 * Усі дані вкладка отримує тільки через HttpEventManagerClient: SQL і MySQL у клієнті немає.
 * Кожна операція повертає актуальний список із сервера, який одразу показується в таблиці.
 */
abstract class CrudTab<T> extends Tab {

    protected final HttpEventManagerClient api;
    protected final TableView<T> table = new TableView<>();
    private final Consumer<String> status;
    protected final HBox buttons = new HBox(8);

    protected CrudTab(String title, HttpEventManagerClient api, Consumer<String> status) {
        super(title);
        this.api = api;
        this.status = status;
        setClosable(false);
    }

    // ---------- що реалізує конкретна вкладка ----------
    protected abstract List<TableColumn<T, ?>> columns();
    protected abstract Node buildForm();
    protected abstract void fillForm(T selected);
    protected abstract void clearForm();
    protected abstract List<T> load();
    protected abstract List<T> doAdd();
    protected abstract List<T> doUpdate(T selected);
    protected abstract List<T> doDelete(T selected);

    /** Викликається перед завантаженням таблиці: підкласи оновлюють допоміжні списки (ComboBox тощо). */
    protected void beforeLoad() {
    }

    /** Викликається після оновлення таблиці. */
    protected void afterLoad() {
    }

    /** Викликається підкласом наприкінці власного конструктора, коли всі поля вже створені. */
    protected final void init() {
        table.getColumns().addAll(columns());

        Button add = new Button("Додати");
        Button update = new Button("Змінити");
        Button delete = new Button("Видалити");
        Button refresh = new Button("Оновити");
        add.setOnAction(e -> apply("Запис додано", this::doAdd));
        update.setOnAction(e -> withSelection(sel -> apply("Запис змінено", () -> doUpdate(sel))));
        delete.setOnAction(e -> withSelection(sel -> {
            if (Ui.confirm("Видалити вибраний запис?")) {
                apply("Запис видалено", () -> doDelete(sel));
            }
        }));
        refresh.setOnAction(e -> refresh());
        buttons.getChildren().addAll(add, update, delete, refresh);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            if (now != null) {
                fillForm(now);
            }
        });
        setOnSelectionChanged(e -> {
            if (isSelected()) {
                refresh();
            }
        });

        VBox bottom = new VBox(10, buildForm(), buttons);
        bottom.setPadding(new Insets(10));
        BorderPane root = new BorderPane();
        root.setCenter(table);
        root.setBottom(bottom);
        setContent(root);
    }

    /** Повторно завантажує дані з сервера. */
    public void refresh() {
        try {
            beforeLoad();
            show(load());
            status.accept("Дані оновлено");
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    protected final void apply(String okMessage, Supplier<List<T>> action) {
        try {
            beforeLoad();
            show(action.get());
            clearForm();
            status.accept(okMessage);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    protected final void withSelection(Consumer<T> action) {
        T sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Ui.error("Спочатку виберіть запис у таблиці");
            return;
        }
        action.accept(sel);
    }

    protected final void report(String message) {
        status.accept(message);
    }

    protected final void fail(RuntimeException e) {
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        status.accept("Помилка: " + msg);
        Ui.error(msg);
    }

    private void show(List<T> items) {
        table.setItems(FXCollections.observableArrayList(items));
        afterLoad();
    }
}
