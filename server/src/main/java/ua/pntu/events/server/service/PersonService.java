package ua.pntu.events.server.service;

import ua.pntu.events.server.dao.EventDAO;
import ua.pntu.events.server.dao.PersonDAO;
import ua.pntu.events.server.dao.TaskDAO;
import ua.pntu.events.server.model.Person;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/** Предметна логіка та валідація для сутності «Відповідальна особа». */
public class PersonService extends BaseService {

    private static final Logger LOG = Logger.getLogger(PersonService.class.getName());
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9()\\-\\s]{5,30}$");

    private final PersonDAO personDao;
    private final EventDAO eventDao;
    private final TaskDAO taskDao;

    public PersonService() {
        this(new PersonDAO(), new EventDAO(), new TaskDAO());
    }

    public PersonService(PersonDAO personDao, EventDAO eventDao, TaskDAO taskDao) {
        this.personDao = personDao;
        this.eventDao = eventDao;
        this.taskDao = taskDao;
    }

    public int add(Person p) {
        return execute(LOG, "створення особи", () -> {
            validate(p, null);
            int id = personDao.add(p);
            LOG.info("Створено відповідальну особу id=" + id);
            return id;
        });
    }

    public List<Person> getAll() {
        return execute(LOG, "список осіб", personDao::getAll);
    }

    public Optional<Person> getById(int id) {
        return execute(LOG, "отримання особи", () -> personDao.getById(id));
    }

    public void update(Person p) {
        executeVoid(LOG, "редагування особи", () -> {
            if (personDao.getById(p.getId()).isEmpty()) {
                throw new ValidationException("Особу з id=" + p.getId() + " не знайдено");
            }
            validate(p, p.getId());
            personDao.update(p);
            LOG.info("Відредаговано відповідальну особу id=" + p.getId());
            return null;
        });
    }

    public void delete(int id) {
        executeVoid(LOG, "видалення особи", () -> {
            if (personDao.getById(id).isEmpty()) {
                throw new ValidationException("Особу з id=" + id + " не знайдено");
            }
            if (eventDao.countByOrganizer(id) > 0) {
                throw new ValidationException("Не можна видалити особу, яка є організатором подій");
            }
            if (taskDao.countActiveByAssignee(id) > 0) {
                throw new ValidationException("Не можна видалити особу, якій призначено незавершені завдання");
            }
            personDao.delete(id);
            LOG.info("Видалено відповідальну особу id=" + id);
            return null;
        });
    }

    private void validate(Person p, Integer selfId) throws java.sql.SQLException {
        if (p == null) {
            throw new ValidationException("Особа не задана");
        }
        p.setFullName(requireText(p.getFullName(), "ПІБ", 150));
        p.setEmail(requireText(p.getEmail(), "Електронна пошта", 120));
        if (!EMAIL.matcher(p.getEmail()).matches()) {
            throw new ValidationException("Некоректний формат електронної пошти");
        }
        p.setPhone(optionalText(p.getPhone(), "Телефон", 30));
        if (p.getPhone() != null && !PHONE.matcher(p.getPhone()).matches()) {
            throw new ValidationException("Некоректний формат телефону");
        }
        p.setJobTitle(optionalText(p.getJobTitle(), "Посада", 100));
        Optional<Person> same = personDao.findByEmail(p.getEmail());
        if (same.isPresent() && (selfId == null || same.get().getId() != selfId)) {
            throw new ValidationException("Особа з електронною поштою " + p.getEmail() + " вже існує");
        }
    }
}
