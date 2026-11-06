package com.smartclassroom.service;

import com.smartclassroom.dao.AttendanceDao;
import com.smartclassroom.dao.SettingsDao;
import com.smartclassroom.dao.StudentDao;
import com.smartclassroom.dao.SubjectDao;
import com.smartclassroom.dao.TimetableDao;
import com.smartclassroom.model.AttendanceRecord;
import com.smartclassroom.model.AttendanceStatus;
import com.smartclassroom.model.AttendanceSummary;
import com.smartclassroom.model.Faculty;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.TimetableEntry;
import com.smartclassroom.model.User;
import com.smartclassroom.service.exception.ConflictException;
import com.smartclassroom.service.exception.DataAccessException;
import com.smartclassroom.service.exception.ValidationException;
import com.smartclassroom.util.DbConnection;
import com.smartclassroom.util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Attendance rules: who may mark, no duplicates, percentages and the low-attendance threshold. */
public class AttendanceService {
    public static final String THRESHOLD_KEY = "attendance_threshold";
    public static final int DEFAULT_THRESHOLD = 75;

    private final AttendanceDao attendanceDao;
    private final StudentDao studentDao;
    private final TimetableDao timetableDao;
    private final SettingsDao settingsDao;
    private final FacultyGuard guard;

    public AttendanceService() {
        this(new AttendanceDao(), new StudentDao(), new SubjectDao(), new TimetableDao(), new SettingsDao());
    }

    public AttendanceService(AttendanceDao attendanceDao, StudentDao studentDao, SubjectDao subjectDao,
                             TimetableDao timetableDao, SettingsDao settingsDao) {
        this.attendanceDao = attendanceDao;
        this.studentDao = studentDao;
        this.timetableDao = timetableDao;
        this.settingsDao = settingsDao;
        this.guard = new FacultyGuard(subjectDao);
    }

    // ---------- threshold ----------

    public int getThreshold() {
        String value = settingsDao.get(THRESHOLD_KEY, String.valueOf(DEFAULT_THRESHOLD));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_THRESHOLD;
        }
    }

    public void setThreshold(User actor, int threshold) {
        AccessControl.requireRole(actor, Role.ADMIN);
        Validator.requireIntInRange(threshold, "Attendance threshold", 1, 100);
        settingsDao.set(THRESHOLD_KEY, String.valueOf(threshold));
    }

    // ---------- marking ----------

    /** Sections of a subject that this faculty member teaches (from the timetable), e.g. [A, B]. */
    public List<String> getSectionsTaught(User actor, int subjectId) {
        Faculty faculty = guard.requireFaculty(actor);
        Set<String> sections = new TreeSet<>();
        for (TimetableEntry entry : timetableDao.findByFaculty(faculty.getFacultyId())) {
            if (entry.getSubject().getSubjectId() == subjectId) {
                sections.add(entry.getSection());
            }
        }
        return new ArrayList<>(sections);
    }

    /** The students of the class that will be marked. */
    public List<Student> getRoster(User actor, int subjectId, String section) {
        Faculty faculty = guard.requireFaculty(actor);
        guard.requireTeaches(faculty, subjectId);
        Subject subject = guard.requireSubject(subjectId);
        return studentDao.findByClass(subject.getBranch(), subject.getSemester(), section);
    }

    /** Records already saved for this class, subject and date (empty if the day is not marked yet). */
    public List<AttendanceRecord> getExisting(User actor, int subjectId, LocalDate date, String section) {
        Faculty faculty = guard.requireFaculty(actor);
        guard.requireTeaches(faculty, subjectId);
        return attendanceDao.findBySubjectDateAndSection(subjectId, date, section);
    }

    /**
     * Saves attendance for a whole class in one transaction.
     * Rejects the request if any student already has attendance for this subject and date.
     */
    public void markAttendance(User actor, int subjectId, LocalDate date, Map<Integer, AttendanceStatus> statuses) {
        Faculty faculty = guard.requireFaculty(actor);
        validateRequest(date, statuses);
        guard.requireTeaches(faculty, subjectId);

        Set<Integer> alreadyMarked = attendanceDao.findMarkedStudentIds(subjectId, date);
        long duplicates = statuses.keySet().stream().filter(alreadyMarked::contains).count();
        if (duplicates > 0) {
            throw new ConflictException("Attendance for " + date + " is already recorded for " + duplicates
                    + " student(s). Use 'Update attendance' to correct it instead of adding it again.");
        }

        List<AttendanceRecord> records = toRecords(faculty, subjectId, date, statuses);
        try {
            DbConnection.executeInTransaction(connection -> {
                attendanceDao.insertBatch(connection, records);
                return null;
            });
        } catch (DataAccessException e) {
            // two teachers saving at the same moment: the database UNIQUE key still protects us
            throw e.isDuplicateKey() ? ServiceSupport.duplicate(e) : e;
        }
    }

    /** Corrects statuses of students whose attendance was already saved for this subject and date. */
    public void updateAttendance(User actor, int subjectId, LocalDate date, Map<Integer, AttendanceStatus> statuses) {
        Faculty faculty = guard.requireFaculty(actor);
        validateRequest(date, statuses);
        guard.requireTeaches(faculty, subjectId);

        Set<Integer> alreadyMarked = attendanceDao.findMarkedStudentIds(subjectId, date);
        if (!alreadyMarked.containsAll(statuses.keySet())) {
            throw new ValidationException("Some students have no attendance saved for this date yet, so they cannot be updated.");
        }
        List<AttendanceRecord> records = toRecords(faculty, subjectId, date, statuses);
        DbConnection.executeInTransaction(connection -> {
            attendanceDao.updateStatusBatch(connection, records);
            return null;
        });
    }

    private static void validateRequest(LocalDate date, Map<Integer, AttendanceStatus> statuses) {
        Validator.requireNotNull(date, "a date");
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("Attendance cannot be marked for a future date.");
        }
        if (statuses == null || statuses.isEmpty()) {
            throw new ValidationException("There are no students to mark.");
        }
        if (statuses.containsValue(null)) {
            throw new ValidationException("Every student needs a status (Present, Absent or Late).");
        }
    }

    private static List<AttendanceRecord> toRecords(Faculty faculty, int subjectId, LocalDate date,
                                                   Map<Integer, AttendanceStatus> statuses) {
        List<AttendanceRecord> records = new ArrayList<>();
        statuses.forEach((studentId, status) ->
                records.add(new AttendanceRecord(studentId, subjectId, date, status, faculty.getFacultyId())));
        return records;
    }

    // ---------- reading / summaries ----------

    /** Subject-wise attendance of one student. */
    public List<AttendanceSummary> getSummaryForStudent(Student student) {
        return attendanceDao.summarize(student.getStudentId(), null, null, null, null);
    }

    public double getOverallPercentage(Student student) {
        return AttendanceSummary.overallPercentage(getSummaryForStudent(student));
    }

    /** Per-student totals for a class, optionally within a date range. Admin and faculty only. */
    public List<AttendanceSummary> getClassSummaries(User actor, Integer subjectId, String section,
                                                     LocalDate from, LocalDate to) {
        requireAdminOrTeacherOf(actor, subjectId);
        checkRange(from, to);
        return attendanceDao.summarize(null, subjectId, section, from, to);
    }

    /** Only the rows under the configured threshold. */
    public List<AttendanceSummary> getBelowThreshold(User actor, Integer subjectId, String section) {
        int threshold = getThreshold();
        return getClassSummaries(actor, subjectId, section, null, null).stream()
                .filter(s -> s.isBelowThreshold(threshold))
                .toList();
    }

    /** Detailed day-by-day records for the reports screen. */
    public List<AttendanceRecord> getReportRecords(User actor, Integer studentId, Integer subjectId,
                                                   LocalDate from, LocalDate to) {
        requireAdminOrTeacherOf(actor, subjectId);
        checkRange(from, to);
        return attendanceDao.findReport(studentId, subjectId, from, to);
    }

    /** Admin sees everything; a faculty member must pick one of the subjects he or she teaches. */
    private void requireAdminOrTeacherOf(User actor, Integer subjectId) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        if (actor.getRole() == Role.FACULTY) {
            if (subjectId == null) {
                throw new ValidationException("Please select one of your subjects.");
            }
            guard.requireTeaches((Faculty) actor, subjectId);
        }
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("The 'from' date must not be after the 'to' date.");
        }
    }
}
