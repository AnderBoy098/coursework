package ua.pntu.events.server.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Налаштування логування сервера: консоль + файл logs/server.log (відносний шлях, ігнорується git). */
public final class ServerLogging {

    private static boolean initialized;

    private ServerLogging() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT [%4$s] %3$s: %5$s%6$s%n");
        Logger root = Logger.getLogger("ua.pntu.events");
        root.setLevel(Level.INFO);
        try {
            Path dir = Path.of("logs");
            Files.createDirectories(dir);
            FileHandler fh = new FileHandler(dir.resolve("server.log").toString(), true);
            fh.setEncoding("UTF-8");
            fh.setFormatter(new SimpleFormatter());
            root.addHandler(fh);
        } catch (IOException e) {
            root.log(Level.WARNING, "Не вдалося створити файл журналу: " + e.getMessage());
        }
        initialized = true;
    }
}
