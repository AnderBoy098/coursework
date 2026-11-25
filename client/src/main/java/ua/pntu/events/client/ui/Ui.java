package ua.pntu.events.client.ui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import ua.pntu.events.client.csv.CsvImporter;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/** Допоміжні методи JavaFX-інтерфейсу: повідомлення, колонки таблиць, формати, запуск імпорту. */
final class Ui {

    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Ui() {
    }

    static void error(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setTitle("Помилка");
        a.setHeaderText(null);
        a.showAndWait();
    }

    static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    static boolean confirm(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Підтвердження");
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /** Колонка TableView, що показує текстове значення, обчислене з рядка таблиці. */
    static <T> TableColumn<T, String> column(String title, double width, Function<T, String> value) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(cd -> new ReadOnlyStringWrapper(value.apply(cd.getValue())));
        return col;
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static String format(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    /** Дата й час із текстового поля у форматі «рррр-мм-дд гг:хх». */
    static LocalDateTime parseDateTime(String value, String field) {
        try {
            return LocalDateTime.parse(value.trim().replace(' ', 'T'));
        } catch (DateTimeException | NullPointerException e) {
            throw new IllegalArgumentException("Поле «" + field + "» має формат рррр-мм-дд гг:хх");
        }
    }

    static int parseInt(String value, String field) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Поле «" + field + "» має бути цілим числом");
        }
    }

    /** Підсумок імпорту: інформаційне вікно, а якщо імпорт перервано — вікно помилки. */
    static void showImportSummary(CsvImporter.ImportResult r) {
        StringBuilder sb = new StringBuilder(r.summary());
        List<String> msgs = r.messages();
        if (!msgs.isEmpty()) {
            sb.append("\n\nПерші повідомлення:\n");
            for (int i = 0; i < Math.min(10, msgs.size()); i++) {
                sb.append("• ").append(msgs.get(i)).append('\n');
            }
            if (msgs.size() > 10) {
                sb.append("… ще ").append(msgs.size() - 10);
            }
        }
        if (r.aborted()) {
            error(sb.toString());
        } else {
            info("Результат імпорту CSV", sb.toString());
        }
    }

    /** Безпечний виклик у потоці JavaFX. */
    static void later(Runnable r) {
        Platform.runLater(r);
    }
}
