# Classroom and Laboratory Scheduling & Management System (CLSMS)

A small web app that helps an administrator manage rooms, courses and instructors, and book classes into a
weekly timetable. Before a booking is saved, the app checks it against the existing bookings, so the same room
or the same instructor cannot be booked twice at the same time.

## What it does

- Keep records of **rooms** (classroom or laboratory), **courses** and **instructors**: add, edit, search, delete.
- Create, edit and cancel **bookings**: one course, one instructor, one room, one day, one time range.
- Reject bookings that have a **room conflict**, an **instructor conflict**, a room that is **too small** for the
  course, or a **wrong room type**. All problems are shown together. Back-to-back bookings (09:00-10:00 then
  10:00-11:00) are allowed.
- Show a **weekly timetable** (Monday to Friday) with blue blocks for classrooms and green blocks for laboratories.
- Filter bookings by room, instructor and course.
- **Find available rooms** for a day and time, with optional minimum capacity and room type.
- **Reports**: daily, room and instructor timetables, plus summary numbers.

## What you need

- Java 17 or newer (check with `java -version`)
- Nothing else. Maven is included through the `mvnw` wrapper, and the first run downloads the libraries.

## Build and run

```
.\mvnw.cmd spring-boot:run
```

Then open **http://localhost:8080** in your browser. Stop the server with Ctrl+C.

The app starts **empty**. Add rooms, courses and instructors on their pages first, then create bookings.

To start with the sample data from the PRD (4 rooms, 3 instructors, 3 courses) instead, for example for the demo:

```
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--clsms.load-sample-data=true"
```

## Run the tests

```
.\mvnw.cmd test
```

There are 176 tests. They cover the model, service and util classes, every REST endpoint, the PRD test strategy,
and one end-to-end demo flow. The results are explained in [docs/TEST_REPORT.md](docs/TEST_REPORT.md).

## Using the web pages

| Page | File | What you do there |
|------|------|-------------------|
| Schedule (home) | `index.html` | See the weekly timetable, filter it, create a booking, click a block to edit or cancel it |
| Rooms | `rooms.html` | Add, edit, search and delete rooms (a booked room is deactivated instead of deleted) |
| Courses | `courses.html` | Add, edit, search and delete courses |
| Instructors | `instructors.html` | Add, edit, search and delete instructors |
| Room Availability | `availability.html` | Find rooms that are free at a given time |
| Reports | `reports.html` | Daily, room and instructor timetables |

A demo walkthrough is in [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md).

## Project structure

```
src/main/java
  model/        Room, Course, Instructor, Schedule, RoomType (plain Java data classes)
  service/      RoomManager, CourseManager, InstructorManager, ScheduleManager,
                ConflictChecker, ReportManager, ScheduleReferenceChecker (business rules)
  util/         TimeUtil (the one overlap check), ValidationResult, IdRules, SampleData
  controller/   REST controllers, AppConfig, ApiExceptionHandler, Application,
                dto/ (request and response records)
src/main/resources
  application.properties
  static/       the web pages: *.html, *.js, styles.css
src/test/java   tests for every package
docs/           PRD, DESIGN, DESIGN_NOTES, DEMO_SCRIPT, TEST_REPORT
```

How the parts fit together:

1. The browser pages call the REST API under `/api` with `fetch` (only `api.js` does this).
2. A **controller** receives the request and calls a **service** (manager).
3. The service checks the rules and changes the in-memory data, then returns a `ValidationResult`.
4. The controller turns the result into JSON: the data, or `{"errors": ["..."]}` with status 400, 404 or 409.

`model`, `service` and `util` have no Spring code. `AppConfig` is the only place that creates the services as beans.

## REST API in short

| Path | What it does |
|------|--------------|
| `/api/rooms`, `/api/courses`, `/api/instructors` | list (with `?search=`), get, create, update, delete |
| `/api/schedules` | list (with `?room=&course=&instructor=&day=&time=`), get, create, update, cancel |
| `/api/availability?day=&start=&end=&minCapacity=&type=` | free rooms |
| `/api/reports/daily?day=`, `/api/reports/room/{id}`, `/api/reports/instructor/{id}`, `/api/reports/summary` | reports |

Errors always look like `{"errors": ["message 1", "message 2"]}`: **400** for invalid input, **404** for an unknown
ID, **409** for a conflict or a duplicate. The full list is in [docs/DESIGN.md](docs/DESIGN.md).

## Known limits

- **Data is kept in memory.** It is lost when the server stops, and the app starts empty again (or with the sample data if you start it with the flag above).
- **Single user, no login.** There are no accounts, roles or read-only mode.
- Editing a room or a course (for example a smaller capacity) is not re-checked against bookings that already
  exist.
- The weekly timetable is a fixed pattern (day of week plus time). There are no dates, holidays or repeating rules.
- The sample course CS201 (35 students, needs a lab) cannot be booked into any sample room, because the biggest
  sample lab has 30 seats.
