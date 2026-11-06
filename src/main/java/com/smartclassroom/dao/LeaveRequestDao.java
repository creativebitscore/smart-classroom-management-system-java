package com.smartclassroom.dao;

import com.smartclassroom.model.LeaveRequest;
import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for leave requests. */
public class LeaveRequestDao {
    private static final String SELECT_BASE =
            "SELECT l.leave_id, l.student_id, l.from_date, l.to_date, l.reason, l.status, l.review_remark, "
                    + "l.created_at, su.full_name AS student_name, s.roll_no, ru.full_name AS reviewer_name "
                    + "FROM leave_requests l "
                    + "JOIN students s ON s.student_id = l.student_id "
                    + "JOIN users su ON su.user_id = s.user_id "
                    + "LEFT JOIN users ru ON ru.user_id = l.reviewed_by ";
    /** Pending requests first (they need action), then newest first. */
    private static final String ORDER = " ORDER BY (l.status = 'PENDING') DESC, l.created_at DESC, l.leave_id DESC";

    public List<LeaveRequest> findAll() {
        return query(SELECT_BASE + ORDER.trim());
    }

    public List<LeaveRequest> findByStatus(RequestStatus status) {
        return query(SELECT_BASE + "WHERE l.status = ?" + ORDER, status.name());
    }

    public List<LeaveRequest> findByStudent(int studentId) {
        return query(SELECT_BASE + "WHERE l.student_id = ?" + ORDER, studentId);
    }

    public Optional<LeaveRequest> findById(int leaveId) {
        List<LeaveRequest> list = query(SELECT_BASE + "WHERE l.leave_id = ?", leaveId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Pending or approved requests of this student that share at least one day with from..to. */
    public List<LeaveRequest> findOverlapping(int studentId, LocalDate from, LocalDate to) {
        return query(SELECT_BASE + "WHERE l.student_id = ? AND l.status IN ('PENDING','APPROVED') "
                + "AND l.from_date <= ? AND l.to_date >= ?" + ORDER, studentId, to, from);
    }

    public int countByStatus(RequestStatus status) {
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM leave_requests WHERE status = ?")) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    public int insert(LeaveRequest r) {
        String sql = "INSERT INTO leave_requests (student_id, from_date, to_date, reason) VALUES (?, ?, ?, ?)";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getStudentId());
            ps.setObject(2, r.getFromDate());
            ps.setObject(3, r.getToDate());
            ps.setString(4, r.getReason());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    /** Records a decision. reviewerUserId is null when the student cancels his or her own request. */
    public void updateStatus(int leaveId, RequestStatus status, Integer reviewerUserId, String remark) {
        String sql = "UPDATE leave_requests SET status = ?, reviewed_by = ?, review_remark = ? WHERE leave_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setObject(2, reviewerUserId);
            ps.setString(3, remark);
            ps.setInt(4, leaveId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private List<LeaveRequest> query(String sql, Object... params) {
        List<LeaveRequest> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LeaveRequest r = new LeaveRequest();
                    r.setLeaveId(rs.getInt("leave_id"));
                    r.setStudentId(rs.getInt("student_id"));
                    r.setFromDate(rs.getObject("from_date", LocalDate.class));
                    r.setToDate(rs.getObject("to_date", LocalDate.class));
                    r.setReason(rs.getString("reason"));
                    r.setStatus(RequestStatus.valueOf(rs.getString("status")));
                    r.setReviewRemark(rs.getString("review_remark"));
                    Timestamp created = rs.getTimestamp("created_at");
                    r.setCreatedAt(created == null ? null : created.toLocalDateTime());
                    r.setStudentName(rs.getString("student_name"));
                    r.setRollNo(rs.getString("roll_no"));
                    r.setReviewerName(rs.getString("reviewer_name"));
                    result.add(r);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }
}
