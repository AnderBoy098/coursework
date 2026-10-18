package ua.pntu.events.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Початкова заглушка JavaFX-клієнта (етап 1). Повноцінний інтерфейс буде реалізовано на наступних етапах. */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("Інформаційна система організації подій і контролю завдань");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label stageInfo = new Label("Клієнтська частина: початкова структура проєкту (етап 1)");

        VBox root = new VBox(12, title, stageInfo);
        root.setPadding(new Insets(24));

        stage.setTitle("Event Manager — клієнт");
        stage.setScene(new Scene(root, 640, 200));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
