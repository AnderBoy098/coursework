package ua.pntu.events.server.dao;

import ua.pntu.events.server.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO таблиці task_dependencies: запис (taskId, dependsOnId) — taskId залежить від dependsOnId. */
public class TaskDependencyDAO {

    public void add(int taskId, int dependsOnId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO task_dependencies (task_id, depends_on_id) VALUES (?, ?)")) {
            ps.setInt(1, taskId);
            ps.setInt(2, dependsOnId);
            ps.executeUpdate();
        }
    }

    public boolean delete(int taskId, int dependsOnId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM task_dependencies WHERE task_id = ? AND depends_on_id = ?")) {
            ps.setInt(1, taskId);
            ps.setInt(2, dependsOnId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean exists(int taskId, int dependsOnId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM task_dependencies WHERE task_id = ? AND depends_on_id = ?")) {
            ps.setInt(1, taskId);
            ps.setInt(2, dependsOnId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Завдання, від яких залежить taskId. */
    public List<Integer> getDependsOnIds(int taskId) throws SQLException {
        return ids("SELECT depends_on_id FROM task_dependencies WHERE task_id = ? ORDER BY depends_on_id", taskId);
    }

    /** Завдання, які залежать від taskId. */
    public List<Integer> getDependentIds(int taskId) throws SQLException {
        return ids("SELECT task_id FROM task_dependencies WHERE depends_on_id = ? ORDER BY task_id", taskId);
    }

    /** Кількість завдань, що залежать від taskId (для фактора F3). */
    public int countDependents(int taskId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM task_dependencies WHERE depends_on_id = ?")) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<Integer> ids(String sql, int param) throws SQLException {
        List<Integer> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getInt(1));
                }
            }
        }
        return list;
    }
}
