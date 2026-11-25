# Project Rules & Development Guidelines

> **Project:** Classroom and Laboratory Scheduling & Management System (CLSMS) Web Application  
> **Target Audience:** Beginner-to-intermediate student development team  
> **Source Documents:** `docs/PRD.md` (v1.0 baseline) & `docs/DESIGN.md`  
> **Important PRD Note:** While the PRD originally discussed a console interface, this project is implemented as a **Web Application**. All UI requirements target a REST API + vanilla HTML/CSS/JS frontend. Everything else from the PRD (scope, requirements, conflict logic, validation, acceptance criteria, testing strategy) strictly applies.

---

## 1. Project Rules (Apply to Every Stage)

- **Technology Stack:** Java 17 or newer, Maven, Spring Boot 3 with **only** `spring-boot-starter-web` and `spring-boot-starter-test`. JUnit 5 for tests. **No database, no JPA, no Spring Security, no other external libraries.**
- **In-Memory Storage:** Data is stored in memory (`ArrayList` plus `HashMap` for ID lookup) and resets when the server restarts.
- **Package Structure:** Strictly four packages: `model`, `service`, `controller`, `util`.
- **Frontend Architecture:** Plain HTML, CSS, and vanilla JavaScript in `src/main/resources/static`. **No React, no npm, no build tools, no CDN frameworks.** It communicates with the backend through a REST API under `/api` using browser `fetch`.
- **UI Visual Target:** The visual target for the UI is `docs/mockup.png`. Match its layout, colors, and wording.
- **Room Type Representation:** Use a `RoomType` enum (`CLASSROOM`, `LABORATORY`). Do **not** create `Classroom` or `Laboratory` subclasses.
- **Date & Time Types:** Use `java.time.DayOfWeek` and `java.time.LocalTime` for days and times.
- **Encapsulation & Validation:** All fields are `private`. Validate data in constructors and setters. Throw `IllegalArgumentException` with a clear message for bad values.
- **Single Overlap Method:** The interval overlap check lives in exactly **ONE** method in `util/TimeUtil`. Every overlap check in the project must call it. Never rewrite the formula elsewhere.
- **Clean Architecture & Separation of Concerns:**
  - `model`, `service`, and `util` classes are plain Java: **no Spring annotations, no HTTP code**.
  - One `AppConfig` class in the `controller` package registers the services as Spring beans. This keeps business logic completely isolated and testable on its own.
  - Controllers contain **no business logic**. They call services and convert results to JSON.
- **Comprehensive Validation:** Validation collects **ALL** errors and returns them together. It never stops at the first problem.
- **Atomic Operations:** Failed operations never leave partial changes in the data.
- **Standardized API Error Format:** `{"errors": ["message 1", "message 2"]}` with status:
  - `400 Bad Request` for invalid input / malformed data.
  - `404 Not Found` for unknown IDs.
  - `409 Conflict` for timetable conflicts and duplicate IDs/codes.
- **Fault Tolerance:** Nothing may crash on bad input. The UI displays the API's error messages clearly.

---

## 2. 10-Stage Development Protocol

Development proceeds strictly through 10 incremental stages: Stage 0 (setup) followed by Steps 1 through 9.

1. **One Stage at a Time:** Do ONE stage at a time. Never start the next stage on your own and never build features from a later stage.
2. **Mandatory Checkpoint Message:** When a stage is finished, STOP and send a checkpoint message containing:
   - What you built, in plain language.
   - The files you created or changed.
   - How the user can run and test it themselves, with exact commands.
   - The test results (run the tests and show real output).
   - A short explanation of the key classes or ideas, written so a student could explain them in a presentation.
   - Any decisions made or anything in the PRD that was unclear.
   - The required prompt question: *"Do you approve this step? Reply 'continue' to move on, or tell me what to change."*
3. **Wait for Approval:** Do NOT continue until the user explicitly approves. If changes are requested, make them, re-run tests, show results, and ask again.
4. **Clarify Ambiguities Upfront:** If something at the start of a stage is ambiguous, ask the user before writing code with short, numbered questions and proposed defaults.
5. **Git Commit Suggestion:** Suggest a Git commit message at the end of each approved stage.
6. **Simplicity & Readability:** Keep code simple with short comments. Do not over-engineer.
7. **Design Adjustments:** If a later stage reveals that an earlier design decision was wrong, notify the user, propose the fix, and wait for approval before changing earlier work.
