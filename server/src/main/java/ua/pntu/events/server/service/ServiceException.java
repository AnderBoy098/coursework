package ua.pntu.events.server.service;

/** Технічна помилка доступу до бази даних, обгорнута на Service-рівні. */
public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
