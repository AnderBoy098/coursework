package ua.pntu.events.server.demo;

import ua.pntu.events.server.model.Event;
import ua.pntu.events.server.model.Person;
import ua.pntu.events.server.model.Resource;
import ua.pntu.events.server.model.Task;
import ua.pntu.events.server.model.TaskStatus;
import ua.pntu.events.server.service.EventService;
import ua.pntu.events.server.service.PersonService;
import ua.pntu.events.server.service.ResourceService;
import ua.pntu.events.server.service.TaskService;
import ua.pntu.events.server.util.ServerLogging;

import java.time.LocalDateTime;

/**
 * Демонстраційний сценарій етапу 2: CRUD, валідація, залежності, життєвий цикл та інтегральний показник
 * на реальній БД MySQL. Усі створені записи наприкінці видаляються.
 * Запуск: mvn compile exec:java -Dexec.mainClass=ua.pntu.events.server.demo.ServerDemo
 */
public class ServerDemo {

    public static void main(String[] args) {
        ServerLogging.init();
        PersonService persons = new PersonService();
        EventService events = new EventService();
        ResourceService resources = new ResourceService();
        TaskService tasks = new TaskService();

        int personId = 0, eventId = 0, resourceId = 0;
        try {
            System.out.println("== 1. CRUD особи, події, ресурсу ==");
            Person p = new Person(0, "Демо Виконавець", "demo." + System.currentTimeMillis() + "@example.com", "+380501234567", "Тестувальник");
            personId = persons.add(p);
            p.setJobTitle("Старший тестувальник");
            persons.update(p);
            System.out.println("Особа: " + persons.getById(personId).orElseThrow());

            Event e = new Event(0, "Демо-подія", "Перевірка етапу 2", "Полтава",
                    LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(10).plusHours(4), personId);
            eventId = events.add(e);
            resourceId = resources.add(new Resource(0, "Демо-проєктор", "Обладнання", 3, "шт."));
            System.out.println("Подія id=" + eventId + ", ресурс id=" + resourceId);

            System.out.println("\n== 2. Завдання, залежності, показник I ==");
            Task a = new Task(); a.setEventId(eventId); a.setTitle("Підготувати зал");
            a.setDeadline(LocalDateTime.now().plusDays(3)); a.setCriticality(5);
            Task b = new Task(); b.setEventId(eventId); b.setTitle("Розставити обладнання");
            b.setDeadline(LocalDateTime.now().plusDays(8)); b.setCriticality(3);
            int idA = tasks.add(a);
            int idB = tasks.add(b);
            tasks.addDependency(idB, idA);          // B залежить від A -> у A зростає F3
            tasks.addResource(idB, resourceId, 2);
            printTasks(tasks, eventId);

            System.out.println("\n== 3. Валідація та заборонені дії ==");
            attempt("Критичність 9", () -> { Task bad = new Task(); bad.setEventId(a.getEventId());
                bad.setTitle("X"); bad.setDeadline(LocalDateTime.now()); bad.setCriticality(9); tasks.add(bad); });
            attempt("Подія не існує", () -> { Task bad = new Task(); bad.setEventId(-1);
                bad.setTitle("X"); bad.setDeadline(LocalDateTime.now()); bad.setCriticality(3); tasks.add(bad); });
            attempt("Цикл залежностей", () -> tasks.addDependency(idA, idB));
            attempt("CREATED → DONE", () -> tasks.changeStatus(idA, TaskStatus.DONE));
            attempt("Старт без призначення", () -> tasks.changeStatus(idA, TaskStatus.IN_PROGRESS));

            System.out.println("\n== 4. Життєвий цикл ==");
            attempt("Старт B раніше за A", () -> { tasks.assign(idB, persons.getAll().get(0).getId());
                tasks.changeStatus(idB, TaskStatus.IN_PROGRESS); });
            int pid = personId;
            tasks.assign(idA, pid);
            tasks.changeStatus(idA, TaskStatus.IN_PROGRESS);
            tasks.changeStatus(idA, TaskStatus.DONE);
            attempt("Повернення DONE → IN_PROGRESS", () -> tasks.changeStatus(idA, TaskStatus.IN_PROGRESS));
            printTasks(tasks, eventId);
        } finally {
            System.out.println("\n== 5. Прибирання демонстраційних даних ==");
            try {
                if (eventId != 0) events.delete(eventId);     // каскадно видаляє завдання
                if (resourceId != 0) resources.delete(resourceId);
                if (personId != 0) persons.delete(personId);
                System.out.println("Готово.");
            } catch (RuntimeException ex) {
                System.out.println("Прибирання не завершено: " + ex.getMessage());
            }
        }
    }

    private static void printTasks(TaskService tasks, int eventId) {
        for (Task t : tasks.getAllByPriority()) {
            if (t.getEventId() == eventId) {
                System.out.printf("  #%d %-24s %-12s I=%s%n", t.getId(), t.getTitle(), t.getStatus().getLabel(), t.getPriorityIndex());
            }
        }
    }

    private static void attempt(String what, Runnable action) {
        try {
            action.run();
            System.out.println("  [!] " + what + ": операцію виконано (очікувалась відмова)");
        } catch (RuntimeException e) {
            System.out.println("  [ok] " + what + " → відхилено: " + e.getMessage());
        }
    }
}
