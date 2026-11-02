package ua.pntu.events.server.model;

/** Зв'язок «завдання — ресурс» (таблиця task_resources). */
public class TaskResource {

    private int taskId;          // PK, FK -> tasks.id
    private int resourceId;      // PK, FK -> resources.id
    private int quantityNeeded;  // потрібна кількість

    public TaskResource() {
    }

    public TaskResource(int taskId, int resourceId, int quantityNeeded) {
        this.taskId = taskId;
        this.resourceId = resourceId;
        this.quantityNeeded = quantityNeeded;
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public int getQuantityNeeded() { return quantityNeeded; }
    public void setQuantityNeeded(int quantityNeeded) { this.quantityNeeded = quantityNeeded; }

    @Override
    public String toString() {
        return "TaskResource{taskId=" + taskId + ", resourceId=" + resourceId + ", quantityNeeded=" + quantityNeeded + "}";
    }
}
