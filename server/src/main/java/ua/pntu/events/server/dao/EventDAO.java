package ua.pntu.events.server.dao;

import ua.pntu.events.server.db.DatabaseConnection;
import ua.pntu.events.server.model.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO таблиці events. */
public class EventDAO {

    private static final String SELECT =
            "SELECT id, title, description, location, start_date, end_date, organizer_id FROM events";

    public int add(Event e) throws SQLException {
        String sql = "INSERT INTO events (title, description, location, start_date, end_date, organizer_id) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, e);
            int id = DaoSupport.insertAndGetId(ps);
            e.setId(id);
            return id;
        }
    }

    public List<Event> getAll() throws SQLException {
        List<Event> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY start_date, id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<Event> getById(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean update(Event e) throws SQLException {
        String sql = "UPDATE events SET title = ?, description = ?, location = ?, start_date = ?, "
                + "end_date = ?, organizer_id = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, e);
            ps.setInt(7, e.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM events WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Вузька операція: кількість подій, організованих особою. */
    public int countByOrganizer(int personId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM events WHERE organizer_id = ?")) {
            ps.setInt(1, personId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void bind(PreparedStatement ps, Event e) throws SQLException {
        ps.setString(1, e.getTitle());
        ps.setString(2, e.getDescription());
        ps.setString(3, e.getLocation());
        ps.setObject(4, e.getStartDate());
        ps.setObject(5, e.getEndDate());
        ps.setInt(6, e.getOrganizerId());
    }

    private Event map(ResultSet rs) throws SQLException {
        return new Event(rs.getInt("id"), rs.getString("title"), rs.getString("description"),
                rs.getString("location"), rs.getObject("start_date", LocalDateTime.class),
                rs.getObject("end_date", LocalDateTime.class), rs.getInt("organizer_id"));
    }
}
