package ua.pntu.events.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ua.pntu.events.client.api.HttpEventManagerClient;
import ua.pntu.events.client.ui.MainTabs;

/**
 * JavaFX-клієнт системи організації подій і контролю завдань.
 * Маршрут: JavaFX → HttpEventManagerClient → HTTP/JSON → Servlet → Service → DAO → JDBC → MySQL.
 * Адресу сервера можна змінити параметром -Dserver.url=http://host:port/event-manager-server.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        HttpEventManagerClient api = new HttpEventManagerClient();

        Label title = new Label("Інформаційна система організації подій і контролю завдань");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        title.setPadding(new Insets(10));
        Label status = new Label("Сервер: " + api.getBase());
        status.setPadding(new Insets(6, 10, 6, 10));

        MainTabs tabs = new MainTabs(api, status::setText);
        TabPane pane = tabs.pane();

        BorderPane root = new BorderPane();
        root.setTop(title);
        root.setCenter(pane);
        root.setBottom(status);

        stage.setTitle("Event Manager — клієнт");
        stage.setScene(new Scene(root, 1180, 780));
        stage.show();
        Platform.runLater(tabs::refreshFirst);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
