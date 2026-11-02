package ua.pntu.events.client.csv;

import ua.pntu.events.client.api.ApiException;
import ua.pntu.events.client.api.HttpEventManagerClient;
import ua.pntu.events.client.model.Person;
import ua.pntu.events.client.model.Resource;
import ua.pntu.events.client.model.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Початок CSV-імпорту (етап 3): читання файла, розбір рядків і передавання коректних даних серверу
 * через HttpEventManagerClient — тим самим маршрутом, що й ручне введення.
 * Імпорт напряму в MySQL не виконується. Фоновий Task із ProgressBar буде додано на етапі 4.
 *
 * Формат: UTF-8, роздільник «;», перший рядок — заголовок.
 *   tasks.csv:     event_id;title;description;deadline;criticality;assignee_email
 *   resources.csv: name;resource_type;total_quantity;unit
 * Клієнт перевіряє лише формат (кількість стовпців, обов'язковість, числа, дата);
 * предметні правила (діапазон критичності, існування події тощо) перевіряє сервер.
 */
public class CsvImporter {

    private static final Logger LOG = Logger.getLogger(CsvImporter.class.getName());

    public static final String SEPARATOR = ";";
    public static final String[] TASK_HEADER = {"event_id", "title", "description", "deadline", "criticality", "assignee_email"};
    public static final String[] RESOURCE_HEADER = {"name", "resource_type", "total_quantity", "unit"};

    /** Підсумок імпорту: прочитано, імпортовано, пропущено через формат, відхилено сервером. */
    public record ImportResult(int total, int imported, int skipped, int rejected, List<String> messages) {
        public String summary() {
            return "Прочитано рядків: " + total + "; імпортовано: " + imported
                    + "; пропущено (некоректний формат): " + skipped + "; відхилено сервером: " + rejected;
        }
    }

    private record Row(int line, String[] cells) {
    }

    private final HttpEventManagerClient api;

    public CsvImporter(HttpEventManagerClient api) {
        this.api = api;
    }

    // ===================== Завдання =====================

    public ImportResult importTasks(Path file) throws IOException {
        List<Row> rows = readRows(file, TASK_HEADER);
        Map<String, Integer> personByEmail = new HashMap<>();
        for (Person p : api.getPersons()) {
            personByEmail.put(p.getEmail().toLowerCase(Locale.ROOT), p.getId());
        }
        int imported = 0;
        int skipped = 0;
        int rejected = 0;
        List<String> messages = new ArrayList<>();

        for (Row row : rows) {
            String[] c = row.cells();
            if (c.length != TASK_HEADER.length) {
                skipped++;
                messages.add(skip(row, "очікувалось " + TASK_HEADER.length + " стовпців, знайдено " + c.length));
                continue;
            }
            Task t;
            Integer assigneeId = null;
            try {
                t = new Task();
                t.setEventId(parseInt(c[0], "event_id"));
                t.setTitle(required(c[1], "title"));
                t.setDescription(c[2].isEmpty() ? null : c[2]);
                t.setDeadline(parseDateTime(c[3], "deadline"));
                t.setCriticality(parseInt(c[4], "criticality"));
                if (!c[5].isEmpty()) {
                    assigneeId = personByEmail.get(c[5].toLowerCase(Locale.ROOT));
                    if (assigneeId == null) {
                        throw new IllegalArgumentException("виконавця з поштою " + c[5] + " не знайдено");
                    }
                }
            } catch (IllegalArgumentException e) {
                skipped++;
                messages.add(skip(row, e.getMessage()));
                continue;
            }
            try {
                api.addTask(t, assigneeId);   // JavaFX → HTTP → Servlet → Service → DAO → MySQL
                imported++;
            } catch (ApiException e) {
                rejected++;
                messages.add("Рядок " + row.line() + " відхилено сервером: " + e.getMessage());
                LOG.warning("CSV: рядок " + row.line() + " відхилено сервером: " + e.getMessage());
            }
        }
        return finish(file, rows.size(), imported, skipped, rejected, messages);
    }

    // ===================== Ресурси =====================

    public ImportResult importResources(Path file) throws IOException {
        List<Row> rows = readRows(file, RESOURCE_HEADER);
        int imported = 0;
        int skipped = 0;
        int rejected = 0;
        List<String> messages = new ArrayList<>();

        for (Row row : rows) {
            String[] c = row.cells();
            if (c.length != RESOURCE_HEADER.length) {
                skipped++;
                messages.add(skip(row, "очікувалось " + RESOURCE_HEADER.length + " стовпці, знайдено " + c.length));
                continue;
            }
            Resource r;
            try {
                r = new Resource();
                r.setName(required(c[0], "name"));
                r.setResourceType(required(c[1], "resource_type"));
                r.setTotalQuantity(parseInt(c[2], "total_quantity"));
                r.setUnit(required(c[3], "unit"));
            } catch (IllegalArgumentException e) {
                skipped++;
                messages.add(skip(row, e.getMessage()));
                continue;
            }
            try {
                api.addResource(r);
                imported++;
            } catch (ApiException e) {
                rejected++;
                messages.add("Рядок " + row.line() + " відхилено сервером: " + e.getMessage());
                LOG.warning("CSV: рядок " + row.line() + " відхилено сервером: " + e.getMessage());
            }
        }
        return finish(file, rows.size(), imported, skipped, rejected, messages);
    }

    // ===================== Читання та розбір =====================

    /** Читає файл, перевіряє заголовок і повертає непорожні рядки з номерами (нумерація з 1). */
    private List<Row> readRows(Path file, String[] expectedHeader) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException("CSV-файл не знайдено: " + file);
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IOException("CSV-файл порожній: " + file);
        }
        String header = lines.get(0).replace("\uFEFF", "").trim(); // прибираємо BOM
        if (!header.equalsIgnoreCase(String.join(SEPARATOR, expectedHeader))) {
            throw new IOException("Некоректний заголовок CSV. Очікується: " + String.join(SEPARATOR, expectedHeader));
        }
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] cells = line.split(SEPARATOR, -1);
            for (int k = 0; k < cells.length; k++) {
                cells[k] = cells[k].trim();
            }
            rows.add(new Row(i + 1, cells));
        }
        return rows;
    }

    private ImportResult finish(Path file, int total, int imported, int skipped, int rejected, List<String> messages) {
        ImportResult result = new ImportResult(total, imported, skipped, rejected, messages);
        LOG.info("CSV " + file + ": " + result.summary());
        return result;
    }

    private String skip(Row row, String reason) {
        String msg = "Рядок " + row.line() + " пропущено: " + reason;
        LOG.warning("CSV: " + msg);
        return msg;
    }

    private static String required(String value, String field) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("порожнє обов'язкове поле «" + field + "»");
        }
        return value;
    }

    private static int parseInt(String value, String field) {
        try {
            return Integer.parseInt(required(value, field));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("«" + field + "» має бути цілим числом: " + value);
        }
    }

    private static LocalDateTime parseDateTime(String value, String field) {
        try {
            return LocalDateTime.parse(required(value, field).replace(' ', 'T'));
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("«" + field + "» має формат рррр-мм-дд гг:хх: " + value);
        }
    }
}
