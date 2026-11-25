package ua.pntu.events.client.csv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.pntu.events.client.api.ApiException;
import ua.pntu.events.client.api.HttpEventManagerClient;
import ua.pntu.events.client.model.Person;
import ua.pntu.events.client.model.Resource;
import ua.pntu.events.client.model.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Перевірка читання та розбору CSV без JavaFX, Tomcat і MySQL: сервер замінено фальшивим HTTP-клієнтом,
 * який фіксує, які записи були б передані на сервер.
 */
class CsvImporterTest {

    private static final String TASK_HEADER = "event_id;title;description;deadline;criticality;assignee_email";

    private final List<Task> sentTasks = new ArrayList<>();
    private final List<Integer> sentAssignees = new ArrayList<>();
    private final List<Resource> sentResources = new ArrayList<>();
    private int serverDownAfter = Integer.MAX_VALUE;   // після скількох успішних запитів сервер «зникає»
    private CsvImporter importer;

    /** Фальшивий HTTP-клієнт: правила сервера (критичність 1–5) імітуються відхиленням з кодом 400. */
    private class FakeApi extends HttpEventManagerClient {
        FakeApi() {
            super("http://localhost:1");
        }

        @Override
        public List<Person> getPersons() {      // ті самі особи, що й у демонстраційних даних database.sql
            String[] emails = {"sklyar@example.com", "Ivanenko@Example.com", "kovalenko@example.com", "melnyk@example.com"};
            List<Person> list = new ArrayList<>();
            for (int i = 0; i < emails.length; i++) {
                Person p = new Person();
                p.setId(i + 1);
                p.setEmail(emails[i]);
                list.add(p);
            }
            return list;
        }

        @Override
        public List<Task> addTask(Task t, Integer assigneeId) {
            if (sentTasks.size() >= serverDownAfter) {
                throw new ApiException(0, "Сервер недоступний");
            }
            if (t.getCriticality() < 1 || t.getCriticality() > 5) {
                throw new ApiException(400, "Критичність має бути від 1 до 5");
            }
            sentTasks.add(t);
            sentAssignees.add(assigneeId);
            return sentTasks;
        }

        @Override
        public List<Resource> addResource(Resource r) {
            if (r.getTotalQuantity() < 0) {
                throw new ApiException(400, "Кількість ресурсу не може бути від'ємною");
            }
            sentResources.add(r);
            return sentResources;
        }
    }

    @BeforeEach
    void setUp() {
        importer = new CsvImporter(new FakeApi());
    }

    private static Path csv(String... lines) throws IOException {
        Path file = Files.createTempFile("import-test", ".csv");
        Files.write(file, List.of(lines), StandardCharsets.UTF_8);
        file.toFile().deleteOnExit();
        return file;
    }

    @Test
    @DisplayName("Коректний файл завдань із sample-data імпортується повністю (12 рядків)")
    void importsSampleTasks() throws IOException {
        CsvImporter.ImportResult r = importer.importTasks(SamplePaths.resolve("tasks.csv"));
        assertEquals(12, r.total());
        assertEquals(12, r.imported());
        assertEquals(0, r.skipped());
        assertEquals(0, r.rejected());
        assertFalse(r.aborted());
    }

    @Test
    @DisplayName("Коректний файл ресурсів із sample-data імпортується повністю (10 рядків)")
    void importsSampleResources() throws IOException {
        CsvImporter.ImportResult r = importer.importResources(SamplePaths.resolve("resources.csv"));
        assertEquals(10, r.imported());
        assertEquals("Ноутбук", sentResources.get(0).getName());
        assertEquals(10, sentResources.get(0).getTotalQuantity());
    }

    @Test
    @DisplayName("Файл із помилками: 1 імпортовано, 4 пропущено через формат, 1 відхилено сервером")
    void sampleFileWithErrors() throws IOException {
        CsvImporter.ImportResult r = importer.importTasks(SamplePaths.resolve("tasks_with_errors.csv"));
        assertEquals(6, r.total());
        assertEquals(1, r.imported());
        assertEquals(4, r.skipped());
        assertEquals(1, r.rejected());
        assertEquals(5, r.messages().size());
    }

    @Test
    @DisplayName("Неправильна кількість полів у рядку: рядок пропускається, решта імпортується")
    void wrongColumnCountSkipped() throws IOException {
        Path f = csv(TASK_HEADER, "1;Завдання А;опис;2026-11-18 12:00;3;", "1;Замало;полів", "1;Завдання Б;;2026-11-19 12:00;2;");
        CsvImporter.ImportResult r = importer.importTasks(f);
        assertEquals(3, r.total());
        assertEquals(2, r.imported());
        assertEquals(1, r.skipped());
        assertTrue(r.messages().get(0).contains("Рядок 3"));
    }

    @Test
    @DisplayName("Некоректні числові значення, дата та порожня назва пропускаються")
    void invalidValuesSkipped() throws IOException {
        Path f = csv(TASK_HEADER,
                "abc;Подія не число;;2026-11-18 12:00;3;",
                "1;Критичність не число;;2026-11-18 12:00;high;",
                "1;Дата;;18.11.2026;3;",
                "1;Порожня дата;;;3;",
                "1;;Порожня назва;2026-11-18 12:00;3;");
        CsvImporter.ImportResult r = importer.importTasks(f);
        assertEquals(0, r.imported());
        assertEquals(5, r.skipped());
        assertTrue(sentTasks.isEmpty());
    }

    @Test
    @DisplayName("Відсутній або порожній CSV-файл викликає IOException")
    void missingOrEmptyFileThrows() throws IOException {
        assertThrows(IOException.class, () -> importer.importTasks(Path.of("sample-data", "no-such-file.csv")));
        Path empty = Files.createTempFile("empty", ".csv");
        empty.toFile().deleteOnExit();
        assertThrows(IOException.class, () -> importer.importTasks(empty));
    }

    @Test
    @DisplayName("Неправильний заголовок CSV: імпорт не починається")
    void wrongHeaderThrows() throws IOException {
        Path f = csv("a;b;c", "1;2;3");
        assertThrows(IOException.class, () -> importer.importTasks(f));
        assertTrue(sentTasks.isEmpty());
        assertThrows(IOException.class, () -> importer.importTasks(SamplePaths.resolve("resources.csv")));
    }

    @Test
    @DisplayName("Виконавця знайдено за поштою без урахування регістру; невідомий виконавець — рядок пропущено")
    void assigneeLookupByEmail() throws IOException {
        Path f = csv(TASK_HEADER,
                "1;З виконавцем;;2026-11-18 12:00;3;ivanenko@example.com",
                "1;Невідомий;;2026-11-18 12:00;3;nobody@example.com");
        CsvImporter.ImportResult r = importer.importTasks(f);
        assertEquals(1, r.imported());
        assertEquals(1, r.skipped());
        assertEquals(Integer.valueOf(2), sentAssignees.get(0));
    }

    @Test
    @DisplayName("Відхилення сервером (критичність 9) рахується окремо від пропущених")
    void serverRejectionCounted() throws IOException {
        CsvImporter.ImportResult r = importer.importTasks(csv(TASK_HEADER, "1;Критичність 9;;2026-11-18 12:00;9;"));
        assertEquals(1, r.rejected());
        assertEquals(0, r.skipped());
        assertTrue(r.messages().get(0).contains("відхилено сервером"));
    }

    @Test
    @DisplayName("Недоступність сервера перериває імпорт і зберігає кількість уже імпортованих")
    void serverUnavailableAbortsImport() throws IOException {
        serverDownAfter = 2;
        CsvImporter.ImportResult r = importer.importTasks(SamplePaths.resolve("tasks.csv"));
        assertTrue(r.aborted());
        assertEquals(2, r.imported());
        assertEquals(0, r.rejected());
        assertTrue(r.summary().contains("ПЕРЕРВАНО"));
    }

    @Test
    @DisplayName("Слухач прогресу отримує початок, кожен рядок і завершення (done = total)")
    void progressListenerReportsAllRows() throws IOException {
        List<Integer> seen = new ArrayList<>();
        int[] total = new int[1];
        importer.importResources(SamplePaths.resolve("resources.csv"), (done, t, message) -> {
            seen.add(done);
            total[0] = t;
        });
        assertEquals(10, total[0]);
        assertEquals(Integer.valueOf(0), seen.get(0));
        assertEquals(Integer.valueOf(10), seen.get(seen.size() - 1));
    }

    @Test
    @DisplayName("Порожні рядки та BOM на початку файла ігноруються")
    void blankLinesAndBomIgnored() throws IOException {
        Path f = csv("\uFEFF" + TASK_HEADER, "", "1;Завдання;;2026-11-18 12:00;3;", "   ");
        CsvImporter.ImportResult r = importer.importTasks(f);
        assertEquals(1, r.total());
        assertEquals(1, r.imported());
    }

    @Test
    @DisplayName("Повторний імпорт: клієнт передає ті самі рядки, а дублікати відхиляє сервер (ResourceService/TaskService)")
    void repeatedImportSendsRowsAgain() throws IOException {
        importer.importResources(SamplePaths.resolve("resources.csv"));
        importer.importResources(SamplePaths.resolve("resources.csv"));
        assertEquals(20, sentResources.size());
    }

    @Test
    @DisplayName("SamplePaths знаходить файли в sample-data за відносним шляхом")
    void samplePathsResolveRelative() {
        assertFalse(SamplePaths.resolve("tasks.csv").isAbsolute());
        assertTrue(Files.exists(SamplePaths.resolve("tasks.csv")));
    }
}
