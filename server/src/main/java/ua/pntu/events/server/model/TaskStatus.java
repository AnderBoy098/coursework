package ua.pntu.events.server.model;

import java.util.Optional;

/**
 * Стани життєвого циклу завдання:
 * СТВОРЕНО → ПРИЗНАЧЕНО → ВИКОНУЄТЬСЯ → ВИКОНАНО.
 * Допустимі лише послідовні переходи вперед.
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

    /** Наступний стан; для кінцевого стану DONE — порожньо. */
    public Optional<TaskStatus> next() {
        TaskStatus[] all = values();
        return ordinal() + 1 < all.length ? Optional.of(all[ordinal() + 1]) : Optional.empty();
    }

    /** Чи допустимий перехід з поточного стану до target. */
    public boolean canTransitionTo(TaskStatus target) {
        return next().filter(n -> n == target).isPresent();
    }
}
