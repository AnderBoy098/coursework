package ua.pntu.events.server.servlet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.pntu.events.server.service.ValidationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Протокол Servlet-рівня: коди відповідей 200/400/500 та обробка помилкових запитів без Tomcat. */
class BaseServletTest {

    /** Мінімальний Servlet для перевірки спільної поведінки BaseServlet. */
    private static class Probe extends BaseServlet {
        private static final long serialVersionUID = 1L;

        @Override
        protected Object handleGet(HttpServletRequest req) {
            return List.of(1, 2, 3);
        }

        @Override
        protected Object handlePost(HttpServletRequest req, String action) {
            switch (action) {
                case "ok": return Map.of("ok", true);
                case "int": return requireInt(req, "id");
                case "date": return requireDateTime(req, "d").toString();
                case "state": throw new IllegalStateException("Перехід CREATED → DONE заборонений");
                case "boom": throw new RuntimeException("пароль до бази: secret123");
                default: throw new ValidationException("Невідома операція: " + action);
            }
        }

        void post(HttpServletRequest rq, HttpServletResponse rs) throws Exception {
            doPost(rq, rs);
        }

        void get(HttpServletRequest rq, HttpServletResponse rs) throws Exception {
            doGet(rq, rs);
        }
    }

    private static final class Result {
        int status;
        final StringWriter body = new StringWriter();
    }

    private static HttpServletResponse response(Result r) {
        return (HttpServletResponse) Proxy.newProxyInstance(BaseServletTest.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (p, m, a) -> {
                    if (m.getName().equals("setStatus")) r.status = (Integer) a[0];
                    if (m.getName().equals("getWriter")) return new PrintWriter(r.body, true);
                    return null;
                });
    }

    private static HttpServletRequest request(Map<String, String> params) {
        return (HttpServletRequest) Proxy.newProxyInstance(BaseServletTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (p, m, a) -> m.getName().equals("getParameter") ? params.get((String) a[0]) : null);
    }

    private Result post(Map<String, String> params) throws Exception {
        Result r = new Result();
        new Probe().post(request(params), response(r));
        return r;
    }

    @Test
    @DisplayName("GET повертає 200 і JSON-список; коректний POST — 200")
    void successfulRequests() throws Exception {
        Result r = new Result();
        new Probe().get(request(Map.of()), response(r));
        assertEquals(200, r.status);
        assertTrue(r.body.toString().contains("1"));
        assertEquals(200, post(Map.of("action", "ok")).status);
    }

    @Test
    @DisplayName("Відсутній параметр action і невідома операція дають 400")
    void missingOrUnknownActionGives400() throws Exception {
        Result missing = post(Map.of());
        assertEquals(400, missing.status);
        assertTrue(missing.body.toString().contains("action"));
        Result unknown = post(Map.of("action", "zzz"));
        assertEquals(400, unknown.status);
        assertTrue(unknown.body.toString().contains("Невідома операція"));
    }

    @Test
    @DisplayName("Нечислове або відсутнє значення числового параметра дає 400")
    void badIntegerGives400() throws Exception {
        assertEquals(400, post(Map.of("action", "int", "id", "abc")).status);
        assertEquals(400, post(Map.of("action", "int")).status);
        assertEquals(200, post(Map.of("action", "int", "id", "7")).status);
    }

    @Test
    @DisplayName("Дата приймається у форматі з пробілом; неправильний формат дає 400")
    void dateParsing() throws Exception {
        Result ok = post(Map.of("action", "date", "d", "2026-11-20 18:00"));
        assertEquals(200, ok.status);
        assertTrue(ok.body.toString().contains("2026-11-20T18:00"));
        assertEquals(400, post(Map.of("action", "date", "d", "вчора")).status);
    }

    @Test
    @DisplayName("Заборонений перехід життєвого циклу дає 400")
    void forbiddenTransitionGives400() throws Exception {
        Result r = post(Map.of("action", "state"));
        assertEquals(400, r.status);
        assertTrue(r.body.toString().contains("заборонений"));
    }

    @Test
    @DisplayName("Внутрішня помилка дає 500 і не розкриває технічних деталей клієнту")
    void internalErrorGives500WithoutDetails() throws Exception {
        Result r = post(Map.of("action", "boom"));
        assertEquals(500, r.status);
        assertTrue(r.body.toString().contains("Внутрішня помилка сервера"));
        assertFalse(r.body.toString().contains("secret123"));
    }
}
