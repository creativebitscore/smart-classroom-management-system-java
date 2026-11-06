# Demonstration plan (about 10 minutes) and screenshot checklist

## Before the demo
1. MySQL running; `sql/schema.sql` freshly run (so the demo data is clean).
2. `db.properties` created; start the app once and close it (this sets the demo passwords).
3. Start the app again. Keep this document open.
4. **Dates:** the demo assignments are due on 2 Nov and 5 Nov 2026 and the attendance records are dated 19 and 21 Oct 2026.
   If you demo after those dates the assignments show as "Overdue"; add a new assignment with a future date during the demo.

## Script

| Min | Screen | What to do and say |
|---|---|---|
| 0 | Login | "Three roles, one login screen. Passwords are stored as salted PBKDF2 hashes." Try a wrong password to show the message. |
| 1 | Admin dashboard | `admin` / `Admin@123`. Point at the cards: 6 students, 3 faculty, 3 classrooms, overall attendance 72.2%. Low-attendance alerts list: Priya (CS301 50%), Sneha (CS302 0%), Divya (CS301, CS302 0%). |
| 2 | Students | Search "priya". Add a student with an empty name to show validation, then add a real one. Try the same roll number again to show the duplicate message. Delete shows a confirmation. |
| 3 | Classrooms and Subjects | Show capacities. Add classroom `R-MINI` capacity 5. |
| 4 | Timetable | Weekly grid tab. Then add a class that clashes (cases M16-M18) and a class larger than `R-MINI` (M19) to show every rule. |
| 5 | Attendance | Show red rows. Change the threshold to 50 and save to show it is configurable. |
| 6 | Reports | Generate, show both tabs, class statistics, export CSV and open it. |
| 7 | Logout, Faculty | `faculty1` / `Faculty@123`. My Classes grid. Mark Attendance: today = new, `2026-10-19` = existing (Update mode, no duplicates). Create an assignment. Enter marks. |
| 9 | Student | `student2` / `Student@123`. Dashboard alert, My Attendance in red, timetable with rooms, assignments, marks. |
| 10 | v2 features | `student1`: *My Leave* apply, *Grade Card* (SGPA, rank, bar chart). `faculty1`: *Room Booking* > Find free rooms > request. `admin`: *Leave Requests* approve, *Room Requests* approve, *Class Rankings*. |
| 12 | Close | "UI, service and DAO layers are separate; 141 unit tests cover the rules." |

## Screenshot checklist for the report (File > screenshot, 1280 x 800)
1. Login screen
2. Admin dashboard (cards + alerts)
3. Students list with the search box in use
4. Add student dialog showing a validation message
5. Timetable weekly grid
6. Timetable conflict error message
7. Attendance overview with red rows
8. Mark attendance screen (existing date, "Update attendance")
9. Reports with the summary tab
10. Faculty dashboard
11. Assignments (faculty)
12. Student dashboard
13. Student My Attendance
14. Student My Timetable
15. Student Grade Card (cards + table + bar chart)
16. Leave request approval screen (admin)
17. Room booking dialog after "Find free rooms"
18. Class Rankings
19. Test run output (`mvn test` showing 141 tests, 0 failures)
20. MySQL Workbench showing the 13 tables
