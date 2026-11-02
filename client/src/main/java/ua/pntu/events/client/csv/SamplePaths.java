package ua.pntu.events.client.csv;

import java.nio.file.Files;
import java.nio.file.Path;

/** Відносні шляхи до тестових CSV-файлів; абсолютні шляхи (C:\Users\...) не використовуються. */
public final class SamplePaths {

    public static final String DIR = "sample-data";

    private SamplePaths() {
    }

    /**
     * Шукає файл у sample-data відносно робочого каталогу, а якщо запуск виконано з кореня
     * репозиторію — у client/sample-data.
     */
    public static Path resolve(String fileName) {
        Path direct = Path.of(DIR, fileName);
        if (Files.exists(direct)) {
            return direct;
        }
        Path fromRoot = Path.of("client", DIR, fileName);
        return Files.exists(fromRoot) ? fromRoot : direct;
    }
}
