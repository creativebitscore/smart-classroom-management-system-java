package com.smartclassroom.service;

import com.smartclassroom.dao.AnnouncementDao;
import com.smartclassroom.dao.AssignmentDao;
import com.smartclassroom.dao.AttendanceDao;
import com.smartclassroom.dao.ClassroomDao;
import com.smartclassroom.dao.LeaveRequestDao;
import com.smartclassroom.dao.RoomBookingDao;
import com.smartclassroom.dao.FacultyDao;
import com.smartclassroom.dao.MarkDao;
import com.smartclassroom.dao.SettingsDao;
import com.smartclassroom.dao.StudentDao;
import com.smartclassroom.dao.SubjectDao;
import com.smartclassroom.dao.TimetableDao;
import com.smartclassroom.dao.UserDao;
import com.smartclassroom.model.Admin;
import com.smartclassroom.model.Announcement;
import com.smartclassroom.model.Assignment;
import com.smartclassroom.model.AttendanceRecord;
import com.smartclassroom.model.AttendanceSummary;
import com.smartclassroom.model.Classroom;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.LeaveRequest;
import com.smartclassroom.model.RequestStatus;
import com.smartclassroom.model.RoomBooking;
import com.smartclassroom.model.Mark;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.service.exception.DataAccessException;
import com.smartclassroom.util.DbConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Hand-written fake DAOs for the unit tests. Each one extends the real DAO but keeps data in memory,
 * so the service rules can be tested without MySQL. Public fields let a test arrange the situation.
 */
final class Fakes {
    private Fakes() {
    }

    /** Runs "transactions" directly (no database) and converts SQLException the same way the real code does. */
    static final class DirectTransactions implements DbConnection.TransactionRunner {
        @Override
        public <T> T run(DbConnection.TransactionalWork<T> work) {
            try {
                return work.execute((Connection) null);
            } catch (SQLException e) {
                throw new DataAccessException("Database operation failed: " + e.getMessage(), e);
            }
        }
    }

    // ---------- sample people ----------

    static Admin admin() {
        Admin a = new Admin();
        a.setUserId(1);
        a.setUsername("admin");
        a.setFullName("System Administrator");
        a.setEmail("admin@college.edu");
        return a;
    }

    static Faculty faculty(int facultyId, int userId, String name) {
        Faculty f = new Faculty();
        f.setFacultyId(facultyId);
        f.setUserId(userId);
        f.setUsername("faculty" + facultyId);
        f.setFullName(name);
        f.setEmail("f" + facultyId + "@college.edu");
        f.setEmployeeCode("EMP00" + facultyId);
        f.setDepartment("CSE");
        f.setDesignation("Professor");
        return f;
    }

    static Student student(int studentId, int userId, String name) {
        Student s = new Student();
        s.setStudentId(studentId);
        s.setUserId(userId);
        s.setUsername("student" + studentId);
        s.setFullName(name);
        s.setEmail("s" + studentId + "@student.edu");
        s.setRollNo("CSE230" + studentId);
        s.setBranch("CSE");
        s.setSemester(3);
        s.setSection("A");
        return s;
    }

    static Student newStudentForm() {
        Student s = new Student();
        s.setFullName("Nisha Gupta");
        s.setUsername("nisha.gupta");
        s.setEmail("nisha@student.edu");
        s.setRollNo("cse2307");
        s.setPhone("9876543210");
        s.setBranch("cse");
        s.setSemester(3);
        s.setSection("a");
        return s;
    }

    static Subject subject(int id, String code, String name) {
        Subject s = new Subject();
        s.setSubjectId(id);
        s.setSubjectCode(code);
        s.setSubjectName(name);
        s.setBranch("CSE");
        s.setSemester(3);
        s.setCredits(4);
        return s;
    }

    static Classroom room(int id, String name, int capacity) {
        Classroom c = new Classroom();
        c.setClassroomId(id);
        c.setRoomName(name);
        c.setCapacity(capacity);
        return c;
    }

    static TimetableEntry entry(Subject subject, Faculty faculty, Classroom room, String section, DayOfWeek day,
                                String start, String end) {
        TimetableEntry e = new TimetableEntry();
        e.setSubject(subject);
        e.setFaculty(faculty);
        e.setClassroom(room);
        e.setSection(section);
        e.setDayOfWeek(day);
        e.setStartTime(LocalTime.parse(start));
        e.setEndTime(LocalTime.parse(end));
        return e;
    }

    // ---------- fake DAOs ----------

    static class FakeUserDao extends UserDao {
        final Map<String, Credentials> accounts = new HashMap<>();
        final Map<Integer, Admin> admins = new HashMap<>();
        final List<String> insertedUsernames = new ArrayList<>();
        final Map<Integer, String> updatedHashes = new HashMap<>();
        int nextId = 100;

        @Override
        public Optional<Credentials> findCredentials(String username) {
            return Optional.ofNullable(accounts.get(username));
        }

        @Override
        public Optional<Admin> findAdmin(int userId) {
            return Optional.ofNullable(admins.get(userId));
        }

        @Override
        public int insert(Connection c, String username, String hash, Role role, String fullName, String email) {
            insertedUsernames.add(username);
            return nextId++;
        }

        @Override
        public void updateProfile(Connection c, int userId, String fullName, String email) {
        }

        @Override
        public void updatePasswordHash(int userId, String passwordHash) {
            updatedHashes.put(userId, passwordHash);
        }
    }

    static class FakeStudentDao extends StudentDao {
        final List<Student> students = new ArrayList<>();
        final List<Student> inserted = new ArrayList<>();
        final List<Student> updated = new ArrayList<>();
        final List<Integer> deleted = new ArrayList<>();
        SQLException failOnInsert;
        int classSize = 6;

        @Override
        public List<Student> findAll() {
            return students;
        }

        @Override
        public Optional<Student> findByUserId(int userId) {
            return students.stream().filter(s -> s.getUserId() == userId).findFirst();
        }

        @Override
        public List<Student> findByClass(String branch, int semester, String section) {
            return students.stream().filter(s -> s.getBranch().equals(branch) && s.getSemester() == semester
                    && s.getSection().equals(section)).toList();
        }

        @Override
        public int countByClass(String branch, int semester, String section) {
            return classSize;
        }

        @Override
        public int insert(Connection c, Student s) throws SQLException {
            if (failOnInsert != null) {
                throw failOnInsert;
            }
            inserted.add(s);
            return 500 + inserted.size();
        }

        @Override
        public void update(Connection c, Student s) {
            updated.add(s);
        }

        @Override
        public void delete(int id) {
            deleted.add(id);
        }
    }

    static class FakeFacultyDao extends FacultyDao {
        final Map<Integer, Faculty> byUserId = new HashMap<>();

        @Override
        public Optional<Faculty> findByUserId(int userId) {
            return Optional.ofNullable(byUserId.get(userId));
        }
    }

    static class FakeSubjectDao extends SubjectDao {
        final Map<Integer, Subject> subjects = new HashMap<>();
        /** facultyId -> subjects that faculty teaches */
        final Map<Integer, List<Subject>> taught = new HashMap<>();

        @Override
        public Optional<Subject> findById(int id) {
            return Optional.ofNullable(subjects.get(id));
        }

        @Override
        public List<Subject> findByFaculty(int facultyId) {
            return taught.getOrDefault(facultyId, List.of());
        }

        @Override
        public List<Subject> findByBranchAndSemester(String branch, int semester) {
            return subjects.values().stream()
                    .filter(s -> s.getBranch().equals(branch) && s.getSemester() == semester)
                    .sorted(java.util.Comparator.comparing(Subject::getSubjectCode)).toList();
        }
    }

    static class FakeTimetableDao extends TimetableDao {
        List<TimetableEntry> roomConflicts = new ArrayList<>();
        List<TimetableEntry> facultyConflicts = new ArrayList<>();
        List<TimetableEntry> classConflicts = new ArrayList<>();
        List<TimetableEntry> facultyEntries = new ArrayList<>();
        final List<TimetableEntry> inserted = new ArrayList<>();
        final List<TimetableEntry> updated = new ArrayList<>();
        int lastExcludeId = -1;

        @Override
        public List<TimetableEntry> findRoomConflicts(int classroomId, DayOfWeek day, LocalTime start, LocalTime end,
                                                      int excludeId) {
            lastExcludeId = excludeId;
            return roomConflicts;
        }

        @Override
        public List<TimetableEntry> findFacultyConflicts(int facultyId, DayOfWeek day, LocalTime start,
                                                         LocalTime end, int excludeId) {
            return facultyConflicts;
        }

        @Override
        public List<TimetableEntry> findClassConflicts(String branch, int semester, String section, DayOfWeek day,
                                                       LocalTime start, LocalTime end, int excludeId) {
            return classConflicts;
        }

        @Override
        public List<TimetableEntry> findByFaculty(int facultyId) {
            return facultyEntries;
        }

        @Override
        public int insert(TimetableEntry e) {
            inserted.add(e);
            return 900 + inserted.size();
        }

        @Override
        public void update(TimetableEntry e) {
            updated.add(e);
        }
    }

    static class FakeAttendanceDao extends AttendanceDao {
        Set<Integer> alreadyMarked = new HashSet<>();
        final List<AttendanceRecord> inserted = new ArrayList<>();
        final List<AttendanceRecord> updated = new ArrayList<>();
        List<AttendanceSummary> summaries = new ArrayList<>();
        SQLException failOnInsert;

        @Override
        public Set<Integer> findMarkedStudentIds(int subjectId, LocalDate date) {
            return alreadyMarked;
        }

        @Override
        public void insertBatch(Connection c, List<AttendanceRecord> records) throws SQLException {
            if (failOnInsert != null) {
                throw failOnInsert;
            }
            inserted.addAll(records);
        }

        @Override
        public void updateStatusBatch(Connection c, List<AttendanceRecord> records) {
            updated.addAll(records);
        }

        @Override
        public List<AttendanceSummary> summarize(Integer studentId, Integer subjectId, String section,
                                                 LocalDate from, LocalDate to) {
            return summaries;
        }
    }

    static class FakeSettingsDao extends SettingsDao {
        final Map<String, String> values = new HashMap<>();

        @Override
        public String get(String key, String defaultValue) {
            return values.getOrDefault(key, defaultValue);
        }

        @Override
        public void set(String key, String value) {
            values.put(key, value);
        }
    }

    static class FakeAssignmentDao extends AssignmentDao {
        final Map<Integer, Assignment> byId = new HashMap<>();
        final List<Assignment> inserted = new ArrayList<>();
        final List<Assignment> updated = new ArrayList<>();
        final List<Integer> deleted = new ArrayList<>();

        @Override
        public Optional<Assignment> findById(int id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public int insert(Assignment a) {
            inserted.add(a);
            return 70 + inserted.size();
        }

        @Override
        public void update(Assignment a) {
            updated.add(a);
        }

        @Override
        public void delete(int id) {
            deleted.add(id);
        }
    }

    static class FakeMarkDao extends MarkDao {
        final List<Mark> saved = new ArrayList<>();
        /** studentId -> marks returned by findByStudent (used by the grade card tests) */
        final Map<Integer, List<Mark>> byStudent = new HashMap<>();

        @Override
        public List<Mark> findByStudent(int studentId) {
            return byStudent.getOrDefault(studentId, List.of());
        }

        @Override
        public void upsertBatch(Connection c, List<Mark> marks) {
            saved.addAll(marks);
        }
    }

    static class FakeAnnouncementDao extends AnnouncementDao {
        final List<Announcement> inserted = new ArrayList<>();
        final List<Integer> deleted = new ArrayList<>();

        @Override
        public int insert(Announcement a) {
            inserted.add(a);
            return 1;
        }

        @Override
        public void delete(int id) {
            deleted.add(id);
        }
    }

    static class FakeLeaveDao extends LeaveRequestDao {
        final Map<Integer, LeaveRequest> byId = new HashMap<>();
        final List<LeaveRequest> inserted = new ArrayList<>();
        List<LeaveRequest> overlapping = new ArrayList<>();
        /** every call to updateStatus is recorded as "id:STATUS:reviewerUserId:remark" */
        final List<String> updates = new ArrayList<>();

        @Override
        public Optional<LeaveRequest> findById(int leaveId) {
            return Optional.ofNullable(byId.get(leaveId));
        }

        @Override
        public List<LeaveRequest> findOverlapping(int studentId, LocalDate from, LocalDate to) {
            return overlapping;
        }

        @Override
        public List<LeaveRequest> findByStudent(int studentId) {
            return new ArrayList<>(byId.values());
        }

        @Override
        public int insert(LeaveRequest r) {
            inserted.add(r);
            return 300 + inserted.size();
        }

        @Override
        public void updateStatus(int leaveId, RequestStatus status, Integer reviewerUserId, String remark) {
            updates.add(leaveId + ":" + status.name() + ":" + reviewerUserId + ":" + remark);
        }
    }

    static class FakeClassroomDao extends ClassroomDao {
        final Map<Integer, Classroom> rooms = new java.util.LinkedHashMap<>();

        @Override
        public List<Classroom> findAll() {
            return new ArrayList<>(rooms.values());
        }

        @Override
        public Optional<Classroom> findById(int id) {
            return Optional.ofNullable(rooms.get(id));
        }
    }

    static class FakeBookingDao extends RoomBookingDao {
        final Map<Integer, RoomBooking> byId = new HashMap<>();
        final List<RoomBooking> inserted = new ArrayList<>();
        List<RoomBooking> approvedConflicts = new ArrayList<>();
        int lastExcludeId = -1;
        final List<String> updates = new ArrayList<>();

        @Override
        public Optional<RoomBooking> findById(int bookingId) {
            return Optional.ofNullable(byId.get(bookingId));
        }

        @Override
        public List<RoomBooking> findApprovedConflicts(int classroomId, LocalDate date, LocalTime start,
                                                       LocalTime end, int excludeBookingId) {
            lastExcludeId = excludeBookingId;
            return approvedConflicts;
        }

        @Override
        public int insert(RoomBooking b) {
            inserted.add(b);
            return 400 + inserted.size();
        }

        @Override
        public void updateStatus(int bookingId, RequestStatus status, String remark) {
            updates.add(bookingId + ":" + status.name() + ":" + remark);
        }
    }
}
