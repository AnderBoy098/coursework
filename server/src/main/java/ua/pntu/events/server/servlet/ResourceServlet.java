package ua.pntu.events.server.servlet;

import ua.pntu.events.server.model.Resource;
import ua.pntu.events.server.service.ResourceService;
import ua.pntu.events.server.service.ValidationException;

import javax.servlet.http.HttpServletRequest;

/** URL: /resources. GET — список; POST action = add | update | delete. */
public class ResourceServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient ResourceService service = new ResourceService();

    @Override
    protected Object handleGet(HttpServletRequest req) {
        return service.getAll();
    }

    @Override
    protected Object handlePost(HttpServletRequest req, String action) {
        switch (action) {
            case "add":
                service.add(read(req, 0));
                break;
            case "update":
                service.update(read(req, requireInt(req, "id")));
                break;
            case "delete":
                service.delete(requireInt(req, "id"));
                break;
            default:
                throw new ValidationException("Невідома операція: " + action);
        }
        return service.getAll();
    }

    private Resource read(HttpServletRequest req, int id) {
        return new Resource(id, text(req, "name"), text(req, "resourceType"),
                requireInt(req, "totalQuantity"), text(req, "unit"));
    }
}
