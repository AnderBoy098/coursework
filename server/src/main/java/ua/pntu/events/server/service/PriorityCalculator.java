package ua.pntu.events.server.service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Розрахунок інтегрального показника пріоритету завдання (варіант 23):
 * I = 0,40·F1 + 0,35·F2 + 0,25·F3, усі фактори нормовано до діапазону 0–100.
 * F1 — близькість дедлайну, F2 — критичність, F3 — кількість залежних завдань.
 * Клас не є моделлю і не є DAO: він лише виконує обчислення.
 */
public final class PriorityCalculator {

    public static final double W1 = 0.40;
    public static final double W2 = 0.35;
    public static final double W3 = 0.25;

    /** Горизонт планування для F1, годин (30 діб). */
    public static final double HORIZON_HOURS = 720.0;
    /** Кількість залежних завдань, що дає F3 = 100. */
    public static final int MAX_DEPENDENTS = 5;
    public static final int MIN_CRITICALITY = 1;
    public static final int MAX_CRITICALITY = 5;

    private PriorityCalculator() {
    }

    /** F1 = 100·(1 − min(max(h, 0), H)/H); якщо дедлайн настав або минув — 100. */
    public static double factor1(LocalDateTime deadline, LocalDateTime now) {
        if (deadline == null || now == null) {
            throw new IllegalArgumentException("Дедлайн і поточний час мають бути задані");
        }
        double hours = Duration.between(now, deadline).toMinutes() / 60.0;
        if (hours <= 0) {
            return 100.0;
        }
        return 100.0 * (1.0 - Math.min(hours, HORIZON_HOURS) / HORIZON_HOURS);
    }

    /** F2 = 20·c, де c — критичність 1..5. */
    public static double factor2(int criticality) {
        if (criticality < MIN_CRITICALITY || criticality > MAX_CRITICALITY) {
            throw new IllegalArgumentException("Критичність має бути від 1 до 5");
        }
        return 20.0 * criticality;
    }

    /** F3 = 100·min(n, N)/N, де n — кількість залежних завдань. */
    public static double factor3(int dependentsCount) {
        if (dependentsCount < 0) {
            throw new IllegalArgumentException("Кількість залежних завдань не може бути від'ємною");
        }
        return 100.0 * Math.min(dependentsCount, MAX_DEPENDENTS) / MAX_DEPENDENTS;
    }

    /** I = 0,40·F1 + 0,35·F2 + 0,25·F3, округлено до сотих. */
    public static double calculate(double f1, double f2, double f3) {
        checkFactor(f1, "F1");
        checkFactor(f2, "F2");
        checkFactor(f3, "F3");
        double i = W1 * f1 + W2 * f2 + W3 * f3;
        return Math.round(i * 100.0) / 100.0;
    }

    private static void checkFactor(double value, String name) {
        if (Double.isNaN(value) || value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException("Фактор " + name + " має бути в діапазоні 0–100");
        }
    }
}
