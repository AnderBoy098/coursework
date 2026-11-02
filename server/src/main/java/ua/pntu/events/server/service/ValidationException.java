package ua.pntu.events.server.service;

/** Помилка валідації даних або порушення предметних правил (некоректні значення, відсутня пов'язана сутність). */
public class ValidationException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
