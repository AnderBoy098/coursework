package ua.pntu.events.client.model;

/**
 * Прив'язка ресурсу до завдання.
 * Містить лише дані: без DAO, JDBC та прямого підключення до MySQL.
 */
public class TaskResourceLink {

    private int taskId;
    private int resourceId;
    private int quantityNeeded;

    public TaskResourceLink() {
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }
    public int getQuantityNeeded() { return quantityNeeded; }
    public void setQuantityNeeded(int quantityNeeded) { this.quantityNeeded = quantityNeeded; }

    @Override
    public String toString() {
        return "TaskResourceLink{taskId=" + taskId + ", resourceId=" + resourceId + ", quantityNeeded=" + quantityNeeded + "}";
    }
}
