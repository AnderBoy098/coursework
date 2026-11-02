package ua.pntu.events.server.dao;

import ua.pntu.events.server.db.DatabaseConnection;
import ua.pntu.events.server.model.Task;
import ua.pntu.events.server.model.TaskStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO таблиці tasks. Правила життєвого циклу тут не перевіряються — це робота Service. */
public class TaskDAO {

    private static final String SELECT = "SELECT id, event_id, title, description, deadline, criticality, "
            + "status, assignee_id, priority_index, created_at FROM tasks";

    public int add(Task t) throws SQLException {
        String sql = "INSERT INTO tasks (event_id, title, description, deadline, criticality, status, "
                + "assignee_id, priority_index) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getEventId());
            ps.setString(2, t.getTitle());
            ps.setString(3, t.getDescription());
            ps.setObject(4, t.getDeadline());
            ps.setInt(5, t.getCriticality());
            ps.setString(6, t.getStatus().name());
            DaoSupport.setNullableInt(ps, 7, t.getAssigneeId());
            DaoSupport.setNullableDecimal(ps, 8, t.getPriorityIndex());
            int id = DaoSupport.insertAndGetId(ps);
            t.setId(id);
            return id;
        }
    }

    public List<Task> getAll() throws SQLException {
        return query(SELECT + " ORDER BY id", null);
    }

    public List<Task> getByEventId(int eventId) throws SQLException {
        return query(SELECT + " WHERE event_id = ? ORDER BY id", eventId);
    }

    public Optional<Task> getById(int id) throws SQLException {
        List<Task> list = query(SELECT + " WHERE id = ?", id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Редагує лише дані завдання; стан, виконавець і показник змінюються окремими операціями. */
    public boolean update(Task t) throws SQLException {
        String sql = "UPDATE tasks SET event_id = ?, title = ?, description = ?, deadline = ?, "
                + "criticality = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, t.getEventId());
            ps.setString(2, t.getTitle());
            ps.setString(3, t.getDescription());
            ps.setObject(4, t.getDeadline());
            ps.setInt(5, t.getCriticality());
            ps.setInt(6, t.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Вузька операція: зберегти новий стан. */
    public boolean updateStatus(int id, TaskStatus status) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE tasks SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Вузька операція: зберегти виконавця разом зі станом. */
    public boolean updateAssigneeAndStatus(int id, Integer assigneeId, TaskStatus status) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE tasks SET assignee_id = ?, status = ? WHERE id = ?")) {
            DaoSupport.setNullableInt(ps, 1, assigneeId);
            ps.setString(2, status.name());
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Вузька операція: зберегти інтегральний показник (null — показник не застосовується). */
    public boolean updatePriorityIndex(int id, Double value) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE tasks SET priority_index = ? WHERE id = ?")) {
            DaoSupport.setNullableDecimal(ps, 1, value);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Вузька операція: кількість незавершених завдань, призначених особі. */
    public int countActiveByAssignee(int personId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND status <> 'DONE'")) {
            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<Task> query(String sql, Integer param) throws SQLException {
        List<Task> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Task map(ResultSet rs) throws SQLException {
        return new Task(rs.getInt("id"), rs.getInt("event_id"), rs.getString("title"),
                rs.getString("description"), rs.getObject("deadline", LocalDateTime.class),
                rs.getInt("criticality"), TaskStatus.valueOf(rs.getString("status")),
                DaoSupport.getNullableInt(rs, "assignee_id"),
                DaoSupport.getNullableDouble(rs, "priority_index"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
