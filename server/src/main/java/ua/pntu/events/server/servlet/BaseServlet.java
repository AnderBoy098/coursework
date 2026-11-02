package ua.pntu.events.server.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ua.pntu.events.server.service.ValidationException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Спільна частина Servlet-ів: читання параметрів, формування JSON-відповідей і помилок.
 * Servlet не містить SQL і предметної логіки — лише передає виконання відповідному Service.
 *
 * Протокол: GET повертає список записів; POST виконує операцію, задану параметром action,
 * і повертає актуальний JSON. Параметри передаються як звичайна HTTP-форма (не JSON-тіло).
 * Помилка: HTTP 400 (некоректні дані / порушено правила) або 500, тіло {"error": "..."}.
 */
abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(BaseServlet.class.getName());

    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @FunctionalInterface
    protected interface Action {
        Object run() throws Exception;
    }

    /** Результат GET-запиту (як правило, список записів). */
    protected abstract Object handleGet(HttpServletRequest req) throws Exception;

    /** Результат POST-запиту для операції action. */
    protected abstract Object handlePost(HttpServletRequest req, String action) throws Exception;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        respond(resp, () -> handleGet(req));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8"); // до першого getParameter()
        respond(resp, () -> handlePost(req, requireText(req, "action")));
    }

    private void respond(HttpServletResponse resp, Action action) throws IOException {
        try {
            writeJson(resp, HttpServletResponse.SC_OK, action.run());
        } catch (IllegalArgumentException | IllegalStateException | DateTimeException e) {
            // некоректні дані, відсутній пов'язаний запис, заборонений перехід життєвого циклу
            String msg = e.getMessage() == null ? "Некоректний запит" : e.getMessage();
            LOG.log(Level.WARNING, "Некоректний запит: " + msg);
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", msg));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Внутрішня помилка під час обробки запиту", e);
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Map.of("error", "Внутрішня помилка сервера"));
        }
    }

    private void writeJson(HttpServletResponse resp, int status, Object data) throws IOException {
        String json = MAPPER.writeValueAsString(data); // спочатку серіалізуємо, потім пишемо відповідь
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
    }

    // ---------- допоміжні методи читання параметрів ----------

    protected static String text(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? null : v.trim();
    }

    protected static String requireText(HttpServletRequest req, String name) {
        String v = text(req, name);
        if (v == null || v.isEmpty()) {
            throw new ValidationException("Не задано параметр «" + name + "»");
        }
        return v;
    }

    protected static int requireInt(HttpServletRequest req, String name) {
        String v = requireText(req, name);
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new ValidationException("Параметр «" + name + "» має бути цілим числом");
        }
    }

    protected static Integer optionalInt(HttpServletRequest req, String name) {
        String v = text(req, name);
        return v == null || v.isEmpty() ? null : requireInt(req, name);
    }

    /** Дата й час у форматі «рррр-мм-ддTгг:хх» або «рррр-мм-дд гг:хх». */
    protected static LocalDateTime requireDateTime(HttpServletRequest req, String name) {
        String v = requireText(req, name).replace(' ', 'T');
        try {
            return LocalDateTime.parse(v);
        } catch (DateTimeException e) {
            throw new ValidationException("Параметр «" + name + "» має формат рррр-мм-дд гг:хх");
        }
    }
}
