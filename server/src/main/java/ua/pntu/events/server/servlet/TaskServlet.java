package ua.pntu.events.server.servlet;

import ua.pntu.events.server.model.Task;
import ua.pntu.events.server.model.TaskStatus;
import ua.pntu.events.server.service.TaskService;
import ua.pntu.events.server.service.ValidationException;

import javax.servlet.http.HttpServletRequest;

/**
 * URL: /tasks.
 * GET  — список завдань за спаданням показника I (необов'язковий параметр eventId — лише завдання події).
 * POST — action = add | update | delete | assign | transition | refresh.
 * Правила життєвого циклу та розрахунок показника I виконує TaskService, а не Servlet.
 */
public class TaskServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient TaskService service = new TaskService();

    @Override
    protected Object handleGet(HttpServletRequest req) {
        Integer eventId = optionalInt(req, "eventId");
        return eventId == null ? service.getAllByPriority() : service.getByEvent(eventId);
    }

    @Override
    protected Object handlePost(HttpServletRequest req, String action) {
        switch (action) {
            case "add": {
                Task t = read(req, 0);
                Integer assigneeId = optionalInt(req, "assigneeId");
                if (assigneeId == null) {
                    service.add(t);
                } else {
                    service.addWithAssignee(t, assigneeId);
                }
                break;
            }
            case "update":
                service.update(read(req, requireInt(req, "id")));
                break;
            case "delete":
                service.delete(requireInt(req, "id"));
                break;
            case "assign":
                service.assign(requireInt(req, "id"), requireInt(req, "personId"));
                break;
            case "transition":
                service.changeStatus(requireInt(req, "id"), parseStatus(requireText(req, "state")));
                break;
            case "refresh":
                service.refreshAllPriorities();
                break;
            default:
                throw new ValidationException("Невідома операція: " + action);
        }
        return service.getAllByPriority();
    }

    private Task read(HttpServletRequest req, int id) {
        Task t = new Task();
        t.setId(id);
        t.setEventId(requireInt(req, "eventId"));
        t.setTitle(text(req, "title"));
        t.setDescription(text(req, "description"));
        t.setDeadline(requireDateTime(req, "deadline"));
        t.setCriticality(requireInt(req, "criticality"));
        return t;
    }

    private TaskStatus parseStatus(String value) {
        try {
            return TaskStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Невідомий стан завдання: " + value);
        }
    }
}
