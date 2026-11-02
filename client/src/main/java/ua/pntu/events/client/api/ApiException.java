package ua.pntu.events.client.api;

/** Помилка звернення до сервера: повідомлення з поля "error" відповіді або опис проблеми зв'язку. */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /** HTTP-код відповіді; 0 — відповіді не було (сервер недоступний). */
    public int getStatus() {
        return status;
    }
}
