# Design Notes (for the presentation)

These notes explain the main ideas in our project with the real class names, so we can answer questions
about how it is built.

## The big picture

The app has four Java packages and a plain HTML/JS front end:

```
browser (HTML + JS)  ->  controller  ->  service  ->  model
                                            |
                                          util
```

- **model**: the data (`Room`, `Course`, `Instructor`, `Schedule`, `RoomType`).
- **service**: the rules and the stored lists (`RoomManager`, `ScheduleManager`, `ConflictChecker`, ...).
- **util**: small helpers (`TimeUtil`, `ValidationResult`, `IdRules`, `SampleData`).
- **controller**: turns web requests into service calls and results into JSON. It has no business rules.

Because `model`, `service` and `util` have no Spring or web code, we can test the rules on their own
(112 of our 176 tests do exactly that).

## Encapsulation

*Encapsulation means a class keeps its data private and controls how it can change.*

- Every field in `Room`, `Course`, `Instructor` and `Schedule` is `private`.
- Other classes use getters, and changes go through setters that check the value. For example,
  `Room.setCapacity()` throws `IllegalArgumentException("Room capacity must be greater than zero...")`,
  so a `Room` object can never hold a capacity of 0.
- IDs are `final`. A room's `roomId` and a schedule's `scheduleId` can never change after creation.
- `Schedule.setTimeInterval(start, end)` sets both times together and refuses an end that is not after the start,
  so a schedule can never have only one of the two changed.
- The managers keep their lists private (`RoomManager.roomList`, `roomMap`) and give out unmodifiable copies
  (`getAllRooms()`), so nobody can change the data without going through the checks.

## Abstraction

*Abstraction means hiding the details behind a simple method.*

- **`TimeUtil.overlaps(newStart, newEnd, existingStart, existingEnd)`** hides the overlap formula. The rule
  `newStart < existingEnd AND newEnd > existingStart` is written in this one place, and `ConflictChecker` and
  `ScheduleManager.findAvailableRooms()` both call it. A change to the rule happens in one method.
- **`ConflictChecker.checkAllConflicts(...)`** hides four checks (room, instructor, capacity, room type) behind one
  call and returns every problem found.
- **`ValidationResult`** hides success and failure behind one type: `isValid()` and `getErrors()`. The controllers do
  not need to know which rule failed.
- **`ScheduleReferenceChecker`** is a one-method interface (`boolean isReferenced(String id)`). `RoomManager`
  uses it to ask "is this room booked?" without knowing that `ScheduleManager` exists. `ScheduleManager` plugs in
  with `this::isRoomReferenced`. This is also the only place we use polymorphism: the managers call the interface,
  and the real work is done by whatever is plugged in.
- **The controllers** hide the managers from the browser: the page only knows URLs and JSON.

## Composition

*Composition means an object is built from other objects ("has-a").*

- **`Schedule` has a `Course`, an `Instructor` and a `Room`.** A booking is those three things plus a day and a
  time range. The schedule holds the real objects (not just IDs), so a timetable can show the course title,
  instructor name and room name straight from the booking.
- **`ScheduleManager` has a `ConflictChecker`, a `RoomManager`, a `CourseManager` and an `InstructorManager`.**
  It looks up entities in the managers and hands the checks to `ConflictChecker`.
- **`ReportManager` has the four managers** and builds timetables from them.
- **`AppConfig`** creates each service once and passes it to the services that need it (constructor injection).

Composition fits here because these relationships are "has-a", not "is-a": a booking is not a kind of room.

## Why a `RoomType` enum and not Classroom and Laboratory subclasses

We could have written `class Classroom extends Room` and `class Laboratory extends Room`. We used
`enum RoomType { CLASSROOM, LABORATORY }` instead, for these reasons:

1. **The two kinds behave the same.** They have the same fields and the same rules. The only difference is a label
   that the rules compare (`ConflictChecker.checkRoomTypeSuitability()` compares `room.getType()` with
   `course.getRequiredRoomType()`). Subclasses with no different behaviour would be empty classes.
2. **The type can change.** With an enum, editing a room from classroom to laboratory is `room.setType(...)`.
   With subclasses we would have to create a new object and replace the old one in every booking.
3. **Simpler data and JSON.** The type is one value (`"CLASSROOM"`) in the list, the API and the dropdowns.
   Subclasses would need extra code to turn JSON into the right class.
4. **Easy to extend.** A new kind of room (for example `LECTURE_HALL`) is one more enum value.
   A subclass is only worth it when the kinds have different fields or methods. If a laboratory one day had its
   own equipment list and safety rules, we would reconsider.

We still show inheritance and polymorphism ideas in a small way (the interface above), but we did not force
inheritance where the problem does not need it. The PRD asks to use these ideas "where they are justified".

## Other choices worth mentioning

- **Validation collects all errors.** Methods return a `ValidationResult` holding every message, so the user fixes
  everything in one go (the red box in the web page lists all of them).
- **Failed operations change nothing.** Create and update check first and change data only after every check
  passed, so a rejected update keeps the original booking.
- **One error format.** Every API failure is `{"errors": [...]}` with 400, 404 or 409, handled in one class,
  `ApiExceptionHandler`.
- **Java records for DTOs** (`RoomDTO`, `ScheduleRequestDTO`, ...) keep the JSON shape separate from the model
  classes.
- **In-memory storage.** An `ArrayList` keeps the display order and a `HashMap` gives fast lookup by ID.
  The data resets when the server restarts, as the PRD allows for the first release.
- **Honest limits:** single user, no login, and edits to a room or course are not re-checked against existing
  bookings. They are listed in the README.
