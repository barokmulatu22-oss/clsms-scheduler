# Classroom and Laboratory Scheduling & Management System (CLSMS)
**Product Requirements Document (PRD)**  
**Version:** 1.0  
**Status:** Project planning / requirements baseline  
**Project Type:** Java console application (initial release)  
**Primary User:** Administrator / scheduling operator  
**Target:** Student team project  

---

## 1. Executive Summary
The Classroom and Laboratory Scheduling & Management System (CLSMS) is a Java application intended to help an institution organize shared classrooms and laboratories. It provides a structured place to maintain room, course, instructor, and schedule records, then checks requested bookings before they are saved. The primary value is reducing double-bookings, making room availability easier to inspect, and making timetables easier to retrieve.

The first release is deliberately scoped for a student team: a single administrator-facing application, in-memory data storage during a run (with optional file persistence as a stretch goal), a clearly defined weekly timetable, and deterministic conflict checking. It is not intended to reproduce a full university enterprise scheduling platform.

---

## 2. Background and Problem Statement
Institutions share rooms across courses, instructors, and student groups. When scheduling is performed manually or across disconnected records, it is easy to assign the same room to two classes at the same time, schedule an instructor in overlapping sessions, or overlook a room's capacity or type. Staff may also spend unnecessary time checking where a class is held or whether a room is free.

The project addresses these issues through centralized room/course/instructor records and schedule operations that validate each booking against the existing schedule before accepting it.

---

## 3. Product Vision and Goals
- **Centralize:** keep facility, course, instructor, and timetable information in one application.
- **Prevent:** reject conflicting room and instructor bookings before a schedule is saved.
- **Make availability visible:** let an administrator find rooms free for a selected day and time.
- **Improve retrieval:** view and search timetables by room, instructor, or course.
- **Demonstrate OOP:** use classes, encapsulation, composition, abstraction, inheritance, and polymorphism where they are justified.

---

## 4. Objectives and Success Metrics
| Objective | Success Measure / Target |
| :--- | :--- |
| **Avoid double-booking** | No overlapping accepted schedules for the same room. |
| **Avoid instructor conflicts** | No overlapping accepted schedules for the same instructor. |
| **Room suitability** | Booking validation rejects rooms that are too small or have an incompatible room type (if configured). |
| **Usable schedule lookup** | Administrator can filter schedule list by room, course, instructor, day, and/or time. |
| **Maintainable design** | Responsibilities are separated across entity, manager/service, validation, and presentation classes. |
| **Demonstrable project** | Team can show all core workflows and tests in a live demo. |

---

## 5. Users and Personas

### 5.1 Administrator / Scheduling Operator
Primary user who enters and updates institutional data and creates schedules. This is the only required user role for the initial version.
- **Needs:** simple navigation, clear validation, accurate schedules, searchable records, and helpful conflict messages.
- **Typical actions:** add room, register course, register instructor, create booking, review timetable, find available rooms, update or cancel booking.

### 5.2 Viewer (Optional)
A read-only user role may be added if time permits. A viewer can browse timetable information but cannot modify master records or bookings. Authentication and role enforcement are stretch goals, not required for the core build.

---

## 6. Scope

### 6.1 In Scope — MVP
- Manage classrooms and laboratories (create, edit, deactivate/delete, list, search).
- Manage courses and instructors (create, edit, remove, list, search).
- Create, update, view, and cancel schedule entries.
- Validate room and instructor availability using day and time overlap checks.
- Validate room capacity against course/student-group size and room type where applicable.
- Search/view schedules by room, course, instructor, day, and time.
- Display available rooms for a requested day and time.
- Provide simple summary reports / timetable views.
- Use Java collections such as `ArrayList` and `HashMap` where appropriate.

### 6.2 Out of Scope — Initial Release
- Automatic optimization that generates an institution-wide timetable.
- Student registration, attendance, grades, or learning-management features.
- Web application, mobile app, cloud synchronization, or multi-user live editing.
- Database integration, unless selected as an extension after the core system works.
- Authentication, permissions, audit logs, notifications, and calendar integrations (optional enhancements).
- Complex recurring-event rules, exceptions, holidays, exam periods, or rotating weeks.

---

## 7. Assumptions and Constraints
- The first version is a single-user application used by an administrator.
- Schedules use a fixed weekly pattern: day of week plus start/end time.
- A booking represents one course/session, one instructor, one room, one day, and one time range.
- Start and end times are entered in 24-hour format; end time must be later than start time.
- Data may initially be stored in collections in memory and reset when the application exits. File/database persistence is an optional milestone.
- The team is developing with beginner/intermediate Java knowledge and should prioritize correctness and demonstrable features over advanced frameworks.

---

## 8. User Workflows

### 8.1 Create a Schedule
1. Administrator selects Create Schedule.
2. Select a course, instructor, room, day, start time, and end time.
3. System validates required values and time range.
4. System checks room and instructor overlap, room capacity, and room type.
5. If validation passes, booking is saved and a confirmation is shown.
6. If validation fails, booking is not saved and the user receives a reason.

### 8.2 Update a Schedule
Administrator selects an existing booking and edits its fields. The system revalidates the proposed booking while excluding the booking being edited from conflict comparisons. If the updated booking is valid, it replaces the original; otherwise the original remains unchanged.

### 8.3 Find Available Rooms
Administrator enters a day and time range and optionally a minimum capacity or room type. The system returns rooms that meet the filters and have no overlapping booking in the requested interval.

### 8.4 View / Search Timetable
Administrator selects a search filter (room, course, instructor, day, or time). Matching bookings are shown in chronological order, including course, room, instructor, day, start/end time, and relevant enrollment/capacity details.

---

## 9. Functional Requirements
| ID | Requirement | Priority |
| :--- | :--- | :--- |
| **FR-01** | Create a room with unique room ID, name/number, type, location, capacity, and optional description. | Must |
| **FR-02** | View, search, and list rooms; edit room details; remove or deactivate a room if it is not referenced by active schedules. | Must |
| **FR-03** | Create, view, search, edit, and delete course records with unique course code, title, student count, and optional department/section. | Must |
| **FR-04** | Create, view, search, edit, and delete instructor records with unique instructor ID, name, and optional department/contact fields. | Must |
| **FR-05** | Create a schedule by selecting course, instructor, room, day, start time, and end time. | Must |
| **FR-06** | Validate booking input: required fields, valid IDs, valid day/time, start before end, and no invalid values. | Must |
| **FR-07** | Reject overlapping room bookings for the same room and day. | Must |
| **FR-08** | Reject overlapping bookings for the same instructor and day. | Must |
| **FR-09** | Check room capacity is greater than or equal to course/group size. | Must |
| **FR-10** | Check room type compatibility (classroom vs laboratory) when a course requires a type. | Should |
| **FR-11** | Update an existing booking with validation; preserve the old record if validation fails. | Must |
| **FR-12** | Cancel/remove a booking and confirm the action. | Must |
| **FR-13** | List all bookings and display in chronological order by day and start time. | Must |
| **FR-14** | Search timetable by room, course, instructor, day, and time filters. | Must |
| **FR-15** | Find rooms free for a requested day/time interval and optional capacity/type criteria. | Must |
| **FR-16** | Show a clear confirmation for successful operations and a clear error for failed operations. | Must |
| **FR-17** | Produce summary views: schedules by day, by room, and by instructor; optional counts/utilization. | Should |
| **FR-18** | Persist data to a file or database across application runs (optional for MVP). | Could |
| **FR-19** | Provide read-only viewer mode (optional). | Could |

---

## 10. Scheduling Rules and Conflict Logic

### 10.1 Time Overlap Definition
Two time intervals overlap if the new start time is earlier than an existing end time AND the new end time is later than an existing start time. This means back-to-back bookings are allowed (e.g., 09:00–10:00 and 10:00–11:00), while partial, contained, and exact overlaps are rejected.

**Overlap formula:**
$$\text{newStart} < \text{existingEnd} \quad \text{AND} \quad \text{newEnd} > \text{existingStart}$$
Apply the rule only when the day is the same and the resource (room or instructor) is the same.

### 10.2 Conflict Types
- **Room conflict:** same room, same day, intersecting time intervals.
- **Instructor conflict:** same instructor, same day, intersecting time intervals.
- **Capacity conflict:** requested group size exceeds room capacity.
- **Room type conflict:** course requires a laboratory but selected room is a classroom (or other incompatible type).

### 10.3 Validation Behavior
- All validation should occur before saving.
- The system should report all detected reasons when possible, rather than stopping after the first issue.
- Editing a schedule should ignore the schedule's own ID during conflict checks.
- If validation fails, no partial change is made.

---

## 11. Data Requirements and Domain Model
- **Room:** `roomId`, `roomName/number`, `type`, `capacity`, `location`, `active`. A room may have many schedule records at different times.
- **Course:** `courseId/code`, `title`, `expectedEnrollment`, `requiredRoomType` (optional), `section`. A course may appear in many scheduled sessions.
- **Instructor:** `instructorId`, `fullName`, `department`, optional `email`/`phone`. An instructor may teach multiple courses and schedule entries.
- **Schedule:** `scheduleId`, `course`, `instructor`, `room`, `day`, `startTime`, `endTime`. Each booking links one course, one instructor, one room, one day/time.
- **Administrator:** `adminId`, `name` (optional). MVP may represent administrator as the application user, without login.
- **ConflictChecker:** validation methods. Service class; checks constraints but does not own entity records.
- **ScheduleManager:** schedule collection and CRUD/validation orchestration. Coordinates creating, updating, cancelling, querying schedules.
- **ReportManager:** report/query methods. Builds timetable and summary outputs from stored data.

---

## 12. Non-Functional Requirements
- **NFR-01 Usability:** Menus and prompts should be understandable to a first-time user; errors should explain how to correct input.
- **NFR-02 Correctness:** Conflict and suitability rules must be applied consistently before a booking is saved.
- **NFR-03 Maintainability:** Classes should have clear responsibilities; avoid one giant main class.
- **NFR-04 Performance:** For a small institution-sized sample dataset, search and validation should complete quickly (< 1 second).
- **NFR-05 Reliability:** Invalid input should not crash the application; recover to the menu where possible.
- **NFR-06 Portability:** Use standard Java libraries; avoid OS-specific behavior.
- **NFR-07 Data Integrity:** Unique IDs/codes; schedule references should point to existing entities; failed updates must not corrupt data.
- **NFR-08 Testability:** Conflict checking and core logic should be testable independently of the console interface.

---

## 13. Acceptance Criteria & Test Strategy
- Interval overlap logic: exact overlap, partial overlap, containment, equal intervals, and back-to-back intervals.
- Same-room conflicts across same/different days and same/different rooms.
- Instructor conflicts across same/different days and instructors.
- Capacity validation at equal capacity, below capacity, and over capacity.
- Create/update validation ensuring failed updates preserve original schedule.
- Availability search against known bookings.
