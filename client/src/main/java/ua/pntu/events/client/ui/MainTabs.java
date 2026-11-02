package ua.pntu.events.client.ui;

import javafx.scene.control.TabPane;
import ua.pntu.events.client.api.HttpEventManagerClient;

import java.util.function.Consumer;

/** Збирає вкладки головного вікна. Окремий публічний клас потрібен, бо вкладки приховані в пакеті ui. */
public class MainTabs {

    private final TabPane pane;
    private final TasksTab tasks;

    public MainTabs(HttpEventManagerClient api, Consumer<String> status) {
        tasks = new TasksTab(api, status);
        pane = new TabPane(tasks, new EventsTab(api, status), new PersonsTab(api, status), new ResourcesTab(api, status));
    }

    public TabPane pane() {
        return pane;
    }

    /** Завантажує дані першої вкладки після відображення вікна. */
    public void refreshFirst() {
        tasks.refresh();
    }
}
