package ua.pntu.events.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.pntu.events.server.model.Person;
import ua.pntu.events.server.support.Fakes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Валідація та правила сутності «Відповідальна особа». */
class PersonServiceTest {

    private Fakes f;

    @BeforeEach
    void setUp() {
        f = new Fakes();
    }

    @Test
    @DisplayName("Коректну особу збережено, текстові поля очищено від зайвих пробілів")
    void addTrimsAndStores() {
        int id = f.personService.add(new Person(0, "  Іван Іванов  ", " ivan@example.com ", "+380501112233", null));
        assertEquals("Іван Іванов", f.persons.get(id).getFullName());
        assertEquals("ivan@example.com", f.persons.get(id).getEmail());
    }

    @Test
    @DisplayName("Порожній ПІБ, некоректна пошта та телефон відхиляються")
    void rejectsInvalidFields() {
        assertThrows(ValidationException.class, () -> f.personService.add(new Person(0, "   ", "a@b.com", null, null)));
        assertThrows(ValidationException.class, () -> f.personService.add(new Person(0, "Іван", "not-an-email", null, null)));
        assertThrows(ValidationException.class, () -> f.personService.add(new Person(0, "Іван", "a@b.com", "abc", null)));
        assertTrue(f.persons.isEmpty());
    }

    @Test
    @DisplayName("Дублікат пошти відхиляється, редагування зі своєю поштою дозволене")
    void emailUniqueness() {
        int id = f.newPerson("same@example.com");
        assertThrows(ValidationException.class, () -> f.personService.add(new Person(0, "Інший", "same@example.com", null, null)));
        Person p = f.persons.get(id);
        p.setJobTitle("Координатор");
        f.personService.update(p);
        assertEquals("Координатор", f.persons.get(id).getJobTitle());
    }

    @Test
    @DisplayName("Редагування неіснуючої особи відхиляється")
    void updateUnknownRejected() {
        assertThrows(ValidationException.class, () -> f.personService.update(new Person(999, "Хтось", "x@y.com", null, null)));
    }

    @Test
    @DisplayName("Організатора події видалити не можна")
    void deleteForbiddenForOrganizer() {
        int pid = f.newPerson("org@example.com");
        f.newEvent(pid);
        assertThrows(ValidationException.class, () -> f.personService.delete(pid));
        assertTrue(f.persons.containsKey(pid));
    }

    @Test
    @DisplayName("Особу з незавершеними завданнями видалити не можна, вільну — можна")
    void deleteRules() {
        int org = f.newPerson("org@example.com");
        int worker = f.newPerson("worker@example.com");
        int free = f.newPerson("free@example.com");
        int tid = f.newTask(f.newEvent(org), "Завдання", 48, 3);
        f.taskService.assign(tid, worker);
        assertThrows(ValidationException.class, () -> f.personService.delete(worker));
        f.personService.delete(free);
        assertFalse(f.persons.containsKey(free));
    }
}
