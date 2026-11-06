package com.smartclassroom.dao;

import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.model.RoomBooking;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for room bookings (one-off use of a room on a given date, on top of the weekly timetable). */
public class RoomBookingDao {
    private static final String SELECT_BASE =
            "SELECT b.booking_id, b.classroom_id, b.faculty_id, b.purpose, b.booking_date, b.start_time, b.end_time, "
                    + "b.expected_attendees, b.status, b.review_remark, "
                    + "c.room_name, c.capacity, u.full_name AS faculty_name "
                    + "FROM room_bookings b "
                    + "JOIN classrooms c ON c.classroom_id = b.classroom_id "
                    + "JOIN faculty f ON f.faculty_id = b.faculty_id "
                    + "JOIN users u ON u.user_id = f.user_id ";
    private static final String ORDER =
            " ORDER BY (b.status = 'PENDING') DESC, b.booking_date, b.start_time, b.booking_id";

    public List<RoomBooking> findAll() {
        return query(SELECT_BASE + ORDER.trim());
    }

    public List<RoomBooking> findByFaculty(int facultyId) {
        return query(SELECT_BASE + "WHERE b.faculty_id = ?" + ORDER, facultyId);
    }

    public Optional<RoomBooking> findById(int bookingId) {
        List<RoomBooking> list = query(SELECT_BASE + "WHERE b.booking_id = ?", bookingId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** APPROVED bookings of a room that overlap the time range on that date. */
    public List<RoomBooking> findApprovedConflicts(int classroomId, LocalDate date, LocalTime start, LocalTime end,
                                                   int excludeBookingId) {
        return query(SELECT_BASE + "WHERE b.classroom_id = ? AND b.booking_date = ? AND b.status = 'APPROVED' "
                        + "AND b.start_time < ? AND b.end_time > ? AND b.booking_id <> ?" + ORDER,
                classroomId, date, end, start, excludeBookingId);
    }

    public int countByStatus(RequestStatus status) {
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM room_bookings WHERE status = ?")) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    public int insert(RoomBooking b) {
        String sql = "INSERT INTO room_bookings (classroom_id, faculty_id, purpose, booking_date, start_time, end_time, "
                + "expected_attendees) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getClassroomId());
            ps.setInt(2, b.getFacultyId());
            ps.setString(3, b.getPurpose());
            ps.setObject(4, b.getBookingDate());
            ps.setObject(5, b.getStartTime());
            ps.setObject(6, b.getEndTime());
            ps.setInt(7, b.getExpectedAttendees());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    public void updateStatus(int bookingId, RequestStatus status, String remark) {
        String sql = "UPDATE room_bookings SET status = ?, review_remark = ? WHERE booking_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, remark);
            ps.setInt(3, bookingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private List<RoomBooking> query(String sql, Object... params) {
        List<RoomBooking> result = new ArrayList<>();
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoomBooking b = new RoomBooking();
                    b.setBookingId(rs.getInt("booking_id"));
                    b.setClassroomId(rs.getInt("classroom_id"));
                    b.setFacultyId(rs.getInt("faculty_id"));
                    b.setPurpose(rs.getString("purpose"));
                    b.setBookingDate(rs.getObject("booking_date", LocalDate.class));
                    b.setStartTime(rs.getObject("start_time", LocalTime.class));
                    b.setEndTime(rs.getObject("end_time", LocalTime.class));
                    b.setExpectedAttendees(rs.getInt("expected_attendees"));
                    b.setStatus(RequestStatus.valueOf(rs.getString("status")));
                    b.setReviewRemark(rs.getString("review_remark"));
                    b.setRoomName(rs.getString("room_name"));
                    b.setRoomCapacity(rs.getInt("capacity"));
                    b.setFacultyName(rs.getString("faculty_name"));
                    result.add(b);
                }
            }
            return result;
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }
}
