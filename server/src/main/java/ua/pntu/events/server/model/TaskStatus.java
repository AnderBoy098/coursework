package ua.pntu.events.server.model;

/**
 * Стани життєвого циклу завдання:
 * СТВОРЕНО → ПРИЗНАЧЕНО → ВИКОНУЄТЬСЯ → ВИКОНАНО.
 * Допустимість переходів перевіряється на Service-рівні (TaskService.isAllowedTransition).
 */
public enum TaskStatus {
    CREATED("СТВОРЕНО"),
    ASSIGNED("ПРИЗНАЧЕНО"),
    IN_PROGRESS("ВИКОНУЄТЬСЯ"),
    DONE("ВИКОНАНО");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
