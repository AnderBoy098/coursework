package ua.pntu.events.server.model;

import java.time.LocalDateTime;

/**
 * Завдання — об'єкт життєвого циклу.
 * Відповідає таблиці БД, див. database.sql.
 */
public class Task {

    private int id; // Ідентифікатор (PK)
    private int eventId; // Подія (FK -> events.id)
    private String title; // Назва завдання
    private String description; // Опис
    private LocalDateTime deadline; // Дедлайн
    private int criticality; // Критичність 1..5
    private TaskStatus status; // Стан життєвого циклу
    private Integer assigneeId; // Відповідальна особа (FK -> persons.id), може бути null
    private Double priorityIndex; // Інтегральний показник I (0..100), може бути null
    private LocalDateTime createdAt; // Час створення

    public Task() {
    }

    public Task(int id, int eventId, String title, String description, LocalDateTime deadline, int criticality, TaskStatus status, Integer assigneeId, Double priorityIndex, LocalDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.criticality = criticality;
        this.status = status;
        this.assigneeId = assigneeId;
        this.priorityIndex = priorityIndex;
        this.createdAt = createdAt;
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

    @Override
    public String toString() {
        return "Task{id=" + id + ", eventId=" + eventId + ", title=" + title + ", description=" + description + "}";
    }
}
