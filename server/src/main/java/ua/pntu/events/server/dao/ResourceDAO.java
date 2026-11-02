package ua.pntu.events.server.dao;

import ua.pntu.events.server.db.DatabaseConnection;
import ua.pntu.events.server.model.Resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO таблиці resources. */
public class ResourceDAO {

    private static final String SELECT = "SELECT id, name, resource_type, total_quantity, unit FROM resources";

    public int add(Resource r) throws SQLException {
        String sql = "INSERT INTO resources (name, resource_type, total_quantity, unit) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, r);
            int id = DaoSupport.insertAndGetId(ps);
            r.setId(id);
            return id;
        }
    }

    public List<Resource> getAll() throws SQLException {
        List<Resource> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<Resource> getById(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean update(Resource r) throws SQLException {
        String sql = "UPDATE resources SET name = ?, resource_type = ?, total_quantity = ?, unit = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, r);
            ps.setInt(5, r.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM resources WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private void bind(PreparedStatement ps, Resource r) throws SQLException {
        ps.setString(1, r.getName());
        ps.setString(2, r.getResourceType());
        ps.setInt(3, r.getTotalQuantity());
        ps.setString(4, r.getUnit());
    }

    private Resource map(ResultSet rs) throws SQLException {
        return new Resource(rs.getInt("id"), rs.getString("name"), rs.getString("resource_type"),
                rs.getInt("total_quantity"), rs.getString("unit"));
    }
}
