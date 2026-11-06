package com.smartclassroom.dao;

import com.smartclassroom.model.Student;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for students (joins the students and users tables). */
public class StudentDao implements Dao<Student> {
    private static final String SELECT_BASE =
            "SELECT u.user_id, u.username, u.full_name, u.email, "
                    + "s.student_id, s.roll_no, s.phone, s.branch, s.semester, s.section "
                    + "FROM students s JOIN users u ON u.user_id = s.user_id ";

    @Override
    public List<Student> findAll() {
        return query(SELECT_BASE + "ORDER BY s.roll_no");
    }

    @Override
    public Optional<Student> findById(int studentId) {
        return first(query(SELECT_BASE + "WHERE s.student_id = ?", studentId));
    }

    public Optional<Student> findByUserId(int userId) {
        return first(query(SELECT_BASE + "WHERE u.user_id = ?", userId));
    }

    /** Students of one class: same branch, semester and section. */
    public List<Student> findByClass(String branch, int semester, String section) {
        return query(SELECT_BASE + "WHERE s.branch = ? AND s.semester = ? AND s.section = ? ORDER BY s.roll_no",
                branch, semester, section);
    }

    public int countByClass(String branch, int semester, String section) {
        String sql = "SELECT COUNT(*) FROM students WHERE branch = ? AND semester = ? AND section = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, branch);
            ps.setInt(2, semester);
            ps.setString(3, section);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    /** Inserts the students row (the users row must already exist: student.getUserId()). */
    public int insert(Connection c, Student s) throws SQLException {
        String sql = "INSERT INTO students (user_id, roll_no, phone, branch, semester, section) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getUserId());
            ps.setString(2, s.getRollNo());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getBranch());
            ps.setInt(5, s.getSemester());
            ps.setString(6, s.getSection());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void update(Connection c, Student s) throws SQLException {
        String sql = "UPDATE students SET roll_no = ?, phone = ?, branch = ?, semester = ?, section = ? WHERE student_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getRollNo());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getBranch());
            ps.setInt(4, s.getSemester());
            ps.setString(5, s.getSection());
            ps.setInt(6, s.getStudentId());
            ps.executeUpdate();
        }
    }

    /** Deletes the user row; ON DELETE CASCADE removes the student profile, attendance and marks too. */
    @Override
    public void delete(int studentId) {
        String sql = "DELETE u FROM users u JOIN students s ON s.user_id = u.user_id WHERE s.student_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private List<Student> query(String sql, Object... params) {
        List<Student> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private static Optional<Student> first(List<Student> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private static Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setUserId(rs.getInt("user_id"));
        s.setUsername(rs.getString("username"));
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setStudentId(rs.getInt("student_id"));
        s.setRollNo(rs.getString("roll_no"));
        s.setPhone(rs.getString("phone"));
        s.setBranch(rs.getString("branch"));
        s.setSemester(rs.getInt("semester"));
        s.setSection(rs.getString("section"));
        return s;
    }
}
