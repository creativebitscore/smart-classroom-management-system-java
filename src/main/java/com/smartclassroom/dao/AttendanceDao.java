package com.smartclassroom.dao;

import com.smartclassroom.model.AttendanceRecord;
import com.smartclassroom.model.AttendanceStatus;
import com.smartclassroom.model.AttendanceSummary;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** SQL for attendance. The UNIQUE(student, subject, date) key in the database is the last line of defence against duplicates. */
public class AttendanceDao {
    private static final String RECORD_SELECT =
            "SELECT a.attendance_id, a.student_id, a.subject_id, a.attendance_date, a.status, a.marked_by, "
                    + "u.full_name AS student_name, s.roll_no, sub.subject_name "
                    + "FROM attendance a "
                    + "JOIN students s ON s.student_id = a.student_id "
                    + "JOIN users u ON u.user_id = s.user_id "
                    + "JOIN subjects sub ON sub.subject_id = a.subject_id ";

    /** Ids of students who already have attendance for this subject on this date. */
    public Set<Integer> findMarkedStudentIds(int subjectId, LocalDate date) {
        String sql = "SELECT student_id FROM attendance WHERE subject_id = ? AND attendance_date = ?";
        Set<Integer> ids = new HashSet<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
            return ids;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    /** Inserts many records in the transaction owned by the caller. */
    public void insertBatch(Connection c, List<AttendanceRecord> records) throws SQLException {
        String sql = "INSERT INTO attendance (student_id, subject_id, attendance_date, status, marked_by) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (AttendanceRecord r : records) {
                ps.setInt(1, r.getStudentId());
                ps.setInt(2, r.getSubjectId());
                ps.setObject(3, r.getDate());
                ps.setString(4, r.getStatus().name());
                ps.setInt(5, r.getMarkedBy());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Corrects the status of existing records in the transaction owned by the caller. */
    public void updateStatusBatch(Connection c, List<AttendanceRecord> records) throws SQLException {
        String sql = "UPDATE attendance SET status = ?, marked_by = ? WHERE student_id = ? AND subject_id = ? AND attendance_date = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (AttendanceRecord r : records) {
                ps.setString(1, r.getStatus().name());
                ps.setInt(2, r.getMarkedBy());
                ps.setInt(3, r.getStudentId());
                ps.setInt(4, r.getSubjectId());
                ps.setObject(5, r.getDate());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Existing records of one class for a subject and date (used to show/correct what was already marked). */
    public List<AttendanceRecord> findBySubjectDateAndSection(int subjectId, LocalDate date, String section) {
        return queryRecords(RECORD_SELECT + "WHERE a.subject_id = ? AND a.attendance_date = ? AND s.section = ? "
                + "ORDER BY s.roll_no", subjectId, date, section);
    }

    /** Flexible report query; any filter may be null. */
    public List<AttendanceRecord> findReport(Integer studentId, Integer subjectId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(RECORD_SELECT).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, studentId, subjectId, null, from, to);
        sql.append("ORDER BY a.attendance_date DESC, s.roll_no");
        return queryRecords(sql.toString(), params.toArray());
    }

    /** Per student and subject totals. Any filter may be null. */
    public List<AttendanceSummary> summarize(Integer studentId, Integer subjectId, String section,
                                             LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.student_id, s.roll_no, u.full_name, sub.subject_code, sub.subject_name, "
                        + "COUNT(*) AS total, "
                        + "SUM(CASE WHEN a.status IN ('PRESENT','LATE') THEN 1 ELSE 0 END) AS attended, "
                        + "SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) AS late_count "
                        + "FROM attendance a "
                        + "JOIN students s ON s.student_id = a.student_id "
                        + "JOIN users u ON u.user_id = s.user_id "
                        + "JOIN subjects sub ON sub.subject_id = a.subject_id "
                        + "WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, studentId, subjectId, section, from, to);
        sql.append("GROUP BY s.student_id, s.roll_no, u.full_name, sub.subject_id, sub.subject_code, sub.subject_name "
                + "ORDER BY s.roll_no, sub.subject_code");

        List<AttendanceSummary> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new AttendanceSummary(rs.getInt("student_id"), rs.getString("roll_no"),
                            rs.getString("full_name"), rs.getString("subject_code"), rs.getString("subject_name"),
                            rs.getInt("total"), rs.getInt("attended"), rs.getInt("late_count")));
                }
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    /** Attendance percentage over all records in the system (0 when nothing is recorded). */
    public double overallPercentage() {
        String sql = "SELECT COUNT(*), SUM(CASE WHEN status IN ('PRESENT','LATE') THEN 1 ELSE 0 END) FROM attendance";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return AttendanceSummary.calculatePercentage(rs.getInt(2), rs.getInt(1));
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private static void appendFilters(StringBuilder sql, List<Object> params, Integer studentId, Integer subjectId,
                                      String section, LocalDate from, LocalDate to) {
        if (studentId != null) {
            sql.append("AND a.student_id = ? ");
            params.add(studentId);
        }
        if (subjectId != null) {
            sql.append("AND a.subject_id = ? ");
            params.add(subjectId);
        }
        if (section != null) {
            sql.append("AND s.section = ? ");
            params.add(section);
        }
        if (from != null) {
            sql.append("AND a.attendance_date >= ? ");
            params.add(from);
        }
        if (to != null) {
            sql.append("AND a.attendance_date <= ? ");
            params.add(to);
        }
    }

    private List<AttendanceRecord> queryRecords(String sql, Object... params) {
        List<AttendanceRecord> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceRecord r = new AttendanceRecord();
                    r.setAttendanceId(rs.getInt("attendance_id"));
                    r.setStudentId(rs.getInt("student_id"));
                    r.setSubjectId(rs.getInt("subject_id"));
                    r.setDate(rs.getObject("attendance_date", LocalDate.class));
                    r.setStatus(AttendanceStatus.valueOf(rs.getString("status")));
                    r.setMarkedBy(rs.getInt("marked_by"));
                    r.setStudentName(rs.getString("student_name"));
                    r.setRollNo(rs.getString("roll_no"));
                    r.setSubjectName(rs.getString("subject_name"));
                    result.add(r);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }
}
