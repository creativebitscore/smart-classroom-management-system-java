package com.smartclassroom.dao;

import com.smartclassroom.model.Subject;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for the subjects table. */
public class SubjectDao implements Dao<Subject> {
    private static final String COLUMNS = "subject_id, subject_code, subject_name, branch, semester, credits";

    @Override
    public List<Subject> findAll() {
        return query("SELECT " + COLUMNS + " FROM subjects ORDER BY subject_code");
    }

    @Override
    public Optional<Subject> findById(int id) {
        List<Subject> list = query("SELECT " + COLUMNS + " FROM subjects WHERE subject_id = ?", id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Subject> findByBranchAndSemester(String branch, int semester) {
        return query("SELECT " + COLUMNS + " FROM subjects WHERE branch = ? AND semester = ? ORDER BY subject_code",
                branch, semester);
    }

    /** Subjects a faculty member is assigned to teach (derived from the timetable). */
    public List<Subject> findByFaculty(int facultyId) {
        return query("SELECT DISTINCT s.subject_id, s.subject_code, s.subject_name, s.branch, s.semester, s.credits "
                + "FROM subjects s JOIN timetable t ON t.subject_id = s.subject_id "
                + "WHERE t.faculty_id = ? ORDER BY s.subject_code", facultyId);
    }

    public int insert(Subject s) {
        String sql = "INSERT INTO subjects (subject_code, subject_name, branch, semester, credits) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getSubjectCode());
            ps.setString(2, s.getSubjectName());
            ps.setString(3, s.getBranch());
            ps.setInt(4, s.getSemester());
            ps.setInt(5, s.getCredits());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    public void update(Subject s) {
        String sql = "UPDATE subjects SET subject_code = ?, subject_name = ?, branch = ?, semester = ?, credits = ? "
                + "WHERE subject_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getSubjectCode());
            ps.setString(2, s.getSubjectName());
            ps.setString(3, s.getBranch());
            ps.setInt(4, s.getSemester());
            ps.setInt(5, s.getCredits());
            ps.setInt(6, s.getSubjectId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM subjects WHERE subject_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private List<Subject> query(String sql, Object... params) {
        List<Subject> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Subject s = new Subject();
                    s.setSubjectId(rs.getInt("subject_id"));
                    s.setSubjectCode(rs.getString("subject_code"));
                    s.setSubjectName(rs.getString("subject_name"));
                    s.setBranch(rs.getString("branch"));
                    s.setSemester(rs.getInt("semester"));
                    s.setCredits(rs.getInt("credits"));
                    result.add(s);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }
}
