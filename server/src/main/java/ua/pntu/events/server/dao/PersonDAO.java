package ua.pntu.events.server.dao;

import ua.pntu.events.server.db.DatabaseConnection;
import ua.pntu.events.server.model.Person;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO таблиці persons. Містить лише SQL/JDBC, без предметної логіки. */
public class PersonDAO {

    private static final String SELECT = "SELECT id, full_name, email, phone, job_title FROM persons";

    public int add(Person p) throws SQLException {
        String sql = "INSERT INTO persons (full_name, email, phone, job_title) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getFullName());
            ps.setString(2, p.getEmail());
            ps.setString(3, p.getPhone());
            ps.setString(4, p.getJobTitle());
            int id = DaoSupport.insertAndGetId(ps);
            p.setId(id);
            return id;
        }
    }

    public List<Person> getAll() throws SQLException {
        List<Person> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<Person> getById(int id) throws SQLException {
        return findOne(SELECT + " WHERE id = ?", id);
    }

    /** Вузька операція: пошук за унікальною поштою. */
    public Optional<Person> findByEmail(String email) throws SQLException {
        return findOne(SELECT + " WHERE email = ?", email);
    }

    public boolean update(Person p) throws SQLException {
        String sql = "UPDATE persons SET full_name = ?, email = ?, phone = ?, job_title = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getFullName());
            ps.setString(2, p.getEmail());
            ps.setString(3, p.getPhone());
            ps.setString(4, p.getJobTitle());
            ps.setInt(5, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM persons WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Optional<Person> findOne(String sql, Object param) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private Person map(ResultSet rs) throws SQLException {
        return new Person(rs.getInt("id"), rs.getString("full_name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("job_title"));
    }
}
