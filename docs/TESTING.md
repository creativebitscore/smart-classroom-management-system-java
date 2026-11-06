# Testing documentation

## 1. Automated tests (JUnit 5): 141 tests, run with `mvn test`

The service layer takes its DAOs through the constructor, so the tests plug in **hand-written fake DAOs**
(`src/test/java/com/smartclassroom/service/Fakes.java`) that keep data in memory. This tests the *business rules*
without needing MySQL. `DbConnection.useTransactionRunner(...)` lets the same tests exercise the code that normally
runs inside a database transaction.

| Test class | Tests | What it proves |
|---|---|---|
| `PasswordUtilTest` | 5 | hash/verify works, wrong password rejected, same password gives different hashes (random salt), plain text never in the hash, malformed hash never verifies |
| `ValidatorTest` | 8 | empty/blank/too-long text, email, phone, username and password rules, number parsing, ranges |
| `CsvExporterTest` | 4 | commas, quotes, line breaks escaped; spreadsheet formulas neutralised; file has header and rows |
| `DbConnectionTest` | 3 | an unreachable database gives a friendly `DataAccessException`, `isDatabaseAvailable()` returns false, a transaction on a dead database also fails cleanly |
| `ModelTest` | 11 | attendance percentage, divide-by-zero guard, late counts as attended, threshold boundary, overall %, polymorphic menus per role, classroom capacity encapsulation, overdue logic |
| `AuthServiceTest` | 10 | valid login for all three roles, wrong password, unknown user (same message), wrong role tab, disabled account, `PENDING` placeholder, empty fields, change password, admin-only reset |
| `StudentServiceTest` | 10 | add student (login row + profile row linked), fields cleaned, empty fields, invalid values, duplicate roll number -> friendly conflict, update, delete, role permissions |
| `TimetableServiceTest` | 11 | valid entry, room clash, teacher clash, section clash, capacity exceeded, capacity exactly equal allowed, end <= start, missing fields, update excludes its own slot, admin-only |
| `AttendanceServiceTest` | 16 | save a class, duplicate rejected (nothing saved), database UNIQUE key mapped to conflict, future date, empty input, teacher not assigned to subject, admin/student cannot mark, update vs insert, threshold default/range/permission, low-attendance filter, faculty can only see own subjects |
| `OtherServicesTest` | 15 | classroom validation and permissions, assignments (deadline, ownership, permissions, upcoming count), marks (range, permissions), announcements (post/delete rules), CSV report export |
| `LeaveServiceTest` | 16 | apply (starts pending, reason trimmed), only students apply, missing dates/reason, last day before first day, 7-day back-dating limit, 30-day maximum, overlap refused, admin/faculty approve with reviewer recorded, students cannot decide, rejection needs a reason, decided request cannot be decided again, cancel own pending only |
| `RoomBookingServiceTest` | 17 | request a free room, faculty-only, past date / bad times / missing fields, group larger than room (exactly equal allowed), clash with a timetable class, clash with an approved booking, faculty teaching at that time, free-room search (capacity, clashes, teacher busy), approve re-checks the room and excludes itself, past date, admin-only decisions, cancel rules |
| `GradeServiceTest` | 11 | grade boundaries (89.99 vs 90, 39.99 vs 40), credit-weighted SGPA, several exams added together, one failed subject = FAIL, other-semester marks ignored, empty card, ranking by SGPA then percentage, ties share a rank (1, 1, 3), ranked list with unmarked students last, role permissions |
| `PanelFactoryTest` | 4 | every menu entry of every role has a screen and vice versa; the v2 screens are on the right menus |

### Expected results per requirement

| Requirement | Test | Expected result |
|---|---|---|
| Valid login | `AuthServiceTest.validLoginForEachRoleReturnsTheRightObject` | returns an `Admin`, `Faculty`, `Student` object matching the account |
| Invalid login | `wrongPasswordIsRejected`, `unknownUserGetsTheSameMessage...` | `AuthenticationException` "Invalid username or password." |
| Adding students | `StudentServiceTest.validStudentIsAddedWithLoginAndProfile` | one users row and one students row are inserted, ids linked |
| Updating students | `updateChangesTheProfile` | profile row updated; with bad data `ValidationException`, nothing updated |
| Attendance calculation | `ModelTest.percentageIsAttendedOverTotal`, `summaryUsesAttendedClassesIncludingLate` | 3 of 4 = 75.0; 8 of 10 (6 present + 2 late) = 80.0; 0 classes = 0.0 |
| Duplicate attendance | `AttendanceServiceTest.duplicateAttendance...` | `ConflictException`, nothing inserted |
| Timetable conflicts | `TimetableServiceTest.roomAlreadyBooked...`, `facultyTeachingTwice...`, `sectionWithTwoLectures...` | `ConflictException` naming the clash; nothing inserted |
| Classroom capacity | `classLargerThanRoomIsRejected` | `ValidationException` mentioning capacity 30 and class size 35; exactly 30 is accepted |
| Role-based permissions | `facultyAndStudentCannotChangeStudentRecords`, `onlyAdminMayChangeTheTimetable`, `adminAndStudentCannotMarkAttendance` | `AccessDeniedException` |
| Database connection failure | `DbConnectionTest` | `DataAccessException` "Cannot connect to the database..." |
| Empty fields / invalid input | `ValidatorTest`, `emptyFieldsAreRejectedAndNothingIsSaved` | `ValidationException`, message names the field |

## 2. Manual test cases (do these with the real database)

Use the demo data from `sql/schema.sql`. "Expected" is what you should see.

| # | Steps | Expected |
|---|---|---|
| M1 | Login tab Student, `student1` / `Student@123` | Student dashboard opens |
| M2 | Login tab Admin, `student1` / `Student@123` | message "This account cannot sign in as Admin. Choose the correct role tab." |
| M3 | Wrong password for `admin` | red message "Invalid username or password."; password box clears |
| M4 | Click Sign in with empty boxes | "Please enter both username and password." |
| M5 | Admin > Students > Add New, leave name empty, Save | dialog "Full name is required."; dialog stays open |
| M6 | Add student: name Nisha Gupta, username `nisha.gupta`, password `Nisha@1234`, email `nisha@student.edu`, roll `CSE2307`, branch CSE, sem 3, section A | "Student added successfully"; row appears; she can log in as a student |
| M7 | Add another student with the same roll `CSE2307` | "A student with this roll number already exists." and no new login is created (transaction rolled back) |
| M8 | Select Nisha > Edit > change phone to `12345` | "Phone number must be exactly 10 digits." |
| M9 | Select Nisha > Delete | confirmation box first; after Yes the row disappears |
| M10 | `faculty1` > Mark Attendance > CS301, section A, today, Load class > Save attendance | success; reload shows the button "Update attendance" |
| M11 | Same page, date `2026-10-19`, Load class | the saved statuses are shown (Divya Absent, Rahul Late) and the button reads "Update attendance"; saving cannot create duplicates |
| M12 | `faculty1` > Mark Attendance, date tomorrow | "Attendance cannot be marked for a future date." |
| M13 | `student6` (Divya) > My Attendance | rows in red; CS301 0.0%, CS302 0.0%; overall 0.0% |
| M14 | `student2` (Priya) > My Attendance | CS301 50.0% (red), CS302 100%, overall 66.7% |
| M15 | Admin > Attendance, change threshold to 50, Save threshold | Priya's CS301 (50%) is no longer red; Divya/Sneha still red |
| M16 | Admin > Timetable > Add: CS303, faculty3, R101, section A, Monday 09:00-10:00 | "Room R101 is already booked: CS301 (MONDAY 09:00-10:00 ...)" |
| M17 | Add: CS303, faculty1 (Ravi), R102, section B, Monday 09:00-10:00 | "Dr. Ravi Kumar is already teaching at that time: CS301 ..." |
| M18 | Add: CS303, faculty3, R102, section A, Monday 09:00-10:00 | "Section A already has a lecture at that time: CS301 ..." |
| M19 | Admin > Classrooms > Add `R-MINI`, capacity 5; then schedule section A (6 students) in R-MINI on Friday | "Classroom R-MINI seats 5 but CSE semester 3 section A has 6 students." |
| M20 | Add class with end time 09:00 and start time 10:00 | "End time must be after start time." |
| M21 | Classroom capacity `abc` or `0` | "Capacity must be a whole number." / "Capacity must be between 1 and 1000." |
| M22 | Login as `faculty2`; open the menu | no Admin entries; Faculty menu only. (A student also has no Mark Attendance / Reports) |
| M23 | `faculty1` > Assignments > New: title, subject CS301, due date yesterday | "The due date cannot be in the past." |
| M24 | `faculty1` > Marks: CS301, A, MID2, out of 25, enter 30 for a student | "Marks must be between 0 and 25.0." |
| M25 | `student1` > Assignments | 2 assignments with due dates and status ("Due in N days" or "Overdue") |
| M26 | Admin > Reports > Generate, then Export records CSV | file saved; opens in Excel with header `Date,Roll No,Student,Subject,Status` |
| M27 | Stop the MySQL service, start the app | box "Database connection problem ..." and the app exits cleanly (no stack trace window) |
| M28 | Stop MySQL while logged in, click Refresh | message "Cannot connect to the database..." (no crash) |
| M29 | Admin > Students > type `priya` in search | only Priya's row stays; counter shows "1 of 7 records" (or 6) |
| M30 | Admin > Students > Delete a student whose delete you confirm, then check `attendance` in MySQL | that student's attendance and marks rows are gone (ON DELETE CASCADE) |

### Manual test cases for the v2 features (real database, demo data)

The demo room bookings are dated 7 and 9 Nov 2026; if you test later, use dates in the future.

| # | Steps | Expected |
|---|---|---|
| M20 | `student1` > My Leave > Add New: from today, to tomorrow, reason "Fever" > Save | request appears with status Pending |
| M21 | Same student, apply again for tomorrow only | "You already have a pending leave request for ..." |
| M22 | Apply with last day before first day | "The last day cannot be before the first day." |
| M23 | Apply with first day 10 days in the past | "Leave can start at most 7 days in the past..." |
| M24 | `admin` > Leave Requests, select student1's request > Approve > remark "ok" | status Approved, "Decided by" shows System Administrator; `student1` sees it with the remark |
| M25 | Select the same row, press Reject | "This request is already approved." |
| M26 | `faculty1` > Leave Requests > select Priya's pending request > Reject with an empty reason | "A reason for rejecting is required." and nothing changes |
| M27 | `student1` > Grade Card | SGPA 9.00 (22/25 = 88% -> A+), class rank 2 of 6 (Karthik, 92%, is rank 1), one bar in the chart, Result PASS |
| M28 | `student6` > Grade Card | 10/25 = 40% -> grade P (4 points), Result PASS, rank 6 of 6; the bar is red only for values below 40; Export CSV opens in Excel |
| M29 | `admin` > Class Rankings | CSE / Semester 3 / Section A listed 1 to 6 (Karthik, Aarav, Rahul, Priya, Sneha, Divya); export works |
| M30 | `faculty1` > Room Booking > Add New: next Monday, 09:00-10:00, 30 people > Find free rooms | warning "No room is free ... (or you have a regular class then)", because Dr. Ravi teaches CS301 on Monday 09:00 |
| M31 | `faculty1` > Add New: 2026-11-07, 14:00-15:00, 30 people > Find free rooms | R101 is NOT listed (approved demo booking 14:00-16:00); R102 and LAB-1 are listed. Pick R102 > Save: request is Pending |
| M32 | `admin` > Room Requests > approve the demo request of Prof. Anita (R102, 2026-11-09) | status Approved with remark "Approved" |
| M33 | `faculty1` > Room Booking > select the approved R101 booking > Cancel booking | status Cancelled; the slot is free again in "Find free rooms" |

## 3. How I verified the package (and what was not verified)

Verified in the build sandbox:
* every Java source compiles with `javac --release 17` (FlatLaf was replaced by a tiny compile-only stub);
* all 141 unit tests pass using a small JUnit-compatible runner (Maven Central was unreachable, so the real JUnit
  could not be downloaded); the runner was checked to report a deliberately failing test;
* every screen of every role was constructed under a virtual display with no database and rendered to PNG; each role's
  menu entries map to a screen.

**Not verified here:** execution of `schema.sql` and of the JDBC queries against a real MySQL server, the Maven
build (`mvn package`), FlatLaf's appearance, and the real JUnit run. Do the first-run checklist in the README and the
manual cases above to confirm them.
