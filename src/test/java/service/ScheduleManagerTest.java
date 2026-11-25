package service;

import model.Course;
import model.Instructor;
import model.Room;
import model.RoomType;
import model.Schedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.ValidationResult;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScheduleManager Service Tests")
class ScheduleManagerTest {

    private RoomManager roomManager;
    private CourseManager courseManager;
    private InstructorManager instructorManager;
    private ConflictChecker conflictChecker;
    private ScheduleManager scheduleManager;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManager();
        courseManager = new CourseManager();
        instructorManager = new InstructorManager();
        conflictChecker = new ConflictChecker();

        scheduleManager = new ScheduleManager(conflictChecker, roomManager, courseManager, instructorManager);

        // Standard test entities matching PRD section 18.3 matrix
        roomManager.addRoom("R1", "Room 1", RoomType.CLASSROOM, "Building A", 30, null);
        roomManager.addRoom("R2", "Room 2", RoomType.CLASSROOM, "Building B", 50, null);
        roomManager.addRoom("LAB1", "Lab 1", RoomType.LABORATORY, "Science Block", 40, null);

        courseManager.addCourse("CS101", "Programming", 25, null, "CS", "01");
        courseManager.addCourse("CS102", "Big Course", 35, null, "CS", "01");
        courseManager.addCourse("CS201L", "Lab Course", 20, RoomType.LABORATORY, "CS", "L1");

        instructorManager.addInstructor("I1", "Dr. One", "CS", "one@univ.edu", "111");
        instructorManager.addInstructor("I2", "Dr. Two", "Physics", "two@univ.edu", "222");
    }

    @Test
    @DisplayName("Matrix Row 1: Room R1 Monday 09:00-10:00; request R1 Monday 09:30-10:30 -> Reject room overlap")
    void testMatrixRow1RoomOverlap() {
        ValidationResult first = scheduleManager.createSchedule(
                "CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertTrue(first.isValid());

        // Overlapping request in R1 on Monday
        ValidationResult conflict = scheduleManager.createSchedule(
                "CS101", "I2", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(10, 30));
        assertFalse(conflict.isValid());
        assertTrue(conflict.getErrors().get(0).startsWith("Room conflict"));
        assertEquals(1, scheduleManager.getAllSchedules().size());
    }

    @Test
    @DisplayName("Matrix Row 2: Room R1 Monday 09:00-10:00; request R1 Monday 10:00-11:00 -> Accept back-to-back")
    void testMatrixRow2BackToBackAllowed() {
        ValidationResult first = scheduleManager.createSchedule(
                "CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertTrue(first.isValid());

        ValidationResult backToBack = scheduleManager.createSchedule(
                "CS101", "I2", "R1", DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));
        assertTrue(backToBack.isValid());
        assertEquals(2, scheduleManager.getAllSchedules().size());
    }

    @Test
    @DisplayName("Matrix Row 3: Instructor I1 Monday 09:00-10:00 in R1; request I1 Monday 09:30-10:30 in R2 -> Reject instructor overlap")
    void testMatrixRow3InstructorOverlapAcrossDifferentRooms() {
        ValidationResult first = scheduleManager.createSchedule(
                "CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertTrue(first.isValid());

        // Same instructor I1 requested in different room R2 at overlapping time
        ValidationResult conflict = scheduleManager.createSchedule(
                "CS101", "I1", "R2", DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(10, 30));
        assertFalse(conflict.isValid());
        assertTrue(conflict.getErrors().get(0).startsWith("Instructor conflict"));
        assertEquals(1, scheduleManager.getAllSchedules().size());
    }

    @Test
    @DisplayName("Matrix Row 4: R1 Monday 09:00-10:00; request R1 Tuesday 09:00-10:00 -> Accept different day")
    void testMatrixRow4DifferentDayAllowed() {
        ValidationResult first = scheduleManager.createSchedule(
                "CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertTrue(first.isValid());

        ValidationResult tuesday = scheduleManager.createSchedule(
                "CS101", "I1", "R1", DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertTrue(tuesday.isValid());
        assertEquals(2, scheduleManager.getAllSchedules().size());
    }

    @Test
    @DisplayName("Matrix Row 5: Room capacity 30; enrollment 35 -> Reject insufficient capacity")
    void testMatrixRow5InsufficientCapacity() {
        // Course CS102 enrollment is 35, room R1 capacity is 30
        ValidationResult result = scheduleManager.createSchedule(
                "CS102", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).startsWith("Capacity conflict"));
        assertEquals(0, scheduleManager.getAllSchedules().size());
    }

    @Test
    @DisplayName("Matrix Row 6: Cancel existing schedule, then rebook same room/time -> Accept after cancellation")
    void testMatrixRow6CancelThenRebookSucceeds() {
        ValidationResult first = scheduleManager.createSchedule(
                "CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertTrue(first.isValid());
        String scheduleId = scheduleManager.getAllSchedules().get(0).getScheduleId();

        // Cancel the schedule
        ValidationResult cancelResult = scheduleManager.cancelSchedule(scheduleId);
        assertTrue(cancelResult.isValid());
        assertEquals(0, scheduleManager.getAllSchedules().size());

        // Rebook exactly the same room, day, and time
        ValidationResult rebook = scheduleManager.createSchedule(
                "CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertTrue(rebook.isValid(), "Rebooking after cancellation must succeed");
        assertEquals(1, scheduleManager.getAllSchedules().size());
    }

    @Test
    @DisplayName("Reports several validation and conflict errors at once")
    void testSeveralErrorsReportedAtOnce() {
        // Book R1 Monday 09:00-10:00 with I1
        scheduleManager.createSchedule("CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        // Add a lab course with enrollment 40 (CS201L has enrollment 20, let's add CS999 with 50)
        courseManager.addCourse("CS999", "Huge Lab", 50, RoomType.LABORATORY, "CS", "01");

        // Booking on Monday 09:30-10:30 with:
        // 1. Same room R1 (Room conflict)
        // 2. Same instructor I1 (Instructor conflict)
        // 3. Enrollment 50 in capacity 30 (Capacity conflict)
        // 4. Lab course in Classroom R1 (Room type conflict)
        ValidationResult result = scheduleManager.createSchedule(
                "CS999", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(10, 30));

        assertFalse(result.isValid());
        assertEquals(4, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.startsWith("Room conflict")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.startsWith("Instructor conflict")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.startsWith("Capacity conflict")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.startsWith("Room type conflict")));
    }

    @Test
    @DisplayName("Failed update preserves the original schedule unchanged")
    void testFailedUpdatePreservesOriginal() {
        // Booking 1: R1 Monday 09:00-10:00
        scheduleManager.createSchedule("CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        // Booking 2: R2 Monday 14:00-15:00
        scheduleManager.createSchedule("CS101", "I2", "R2", DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));

        String schedule2Id = scheduleManager.getAllSchedules().get(1).getScheduleId();

        // Attempt to update Booking 2 to conflict with Booking 1 (move to R1 at 09:30)
        ValidationResult failedUpdate = scheduleManager.updateSchedule(
                schedule2Id, "CS101", "I2", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(10, 30));

        assertFalse(failedUpdate.isValid());

        // Booking 2 must remain completely unchanged
        Schedule s2 = scheduleManager.getScheduleById(schedule2Id).orElseThrow();
        assertEquals("R2", s2.getRoom().getRoomId());
        assertEquals(LocalTime.of(14, 0), s2.getStartTime());
        assertEquals(LocalTime.of(15, 0), s2.getEndTime());
    }

    @Test
    @DisplayName("Valid update succeeds and modifies schedule fields")
    void testValidUpdateSucceeds() {
        scheduleManager.createSchedule("CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        String scheduleId = scheduleManager.getAllSchedules().get(0).getScheduleId();

        ValidationResult updateResult = scheduleManager.updateSchedule(
                scheduleId, "CS101", "I2", "R2", DayOfWeek.WEDNESDAY, LocalTime.of(11, 0), LocalTime.of(12, 30));

        assertTrue(updateResult.isValid());

        Schedule updated = scheduleManager.getScheduleById(scheduleId).orElseThrow();
        assertEquals("R2", updated.getRoom().getRoomId());
        assertEquals("I2", updated.getInstructor().getInstructorId());
        assertEquals(DayOfWeek.WEDNESDAY, updated.getDay());
        assertEquals(LocalTime.of(11, 0), updated.getStartTime());
        assertEquals(LocalTime.of(12, 30), updated.getEndTime());
    }

    @Test
    @DisplayName("Rejects creation with non-existent course, instructor, or room")
    void testRejectsUnknownEntities() {
        ValidationResult result = scheduleManager.createSchedule(
                "UNKNOWN_COURSE", "UNKNOWN_INST", "UNKNOWN_ROOM",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Course with code 'UNKNOWN_COURSE' does not exist")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Instructor with ID 'UNKNOWN_INST' does not exist")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Room with ID 'UNKNOWN_ROOM' does not exist")));
    }

    @Test
    @DisplayName("Integrates referential integrity checks with entity managers")
    void testReferentialIntegrityWithEntityManagers() {
        scheduleManager.createSchedule("CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        // 1. Room R1 is referenced: deletion must deactivate R1
        ValidationResult roomDelete = roomManager.deleteRoom("R1");
        assertTrue(roomDelete.isValid());
        assertFalse(roomManager.getRoomById("R1").orElseThrow().isActive(), "Referenced room must be deactivated");

        // 2. Course CS101 is referenced: deletion must be blocked
        ValidationResult courseDelete = courseManager.deleteCourse("CS101");
        assertFalse(courseDelete.isValid());
        assertTrue(courseDelete.getErrors().get(0).contains("referenced by active schedule"));
        assertTrue(courseManager.getCourseByCode("CS101").isPresent());

        // 3. Instructor I1 is referenced: deletion must be blocked
        ValidationResult instDelete = instructorManager.deleteInstructor("I1");
        assertFalse(instDelete.isValid());
        assertTrue(instDelete.getErrors().get(0).contains("assigned to active schedule"));
        assertTrue(instructorManager.getInstructorById("I1").isPresent());
    }

    @Test
    @DisplayName("getAllSchedules returns bookings sorted chronologically by day then start time")
    void testGetAllSchedulesChronologicalOrder() {
        scheduleManager.createSchedule("CS101", "I1", "R1", DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));
        scheduleManager.createSchedule("CS101", "I1", "R1", DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0));
        scheduleManager.createSchedule("CS101", "I2", "R2", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        List<Schedule> all = scheduleManager.getAllSchedules();
        assertEquals(3, all.size());
        // First should be Monday 09:00
        assertEquals(DayOfWeek.MONDAY, all.get(0).getDay());
        assertEquals(LocalTime.of(9, 0), all.get(0).getStartTime());
        // Second should be Monday 11:00
        assertEquals(DayOfWeek.MONDAY, all.get(1).getDay());
        assertEquals(LocalTime.of(11, 0), all.get(1).getStartTime());
        // Third should be Wednesday 14:00
        assertEquals(DayOfWeek.WEDNESDAY, all.get(2).getDay());
        assertEquals(LocalTime.of(14, 0), all.get(2).getStartTime());
    }
}
