-- =====================================================
-- Smart Classroom Management System - schema + demo data
-- Requires MySQL 8.0.16+ (CHECK constraints are enforced from that version)
-- WARNING: this script DROPS and recreates the smart_classroom database.
-- =====================================================
DROP DATABASE IF EXISTS smart_classroom;
CREATE DATABASE smart_classroom CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_classroom;

-- ---------- Core tables ----------
CREATE TABLE users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           ENUM('ADMIN','FACULTY','STUDENT') NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(100) NOT NULL UNIQUE,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE students (
    student_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL UNIQUE,
    roll_no        VARCHAR(20) NOT NULL UNIQUE,
    phone          VARCHAR(15),
    branch         VARCHAR(50) NOT NULL,
    semester       TINYINT NOT NULL,
    section        VARCHAR(5) NOT NULL,
    CONSTRAINT chk_student_sem CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE faculty (
    faculty_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL UNIQUE,
    employee_code  VARCHAR(20) NOT NULL UNIQUE,
    phone          VARCHAR(15),
    department     VARCHAR(50) NOT NULL,
    designation    VARCHAR(50) NOT NULL,
    CONSTRAINT fk_faculty_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE subjects (
    subject_id     INT AUTO_INCREMENT PRIMARY KEY,
    subject_code   VARCHAR(20) NOT NULL UNIQUE,
    subject_name   VARCHAR(100) NOT NULL,
    branch         VARCHAR(50) NOT NULL,
    semester       TINYINT NOT NULL,
    credits        TINYINT NOT NULL DEFAULT 3,
    CONSTRAINT chk_subject_sem CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT chk_subject_credits CHECK (credits BETWEEN 1 AND 6)
) ENGINE=InnoDB;

CREATE TABLE classrooms (
    classroom_id   INT AUTO_INCREMENT PRIMARY KEY,
    room_name      VARCHAR(30) NOT NULL UNIQUE,
    capacity       INT NOT NULL,
    room_type      ENUM('LECTURE','LAB') NOT NULL DEFAULT 'LECTURE',
    CONSTRAINT chk_capacity CHECK (capacity > 0)
) ENGINE=InnoDB;

CREATE TABLE timetable (
    timetable_id   INT AUTO_INCREMENT PRIMARY KEY,
    subject_id     INT NOT NULL,
    faculty_id     INT NOT NULL,
    classroom_id   INT NOT NULL,
    section        VARCHAR(5) NOT NULL,
    day_of_week    ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY') NOT NULL,
    start_time     TIME NOT NULL,
    end_time       TIME NOT NULL,
    CONSTRAINT chk_time_order CHECK (start_time < end_time),
    CONSTRAINT fk_tt_subject   FOREIGN KEY (subject_id)   REFERENCES subjects(subject_id)     ON DELETE CASCADE,
    CONSTRAINT fk_tt_faculty   FOREIGN KEY (faculty_id)   REFERENCES faculty(faculty_id)      ON DELETE CASCADE,
    CONSTRAINT fk_tt_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(classroom_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE attendance (
    attendance_id   INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    subject_id      INT NOT NULL,
    attendance_date DATE NOT NULL,
    status          ENUM('PRESENT','ABSENT','LATE') NOT NULL,
    marked_by       INT NOT NULL,   -- faculty_id
    CONSTRAINT uq_attendance UNIQUE (student_id, subject_id, attendance_date),
    CONSTRAINT fk_att_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_faculty FOREIGN KEY (marked_by)  REFERENCES faculty(faculty_id)
) ENGINE=InnoDB;

CREATE TABLE assignments (
    assignment_id  INT AUTO_INCREMENT PRIMARY KEY,
    subject_id     INT NOT NULL,
    faculty_id     INT NOT NULL,
    title          VARCHAR(150) NOT NULL,
    description    TEXT,
    due_date       DATE NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_asg_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_asg_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(faculty_id)  ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE marks (
    mark_id        INT AUTO_INCREMENT PRIMARY KEY,
    student_id     INT NOT NULL,
    subject_id     INT NOT NULL,
    exam_type      VARCHAR(30) NOT NULL,        -- e.g. MID1, MID2, ASSIGNMENT
    marks_obtained DECIMAL(5,2) NOT NULL,
    max_marks      DECIMAL(5,2) NOT NULL,
    CONSTRAINT uq_marks UNIQUE (student_id, subject_id, exam_type),
    CONSTRAINT chk_marks CHECK (marks_obtained >= 0 AND marks_obtained <= max_marks AND max_marks > 0),
    CONSTRAINT fk_marks_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_marks_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE announcements (
    announcement_id INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(150) NOT NULL,
    message         TEXT NOT NULL,
    audience        ENUM('ALL','STUDENTS','FACULTY') NOT NULL DEFAULT 'ALL',
    posted_by       INT NOT NULL,   -- user_id
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ann_user FOREIGN KEY (posted_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE settings (
    setting_key    VARCHAR(50) PRIMARY KEY,
    setting_value  VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_tt_room_day    ON timetable(classroom_id, day_of_week);
CREATE INDEX idx_tt_faculty_day ON timetable(faculty_id, day_of_week);
CREATE INDEX idx_att_subject    ON attendance(subject_id, attendance_date);

-- ---------- v2 feature tables: leave requests and room bookings ----------
CREATE TABLE IF NOT EXISTS leave_requests (
    leave_id       INT AUTO_INCREMENT PRIMARY KEY,
    student_id     INT NOT NULL,
    from_date      DATE NOT NULL,
    to_date        DATE NOT NULL,
    reason         VARCHAR(500) NOT NULL,
    status         ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    reviewed_by    INT NULL,        -- user_id of the admin/faculty who decided
    review_remark  VARCHAR(255) NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_leave_dates CHECK (from_date <= to_date),
    CONSTRAINT fk_leave_student  FOREIGN KEY (student_id)  REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_leave_student (student_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS room_bookings (
    booking_id         INT AUTO_INCREMENT PRIMARY KEY,
    classroom_id       INT NOT NULL,
    faculty_id         INT NOT NULL,
    purpose            VARCHAR(150) NOT NULL,
    booking_date       DATE NOT NULL,
    start_time         TIME NOT NULL,
    end_time           TIME NOT NULL,
    expected_attendees INT NOT NULL,
    status             ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    review_remark      VARCHAR(255) NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_booking_time  CHECK (start_time < end_time),
    CONSTRAINT chk_booking_count CHECK (expected_attendees > 0),
    CONSTRAINT fk_booking_room    FOREIGN KEY (classroom_id) REFERENCES classrooms(classroom_id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_faculty FOREIGN KEY (faculty_id)   REFERENCES faculty(faculty_id)      ON DELETE CASCADE,
    INDEX idx_booking_room (classroom_id, booking_date)
) ENGINE=InnoDB;


-- ---------- Demo data (demo accounts only) ----------
-- Passwords are stored as 'PENDING'. When the Java app starts, DemoDataInitializer
-- replaces every PENDING value with a real PBKDF2 hash of the demo password
-- (Admin@123 / Faculty@123 / Student@123).
INSERT INTO settings VALUES ('attendance_threshold', '75');

-- user_ids: 1 admin | 2-4 faculty | 5-10 students
INSERT INTO users (user_id, username, password_hash, role, full_name, email) VALUES
(1,  'admin',    'PENDING', 'ADMIN',   'System Administrator', 'admin@college.edu'),
(2,  'faculty1', 'PENDING', 'FACULTY', 'Dr. Ravi Kumar',       'ravi.kumar@college.edu'),
(3,  'faculty2', 'PENDING', 'FACULTY', 'Prof. Anita Sharma',   'anita.sharma@college.edu'),
(4,  'faculty3', 'PENDING', 'FACULTY', 'Dr. Suresh Reddy',     'suresh.reddy@college.edu'),
(5,  'student1', 'PENDING', 'STUDENT', 'Aarav Mehta',          'aarav@student.edu'),
(6,  'student2', 'PENDING', 'STUDENT', 'Priya Nair',           'priya@student.edu'),
(7,  'student3', 'PENDING', 'STUDENT', 'Rahul Verma',          'rahul@student.edu'),
(8,  'student4', 'PENDING', 'STUDENT', 'Sneha Iyer',           'sneha@student.edu'),
(9,  'student5', 'PENDING', 'STUDENT', 'Karthik Rao',          'karthik@student.edu'),
(10, 'student6', 'PENDING', 'STUDENT', 'Divya Singh',          'divya@student.edu');

INSERT INTO faculty (faculty_id, user_id, employee_code, phone, department, designation) VALUES
(1, 2, 'EMP001', '9000000001', 'CSE', 'Professor'),
(2, 3, 'EMP002', '9000000002', 'CSE', 'Assistant Professor'),
(3, 4, 'EMP003', '9000000003', 'CSE', 'Associate Professor');

INSERT INTO students (student_id, user_id, roll_no, phone, branch, semester, section) VALUES
(1, 5,  'CSE2301', '9100000001', 'CSE', 3, 'A'),
(2, 6,  'CSE2302', '9100000002', 'CSE', 3, 'A'),
(3, 7,  'CSE2303', '9100000003', 'CSE', 3, 'A'),
(4, 8,  'CSE2304', '9100000004', 'CSE', 3, 'A'),
(5, 9,  'CSE2305', '9100000005', 'CSE', 3, 'A'),
(6, 10, 'CSE2306', '9100000006', 'CSE', 3, 'A');

INSERT INTO subjects (subject_id, subject_code, subject_name, branch, semester, credits) VALUES
(1, 'CS301', 'Data Structures',        'CSE', 3, 4),
(2, 'CS302', 'Database Management',    'CSE', 3, 4),
(3, 'CS303', 'Object Oriented Java',   'CSE', 3, 3),
(4, 'MA301', 'Discrete Mathematics',   'CSE', 3, 3);

INSERT INTO classrooms (classroom_id, room_name, capacity, room_type) VALUES
(1, 'R101',  60, 'LECTURE'),
(2, 'R102',  40, 'LECTURE'),
(3, 'LAB-1', 30, 'LAB');

INSERT INTO timetable (subject_id, faculty_id, classroom_id, section, day_of_week, start_time, end_time) VALUES
(1, 1, 1, 'A', 'MONDAY',    '09:00', '10:00'),
(2, 2, 1, 'A', 'MONDAY',    '10:00', '11:00'),
(3, 3, 3, 'A', 'TUESDAY',   '09:00', '11:00'),
(4, 1, 2, 'A', 'TUESDAY',   '11:00', '12:00'),
(1, 1, 1, 'A', 'WEDNESDAY', '09:00', '10:00'),
(2, 2, 2, 'A', 'THURSDAY',  '10:00', '11:00');

-- Attendance: student 6 (Divya) is deliberately low to demo the threshold warning
INSERT INTO attendance (student_id, subject_id, attendance_date, status, marked_by) VALUES
(1,1,'2026-10-19','PRESENT',1),(2,1,'2026-10-19','PRESENT',1),(3,1,'2026-10-19','LATE',1),
(4,1,'2026-10-19','PRESENT',1),(5,1,'2026-10-19','PRESENT',1),(6,1,'2026-10-19','ABSENT',1),
(1,1,'2026-10-21','PRESENT',1),(2,1,'2026-10-21','ABSENT',1),(3,1,'2026-10-21','PRESENT',1),
(4,1,'2026-10-21','PRESENT',1),(5,1,'2026-10-21','PRESENT',1),(6,1,'2026-10-21','ABSENT',1),
(1,2,'2026-10-19','PRESENT',2),(2,2,'2026-10-19','PRESENT',2),(3,2,'2026-10-19','PRESENT',2),
(4,2,'2026-10-19','ABSENT',2),(5,2,'2026-10-19','PRESENT',2),(6,2,'2026-10-19','ABSENT',2);

INSERT INTO assignments (subject_id, faculty_id, title, description, due_date) VALUES
(1, 1, 'Linked List Implementation', 'Implement singly and doubly linked lists in Java.', '2026-11-02'),
(2, 2, 'ER Diagram Case Study',      'Design an ER diagram for a library system.',        '2026-11-05');

INSERT INTO marks (student_id, subject_id, exam_type, marks_obtained, max_marks) VALUES
(1,1,'MID1',22,25),(2,1,'MID1',18,25),(3,1,'MID1',20,25),
(4,1,'MID1',15,25),(5,1,'MID1',23,25),(6,1,'MID1',10,25);

INSERT INTO announcements (title, message, audience, posted_by) VALUES
('Mid-Term Schedule', 'Mid-term exams begin on 10 November. Check the timetable for details.', 'ALL', 1),
('Lab Record Submission', 'Submit Java lab records by Friday.', 'STUDENTS', 2);

-- v2 demo data
INSERT INTO leave_requests (student_id, from_date, to_date, reason, status, reviewed_by, review_remark) VALUES
(2, '2026-11-10', '2026-11-11', 'Fever, doctor advised rest.', 'PENDING',  NULL, NULL),
(6, '2026-10-19', '2026-10-21', 'Family function out of town.', 'REJECTED', 2, 'Please attach a proper reason and apply earlier.');

INSERT INTO room_bookings (classroom_id, faculty_id, purpose, booking_date, start_time, end_time, expected_attendees, status, review_remark) VALUES
(1, 1, 'Extra class: Data Structures revision', '2026-11-07', '14:00', '16:00', 40, 'APPROVED', 'Approved'),
(2, 2, 'Doubt clearing session',               '2026-11-09', '15:00', '16:00', 25, 'PENDING',  NULL);
