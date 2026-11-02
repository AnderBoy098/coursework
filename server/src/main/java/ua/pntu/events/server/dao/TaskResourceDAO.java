package ua.pntu.events.server.dao;

import ua.pntu.events.server.db.DatabaseConnection;
import ua.pntu.events.server.model.TaskResource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO таблиці task_resources (зв'язок «завдання — ресурс»). */
public class TaskResourceDAO {

    public void add(TaskResource tr) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO task_resources (task_id, resource_id, quantity_needed) VALUES (?, ?, ?)")) {
            ps.setInt(1, tr.getTaskId());
            ps.setInt(2, tr.getResourceId());
            ps.setInt(3, tr.getQuantityNeeded());
            ps.executeUpdate();
        }
    }

    public List<TaskResource> getByTaskId(int taskId) throws SQLException {
        List<TaskResource> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT task_id, resource_id, quantity_needed FROM task_resources WHERE task_id = ? ORDER BY resource_id")) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Optional<TaskResource> getById(int taskId, int resourceId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT task_id, resource_id, quantity_needed FROM task_resources WHERE task_id = ? AND resource_id = ?")) {
            ps.setInt(1, taskId);
            ps.setInt(2, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean updateQuantity(int taskId, int resourceId, int quantity) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE task_resources SET quantity_needed = ? WHERE task_id = ? AND resource_id = ?")) {
            ps.setInt(1, quantity);
            ps.setInt(2, taskId);
            ps.setInt(3, resourceId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int taskId, int resourceId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM task_resources WHERE task_id = ? AND resource_id = ?")) {
            ps.setInt(1, taskId);
            ps.setInt(2, resourceId);
            return ps.executeUpdate() > 0;
        }
    }

    private TaskResource map(ResultSet rs) throws SQLException {
        return new TaskResource(rs.getInt("task_id"), rs.getInt("resource_id"), rs.getInt("quantity_needed"));
    }
}
