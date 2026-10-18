package ua.pntu.events.server.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Допоміжна програма етапу 1: перевірка JDBC-з'єднання та наявності даних. */
public class ConnectionCheck {

    public static void main(String[] args) {
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement()) {
            System.out.println("З'єднання встановлено: " + c.getMetaData().getDatabaseProductName()
                    + " " + c.getMetaData().getDatabaseProductVersion());
            for (String table : List.of("persons", "events", "tasks", "resources",
                    "task_resources", "task_dependencies")) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rs.next();
                    System.out.printf("  %-18s %d записів%n", table, rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка підключення: " + e.getMessage());
            System.exit(1);
        }
    }
}
