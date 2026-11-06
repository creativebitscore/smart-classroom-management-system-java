package com.smartclassroom.dao;

import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for the timetable, including the queries that detect overlapping bookings. */
public class TimetableDao implements Dao<TimetableEntry> {
    private static final String SELECT_BASE =
            "SELECT t.timetable_id, t.section, t.day_of_week, t.start_time, t.end_time, "
                    + "sub.subject_id, sub.subject_code, sub.subject_name, sub.branch, sub.semester, sub.credits, "
                    + "f.faculty_id, f.user_id AS faculty_user_id, f.employee_code, f.department, f.designation, "
                    + "fu.full_name AS faculty_name, "
                    + "c.classroom_id, c.room_name, c.capacity, c.room_type "
                    + "FROM timetable t "
                    + "JOIN subjects sub ON sub.subject_id = t.subject_id "
                    + "JOIN faculty f ON f.faculty_id = t.faculty_id "
                    + "JOIN users fu ON fu.user_id = f.user_id "
                    + "JOIN classrooms c ON c.classroom_id = t.classroom_id ";
    private static final String ORDER =
            " ORDER BY FIELD(t.day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'), t.start_time";

    /** Two time ranges overlap when each starts before the other ends. */
    private static final String OVERLAP = " AND t.day_of_week = ? AND t.start_time < ? AND t.end_time > ? AND t.timetable_id <> ?";

    @Override
    public List<TimetableEntry> findAll() {
        return query(SELECT_BASE + ORDER);
    }

    @Override
    public Optional<TimetableEntry> findById(int id) {
        List<TimetableEntry> list = query(SELECT_BASE + "WHERE t.timetable_id = ?", id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<TimetableEntry> findByFaculty(int facultyId) {
        return query(SELECT_BASE + "WHERE t.faculty_id = ?" + ORDER, facultyId);
    }

    /** Timetable of a class: subjects of that branch + semester, for one section. */
    public List<TimetableEntry> findForClass(String branch, int semester, String section) {
        return query(SELECT_BASE + "WHERE sub.branch = ? AND sub.semester = ? AND t.section = ?" + ORDER,
                branch, semester, section);
    }

    // ----- conflict queries: each returns the existing entries that overlap the proposed time -----

    public List<TimetableEntry> findRoomConflicts(int classroomId, DayOfWeek day, LocalTime start, LocalTime end,
                                                  int excludeTimetableId) {
        return query(SELECT_BASE + "WHERE t.classroom_id = ?" + OVERLAP + ORDER,
                classroomId, day.name(), end, start, excludeTimetableId);
    }

    public List<TimetableEntry> findFacultyConflicts(int facultyId, DayOfWeek day, LocalTime start, LocalTime end,
                                                     int excludeTimetableId) {
        return query(SELECT_BASE + "WHERE t.faculty_id = ?" + OVERLAP + ORDER,
                facultyId, day.name(), end, start, excludeTimetableId);
    }

    /** A class (branch + semester + section) cannot attend two lectures at the same time either. */
    public List<TimetableEntry> findClassConflicts(String branch, int semester, String section, DayOfWeek day,
                                                   LocalTime start, LocalTime end, int excludeTimetableId) {
        return query(SELECT_BASE + "WHERE sub.branch = ? AND sub.semester = ? AND t.section = ?" + OVERLAP + ORDER,
                branch, semester, section, day.name(), end, start, excludeTimetableId);
    }

    public int insert(TimetableEntry e) {
        String sql = "INSERT INTO timetable (subject_id, faculty_id, classroom_id, section, day_of_week, start_time, end_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, e);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException ex) {
            throw DaoSupport.error(ex);
        }
    }

    public void update(TimetableEntry e) {
        String sql = "UPDATE timetable SET subject_id = ?, faculty_id = ?, classroom_id = ?, section = ?, "
                + "day_of_week = ?, start_time = ?, end_time = ? WHERE timetable_id = ?";
        try (Connection c = DbConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, e);
            ps.setInt(8, e.getTimetableId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw DaoSupport.error(ex);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM timetable WHERE timetable_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw DaoSupport.error(e);
        }
    }

    private static void bind(PreparedStatement ps, TimetableEntry e) throws SQLException {
        ps.setInt(1, e.getSubject().getSubjectId());
        ps.setInt(2, e.getFaculty().getFacultyId());
        ps.setInt(3, e.getClassroom().getClassroomId());
        ps.setString(4, e.getSection());
        ps.setString(5, e.getDayOfWeek().name());
        ps.setObject(6, e.getStartTime());
        ps.setObject(7, e.getEndTime());
    }

    private List<TimetableEntry> query(String sql, Object... params) {
        List<TimetableEntry> result = new ArrayList<>();
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

    /** Builds the TimetableEntry together with its Subject, Faculty and Classroom parts (composition). */
    private static TimetableEntry map(ResultSet rs) throws SQLException {
        Subject subject = new Subject();
        subject.setSubjectId(rs.getInt("subject_id"));
        subject.setSubjectCode(rs.getString("subject_code"));
        subject.setSubjectName(rs.getString("subject_name"));
        subject.setBranch(rs.getString("branch"));
        subject.setSemester(rs.getInt("semester"));
        subject.setCredits(rs.getInt("credits"));

        Faculty faculty = new Faculty();
        faculty.setFacultyId(rs.getInt("faculty_id"));
        faculty.setUserId(rs.getInt("faculty_user_id"));
        faculty.setFullName(rs.getString("faculty_name"));
        faculty.setEmployeeCode(rs.getString("employee_code"));
        faculty.setDepartment(rs.getString("department"));
        faculty.setDesignation(rs.getString("designation"));

        Classroom room = new Classroom();
        room.setClassroomId(rs.getInt("classroom_id"));
        room.setRoomName(rs.getString("room_name"));
        room.setCapacity(rs.getInt("capacity"));
        room.setRoomType(Classroom.RoomType.valueOf(rs.getString("room_type")));

        TimetableEntry entry = new TimetableEntry();
        entry.setTimetableId(rs.getInt("timetable_id"));
        entry.setSubject(subject);
        entry.setFaculty(faculty);
        entry.setClassroom(room);
        entry.setSection(rs.getString("section"));
        entry.setDayOfWeek(DayOfWeek.valueOf(rs.getString("day_of_week")));
        entry.setStartTime(rs.getObject("start_time", LocalTime.class));
        entry.setEndTime(rs.getObject("end_time", LocalTime.class));
        return entry;
    }
}
