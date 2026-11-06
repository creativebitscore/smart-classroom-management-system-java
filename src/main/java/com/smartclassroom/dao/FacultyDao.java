package com.smartclassroom.dao;

import com.smartclassroom.model.Faculty;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for faculty (joins the faculty and users tables). */
public class FacultyDao implements Dao<Faculty> {
    private static final String SELECT_BASE =
            "SELECT u.user_id, u.username, u.full_name, u.email, "
                    + "f.faculty_id, f.employee_code, f.phone, f.department, f.designation "
                    + "FROM faculty f JOIN users u ON u.user_id = f.user_id ";

    @Override
    public List<Faculty> findAll() {
        return query(SELECT_BASE + "ORDER BY u.full_name");
    }

    @Override
    public Optional<Faculty> findById(int facultyId) {
        List<Faculty> list = query(SELECT_BASE + "WHERE f.faculty_id = ?", facultyId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<Faculty> findByUserId(int userId) {
        List<Faculty> list = query(SELECT_BASE + "WHERE u.user_id = ?", userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int insert(Connection c, Faculty f) throws SQLException {
        String sql = "INSERT INTO faculty (user_id, employee_code, phone, department, designation) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, f.getUserId());
            ps.setString(2, f.getEmployeeCode());
            ps.setString(3, f.getPhone());
            ps.setString(4, f.getDepartment());
            ps.setString(5, f.getDesignation());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void update(Connection c, Faculty f) throws SQLException {
        String sql = "UPDATE faculty SET employee_code = ?, phone = ?, department = ?, designation = ? WHERE faculty_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, f.getEmployeeCode());
            ps.setString(2, f.getPhone());
            ps.setString(3, f.getDepartment());
            ps.setString(4, f.getDesignation());
            ps.setInt(5, f.getFacultyId());
            ps.executeUpdate();
        }
    }

    /** Deletes the user row (cascade removes the faculty row). Fails with a foreign-key error if attendance was marked by this faculty. */
    @Override
    public void delete(int facultyId) {
        String sql = "DELETE u FROM users u JOIN faculty f ON f.user_id = u.user_id WHERE f.faculty_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, facultyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private List<Faculty> query(String sql, Object... params) {
        List<Faculty> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Faculty f = new Faculty();
                    f.setUserId(rs.getInt("user_id"));
                    f.setUsername(rs.getString("username"));
                    f.setFullName(rs.getString("full_name"));
                    f.setEmail(rs.getString("email"));
                    f.setFacultyId(rs.getInt("faculty_id"));
                    f.setEmployeeCode(rs.getString("employee_code"));
                    f.setPhone(rs.getString("phone"));
                    f.setDepartment(rs.getString("department"));
                    f.setDesignation(rs.getString("designation"));
                    result.add(f);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }
}
