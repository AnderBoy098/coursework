package ua.pntu.events.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Налаштування журналу клієнта: консоль + файл logs/client.log (відносний шлях, ігнорується git).
 * У журнал потрапляють події імпорту CSV та помилки запитів; паролі й токени клієнт не обробляє.
 */
public final class ClientLogging {

    private static boolean initialized;

    private ClientLogging() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT [%4$s] %3$s: %5$s%6$s%n");
        Logger root = Logger.getLogger("ua.pntu.events");
        root.setLevel(Level.INFO);
        try {
            Path dir = Path.of("logs");
            Files.createDirectories(dir);
            FileHandler fh = new FileHandler(dir.resolve("client.log").toString(), true);
            fh.setEncoding("UTF-8");
            fh.setFormatter(new SimpleFormatter());
            root.addHandler(fh);
        } catch (IOException e) {
            root.log(Level.WARNING, "Не вдалося створити файл журналу клієнта: " + e.getMessage());
        }
        initialized = true;
    }
}
