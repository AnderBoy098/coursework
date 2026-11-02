package ua.pntu.events.server.servlet;

import ua.pntu.events.server.service.TaskService;
import ua.pntu.events.server.service.ValidationException;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * URL: /task-links — залежності та ресурси конкретного завдання.
 * GET  id — {"dependsOn": [id, ...], "resources": [{taskId, resourceId, quantityNeeded}, ...]}.
 * POST action = addDependency | removeDependency (id, dependsOnId)
 *             | addResource (id, resourceId, quantity) | removeResource (id, resourceId).
 */
public class TaskLinksServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient TaskService service = new TaskService();

    @Override
    protected Object handleGet(HttpServletRequest req) {
        return links(requireInt(req, "id"));
    }

    @Override
    protected Object handlePost(HttpServletRequest req, String action) {
        int id = requireInt(req, "id");
        switch (action) {
            case "addDependency":
                service.addDependency(id, requireInt(req, "dependsOnId"));
                break;
            case "removeDependency":
                service.removeDependency(id, requireInt(req, "dependsOnId"));
                break;
            case "addResource":
                service.addResource(id, requireInt(req, "resourceId"), requireInt(req, "quantity"));
                break;
            case "removeResource":
                service.removeResource(id, requireInt(req, "resourceId"));
                break;
            default:
                throw new ValidationException("Невідома операція: " + action);
        }
        return links(id);
    }

    private Map<String, Object> links(int taskId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dependsOn", service.getDependsOn(taskId));
        result.put("resources", service.getResources(taskId));
        return result;
    }
}
