# Viva questions and answers

Short, honest answers you can say in your own words. Class names are from this project.

## A. Project and design

**1. What does your project do?**
It is a desktop application for a college classroom. Admin manages students, faculty, subjects, classrooms and the
timetable; faculty mark attendance, publish assignments and enter marks; students view their attendance, timetable,
assignments and marks. It stores everything in MySQL.

**2. Why did you split the code into model, dao, service and ui?**
So each part has one job. The `ui` only shows screens, the `service` holds the rules (for example "a room cannot be
double-booked"), the `dao` holds only SQL, and `model` classes carry data between them. If the database changes,
only the DAOs change; if a rule changes, only a service changes.

**3. Where is the business logic?**
In `service`. Example: `TimetableService.checkConflicts()` and `checkCapacity()`, `AttendanceService.markAttendance()`.

**4. Why does the UI not contain SQL?**
Mixing SQL into screens makes code hard to test and change, and invites SQL injection mistakes. Screens call
`service.xxx()` and only show the result or the error message.

**5. What is a DAO?**
Data Access Object: a class that hides how data is read and written. `StudentDao` is the only class that knows the
SQL for students.

**6. Why Maven?**
It downloads the MySQL driver and FlatLaf automatically and gives one command to build (`mvn package`) and test
(`mvn test`), in any IDE.

**7. Why did you use FlatLaf?**
Plain Swing looks old. FlatLaf is only a Look and Feel; the application is still ordinary Swing code.

## B. OOP

**8. Where did you use inheritance?**
`User` is an abstract class with id, username, name, email and role. `Admin`, `Faculty` and `Student` extend it and add
their own fields (for example `Student` has `rollNo`, `branch`, `semester`, `section`).

**9. Where did you use polymorphism?**
`User.getMenuItems()` and `getDashboardTitle()` are abstract and each subclass overrides them. `DashboardFrame` calls
`user.getMenuItems()` and builds the sidebar without checking the role. Also `AuthService.login()` returns a `User`
that is really an `Admin`, `Faculty` or `Student`.

**10. Why is `User` abstract?**
A user is always one of the three roles; "a user who is nobody" has no meaning, and the role-specific methods must be
supplied by the subclasses.

**11. Where is encapsulation?**
All model fields are private with getters/setters. `Classroom.setCapacity()` throws if the value is <= 0, so the object
can never hold an impossible capacity. `AttendanceSummary` keeps the percentage rule next to its data.

**12. Where did you use interfaces and abstraction?**
`Dao<T>` (a generic interface implemented by the DAOs), `Refreshable` (screens that reload their data),
`DbConnection.TransactionalWork` and `TransactionRunner`. `CrudPanel<T>` is an abstract class: subclasses only supply
`loadItems`, `toRow`, `onAdd`, `onEdit`, `onDelete`; the shared list/search/delete code is written once.

**13. What is composition? Example?**
"Has-a" relation. `TimetableEntry` has a `Subject`, a `Faculty` and a `Classroom`. Services have DAOs.

**14. Composition vs inheritance?**
Use inheritance for "is-a" (Student is a User) and composition for "has-a" (timetable entry has a classroom).

**15. What is method overriding vs overloading?**
Overriding: a subclass replaces a parent method with the same signature (`getMenuItems`). Overloading: same method name,
different parameters (`FormPanel`/`UiFactory` helpers, `DemoDataInitializer` constructors).

**16. Why generics in `CrudPanel<T>`?**
So one class works for `Student`, `Faculty`, `Subject` and others with type safety (no casts when you get the selected item).

**17. Which collections did you use and why?**
`List` for table rows; `Map<Integer, AttendanceStatus>` to pass a whole class's attendance (student id -> status);
`Set<Integer>` for "students already marked" (fast `contains`); `EnumMap`/`TreeMap` to build the weekly grid;
`LinkedHashMap` to keep the sidebar order.

**18. Why enums?**
`Role`, `AttendanceStatus` are fixed sets of values. `AttendanceStatus` also knows whether it counts as attended, so
the rule "late counts as present" is in one place.

## C. Exceptions

**19. How do you handle errors?**
Custom unchecked exceptions all extending `AppException`: `ValidationException` (bad input), `ConflictException`
(clash/duplicate), `AuthenticationException`, `AccessDeniedException`, `DataAccessException` (wraps `SQLException`).
Screens catch them and show `e.getMessage()` in a dialog.

**20. Why unchecked exceptions?**
Otherwise every method between the DAO and the UI would need `throws`. The UI is the single place that handles them.

**21. What is try-with-resources?**
`try (Connection c = ...; PreparedStatement ps = ...) { }` closes the resources automatically, even on error. Every DAO uses it.

**22. How does a duplicate roll number become a friendly message?**
MySQL error code 1062 is stored in `DataAccessException`; `isDuplicateKey()` is true, and `ServiceSupport.duplicate()`
looks at the key name (`roll_no`) and throws a `ConflictException` with a readable sentence.

## D. Database and JDBC

**23. What is JDBC?**
Java's standard API to talk to databases. `DriverManager.getConnection(url, user, password)` gives a `Connection`.

**24. Why `PreparedStatement`?**
It sends the SQL and the values separately, which prevents SQL injection and handles quoting/dates correctly.

**25. What is SQL injection? How did you prevent it?**
Input such as `' OR '1'='1` changing the meaning of a query. We never concatenate user input into SQL; values go
through `?` placeholders. (The few concatenated parts are fixed column lists, never user text.)

**26. Explain your tables and relationships.**
`users` holds login data for everyone; `students` and `faculty` each have one row per user (1-to-1 via `user_id`).
`timetable` links subject, faculty and classroom. `attendance` links student and subject with a date. `marks`,
`assignments`, `announcements`, `settings` complete it. See `docs/ARCHITECTURE.md`.

**27. Is your database normalised?**
Yes, to 3NF: name and email are stored once in `users`, not repeated in `students`/`faculty`; subject details are
stored once in `subjects` and referenced by id.

**28. How do you stop duplicate attendance?**
Twice: `AttendanceService` first asks `findMarkedStudentIds` and throws `ConflictException`; and the table has
`UNIQUE(student_id, subject_id, attendance_date)` so even two teachers saving at the same moment cannot create duplicates.

**29. What is a transaction? Where do you use one?**
A group of operations that must all succeed or all fail. Creating a student inserts a `users` row and a `students`
row; if the second fails, the first is rolled back. Also used for saving a whole class's attendance and marks.
(`DbConnection.executeInTransaction`: `setAutoCommit(false)`, `commit()` or `rollback()`.)

**30. What does `ON DELETE CASCADE` do?**
Deleting a parent row deletes its children. Deleting a student's `users` row also removes the student profile,
attendance and marks. `attendance.marked_by` has no cascade, so a teacher who marked attendance cannot be deleted by mistake.

**31. How are passwords stored?**
Never in plain text. `PasswordUtil` uses PBKDF2 with HMAC-SHA256, a random 16-byte salt and 120,000 iterations; the
stored text is `iterations:salt:hash`. Login hashes the typed password with the stored salt and compares in constant time.

**32. Why a salt?**
So two users with the same password get different hashes, and pre-computed tables (rainbow tables) are useless.

**33. Why not MD5 or SHA-256 alone?**
They are fast, which helps attackers guess millions of passwords per second. PBKDF2 is deliberately slow.

**34. Where is the database password?**
Not in the code. It is read from `db.properties` (listed in `.gitignore`) or from environment variables.

**35. What are the `PENDING` passwords in the SQL?**
SQL cannot generate PBKDF2 hashes safely, so demo users are inserted as `PENDING` and `DemoDataInitializer` replaces
them with real hashes on the first start. `PENDING` can never log in.

## E. Features and rules

**36. How is attendance percentage calculated?**
`(present + late) / total classes * 100` in `AttendanceSummary.calculatePercentage`; zero classes gives 0 to avoid
dividing by zero. The query uses `COUNT(*)` and `SUM(CASE WHEN status IN ('PRESENT','LATE') ...)`.

**37. How does the low-attendance warning work?**
The threshold is stored in the `settings` table (default 75) and the admin can change it. `isBelowThreshold` compares
the percentage with it; the table renderer paints those rows red.

**38. How do you detect overlapping classes?**
Two time ranges overlap when `start1 < end2 AND end1 > start2`. `TimetableDao` runs this query for the room, the
teacher and the section on the same day. When editing, the entry's own id is excluded.

**39. How is capacity validated?**
Class size = number of students with the subject's branch and semester in that section (`StudentDao.countByClass`).
If it is greater than the room capacity, `TimetableService.checkCapacity` rejects it.

**40. How does role-based access control work?**
Two layers: the sidebar only shows the menu items of the role (`User.getMenuItems()`), and every service method that
changes data calls `AccessControl.requireRole(actor, ...)` which throws `AccessDeniedException`. So even if someone
called a service directly, a student could not add a student. A faculty member can only mark attendance for subjects
assigned to him/her in the timetable (`FacultyGuard`).

**41. How are CSV files protected against problems?**
`CsvExporter` quotes commas/quotes/line breaks and prefixes cells starting with `=`, `+`, `@` so Excel does not run them as formulas.

**42. Why does the login run in a `SwingWorker`?**
Hashing and the database call take time; running them on the Event Dispatch Thread would freeze the window.
(Other screens call the database on the EDT because the data is small; for a large system they would also use workers.)

**48a. How does the leave request module work?**
`LeaveService` owns the rules (max 30 days, at most 7 days back-dated, no overlap with a pending/approved request,
only PENDING requests can be decided or cancelled, a rejection needs a reason). Students can only apply/cancel;
admin and faculty can decide (`AccessControl.requireRole`). The status is the enum `RequestStatus`.

**48b. How is the SGPA calculated?**
`GradeScale` (enum) maps percentage to letter and grade points on the 10-point scale. A subject's percentage is the
sum of all its exams over the sum of their maximum marks. SGPA = sum(credits x points) / sum(credits), see
`GradeCard.getSgpa`. Nothing is stored; it is recomputed from the marks table, so it can never be out of date.

**48c. How do you rank students and what about ties?**
`GradeService.rankClass` sorts by SGPA then overall percentage (descending). Students with identical results share a
rank and the next rank is skipped (1, 1, 3), the same as competition ranking. Students without marks are not ranked.

**48d. How do you prevent double booking of a room?**
`RoomBookingService` checks the weekly timetable (`TimetableDao.findRoomConflicts` for that weekday) and the approved
bookings of that date (`RoomBookingDao.findApprovedConflicts`), using the same overlap rule `start1 < end2 AND end1 > start2`.
The check runs when the faculty requests and again when the admin approves, because another request may have been
approved in between.

## F. Testing

**43. How did you test?**
JUnit 5 tests for rules (141 tests). The services get their DAOs through the constructor, so tests inject fake DAOs
that keep data in memory and need no database. Plus manual test cases in `docs/TESTING.md`.

**44. What is a fake DAO?**
A class that extends the real DAO but stores data in a list, so we can test "room already booked" by simply making
the fake return a conflicting entry.

**45. What did you not test automatically?**
The real SQL against MySQL and the Swing screens (tested manually with the cases in `docs/TESTING.md`).

## G. Improvements

**46. Limitations?**
Single-college, desktop only, no email notifications, no file uploads for assignments, database calls on the UI thread,
no audit log. Plain `Statement`-free JDBC without a connection pool.

**47. What would you add next?**
Connection pooling (HikariCP), background threads for all screens, assignment submissions with file upload, email/SMS
alerts for low attendance, PDF reports, a web version with Spring Boot.

**48. If the college has 5000 students, what changes?**
Add pagination and indexes on search columns, load tables lazily, use a connection pool, and move DB calls off the UI thread.
