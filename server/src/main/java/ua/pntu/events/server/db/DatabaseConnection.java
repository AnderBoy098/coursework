package ua.pntu.events.server.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Підключення до MySQL. Налаштування читаються з db.properties (classpath),
 * а за наявності файлу db.local.properties у робочому каталозі — перевизначаються ним.
 * Жорстко заданих абсолютних шляхів немає.
 */
public final class DatabaseConnection {

    private static final Logger LOG = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String DEFAULTS = "db.properties";
    private static final String LOCAL = "db.local.properties";

    private static final Properties PROPS = loadProperties();

    private DatabaseConnection() {
    }

    private static Properties loadProperties() {
        Properties p = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(DEFAULTS)) {
            if (in == null) {
                throw new IllegalStateException("Не знайдено " + DEFAULTS + " у ресурсах");
            }
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Помилка читання " + DEFAULTS, e);
        }
        Path local = Path.of(LOCAL);
        if (Files.exists(local)) {
            try (InputStream in = Files.newInputStream(local)) {
                p.load(in);
                LOG.info("Застосовано локальні налаштування " + LOCAL);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Не вдалося прочитати " + LOCAL, e);
            }
        }
        return p;
    }

    /** Створює нове з'єднання з БД. Закривати має викликач (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(
                    PROPS.getProperty("db.url"),
                    PROPS.getProperty("db.user"),
                    PROPS.getProperty("db.password", ""));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Не вдалося підключитися до БД: " + e.getMessage(), e);
            throw e;
        }
    }
}
