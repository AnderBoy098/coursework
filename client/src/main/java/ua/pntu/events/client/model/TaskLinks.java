package ua.pntu.events.client.model;

import java.util.ArrayList;
import java.util.List;

/** Залежності та ресурси завдання (відповідь сервера на /task-links). */
public class TaskLinks {

    private List<Integer> dependsOn = new ArrayList<>();
    private List<TaskResourceLink> resources = new ArrayList<>();

    public TaskLinks() {
    }

    public List<Integer> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<Integer> dependsOn) { this.dependsOn = dependsOn; }

    public List<TaskResourceLink> getResources() { return resources; }
    public void setResources(List<TaskResourceLink> resources) { this.resources = resources; }
}
