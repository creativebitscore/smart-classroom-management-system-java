package com.smartclassroom.service;

import com.smartclassroom.dao.AttendanceDao;
import com.smartclassroom.dao.ClassroomDao;
import com.smartclassroom.dao.FacultyDao;
import com.smartclassroom.dao.MarkDao;
import com.smartclassroom.dao.StudentDao;
import com.smartclassroom.dao.SubjectDao;
import com.smartclassroom.model.AttendanceRecord;
import com.smartclassroom.model.AttendanceSummary;
import com.smartclassroom.model.Role;
import com.smartclassroom.model.Student;
import com.smartclassroom.model.Subject;
import com.smartclassroom.model.User;
import com.smartclassroom.util.CsvExporter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Dashboard numbers, class statistics and CSV export. */
public class ReportService {
    /** The four summary cards on the admin dashboard. */
    public record DashboardStats(int students, int faculty, int classrooms, int subjects, double overallAttendance) {
    }

    /** Basic statistics for one class (subject + section). */
    public record ClassStatistics(String subjectName, String section, int students, double averageAttendance,
                                  int belowThreshold, Double averageMarksPercent) {
    }

    private final StudentDao studentDao;
    private final FacultyDao facultyDao;
    private final ClassroomDao classroomDao;
    private final SubjectDao subjectDao;
    private final AttendanceDao attendanceDao;
    private final MarkDao markDao;
    private final AttendanceService attendanceService;

    public ReportService() {
        this(new StudentDao(), new FacultyDao(), new ClassroomDao(), new SubjectDao(), new AttendanceDao(),
                new MarkDao(), new AttendanceService());
    }

    public ReportService(StudentDao studentDao, FacultyDao facultyDao, ClassroomDao classroomDao,
                         SubjectDao subjectDao, AttendanceDao attendanceDao, MarkDao markDao,
                         AttendanceService attendanceService) {
        this.studentDao = studentDao;
        this.facultyDao = facultyDao;
        this.classroomDao = classroomDao;
        this.subjectDao = subjectDao;
        this.attendanceDao = attendanceDao;
        this.markDao = markDao;
        this.attendanceService = attendanceService;
    }

    public DashboardStats getDashboardStats(User actor) {
        AccessControl.requireRole(actor, Role.ADMIN);
        return new DashboardStats(studentDao.count(), facultyDao.count(), classroomDao.count(),
                subjectDao.count(), attendanceDao.overallPercentage());
    }

    public ClassStatistics getClassStatistics(User actor, Subject subject, String section) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        int students = studentDao.countByClass(subject.getBranch(), subject.getSemester(), section);
        List<AttendanceSummary> summaries = attendanceDao.summarize(null, subject.getSubjectId(), section, null, null);
        int threshold = attendanceService.getThreshold();
        int below = (int) summaries.stream().filter(s -> s.isBelowThreshold(threshold)).count();
        double average = AttendanceSummary.overallPercentage(summaries);
        return new ClassStatistics(subject.getSubjectName(), section, students, average, below,
                markDao.averagePercentage(subject.getSubjectId()));
    }

    // ---------- CSV exports ----------

    public void exportAttendanceRecords(Path file, List<AttendanceRecord> records) {
        List<List<String>> rows = new ArrayList<>();
        for (AttendanceRecord r : records) {
            rows.add(List.of(r.getDate().toString(), r.getRollNo(), r.getStudentName(), r.getSubjectName(),
                    r.getStatus().toString()));
        }
        CsvExporter.export(file, List.of("Date", "Roll No", "Student", "Subject", "Status"), rows);
    }

    public void exportAttendanceSummaries(Path file, List<AttendanceSummary> summaries, int threshold) {
        List<List<String>> rows = new ArrayList<>();
        for (AttendanceSummary s : summaries) {
            rows.add(List.of(s.getRollNo(), s.getStudentName(), s.getSubjectCode(), s.getSubjectName(),
                    String.valueOf(s.getTotalClasses()), String.valueOf(s.getAttendedClasses()),
                    String.format("%.1f", s.getPercentage()), s.isBelowThreshold(threshold) ? "BELOW THRESHOLD" : "OK"));
        }
        CsvExporter.export(file, List.of("Roll No", "Student", "Subject Code", "Subject", "Total Classes",
                "Attended", "Percentage", "Status (threshold " + threshold + "%)"), rows);
    }

    public void exportStudents(User actor, Path file, List<Student> students) {
        AccessControl.requireRole(actor, Role.ADMIN, Role.FACULTY);
        List<List<String>> rows = new ArrayList<>();
        for (Student s : students) {
            rows.add(List.of(s.getRollNo(), s.getFullName(), s.getEmail(), s.getPhone() == null ? "" : s.getPhone(),
                    s.getBranch(), String.valueOf(s.getSemester()), s.getSection()));
        }
        CsvExporter.export(file, List.of("Roll No", "Name", "Email", "Phone", "Branch", "Semester", "Section"), rows);
    }
}
