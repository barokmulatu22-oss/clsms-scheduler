# CLSMS Test Report (Step 8)

**Result: 176 automated tests, 0 failures** (`.\mvnw.cmd test`), plus a manual walkthrough of all six web pages.

> Update after Step 9: the app now starts empty (`clsms.load-sample-data=false`). The controller tests that
> rely on the PRD sample data turn it on with `@SpringBootTest(properties = "clsms.load-sample-data=true")`,
> and `EmptyStartTest` checks the empty default. The browser walkthrough in section 7 was done with the sample data loaded.

> **Assumption:** `docs/PRD.md` stops at section 13, so PRD sections 17 (acceptance criteria) and 18.2
> (integration tests) were not available as text. We used PRD section 13 (test strategy) as the acceptance
> criteria, the FR/NFR tables as the requirements, and the demo flow from the Step 8 instructions for 18.2.
> If sections 17 / 18.2 add anything else, send them and we will add rows.

## 1. Acceptance criteria (PRD section 13)

| # | Criterion | How it was verified | Result |
|---|-----------|---------------------|--------|
| A1 | Overlap logic: exact, partial, containment, equal, back-to-back | `TimeUtilTest` (17 unit tests); `AcceptanceCriteriaTest.roomOverlapRulesAcrossTheApi` runs all five cases through the API | Pass |
| A2 | Same-room conflicts, same/different days, same/different rooms | `ConflictCheckerTest`, `ScheduleManagerTest`; `AcceptanceCriteriaTest.sameRoomOnAnotherDayOrAnotherRoomIsFine` | Pass |
| A3 | Instructor conflicts, same/different days, same/different instructors | `AcceptanceCriteriaTest.instructorOverlapRulesAcrossTheApi`; `ScheduleControllerTest.instructorConflictIsReported` | Pass |
| A4 | Capacity at equal, below and over | `AcceptanceCriteriaTest.capacityRuleAtEqualBelowAndOver` (30 = 30 ok, 10 ok, 31 rejected) | Pass |
| A5 | Failed update keeps the original | `ScheduleControllerTest.conflictingUpdateKeepsTheOriginal`; `ScheduleManagerTest`; `DemoFlowIntegrationTest` step 5 | Pass |
| A6 | Availability search against known bookings | `AcceptanceCriteriaTest.availabilityMatchesKnownBookings`; `AvailabilityControllerTest`; `SearchAndAvailabilityTest` | Pass |

## 2. Functional requirements

| ID | Priority | Requirement | How it was verified | Result |
|----|----------|-------------|---------------------|--------|
| FR-01 | Must | Create a room with unique ID, name, type, location, capacity | `RoomManagerTest`, `RoomControllerTest.createRoomThenItAppearsInList`, `...duplicateRoomGives409`; browser: Rooms page add + duplicate | Pass |
| FR-02 | Must | View, search, list, edit rooms; remove/deactivate if not referenced | `RoomControllerTest` (search, update, delete, deactivate); browser: Rooms page | Pass |
| FR-03 | Must | Course CRUD with unique code, title, student count | `CourseManagerTest`, `CourseControllerTest`; browser: Courses page | Pass |
| FR-04 | Must | Instructor CRUD with unique ID, name | `InstructorManagerTest`, `InstructorControllerTest`; browser: Instructors page | Pass |
| FR-05 | Must | Create a schedule from course, instructor, room, day, times | `ScheduleControllerTest.validScheduleIsCreated`; browser: New booking form | Pass |
| FR-06 | Must | Validate booking input (required values, IDs, day/time, start before end) | `ScheduleControllerTest.badInputGives400WithEveryMessage`, `...endBeforeStartGives400`, `...unknownIdsGive404`; browser: empty form shows 5 errors | Pass |
| FR-07 | Must | Reject overlapping room bookings (same room, same day) | `AcceptanceCriteriaTest.roomOverlapRulesAcrossTheApi`; `DemoFlowIntegrationTest` step 4; browser: Schedule page | Pass |
| FR-08 | Must | Reject overlapping instructor bookings | `AcceptanceCriteriaTest.instructorOverlapRulesAcrossTheApi`; `DemoFlowIntegrationTest` step 4; browser: Schedule page | Pass |
| FR-09 | Must | Room capacity >= course size | `AcceptanceCriteriaTest.capacityRuleAtEqualBelowAndOver`; browser: capacity error shown | Pass |
| FR-10 | Should | Room type compatibility | `AcceptanceCriteriaTest.courseThatNeedsALabIsRejectedInAClassroom`; browser: room type error shown | Pass |
| FR-11 | Must | Update a booking with validation; keep the old record on failure | `ScheduleControllerTest.validUpdateChangesTheBooking`, `...conflictingUpdateKeepsTheOriginal`; browser: edit flow | Pass |
| FR-12 | Must | Cancel a booking and confirm the action | `ScheduleControllerTest.cancellingFreesTheSlot`; browser: confirm prompt, "No" keeps it, "Yes" cancels | Pass |
| FR-13 | Must | List bookings in order by day and start time | `ScheduleManagerTest`; `DemoFlowIntegrationTest` step 3 (order checked) | Pass |
| FR-14 | Must | Search by room, course, instructor, day, time | `ScheduleControllerTest.filtersNarrowTheList`; `DemoFlowIntegrationTest` step 3; browser: room/instructor/course filters | Pass |
| FR-15 | Must | Find free rooms for a day/time with optional capacity/type | `AvailabilityControllerTest` (7 tests); browser: Availability page | Pass |
| FR-16 | Must | Clear confirmation on success, clear error on failure | Browser: green box after every success, red box listing every API message after failures | Pass |
| FR-17 | Should | Summary views by day, room, instructor | `ReportControllerTest` (7 tests); browser: Reports page | Pass |
| FR-18 | Could | Save data across runs | Not built (optional extra) | Not built |
| FR-19 | Could | Read-only viewer mode | Not built (optional extra) | Not built |

## 3. Non-functional requirements

| ID | Requirement | How it was verified | Result |
|----|-------------|---------------------|--------|
| NFR-01 | Understandable messages | Browser: every error message read on screen | Pass |
| NFR-02 | Rules applied consistently before saving | All conflict tests; one `TimeUtil.overlaps()` method (see section 5) | Pass |
| NFR-03 | Clear responsibilities | Code review: model / service / controller / util split; controllers call services only | Pass |
| NFR-04 | Fast on a small data set | `ScheduleControllerTest` (12 tests, each starting a fresh app) runs in about 4 s; every single request answers in milliseconds | Pass |
| NFR-05 | Bad input does not crash | `AcceptanceCriteriaTest` (text body, JSON array, wrong types, fractional numbers, odd IDs); `ApiExceptionHandler`; browser: server stopped, pages show a message | Pass |
| NFR-06 | Portable | Standard Java 17 + Spring Boot only | Pass |
| NFR-07 | Data integrity | Failed update/create tests; see known limit 2 below | Pass, with a known limit |
| NFR-08 | Core logic testable without the UI | 112 service/model/util tests run with no web server | Pass |

## 4. Integration tests

| Test | What it covers |
|------|----------------|
| `DemoFlowIntegrationTest.fullDemoFlow` | One end-to-end run on a fresh app, API only: create rooms, course, instructor; create 3 schedules; view, filter and report; room conflict and instructor conflict rejected with details (IDs and times in the message) and nothing saved; valid update; conflicting update that keeps the original; availability; cancel; rebook the freed slot; final summary |
| `AcceptanceCriteriaTest` (10 tests) | PRD section 13 matrix plus bad-input robustness |
| Six `*ControllerTest` classes (52 tests) | Every endpoint, status code and error format |

## 5. Code review findings

| Check | Result |
|-------|--------|
| Overlap formula outside `TimeUtil.overlaps()` | None. Searched for `isBefore`/`isAfter`: the other uses are "end must be after start" checks and a single-time "is this minute inside a booking" filter, not an interval overlap. All overlap checks call `TimeUtil.overlaps()` |
| Failed update leaves partial changes | No. `ScheduleManager.updateSchedule` and the Room/Course/Instructor updates validate everything first, then change fields |
| Unchecked parsing | `TimeUtil.parseTime/parseDay` throw `IllegalArgumentException`; controllers catch it while validating, and `ApiExceptionHandler` catches the rest (this fixed the 500 on `?time=banana` in 7A) |
| Places bad input could crash | Three found, fixed (see section 6) |

## 6. Bugs found and fixed

| # | Bug | How found | Fix |
|---|-----|-----------|-----|
| 1 | IDs with spaces or symbols (`A/B`, `A B`, `A?B`) were accepted. A room named `A/B` could not be opened, edited or deleted afterwards because the slash breaks the URL. DESIGN.md section 4 requires `[A-Z0-9-]+` | New test `idsWithUrlCharactersAreRejected` returned 201 | New `util/IdRules`; `RoomManager`, `CourseManager` and `InstructorManager` reject bad IDs with a clear message |
| 2 | A capacity of `2.5` was silently saved as `2` | New test `fractionalCapacityIsRejectedInsteadOfRounded` returned 201 | `spring.jackson.deserialization.accept-float-as-int=false` in `application.properties` (now 400 with "Invalid value '2.5' for field 'capacity'.") |
| 3 | Sending a body that is not JSON (e.g. `text/plain`) gave a 500 | New test `textBodyInsteadOfJsonGives400Not500` returned 500 | `ApiExceptionHandler` now returns 400 "Request body must be JSON" |

## 7. Browser walkthrough (built-in browser, app started with `mvnw spring-boot:run`)

| Page | What was done | Result |
|------|---------------|--------|
| Schedule | Submitted the empty form; booked PHY210/I-002/L-01 Mon 09:00-10:00; tried MAT202 in the same room at 09:30; tried the same instructor in another room at 09:30; booked the same instructor 10:00-11:00; opened a block and edited it; cancelled with the confirm prompt; used the instructor and room filters | 5 errors listed; S-001 created; 3 errors listed (capacity, type, room) and the form kept its values; instructor conflict shown; back-to-back accepted; edit conflict shown, valid edit saved; "No" kept the booking and "Yes" removed it; filters narrowed the grid |
| Rooms | Empty submit; duplicate ID + negative capacity; ID `A/B`; capacity `2.5`; valid add; edit with capacity 0 then 42; delete a booked room; delete a free room; search | 4 errors; 2 errors; ID message; "Invalid value" message; added; error then saved; booked room became Inactive with the API message; free room removed; search worked |
| Courses | Empty submit; duplicate code; bad code `bad code`; add; delete a booked course; delete a free course; search | All errors shown; "Cannot delete..." for the booked course |
| Instructors | Empty submit; bad ID `I 5/x`; add; edit; delete assigned; delete free | All errors shown; edit saved; "Cannot delete..." for the assigned instructor |
| Availability | Empty form; 10:00-11:00 Monday; capacity 50; lab only; end before start | Missing-parameter errors; correct rooms listed; error shown and the result line reset |
| Reports | Summary; daily Monday and Tuesday; room timetable; instructor timetable; instructor with no bookings | Correct counts and tables; "There are no bookings" for empty ones |
| All pages | Server stopped, then submitted a form and changed a filter; 375px wide window | "Cannot reach the server..." message, form kept, no blank page; no sideways page scroll on any page |

## 8. Known limits (not bugs for this release)

1. In-memory data resets when the server restarts; single user, no login.
2. Editing a room (smaller capacity or different type) or a course (more students or a different required type) is **not** re-checked against bookings that already exist, so an old booking can become invalid. The PRD does not require this check. A fix would be to validate such edits against the schedule, which changes earlier approved code, so we left it for your decision.
3. The sample course CS201 (35 students, needs a lab) cannot be booked into any sample room because the largest sample lab holds 30.
4. Two requests at exactly the same moment are not synchronized (single-user app, as the PRD says).
5. The Reports summary says "1 Bookings" with one booking (wording only).
6. `docs/mockup.png` was never supplied, so the UI follows the written rules, not the mockup.
