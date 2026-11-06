# Smart Classroom Management System: Project Report

## Abstract
The Smart Classroom Management System is a Java desktop application that digitises daily classroom administration in a
college: student and faculty records, classrooms, subjects, a clash-free timetable, attendance, assignments, marks,
announcements and reports. It is built with Java 17, Swing and MySQL (through JDBC) in a layered design
(model, dao, service, ui, util). Three roles (Admin, Faculty, Student) sign in separately and see only what they are
allowed to use. Passwords are stored as salted PBKDF2 hashes. The business rules are covered by 141 JUnit tests that run
without a database.

## 1. Introduction
Colleges often manage attendance on paper and timetables in spreadsheets. This causes double-booked rooms, wrong
attendance percentages and slow communication. This project gives one application where each person works with the data
that concerns them.

## 2. Problem statement
- Rooms and teachers get booked twice because clashes are found only after the timetable is published.
- Attendance percentages are calculated by hand and low-attendance students are noticed late.
- Marks, assignments and notices are scattered across files and messages.
- Anyone with the spreadsheet can see or change everything.

## 3. Objectives
1. Maintain student, faculty, subject and classroom records (create, view, update, delete, search).
2. Build a timetable that rejects room, teacher and section clashes and rooms that are too small.
3. Record attendance (Present, Absent, Late) without duplicates and show percentages with a configurable warning threshold.
4. Let faculty publish assignments and enter marks; let students view their own data.
5. Provide role-based access, hashed passwords and CSV reports.
6. Apply object-oriented design, exception handling and automated tests.

## 4. Scope
In scope: a single-college desktop application with one MySQL database and three roles.
Out of scope: web or mobile versions, file upload of submissions, e-mail or SMS alerts, biometric attendance, fee management.

## 5. Requirements
### 5.1 Functional
| Area | Requirement |
|---|---|
| Authentication | Login with role choice; wrong password or wrong role is rejected; users can change their password |
| Admin | Manage students, faculty, subjects, classrooms, timetable; reset passwords; set the attendance threshold; view reports and export CSV |
| Faculty | See own classes; mark or update attendance for own subjects; create assignments; enter marks |
| Student | View profile, attendance (with warning), timetable, assignments, marks |
| Timetable | Validate fields, capacity, room, faculty and section clashes |
| Attendance | One record per student, subject and date; percentage = (present + late) / total x 100 |

### 5.2 Non-functional
Security (hashed passwords, parameterised SQL, role checks in the service layer), usability (clear error messages, search,
sortable tables), maintainability (layers, small classes), portability (any OS with JDK 17 and MySQL 8).

### 5.3 Software requirements
JDK 17 or later, MySQL 8.0.16 or later, Maven 3.8+ (or an IDE with Maven support), mysql-connector-j 8.4.0, FlatLaf 3.5.4, JUnit 5.10.2.

## 6. System design
### 6.1 Architecture
`ui` (Swing screens) calls `service` (rules and permissions), which calls `dao` (SQL through JDBC), which reads and writes
MySQL. `model` classes carry data between layers; `util` holds shared helpers (connection, hashing, validation, CSV, session).
Diagrams (use case, class, ER) are in `docs/ARCHITECTURE.md`.

### 6.2 Database design
Eleven tables: `users`, `students`, `faculty`, `subjects`, `classrooms`, `timetable`, `attendance`, `assignments`,
`marks`, `announcements`, `settings`. Login data lives once in `users`; `students` and `faculty` extend it one-to-one.
Foreign keys keep data consistent, and `UNIQUE(student_id, subject_id, attendance_date)` stops duplicate attendance.
CHECK constraints guard values such as capacity and time ranges. The design is in third normal form.

### 6.3 Object-oriented design
Abstract `User` with `Admin`, `Faculty`, `Student` (inheritance and polymorphic `getMenuItems()`); generic `Dao<T>` and
`CrudPanel<T>`; enums `Role` and `AttendanceStatus`; composition in `TimetableEntry`; custom exception hierarchy under `AppException`.

### 6.4 Security design
PBKDF2-HMAC-SHA256, random salt and 120,000 iterations; credentials for the database kept out of source control;
`AccessControl.requireRole` on every changing service method; `FacultyGuard` limits faculty to their own subjects; CSV
export neutralises spreadsheet formulas.

## 7. Implementation
The project was built module by module: model and utilities, DAOs, services, then screens. Notable points:
- Timetable clash rule: two intervals overlap when `start1 < end2 AND end1 > start2`; checks run for room, faculty and section on the same day, excluding the entry being edited.
- Attendance is saved in one transaction so a class is stored completely or not at all.
- The first start replaces the `PENDING` demo passwords in `schema.sql` with real hashes (`DemoDataInitializer`).
- Login runs in a `SwingWorker` so the window does not freeze.

## 8. Testing
141 JUnit 5 tests cover authentication, student rules, timetable clashes, attendance, other services, password hashing,
validation, CSV export, connection settings and the model. Services receive fake DAOs, so no database is needed.
Thirty manual test cases (M1 to M30) in `docs/TESTING.md` cover the screens and the real database.
Result in the development sandbox: 141 of 141 tests passed and every role's screens were constructed and rendered.

## 9. Results
The application provides every feature in the objectives. Demo data (6 students, 3 faculty, 18 attendance rows) shows
overall attendance of 72.2% and flags four low-attendance student-subject pairs at the 75% threshold.

## 10. Limitations
- The SQL was reviewed but not run against a real MySQL server in the development sandbox; the first-run checklist in the README covers this.
- The Maven build and the FlatLaf look were not exercised in the sandbox (the network blocked Maven Central); a compile-only stub was used.
- Database calls on most screens run on the UI thread; there is no connection pool.
- No file uploads, notifications or audit log.

## 11. Future scope
Connection pooling, background loading everywhere, assignment submissions with files, e-mail alerts for low attendance,
PDF reports, a web front end (for example Spring Boot) over the same service layer.

## 12. Conclusion
The system shows that a layered, role-aware design with strict rule checks can replace manual classroom bookkeeping, and
it keeps the code easy to test and extend.

## 13. References
1. Oracle, Java SE 17 Documentation: https://docs.oracle.com/en/java/javase/17/
2. Oracle, JDBC Basics tutorial: https://docs.oracle.com/javase/tutorial/jdbc/basics/
3. MySQL 8.0 Reference Manual: https://dev.mysql.com/doc/refman/8.0/en/
4. FlatLaf project: https://www.formdev.com/flatlaf/
5. JUnit 5 User Guide: https://junit.org/junit5/docs/current/user-guide/
6. NIST SP 800-132, Recommendation for Password-Based Key Derivation.
