package ua.pntu.events.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.pntu.events.server.model.TaskStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Чотиристанний життєвий цикл: СТВОРЕНО → ПРИЗНАЧЕНО → ВИКОНУЄТЬСЯ → ВИКОНАНО. */
class TaskLifecycleTest {

    @Test
    @DisplayName("Перелік містить рівно чотири стани у правильному порядку")
    void fourStatesInOrder() {
        TaskStatus[] s = TaskStatus.values();
        assertEquals(4, s.length);
        assertEquals(TaskStatus.CREATED, s[0]);
        assertEquals(TaskStatus.ASSIGNED, s[1]);
        assertEquals(TaskStatus.IN_PROGRESS, s[2]);
        assertEquals(TaskStatus.DONE, s[3]);
    }

    @Test
    @DisplayName("Дозволені послідовні переходи вперед")
    void allowedTransitions() {
        assertTrue(TaskService.isAllowedTransition(TaskStatus.CREATED, TaskStatus.ASSIGNED));
        assertTrue(TaskService.isAllowedTransition(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS));
        assertTrue(TaskService.isAllowedTransition(TaskStatus.IN_PROGRESS, TaskStatus.DONE));
    }

    @Test
    @DisplayName("Пропуск стану та повернення назад заборонені")
    void skippingAndBackwardForbidden() {
        assertFalse(TaskService.isAllowedTransition(TaskStatus.CREATED, TaskStatus.IN_PROGRESS));
        assertFalse(TaskService.isAllowedTransition(TaskStatus.CREATED, TaskStatus.DONE));
        assertFalse(TaskService.isAllowedTransition(TaskStatus.ASSIGNED, TaskStatus.DONE));
        assertFalse(TaskService.isAllowedTransition(TaskStatus.ASSIGNED, TaskStatus.CREATED));
        assertFalse(TaskService.isAllowedTransition(TaskStatus.IN_PROGRESS, TaskStatus.ASSIGNED));
        assertFalse(TaskService.isAllowedTransition(TaskStatus.DONE, TaskStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("Серед усіх 16 пар станів дозволено рівно 3 переходи; переходів у той самий стан немає")
    void exactlyThreeTransitionsAllowed() {
        int allowed = 0;
        for (TaskStatus from : TaskStatus.values()) {
            for (TaskStatus to : TaskStatus.values()) {
                if (TaskService.isAllowedTransition(from, to)) {
                    allowed++;
                    assertFalse(from == to);
                }
            }
        }
        assertEquals(3, allowed);
    }

    @Test
    @DisplayName("Стан ВИКОНАНО кінцевий; порожні значення не дозволені")
    void doneIsFinalAndNullRejected() {
        for (TaskStatus to : TaskStatus.values()) {
            assertFalse(TaskService.isAllowedTransition(TaskStatus.DONE, to));
        }
        assertFalse(TaskService.isAllowedTransition(null, TaskStatus.ASSIGNED));
        assertFalse(TaskService.isAllowedTransition(TaskStatus.CREATED, null));
    }
}
