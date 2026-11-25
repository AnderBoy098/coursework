package ua.pntu.events.server.support;

import ua.pntu.events.server.dao.EventDAO;
import ua.pntu.events.server.dao.PersonDAO;
import ua.pntu.events.server.dao.ResourceDAO;
import ua.pntu.events.server.dao.TaskDAO;
import ua.pntu.events.server.dao.TaskDependencyDAO;
import ua.pntu.events.server.dao.TaskResourceDAO;
import ua.pntu.events.server.model.Event;
import ua.pntu.events.server.model.Person;
import ua.pntu.events.server.model.Resource;
import ua.pntu.events.server.model.Task;
import ua.pntu.events.server.model.TaskResource;
import ua.pntu.events.server.model.TaskStatus;
import ua.pntu.events.server.service.EventService;
import ua.pntu.events.server.service.PersonService;
import ua.pntu.events.server.service.ResourceService;
import ua.pntu.events.server.service.TaskService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Тестове середовище без MySQL: DAO-класи замінено версіями, що зберігають дані в пам'яті.
 * Service-класи створюються зі справжнім кодом програми, змінюється лише джерело даних.
 * Час зафіксовано (NOW), тому значення інтегрального показника відтворювані.
 */
public class Fakes {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 12, 0);
    public static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    public final Map<Integer, Person> persons = new LinkedHashMap<>();
    public final Map<Integer, Event> events = new LinkedHashMap<>();
    public final Map<Integer, Task> tasks = new LinkedHashMap<>();
    public final Map<Integer, Resource> resources = new LinkedHashMap<>();
    public final Set<List<Integer>> dependencies = new LinkedHashSet<>();
    public final Map<List<Integer>, TaskResource> links = new LinkedHashMap<>();
    private int seq = 0;

    public final PersonDAO personDao = new PersonDAO() {
        @Override public int add(Person p) { p.setId(++seq); persons.put(p.getId(), p); return p.getId(); }
        @Override public List<Person> getAll() { return new ArrayList<>(persons.values()); }
        @Override public Optional<Person> getById(int id) { return Optional.ofNullable(persons.get(id)); }
        @Override public Optional<Person> findByEmail(String e) { return persons.values().stream().filter(x -> x.getEmail().equals(e)).findFirst(); }
        @Override public boolean update(Person p) { persons.put(p.getId(), p); return true; }
        @Override public boolean delete(int id) { return persons.remove(id) != null; }
    };

    public final EventDAO eventDao = new EventDAO() {
        @Override public int add(Event e) { e.setId(++seq); events.put(e.getId(), e); return e.getId(); }
        @Override public List<Event> getAll() { return new ArrayList<>(events.values()); }
        @Override public Optional<Event> getById(int id) { return Optional.ofNullable(events.get(id)); }
        @Override public boolean update(Event e) { events.put(e.getId(), e); return true; }
        @Override public boolean delete(int id) { return events.remove(id) != null; }
        @Override public int countByOrganizer(int pid) { return (int) events.values().stream().filter(x -> x.getOrganizerId() == pid).count(); }
    };

    public final ResourceDAO resourceDao = new ResourceDAO() {
        @Override public int add(Resource r) { r.setId(++seq); resources.put(r.getId(), r); return r.getId(); }
        @Override public List<Resource> getAll() { return new ArrayList<>(resources.values()); }
        @Override public Optional<Resource> getById(int id) { return Optional.ofNullable(resources.get(id)); }
        @Override public Optional<Resource> findByName(String n) { return resources.values().stream().filter(x -> x.getName().equalsIgnoreCase(n)).findFirst(); }
        @Override public boolean update(Resource r) { resources.put(r.getId(), r); return true; }
        @Override public boolean delete(int id) { return resources.remove(id) != null; }
    };

    public final TaskDAO taskDao = new TaskDAO() {
        @Override public int add(Task t) { t.setId(++seq); tasks.put(t.getId(), t); return t.getId(); }
        @Override public List<Task> getAll() { return new ArrayList<>(tasks.values()); }
        @Override public List<Task> getByEventId(int eid) { List<Task> r = new ArrayList<>(); for (Task t : tasks.values()) if (t.getEventId() == eid) r.add(t); return r; }
        @Override public Optional<Task> getById(int id) { return Optional.ofNullable(tasks.get(id)); }
        @Override public Optional<Task> findByEventAndTitle(int eid, String title) { return tasks.values().stream().filter(x -> x.getEventId() == eid && x.getTitle().equalsIgnoreCase(title)).findFirst(); }
        @Override public boolean update(Task t) { Task o = tasks.get(t.getId()); o.setEventId(t.getEventId()); o.setTitle(t.getTitle()); o.setDescription(t.getDescription()); o.setDeadline(t.getDeadline()); o.setCriticality(t.getCriticality()); return true; }
        @Override public boolean delete(int id) { dependencies.removeIf(l -> l.contains(id)); links.keySet().removeIf(k -> k.get(0) == id); return tasks.remove(id) != null; }
        @Override public boolean updateStatus(int id, TaskStatus s) { tasks.get(id).setStatus(s); return true; }
        @Override public boolean updateAssigneeAndStatus(int id, Integer p, TaskStatus s) { tasks.get(id).setAssigneeId(p); tasks.get(id).setStatus(s); return true; }
        @Override public boolean updatePriorityIndex(int id, Double v) { tasks.get(id).setPriorityIndex(v); return true; }
        @Override public int countActiveByAssignee(int pid) { return (int) tasks.values().stream().filter(x -> Objects.equals(x.getAssigneeId(), pid) && x.getStatus() != TaskStatus.DONE).count(); }
    };

    public final TaskDependencyDAO dependencyDao = new TaskDependencyDAO() {
        @Override public void add(int t, int d) { dependencies.add(List.of(t, d)); }
        @Override public boolean delete(int t, int d) { return dependencies.remove(List.of(t, d)); }
        @Override public boolean exists(int t, int d) { return dependencies.contains(List.of(t, d)); }
        @Override public List<Integer> getDependsOnIds(int t) { List<Integer> r = new ArrayList<>(); for (List<Integer> l : dependencies) if (l.get(0) == t) r.add(l.get(1)); return r; }
        @Override public List<Integer> getDependentIds(int t) { List<Integer> r = new ArrayList<>(); for (List<Integer> l : dependencies) if (l.get(1) == t) r.add(l.get(0)); return r; }
        @Override public int countDependents(int t) { return getDependentIds(t).size(); }
    };

    public final TaskResourceDAO taskResourceDao = new TaskResourceDAO() {
        @Override public void add(TaskResource tr) { links.put(List.of(tr.getTaskId(), tr.getResourceId()), tr); }
        @Override public List<TaskResource> getByTaskId(int t) { List<TaskResource> r = new ArrayList<>(); for (TaskResource x : links.values()) if (x.getTaskId() == t) r.add(x); return r; }
        @Override public Optional<TaskResource> getById(int t, int r) { return Optional.ofNullable(links.get(List.of(t, r))); }
        @Override public boolean updateQuantity(int t, int r, int q) { links.get(List.of(t, r)).setQuantityNeeded(q); return true; }
        @Override public boolean delete(int t, int r) { return links.remove(List.of(t, r)) != null; }
    };

    public final PersonService personService = new PersonService(personDao, eventDao, taskDao);
    public final EventService eventService = new EventService(eventDao, personDao);
    public final ResourceService resourceService = new ResourceService(resourceDao);
    public final TaskService taskService = new TaskService(taskDao, eventDao, personDao, resourceDao, dependencyDao, taskResourceDao, CLOCK);

    // ---------- допоміжні фабрики коректних даних ----------

    public int newPerson(String email) {
        return personService.add(new Person(0, "Тест Користувач", email, null, null));
    }

    public int newEvent(int organizerId) {
        return eventService.add(new Event(0, "Подія " + (seq + 1), null, null, NOW.plusDays(10), NOW.plusDays(10).plusHours(3), organizerId));
    }

    /** Коректне завдання; hoursToDeadline відраховується від зафіксованого часу NOW. */
    public Task task(int eventId, String title, int hoursToDeadline, int criticality) {
        Task t = new Task();
        t.setEventId(eventId);
        t.setTitle(title);
        t.setDeadline(NOW.plusHours(hoursToDeadline));
        t.setCriticality(criticality);
        return t;
    }

    public int newTask(int eventId, String title, int hoursToDeadline, int criticality) {
        return taskService.add(task(eventId, title, hoursToDeadline, criticality));
    }
}
