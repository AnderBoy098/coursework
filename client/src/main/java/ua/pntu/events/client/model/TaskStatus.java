package ua.pntu.events.client.model;

/**
 * Стани завдання. Значення збігаються зі станами на сервері, бо передаються через JSON.
 * Правила переходів тут НЕ дублюються: їх перевіряє TaskService на сервері.
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

    @Override
    public String toString() {
        return label + " (" + name() + ")";
    }
}
