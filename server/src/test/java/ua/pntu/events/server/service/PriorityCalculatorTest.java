package ua.pntu.events.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Інтегральний показник I = 0,40·F1 + 0,35·F2 + 0,25·F3: нормування факторів і відхилення недопустимих значень. */
class PriorityCalculatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 12, 0);

    @Test
    @DisplayName("Ваги показника в сумі дають 1")
    void weightsSumToOne() {
        assertEquals(1.0, PriorityCalculator.W1 + PriorityCalculator.W2 + PriorityCalculator.W3, 1e-9);
    }

    @Test
    @DisplayName("F1: дедлайн через 6 діб (144 год) дає 80; через 15 діб — 50")
    void factor1Normalization() {
        assertEquals(80.0, PriorityCalculator.factor1(NOW.plusHours(144), NOW), 1e-9);
        assertEquals(50.0, PriorityCalculator.factor1(NOW.plusDays(15), NOW), 1e-9);
    }

    @Test
    @DisplayName("F1: дедлайн на межі горизонту та далі дає 0")
    void factor1BeyondHorizon() {
        assertEquals(0.0, PriorityCalculator.factor1(NOW.plusDays(30), NOW), 1e-9);
        assertEquals(0.0, PriorityCalculator.factor1(NOW.plusDays(90), NOW), 1e-9);
    }

    @Test
    @DisplayName("F1: настання або прострочення дедлайну дає 100")
    void factor1OverdueOrNow() {
        assertEquals(100.0, PriorityCalculator.factor1(NOW, NOW), 1e-9);
        assertEquals(100.0, PriorityCalculator.factor1(NOW.minusDays(3), NOW), 1e-9);
    }

    @Test
    @DisplayName("F1: відсутній дедлайн відхиляється")
    void factor1RejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.factor1(null, NOW));
    }

    @Test
    @DisplayName("F2: критичність 1..5 нормується як 20·c")
    void factor2Mapping() {
        assertEquals(20.0, PriorityCalculator.factor2(1), 1e-9);
        assertEquals(60.0, PriorityCalculator.factor2(3), 1e-9);
        assertEquals(100.0, PriorityCalculator.factor2(5), 1e-9);
    }

    @Test
    @DisplayName("F2: критичність поза діапазоном 1–5 відхиляється")
    void factor2RejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.factor2(0));
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.factor2(6));
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.factor2(-1));
    }

    @Test
    @DisplayName("F3: кількість залежних завдань нормується з обмеженням у 5")
    void factor3Mapping() {
        assertEquals(0.0, PriorityCalculator.factor3(0), 1e-9);
        assertEquals(20.0, PriorityCalculator.factor3(1), 1e-9);
        assertEquals(40.0, PriorityCalculator.factor3(2), 1e-9);
        assertEquals(100.0, PriorityCalculator.factor3(5), 1e-9);
        assertEquals(100.0, PriorityCalculator.factor3(9), 1e-9);
    }

    @Test
    @DisplayName("F3: від'ємна кількість відхиляється")
    void factor3RejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.factor3(-1));
    }

    @Test
    @DisplayName("I: контрольний приклад F1=80, F2=80, F3=40 дає 70; крайні значення — 0 та 100")
    void calculateControlValues() {
        assertEquals(70.0, PriorityCalculator.calculate(80, 80, 40), 1e-9);
        assertEquals(0.0, PriorityCalculator.calculate(0, 0, 0), 1e-9);
        assertEquals(100.0, PriorityCalculator.calculate(100, 100, 100), 1e-9);
    }

    @Test
    @DisplayName("I: результат округлюється до сотих")
    void calculateRoundsToHundredths() {
        assertEquals(33.33, PriorityCalculator.calculate(33.3333, 33.3333, 33.3333), 1e-9);
    }

    @Test
    @DisplayName("I: фактор поза діапазоном 0–100 відхиляється")
    void calculateRejectsFactorOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.calculate(101, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.calculate(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.calculate(0, 0, Double.NaN));
    }

    @Test
    @DisplayName("I завжди лежить у діапазоні 0–100 для допустимих вхідних даних")
    void resultAlwaysInRange() {
        for (int c = 1; c <= 5; c++) {
            for (int n = 0; n <= 7; n++) {
                for (int h = -48; h <= 900; h += 47) {
                    double i = PriorityCalculator.calculate(PriorityCalculator.factor1(NOW.plusHours(h), NOW),
                            PriorityCalculator.factor2(c), PriorityCalculator.factor3(n));
                    assertTrue(i >= 0.0 && i <= 100.0, "I поза діапазоном: " + i);
                }
            }
        }
    }
}
