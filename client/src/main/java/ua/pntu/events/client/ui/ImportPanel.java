package ua.pntu.events.client.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import ua.pntu.events.client.csv.CsvImporter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Панель фонового CSV-імпорту: кнопка запуску, ProgressBar і текстове повідомлення про стан.
 * Імпорт виконує javafx.concurrent.Task, тому інтерфейс не блокується. Одночасний повторний запуск
 * неможливий: прапорець RUNNING спільний для всіх панелей (завдання, ресурси).
 */
class ImportPanel extends HBox {

    /** Дія імпорту; отримує слухача ходу виконання. */
    @FunctionalInterface
    interface ImportJob {
        CsvImporter.ImportResult run(CsvImporter.ProgressListener progress) throws Exception;
    }

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private final Button button;
    private final ProgressBar bar = new ProgressBar(0);
    private final Label message = new Label("Імпорт не виконувався");
    private final ImportJob job;
    private final Runnable onFinished;
    private final Consumer<String> status;

    ImportPanel(String buttonText, ImportJob job, Runnable onFinished, Consumer<String> status) {
        super(10);
        this.job = job;
        this.onFinished = onFinished;
        this.status = status;
        this.button = new Button(buttonText);
        bar.setPrefWidth(220);
        getChildren().addAll(button, bar, message);
        button.setOnAction(e -> start());
    }

    private void start() {
        if (!RUNNING.compareAndSet(false, true)) {
            Ui.info("Імпорт CSV", "Імпорт уже виконується. Дочекайтеся його завершення.");
            return;
        }
        button.setDisable(true);
        status.accept("Імпорт CSV виконується…");

        javafx.concurrent.Task<CsvImporter.ImportResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected CsvImporter.ImportResult call() throws Exception {
                updateMessage("Читання файла…");
                updateProgress(0, 1);
                return job.run((done, total, text) -> {
                    updateProgress(done, Math.max(total, 1));
                    updateMessage(text);
                });
            }
        };
        bar.progressProperty().bind(task.progressProperty());
        message.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(e -> finish(task.getValue(), null));
        task.setOnFailed(e -> finish(null, task.getException()));

        Thread thread = new Thread(task, "csv-import");
        thread.setDaemon(true);
        thread.start();
    }

    /** Викликається в потоці JavaFX після завершення Task. */
    private void finish(CsvImporter.ImportResult result, Throwable error) {
        bar.progressProperty().unbind();
        message.textProperty().unbind();
        try {
            if (result != null) {
                bar.setProgress(result.aborted() ? bar.getProgress() : 1.0);
                message.setText(result.summary());
                status.accept(result.summary());
                Ui.showImportSummary(result);
            } else {
                String msg = error == null || error.getMessage() == null ? "Невідома помилка імпорту" : error.getMessage();
                bar.setProgress(0);
                message.setText("Помилка: " + msg);
                status.accept("Помилка імпорту: " + msg);
                Ui.error(msg);
            }
        } finally {
            RUNNING.set(false);
            button.setDisable(false);
            onFinished.run();                // оновлення таблиці після імпорту
        }
    }
}
