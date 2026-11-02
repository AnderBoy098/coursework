package ua.pntu.events.server.servlet;

import ua.pntu.events.server.model.Person;
import ua.pntu.events.server.service.PersonService;
import ua.pntu.events.server.service.ValidationException;

import javax.servlet.http.HttpServletRequest;

/** URL: /persons. GET — список; POST action = add | update | delete. */
public class PersonServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient PersonService service = new PersonService();

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

    private Person read(HttpServletRequest req, int id) {
        return new Person(id, text(req, "fullName"), text(req, "email"), text(req, "phone"), text(req, "jobTitle"));
    }
}
