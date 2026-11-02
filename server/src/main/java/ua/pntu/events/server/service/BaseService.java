package ua.pntu.events.server.service;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Спільна поведінка Service-класів: єдине місце логування відхилених операцій і помилок БД.
 * Помилки не приховуються — вони логуються й повторно передаються викликачу.
 */
abstract class BaseService {

    @FunctionalInterface
    protected interface DbAction<T> {
        T run() throws SQLException;
    }

    /** Виконує операцію: ValidationException/IllegalStateException → WARNING, SQLException → SEVERE. */
    protected <T> T execute(Logger log, String operation, DbAction<T> action) {
        try {
            return action.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.log(Level.WARNING, "Операцію відхилено [" + operation + "]: " + e.getMessage());
            throw e;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Помилка БД [" + operation + "]: " + e.getMessage(), e);
            throw new ServiceException("Помилка доступу до бази даних під час операції: " + operation, e);
        }
    }

    protected void executeVoid(Logger log, String operation, DbAction<Void> action) {
        execute(log, operation, action);
    }

    // ---------- допоміжні перевірки ----------

    protected static String requireText(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Поле «" + field + "» є обов'язковим");
        }
        String v = value.trim();
        if (v.length() > maxLength) {
            throw new ValidationException("Поле «" + field + "» не може бути довшим за " + maxLength + " символів");
        }
        return v;
    }

    protected static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();
        if (v.length() > maxLength) {
            throw new ValidationException("Поле «" + field + "» не може бути довшим за " + maxLength + " символів");
        }
        return v;
    }
}
