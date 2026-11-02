package ua.pntu.events.server.servlet;

import ua.pntu.events.server.model.Event;
import ua.pntu.events.server.service.EventService;
import ua.pntu.events.server.service.ValidationException;

import javax.servlet.http.HttpServletRequest;

/** URL: /events. GET — список; POST action = add | update | delete. */
public class EventServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient EventService service = new EventService();

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

    private Event read(HttpServletRequest req, int id) {
        return new Event(id, text(req, "title"), text(req, "description"), text(req, "location"),
                requireDateTime(req, "startDate"), requireDateTime(req, "endDate"), requireInt(req, "organizerId"));
    }
}
