package ua.pntu.events.server.service;

import ua.pntu.events.server.dao.ResourceDAO;
import ua.pntu.events.server.model.Resource;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/** Предметна логіка та валідація для сутності «Ресурс». */
public class ResourceService extends BaseService {

    private static final Logger LOG = Logger.getLogger(ResourceService.class.getName());

    private final ResourceDAO resourceDao;

    public ResourceService() {
        this(new ResourceDAO());
    }

    public ResourceService(ResourceDAO resourceDao) {
        this.resourceDao = resourceDao;
    }

    public int add(Resource r) {
        return execute(LOG, "створення ресурсу", () -> {
            validate(r);
            int id = resourceDao.add(r);
            LOG.info("Створено ресурс id=" + id + ", назва=\"" + r.getName() + "\"");
            return id;
        });
    }

    public List<Resource> getAll() {
        return execute(LOG, "список ресурсів", resourceDao::getAll);
    }

    public Optional<Resource> getById(int id) {
        return execute(LOG, "отримання ресурсу", () -> resourceDao.getById(id));
    }

    public void update(Resource r) {
        executeVoid(LOG, "редагування ресурсу", () -> {
            if (resourceDao.getById(r.getId()).isEmpty()) {
                throw new ValidationException("Ресурс з id=" + r.getId() + " не знайдено");
            }
            validate(r);
            resourceDao.update(r);
            LOG.info("Відредаговано ресурс id=" + r.getId());
            return null;
        });
    }

    /** Видалення ресурсу каскадно прибирає його прив'язки до завдань. */
    public void delete(int id) {
        executeVoid(LOG, "видалення ресурсу", () -> {
            if (resourceDao.getById(id).isEmpty()) {
                throw new ValidationException("Ресурс з id=" + id + " не знайдено");
            }
            resourceDao.delete(id);
            LOG.info("Видалено ресурс id=" + id);
            return null;
        });
    }

    private void validate(Resource r) {
        if (r == null) {
            throw new ValidationException("Ресурс не заданий");
        }
        r.setName(requireText(r.getName(), "Назва ресурсу", 150));
        r.setResourceType(requireText(r.getResourceType(), "Тип ресурсу", 50));
        r.setUnit(requireText(r.getUnit(), "Одиниця виміру", 20));
        if (r.getTotalQuantity() < 0) {
            throw new ValidationException("Кількість ресурсу не може бути від'ємною");
        }
    }
}
