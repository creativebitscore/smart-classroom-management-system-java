package com.smartclassroom.dao;

import com.smartclassroom.model.Mark;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL for marks. One row per student + subject + exam type. */
public class MarkDao {
    private static final String SELECT_BASE =
            "SELECT m.mark_id, m.student_id, m.subject_id, m.exam_type, m.marks_obtained, m.max_marks, "
                    + "u.full_name AS student_name, s.roll_no, sub.subject_name "
                    + "FROM marks m "
                    + "JOIN students s ON s.student_id = m.student_id "
                    + "JOIN users u ON u.user_id = s.user_id "
                    + "JOIN subjects sub ON sub.subject_id = m.subject_id ";

    /** Inserts new marks or updates existing ones (same student + subject + exam) inside the caller's transaction. */
    public void upsertBatch(Connection c, List<Mark> marks) throws SQLException {
        String sql = "INSERT INTO marks (student_id, subject_id, exam_type, marks_obtained, max_marks) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE marks_obtained = VALUES(marks_obtained), max_marks = VALUES(max_marks)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Mark m : marks) {
                ps.setInt(1, m.getStudentId());
                ps.setInt(2, m.getSubjectId());
                ps.setString(3, m.getExamType());
                ps.setDouble(4, m.getMarksObtained());
                ps.setDouble(5, m.getMaxMarks());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Mark> findByStudent(int studentId) {
        return query(SELECT_BASE + "WHERE m.student_id = ? ORDER BY sub.subject_name, m.exam_type", studentId);
    }

    public List<Mark> findBySubjectAndExam(int subjectId, String examType) {
        return query(SELECT_BASE + "WHERE m.subject_id = ? AND m.exam_type = ? ORDER BY s.roll_no", subjectId, examType);
    }

    /** Average percentage of all marks recorded for a subject; null if there are none. */
    public Double averagePercentage(int subjectId) {
        String sql = "SELECT AVG(marks_obtained * 100 / max_marks) FROM marks WHERE subject_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                double value = rs.getDouble(1);
                return rs.wasNull() ? null : value;
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private List<Mark> query(String sql, Object... params) {
        List<Mark> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Mark m = new Mark();
                    m.setMarkId(rs.getInt("mark_id"));
                    m.setStudentId(rs.getInt("student_id"));
                    m.setSubjectId(rs.getInt("subject_id"));
                    m.setExamType(rs.getString("exam_type"));
                    m.setMarksObtained(rs.getDouble("marks_obtained"));
                    m.setMaxMarks(rs.getDouble("max_marks"));
                    m.setStudentName(rs.getString("student_name"));
                    m.setRollNo(rs.getString("roll_no"));
                    m.setSubjectName(rs.getString("subject_name"));
                    result.add(m);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }
}
