package ua.pntu.events.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.pntu.events.server.model.Event;
import ua.pntu.events.server.support.Fakes;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Валідація сутності «Подія» та перевірка існування організатора. */
class EventServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 20, 10, 0);
    private Fakes f;
    private int organizer;

    @BeforeEach
    void setUp() {
        f = new Fakes();
        organizer = f.newPerson("org@example.com");
    }

    private Event event(String title, LocalDateTime start, LocalDateTime end, int organizerId) {
        return new Event(0, title, null, "Полтава", start, end, organizerId);
    }

    @Test
    @DisplayName("Коректну подію збережено")
    void addValidEvent() {
        int id = f.eventService.add(event("Конференція", START, START.plusHours(5), organizer));
        assertEquals("Конференція", f.events.get(id).getTitle());
    }

    @Test
    @DisplayName("Порожня назва та відсутні дати відхиляються")
    void rejectsMissingTitleOrDates() {
        assertThrows(ValidationException.class, () -> f.eventService.add(event(" ", START, START.plusHours(1), organizer)));
        assertThrows(ValidationException.class, () -> f.eventService.add(event("Подія", null, null, organizer)));
    }

    @Test
    @DisplayName("Дата завершення раніше початку відхиляється")
    void rejectsEndBeforeStart() {
        assertThrows(ValidationException.class, () -> f.eventService.add(event("Подія", START, START.minusHours(1), organizer)));
    }

    @Test
    @DisplayName("Неіснуючий організатор відхиляється (перевірка пов'язаного запису)")
    void rejectsUnknownOrganizer() {
        assertThrows(ValidationException.class, () -> f.eventService.add(event("Подія", START, START.plusHours(1), 999)));
    }

    @Test
    @DisplayName("Редагування неіснуючої події відхиляється, видалення прибирає подію")
    void updateAndDelete() {
        Event e = event("Подія", START, START.plusHours(1), organizer);
        e.setId(555);
        assertThrows(ValidationException.class, () -> f.eventService.update(e));
        int id = f.eventService.add(event("Подія", START, START.plusHours(1), organizer));
        f.eventService.delete(id);
        assertTrue(f.eventService.getById(id).isEmpty());
    }
}
