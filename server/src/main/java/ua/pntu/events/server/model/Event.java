package ua.pntu.events.server.model;

import java.time.LocalDateTime;

/**
 * Подія.
 * Відповідає таблиці БД, див. database.sql.
 */
public class Event {

    private int id; // Ідентифікатор (PK)
    private String title; // Назва події
    private String description; // Опис
    private String location; // Місце проведення
    private LocalDateTime startDate; // Дата й час початку
    private LocalDateTime endDate; // Дата й час завершення
    private int organizerId; // Організатор (FK -> persons.id)

    public Event() {
    }

    public Event(int id, String title, String description, String location, LocalDateTime startDate, LocalDateTime endDate, int organizerId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.organizerId = organizerId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public int getOrganizerId() { return organizerId; }
    public void setOrganizerId(int organizerId) { this.organizerId = organizerId; }

    @Override
    public String toString() {
        return "Event{id=" + id + ", title=" + title + ", description=" + description + ", location=" + location + "}";
    }
}
