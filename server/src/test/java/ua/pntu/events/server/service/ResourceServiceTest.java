package ua.pntu.events.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.pntu.events.server.model.Resource;
import ua.pntu.events.server.support.Fakes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Валідація сутності «Ресурс». */
class ResourceServiceTest {

    private Fakes f;

    @BeforeEach
    void setUp() {
        f = new Fakes();
    }

    @Test
    @DisplayName("Коректний ресурс збережено")
    void addValid() {
        int id = f.resourceService.add(new Resource(0, "Проєктор", "Обладнання", 3, "шт."));
        assertEquals(3, f.resources.get(id).getTotalQuantity());
    }

    @Test
    @DisplayName("Від'ємна кількість і порожні обов'язкові поля відхиляються")
    void rejectsInvalid() {
        assertThrows(ValidationException.class, () -> f.resourceService.add(new Resource(0, "Х", "Т", -1, "шт.")));
        assertThrows(ValidationException.class, () -> f.resourceService.add(new Resource(0, "", "Т", 1, "шт.")));
        assertThrows(ValidationException.class, () -> f.resourceService.add(new Resource(0, "Х", " ", 1, "шт.")));
        assertThrows(ValidationException.class, () -> f.resourceService.add(new Resource(0, "Х", "Т", 1, "")));
    }

    @Test
    @DisplayName("Дублікат назви (без урахування регістру) відхиляється — захист від повторного імпорту")
    void rejectsDuplicateName() {
        f.resourceService.add(new Resource(0, "Ноутбук", "Обладнання", 10, "шт."));
        assertThrows(ValidationException.class, () -> f.resourceService.add(new Resource(0, "НОУТБУК", "Обладнання", 5, "шт.")));
        assertEquals(1, f.resources.size());
    }

    @Test
    @DisplayName("Редагування ресурсу зі своєю назвою дозволене, видалення неіснуючого відхиляється")
    void updateAndDelete() {
        int id = f.resourceService.add(new Resource(0, "Екран", "Обладнання", 2, "шт."));
        Resource r = f.resources.get(id);
        r.setTotalQuantity(4);
        f.resourceService.update(r);
        assertEquals(4, f.resources.get(id).getTotalQuantity());
        assertThrows(ValidationException.class, () -> f.resourceService.delete(404));
    }
}
