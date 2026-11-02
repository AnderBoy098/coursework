package ua.pntu.events.server.service;

import ua.pntu.events.server.dao.EventDAO;
import ua.pntu.events.server.dao.PersonDAO;
import ua.pntu.events.server.model.Event;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/** Предметна логіка та валідація для сутності «Подія». */
public class EventService extends BaseService {

    private static final Logger LOG = Logger.getLogger(EventService.class.getName());

    private final EventDAO eventDao;
    private final PersonDAO personDao;

    public EventService() {
        this(new EventDAO(), new PersonDAO());
    }

    public EventService(EventDAO eventDao, PersonDAO personDao) {
        this.eventDao = eventDao;
        this.personDao = personDao;
    }

    public int add(Event e) {
        return execute(LOG, "створення події", () -> {
            validate(e);
            int id = eventDao.add(e);
            LOG.info("Створено подію id=" + id + ", назва=\"" + e.getTitle() + "\"");
            return id;
        });
    }

    public List<Event> getAll() {
        return execute(LOG, "список подій", eventDao::getAll);
    }

    public Optional<Event> getById(int id) {
        return execute(LOG, "отримання події", () -> eventDao.getById(id));
    }

    public void update(Event e) {
        executeVoid(LOG, "редагування події", () -> {
            if (eventDao.getById(e.getId()).isEmpty()) {
                throw new ValidationException("Подію з id=" + e.getId() + " не знайдено");
            }
            validate(e);
            eventDao.update(e);
            LOG.info("Відредаговано подію id=" + e.getId());
            return null;
        });
    }

    /** Видалення події каскадно видаляє її завдання (ON DELETE CASCADE). */
    public void delete(int id) {
        executeVoid(LOG, "видалення події", () -> {
            if (eventDao.getById(id).isEmpty()) {
                throw new ValidationException("Подію з id=" + id + " не знайдено");
            }
            eventDao.delete(id);
            LOG.info("Видалено подію id=" + id + " разом із її завданнями");
            return null;
        });
    }

    private void validate(Event e) throws java.sql.SQLException {
        if (e == null) {
            throw new ValidationException("Подія не задана");
        }
        e.setTitle(requireText(e.getTitle(), "Назва події", 200));
        e.setDescription(optionalText(e.getDescription(), "Опис", 5000));
        e.setLocation(optionalText(e.getLocation(), "Місце проведення", 200));
        if (e.getStartDate() == null || e.getEndDate() == null) {
            throw new ValidationException("Дати початку і завершення події є обов'язковими");
        }
        if (e.getEndDate().isBefore(e.getStartDate())) {
            throw new ValidationException("Дата завершення не може бути раніше дати початку");
        }
        if (personDao.getById(e.getOrganizerId()).isEmpty()) {
            throw new ValidationException("Організатора з id=" + e.getOrganizerId() + " не існує");
        }
    }
}
