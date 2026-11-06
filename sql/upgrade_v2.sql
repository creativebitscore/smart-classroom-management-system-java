-- =====================================================
-- Smart Classroom - UPGRADE to v2 (leave requests + room bookings)
-- Safe to run on an EXISTING database: it only ADDS two tables and keeps all your data.
-- Run:  mysql -u root -p smart_classroom < sql/upgrade_v2.sql
-- (New installs do not need this file: sql/schema.sql already contains these tables.)
-- =====================================================
USE smart_classroom;

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

