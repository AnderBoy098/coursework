package ua.pntu.events.client.model;

import java.time.LocalDateTime;

/**
 * Завдання.
 * Містить лише дані: без DAO, JDBC та прямого підключення до MySQL.
 */
public class Task {

    private int id;
    private int eventId;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private int criticality;
    private TaskStatus status;
    private Integer assigneeId;
    private Double priorityIndex;
    private LocalDateTime createdAt;

    public Task() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public int getCriticality() { return criticality; }
    public void setCriticality(int criticality) { this.criticality = criticality; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Integer getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Integer assigneeId) { this.assigneeId = assigneeId; }
    public Double getPriorityIndex() { return priorityIndex; }
    public void setPriorityIndex(Double priorityIndex) { this.priorityIndex = priorityIndex; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Текст для ComboBox та повідомлень. */
    @Override
    public String toString() {
        return "#" + id + " " + title;
    }
}
