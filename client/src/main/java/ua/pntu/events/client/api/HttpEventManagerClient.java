package ua.pntu.events.client.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ua.pntu.events.client.model.Event;
import ua.pntu.events.client.model.Person;
import ua.pntu.events.client.model.Resource;
import ua.pntu.events.client.model.Task;
import ua.pntu.events.client.model.TaskLinks;
import ua.pntu.events.client.model.TaskStatus;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Єдиний клас, через який JavaFX спілкується із сервером.
 * GET повертає JSON-список; POST передає параметри форми (action=add|update|delete|…).
 * JSON перетворюється на клієнтські Java-об'єкти за допомогою ObjectMapper.
 * Клас не містить SQL і не працює з MySQL.
 */
public class HttpEventManagerClient {

    private static final Logger LOG = Logger.getLogger(HttpEventManagerClient.class.getName());
    public static final String DEFAULT_BASE = "http://localhost:8080/event-manager-server";

    private final String base;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Адреса сервера береться з -Dserver.url=..., інакше — стандартна адреса Tomcat. */
    public HttpEventManagerClient() {
        this(System.getProperty("server.url", DEFAULT_BASE));
    }

    public HttpEventManagerClient(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public String getBase() {
        return base;
    }

    // ===================== Відповідальні особи =====================

    public List<Person> getPersons() {
        return readList(get("/persons"), Person.class);
    }

    public List<Person> addPerson(Person p) {
        return readList(post("/persons", form("add").put("fullName", p.getFullName()).put("email", p.getEmail())
                .put("phone", p.getPhone()).put("jobTitle", p.getJobTitle())), Person.class);
    }

    public List<Person> updatePerson(Person p) {
        return readList(post("/persons", form("update").put("id", p.getId()).put("fullName", p.getFullName())
                .put("email", p.getEmail()).put("phone", p.getPhone()).put("jobTitle", p.getJobTitle())), Person.class);
    }

    public List<Person> deletePerson(int id) {
        return readList(post("/persons", form("delete").put("id", id)), Person.class);
    }

    // ===================== Події =====================

    public List<Event> getEvents() {
        return readList(get("/events"), Event.class);
    }

    public List<Event> addEvent(Event e) {
        return readList(post("/events", eventForm("add", e)), Event.class);
    }

    public List<Event> updateEvent(Event e) {
        return readList(post("/events", eventForm("update", e).put("id", e.getId())), Event.class);
    }

    public List<Event> deleteEvent(int id) {
        return readList(post("/events", form("delete").put("id", id)), Event.class);
    }

    private Form eventForm(String action, Event e) {
        return form(action).put("title", e.getTitle()).put("description", e.getDescription())
                .put("location", e.getLocation()).put("startDate", e.getStartDate())
                .put("endDate", e.getEndDate()).put("organizerId", e.getOrganizerId());
    }

    // ===================== Ресурси =====================

    public List<Resource> getResources() {
        return readList(get("/resources"), Resource.class);
    }

    public List<Resource> addResource(Resource r) {
        return readList(post("/resources", resourceForm("add", r)), Resource.class);
    }

    public List<Resource> updateResource(Resource r) {
        return readList(post("/resources", resourceForm("update", r).put("id", r.getId())), Resource.class);
    }

    public List<Resource> deleteResource(int id) {
        return readList(post("/resources", form("delete").put("id", id)), Resource.class);
    }

    private Form resourceForm(String action, Resource r) {
        return form(action).put("name", r.getName()).put("resourceType", r.getResourceType())
                .put("totalQuantity", r.getTotalQuantity()).put("unit", r.getUnit());
    }

    // ===================== Завдання =====================

    /** Список завдань за спаданням інтегрального показника I (обчислює сервер). */
    public List<Task> getTasks() {
        return readList(get("/tasks"), Task.class);
    }

    /** Створює завдання; якщо assigneeId задано — одразу призначає виконавця. */
    public List<Task> addTask(Task t, Integer assigneeId) {
        return readList(post("/tasks", taskForm("add", t).put("assigneeId", assigneeId)), Task.class);
    }

    public List<Task> updateTask(Task t) {
        return readList(post("/tasks", taskForm("update", t).put("id", t.getId())), Task.class);
    }

    public List<Task> deleteTask(int id) {
        return readList(post("/tasks", form("delete").put("id", id)), Task.class);
    }

    public List<Task> assignTask(int id, int personId) {
        return readList(post("/tasks", form("assign").put("id", id).put("personId", personId)), Task.class);
    }

    /** Запит на зміну стану; допустимість переходу перевіряє сервер (TaskService), а не клієнт. */
    public List<Task> changeTaskStatus(int id, TaskStatus state) {
        return readList(post("/tasks", form("transition").put("id", id).put("state", state.name())), Task.class);
    }

    /** Просить сервер перерахувати показник I усіх активних завдань і повертає список. */
    public List<Task> refreshPriorities() {
        return readList(post("/tasks", form("refresh")), Task.class);
    }

    private Form taskForm(String action, Task t) {
        return form(action).put("eventId", t.getEventId()).put("title", t.getTitle())
                .put("description", t.getDescription()).put("deadline", t.getDeadline())
                .put("criticality", t.getCriticality());
    }

    // ===================== Залежності та ресурси завдання =====================

    public TaskLinks getTaskLinks(int taskId) {
        return read(get("/task-links?id=" + taskId), TaskLinks.class);
    }

    public TaskLinks addDependency(int taskId, int dependsOnId) {
        return read(post("/task-links", form("addDependency").put("id", taskId).put("dependsOnId", dependsOnId)), TaskLinks.class);
    }

    public TaskLinks removeDependency(int taskId, int dependsOnId) {
        return read(post("/task-links", form("removeDependency").put("id", taskId).put("dependsOnId", dependsOnId)), TaskLinks.class);
    }

    public TaskLinks addTaskResource(int taskId, int resourceId, int quantity) {
        return read(post("/task-links", form("addResource").put("id", taskId)
                .put("resourceId", resourceId).put("quantity", quantity)), TaskLinks.class);
    }

    public TaskLinks removeTaskResource(int taskId, int resourceId) {
        return read(post("/task-links", form("removeResource").put("id", taskId).put("resourceId", resourceId)), TaskLinks.class);
    }

    // ===================== HTTP =====================

    private String get(String path) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(15)).GET().build();
        return send(req);
    }

    private String post(String path, Form form) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(form.encode(), StandardCharsets.UTF_8))
                .build();
        return send(req);
    }

    private String send(HttpRequest req) {
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                String msg = errorText(resp.body(), resp.statusCode());
                LOG.warning("Сервер повернув " + resp.statusCode() + " на " + req.method() + " " + req.uri().getPath() + ": " + msg);
                throw new ApiException(resp.statusCode(), msg);
            }
            return resp.body();
        } catch (ConnectException e) {
            throw new ApiException(0, "Сервер недоступний (" + base + "). Перевірте, що Tomcat запущено.", e);
        } catch (IOException e) {
            throw new ApiException(0, "Помилка зв'язку із сервером: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Запит перервано", e);
        }
    }

    private String errorText(String body, int status) {
        try {
            JsonNode node = mapper.readTree(body);
            String msg = node.path("error").asText(null);
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
        } catch (IOException | RuntimeException ignored) {
            // тіло не є JSON — повернемо загальний опис нижче
        }
        return "Помилка сервера (HTTP " + status + ")";
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new ApiException(0, "Некоректна відповідь сервера (JSON)", e);
        }
    }

    private <T> List<T> readList(String json, Class<T> type) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new ApiException(0, "Некоректна відповідь сервера (JSON)", e);
        }
    }

    // ===================== Параметри форми =====================

    private static Form form(String action) {
        return new Form().put("action", action);
    }

    /** Параметри application/x-www-form-urlencoded; значення null пропускаються. */
    private static final class Form {
        private final Map<String, String> params = new LinkedHashMap<>();

        Form put(String key, Object value) {
            if (value != null) {
                params.put(key, String.valueOf(value));
            }
            return this;
        }

        String encode() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                  .append('=')
                  .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
            return sb.toString();
        }
    }
}
