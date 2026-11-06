package com.smartclassroom.dao;

import com.smartclassroom.model.Assignment;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for assignments. */
public class AssignmentDao implements Dao<Assignment> {
    private static final String SELECT_BASE =
            "SELECT a.assignment_id, a.subject_id, a.faculty_id, a.title, a.description, a.due_date, "
                    + "sub.subject_name, u.full_name AS faculty_name "
                    + "FROM assignments a "
                    + "JOIN subjects sub ON sub.subject_id = a.subject_id "
                    + "JOIN faculty f ON f.faculty_id = a.faculty_id "
                    + "JOIN users u ON u.user_id = f.user_id ";

    @Override
    public List<Assignment> findAll() {
        return query(SELECT_BASE + "ORDER BY a.due_date");
    }

    @Override
    public Optional<Assignment> findById(int id) {
        List<Assignment> list = query(SELECT_BASE + "WHERE a.assignment_id = ?", id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Assignment> findByFaculty(int facultyId) {
        return query(SELECT_BASE + "WHERE a.faculty_id = ? ORDER BY a.due_date", facultyId);
    }

    /** Assignments of the subjects that belong to a student's branch and semester. */
    public List<Assignment> findForClass(String branch, int semester) {
        return query(SELECT_BASE + "WHERE sub.branch = ? AND sub.semester = ? ORDER BY a.due_date", branch, semester);
    }

    public int insert(Assignment a) {
        String sql = "INSERT INTO assignments (subject_id, faculty_id, title, description, due_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getSubjectId());
            ps.setInt(2, a.getFacultyId());
            ps.setString(3, a.getTitle());
            ps.setString(4, a.getDescription());
            ps.setObject(5, a.getDueDate());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    public void update(Assignment a) {
        String sql = "UPDATE assignments SET subject_id = ?, title = ?, description = ?, due_date = ? WHERE assignment_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, a.getSubjectId());
            ps.setString(2, a.getTitle());
            ps.setString(3, a.getDescription());
            ps.setObject(4, a.getDueDate());
            ps.setInt(5, a.getAssignmentId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM assignments WHERE assignment_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private List<Assignment> query(String sql, Object... params) {
        List<Assignment> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Assignment a = new Assignment();
                    a.setAssignmentId(rs.getInt("assignment_id"));
                    a.setSubjectId(rs.getInt("subject_id"));
                    a.setFacultyId(rs.getInt("faculty_id"));
                    a.setTitle(rs.getString("title"));
                    a.setDescription(rs.getString("description"));
                    a.setDueDate(rs.getObject("due_date", LocalDate.class));
                    a.setSubjectName(rs.getString("subject_name"));
                    a.setFacultyName(rs.getString("faculty_name"));
                    result.add(a);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }
}
