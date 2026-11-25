# CLSMS Demo Script (about 7 minutes)

This script is for a live demo in the browser. Every step says what to click or type and what should happen.
We checked every expected result against the running app.

## Before you start

1. Start a fresh app **with the sample data** (the app normally starts empty, and data resets on every restart):
   ```
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--clsms.load-sample-data=true"
   ```
2. Open http://localhost:8080 in a browser.
3. The app now has the sample data below: 4 rooms, 3 instructors, 3 courses and no bookings.
   (If you started it empty, add these by hand first on the Rooms, Instructors and Courses pages.)

| Rooms | Type | Seats |
|-------|------|-------|
| C-101 Classroom 101 | Classroom | 40 |
| C-205 Classroom 205 | Classroom | 60 |
| L-01 Laboratory 01 | Laboratory | 24 |
| L-02 Laboratory 02 | Laboratory | 30 |

| Instructors | | Courses | Students | Needs |
|---|---|---|---|---|
| I-001 Dr. Example (CS) | | CS201 Object-Oriented Programming | 35 | Laboratory |
| I-002 Dr. Example (Physics) | | PHY210 Modern Physics | 22 | Laboratory |
| I-003 Dr. Example (Math) | | MAT202 Discrete Mathematics | 45 | Classroom |

## Part 1: Set up (1 minute)

**Step 1. Look at the data.** Click **Rooms**, **Courses** and **Instructors** in the left menu.
Expected: the tables show the sample data above. Mention the search box on each page.

**Step 2. Add a course.** Click **Courses**. Fill in: Course code `GEN101`, Title `Intro Seminar`,
Expected students `20`, Required room type `Any type`. Click **Add course**.
Expected: green box "Course GEN101 added." and a new row in the table.

## Part 2: Bookings and conflicts (3 minutes)

Click **Schedule** in the menu. Use the "New booking" form on the right.

**Step 3. Empty form.** Click **Create booking** without choosing anything.
Expected: red box "Please fix these problems:" listing 5 messages (course, instructor, room, start time, end time).
The API reports all the problems together.

**Step 4. A valid booking.** Course `PHY210`, Instructor `Dr. Example (Physics)`, Room `L-01`,
Day `Monday`, Start `09:00`, End `10:00`. Click **Create booking**.
Expected: green box "Booking S-001 created: PHY210 in Laboratory 01 on Monday 09:00 - 10:00." and a green
(laboratory) block on Monday at 09:00.

**Step 5. A second valid booking at the same time in another room.** Course `MAT202`,
Instructor `Dr. Example (Math)`, Room `C-205`, Day `Monday`, Start `09:00`, End `10:30`.
Expected: "Booking S-002 created". The blue (classroom) block appears next to the green one.

**Step 6. Room conflict.** Course `GEN101`, Instructor `Dr. Example (CS)`, Room `C-205`,
Monday, Start `10:00`, End `11:00`. Click **Create booking**.
Expected: red box with one message: "Room conflict: Room 'C-205' is already booked on MONDAY from 09:00 to 10:30
for course MAT202 (Schedule ID: S-002)." The form keeps what you typed.

**Step 7. Instructor conflict.** Course `GEN101`, Instructor `Dr. Example (Physics)`, Room `C-101`,
Monday, Start `09:30`, End `10:30`.
Expected: red box: "Instructor conflict: Instructor 'Dr. Example (Physics)' (I-002) is already scheduled on MONDAY
from 09:00 to 10:00 in room L-01 for course PHY210 (Schedule ID: S-001)."

**Step 8. Several problems at once.** Course `MAT202`, Instructor `Dr. Example (Math)`, Room `L-01`,
Monday, Start `09:30`, End `10:30`.
Expected: red box with **4** messages: capacity (45 students, room holds 24), room type (needs a classroom,
L-01 is a laboratory), room conflict with S-001, instructor conflict with S-002.

**Step 9. Capacity check.** Course `CS201`, Instructor `Dr. Example (CS)`, Room `L-02`,
Tuesday, Start `09:00`, End `10:00`.
Expected: one message: "Capacity conflict: Course enrollment (35) exceeds room capacity (30) for room 'L-02'."
(CS201 can never be booked in the sample rooms because the biggest lab has 30 seats. This is the rule working.)

**Step 10. Back-to-back is allowed.** Course `GEN101`, Instructor `Dr. Example (Physics)`, Room `C-101`,
Monday, Start `10:00`, End `11:00`.
Expected: "Booking S-003 created". The instructor's first class ends at 10:00 and this one starts at 10:00,
so there is no overlap.

## Part 3: Search and find rooms (1.5 minutes)

**Step 11. Search by instructor.** On the Schedule page, set the **Instructor** filter to `Dr. Example (Physics)`.
Expected: only S-001 and S-003 stay in the timetable.

**Step 12. Search by room.** Clear the filters, then set **Room** to `C-205`.
Expected: only the MAT202 block (S-002).

**Step 13. Search by course.** Clear the filters, then set **Course** to `MAT202`.
Expected: only S-002. Click **Clear filters** to show everything again.

**Step 14. Find available rooms.** Click **Room Availability**. Day `Monday`, Start `09:30`, End `10:30`.
Click **Find rooms**.
Expected: "1 room(s) free on Monday 09:30 - 10:30." and only L-02 in the table.
Now set Minimum capacity `50` and search again. Expected: "No rooms are free on Monday 09:30 - 10:30 with these filters."
Clear the capacity and set Start `11:00`, End `12:00`. Expected: all 4 rooms are free
(the bookings that end at 10:30 and 11:00 do not block it).

## Part 4: Update, cancel and rebook (1.5 minutes)

**Step 15. A conflicting update is refused.** Back on **Schedule**, click the GEN101 block on Monday 10:00
(S-003). The form changes to "Edit booking S-003". Change Start to `09:00` and click **Save changes**.
Expected: red box with the instructor conflict with S-001. The block on the timetable did not move: the
original booking is kept.

**Step 16. A valid update.** Set Start back to `10:00` and End to `11:30`. Click **Save changes**.
Expected: green box "Booking S-003 updated: GEN101 in Classroom 101 on Monday 10:00 - 11:30." The block gets taller.

**Step 17. Cancel.** Click the PHY210 block (S-001). Click **Cancel this booking**. A browser prompt asks
"Cancel booking S-001? This cannot be undone." Click OK.
Expected: green box "Schedule 'S-001' was cancelled successfully." and the block disappears.

**Step 18. Rebook the freed slot.** Create PHY210 / `Dr. Example (Physics)` / `L-01` / Monday / `09:00` - `10:00` again.
Expected: "Booking S-004 created". The slot that was taken a minute ago is now free again.

## Part 5: Reports (optional, 30 seconds)

**Step 19.** Click **Reports**. Expected: summary cards (rooms, courses, instructors, bookings). Choose
"Room timetable" and room `L-01`, click **Show report**, and the bookings of that room appear grouped by day.

## If something goes wrong

- A page shows "Cannot reach the server": the app is not running. Start it again with the command above.
- The data looks different from this script: restart the app with the command above to get the clean sample data back.
