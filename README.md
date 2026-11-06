# Smart Classroom Management System

A desktop application (Java Swing + MySQL) that manages students, faculty, subjects, classrooms,
timetables, attendance, assignments, marks, announcements and reports. Built as a BTech project.

* **Roles:** Admin, Faculty, Student (separate login for each, role-based access)
* **Stack:** Java 17, Swing (FlatLaf look and feel), MySQL 8, JDBC, Maven
* **Architecture:** `ui` -> `service` -> `dao` -> MySQL, with `model` objects passed between layers

> **Honest status note.** The code compiles, all 141 unit tests pass and every screen of every role builds
> without errors (checked in a sandbox that had Java but **no MySQL, no Maven Central access**).
> The SQL script and the JDBC queries have been reviewed line by line but **were not run against a real MySQL
> server by the author of this package**. The same is true of the v2 additions (leave requests, grade card,
> room booking): their SQL (`sql/upgrade_v2.sql`, the new DAOs) has not been run on MySQL; the rules in the
> services are covered by unit tests and the screens were constructed headlessly. Follow the "First run checklist" below on your machine to confirm
> it end to end, and report any error you see.

---

## 1. Features

| Area | What it does |
|---|---|
| Login | Admin / Faculty / Student tabs, PBKDF2 password hashing, wrong-credential messages, logout, change password |
| Admin | Add/edit/delete/search students and faculty, reset passwords, manage subjects and classrooms (capacity), schedule the timetable, view attendance and reports, post announcements, set attendance threshold |
| Faculty | Own weekly timetable, mark / correct attendance, student list (CSV export), assignments, marks entry, attendance summary with low-attendance highlight, announcements |
| Student | Profile, attendance percentage (overall and per subject), weekly timetable with rooms, assignments with due dates, marks, announcements |
| Attendance | Present / Absent / Late, one record per student + subject + date (checked in code **and** by a UNIQUE key), late counts as attended, configurable threshold (default 75%) |
| Timetable | Rejects: room double-booked, teacher teaching twice, section with two lectures, class larger than room capacity, end time before start time |
| Reports | Attendance by student / subject / date range, per-student summary, class statistics, CSV export |
| **Leave requests** *(new in v2)* | Student applies for leave (dates + reason), sees Pending / Approved / Rejected with the reviewer's remark, can cancel while pending. Admin and faculty approve or reject (a rejection needs a reason). Rules: max 30 days, may start at most 7 days back, no overlap with another pending/approved request |
| **Grade card and rankings** *(new in v2)* | Student sees a credit-weighted SGPA (10-point scale), overall %, pass/fail, class rank, a grade per subject, a bar chart and CSV export. Admin and faculty get a ranked list per class (ties share a rank). Calculated live from the marks, nothing extra is stored |
| **Room booking** *(new in v2)* | Faculty request a room for an extra class / doubt session / event on a date. "Find free rooms" lists only rooms that are big enough and free (no timetable class, no other approved booking, and the teacher is not teaching). Admin approves or rejects; approval re-checks the room so double booking is impossible |

## 2. Project structure

```
SmartClassroom/
├── pom.xml                       Maven build (MySQL driver, FlatLaf, JUnit)
├── db.properties.example         copy to db.properties and put YOUR MySQL password in it
├── sql/schema.sql                creates database + 13 tables + demo data (NEW installs)
├── sql/upgrade_v2.sql            adds only the 2 new tables to an EXISTING database (keeps your data)
├── docs/                         architecture, testing, report, viva Q&A, demo plan
└── src
    ├── main/java/com/smartclassroom
    │   ├── Main.java             entry point
    │   ├── model/                User (abstract) -> Admin, Faculty, Student; Subject, Classroom, TimetableEntry,
    │   │                         AttendanceRecord, AttendanceSummary, Assignment, Mark, Announcement, enums
    │   ├── dao/                  Dao<T> interface + one class per table (all SQL, PreparedStatement only)
    │   ├── service/              business rules: AuthService, StudentService, TimetableService,
    │   │   │                     AttendanceService, LeaveService, RoomBookingService, GradeService ... + AccessControl, DemoDataInitializer
    │   │   └── exception/        AppException -> Validation / Conflict / Authentication / AccessDenied / DataAccess
    │   ├── ui/                   LoginFrame, DashboardFrame, NavButton, PanelFactory
    │   │   ├── theme/            Theme (colours, fonts, look and feel), UiFactory, RoundedPanel
    │   │   ├── common/           CrudPanel<T>, FormDialog, WeeklyTimetablePanel, StatCard, AnnouncementsPanel ...
    │   │   ├── admin/ faculty/ student/    one class per screen
    │   └── util/                 DbConnection, PasswordUtil, Validator, CsvExporter, Session
    └── test/java/com/smartclassroom   JUnit 5 tests (141) with hand-written fake DAOs
```

## 3. Demo accounts (demo data only)

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `Admin@123` |
| Faculty | `faculty1`, `faculty2`, `faculty3` | `Faculty@123` |
| Student | `student1` ... `student6` | `Student@123` |

The SQL script stores these as the placeholder `PENDING`. When the app starts, `DemoDataInitializer` replaces the
placeholder with a real salted PBKDF2 hash. Change or delete these accounts before any real use.

## 4. Setup, step by step

### 4.1 Install the tools
1. **JDK 17 or newer** (Temurin from adoptium.net). Check: `java -version` and `javac -version`.
2. **MySQL 8.0.16 or newer** (MySQL Installer on Windows, `sudo apt install mysql-server` on Ubuntu).
   Remember the root password you set. MySQL Workbench is optional.
3. **Maven 3.8+** (only if you build from a terminal; NetBeans and VS Code have it built in).
4. An IDE: **Apache NetBeans 21+** (File > Open Project > select the `SmartClassroom` folder) or **VS Code** with
   the *Extension Pack for Java*.

### 4.2 Create the database
```bash
mysql -u root -p < sql/schema.sql
```
(or open `sql/schema.sql` in MySQL Workbench and click the lightning button). **The script drops and recreates the
`smart_classroom` database**, so do not run it on a database you want to keep.

Check it worked:
```sql
USE smart_classroom;
SHOW TABLES;                      -- 13 tables
SELECT COUNT(*) FROM users;       -- 10
SELECT COUNT(*) FROM attendance;  -- 18
```

**Already have the v1 database with your own data?** Do not run `schema.sql` (it drops the database). Run this instead,
it only adds the two new tables and is safe to run twice:
```bash
mysql -u root -p smart_classroom < sql/upgrade_v2.sql
```

### 4.3 Tell the app how to reach MySQL
Copy `db.properties.example` to `db.properties` (same folder as `pom.xml`) and edit:
```properties
db.url=jdbc:mysql://localhost:3306/smart_classroom?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000
db.user=root
db.password=your_real_password
```
`db.properties` is in `.gitignore`, so your password is never uploaded to GitHub. Alternatively set the environment
variables `DB_URL`, `DB_USER`, `DB_PASSWORD` (they override the file).

### 4.4 Run
* **NetBeans:** open the project, right-click it > *Run*. (If asked for a main class: `com.smartclassroom.Main`.)
* **VS Code:** open the folder, open `Main.java`, click *Run* above `main`.
* **Terminal:**
  ```bash
  mvn compile exec:java            # run directly
  mvn package                      # build target/smart-classroom.jar
  java -jar target/smart-classroom.jar
  ```
  Run it from the folder that contains `db.properties`.

### 4.5 Run the tests
```bash
mvn test
```
The tests use fake DAOs, so **they do not need MySQL** (the three connection-failure tests deliberately point at a
port where no database exists).

### 4.6 First run checklist (please do this once)
1. `mvn test` shows `Tests run: 141, Failures: 0`.
2. Start the app; the login window opens (if MySQL is wrong you get a clear "Database connection problem" box).
3. Log in as `admin` / `Admin@123` on the **Admin** tab. The dashboard should show 6 students, 3 faculty, 3 classrooms.
4. Log out, log in as `faculty1` (Faculty tab) and `student2` (Student tab). Student2 should show a red attendance alert.
5. Try the v2 features: `student1` > *My Leave* > apply; `admin` > *Leave Requests* > approve. `student1` > *Grade Card* (SGPA, rank, chart).
   `faculty1` > *Room Booking* > request a room; `admin` > *Room Requests* > approve.
6. Follow `docs/DEMO_PLAN.md` for the full walkthrough.

## 5. Troubleshooting

| Message / symptom | Cause and fix |
|---|---|
| `Cannot connect to the database ... Communications link failure` | MySQL is not running (start the MySQL service) or the URL/port in `db.properties` is wrong |
| `Access denied for user 'root'` | wrong password in `db.properties` |
| `Unknown database 'smart_classroom'` | you have not run `sql/schema.sql` |
| `Check constraint syntax error` while running the script | MySQL older than 8.0.16: upgrade to MySQL 8 |
| `Public Key Retrieval is not allowed` | keep `allowPublicKeyRetrieval=true` in the URL |
| Login says *Invalid username or password* for demo users | start the app once so `DemoDataInitializer` can set the demo passwords; make sure the correct role tab is selected |
| `No suitable driver` | the Maven dependencies were not downloaded: run `mvn dependency:resolve` or *Reload project* in the IDE |
| `release version 17 not supported` | your JDK is older than 17 |
| Window looks plain / old style | FlatLaf jar missing: reload Maven dependencies |
| `mysql` not recognised (Windows) | add MySQL's `bin` folder to PATH or use Workbench |
| Maven cannot download anything | check your internet/proxy settings in `~/.m2/settings.xml` |

## 6. Documentation
* `docs/ARCHITECTURE.md` - layers, use-case diagram, class diagram, ER diagram, where each OOP concept is used
* `docs/TESTING.md` - automated tests and manual test cases with expected results
* `docs/PROJECT_REPORT.md` - the college project report
* `docs/VIVA_QA.md` - viva questions and answers
* `docs/DEMO_PLAN.md` - demonstration script and screenshot checklist
