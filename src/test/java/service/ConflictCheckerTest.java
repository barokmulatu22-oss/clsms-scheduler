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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConflictChecker Service Tests")
class ConflictCheckerTest {

    private ConflictChecker conflictChecker;
    private Room room1;
    private Room room2;
    private Course courseSmall;
    private Course courseLarge;
    private Course courseLabOnly;
    private Instructor instructor1;
    private Instructor instructor2;
    private List<Schedule> existingSchedules;

    @BeforeEach
    void setUp() {
        conflictChecker = new ConflictChecker();
        room1 = new Room("R1", "Room 1", RoomType.CLASSROOM, "Building A", 30, null);
        room2 = new Room("R2", "Room 2", RoomType.LABORATORY, "Building B", 50, null);

        courseSmall = new Course("CS101", "Intro CS", 25, null, "CS", "01");
        courseLarge = new Course("CS102", "Big Class", 35, null, "CS", "01");
        courseLabOnly = new Course("CS103L", "Lab Work", 20, RoomType.LABORATORY, "CS", "L1");

        instructor1 = new Instructor("I1", "Dr. One", "CS", "one@univ.edu", null);
        instructor2 = new Instructor("I2", "Dr. Two", "CS", "two@univ.edu", null);

        existingSchedules = new ArrayList<>();
    }

    @Test
    @DisplayName("Reports multiple conflicts simultaneously without failing fast")
    void testSeveralErrorsReportedAtOnce() {
        // Schedule R1 Monday 09:00-10:00 with instructor1
        existingSchedules.add(new Schedule("S-001", courseSmall, instructor1, room1,
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        // Request on Monday 09:30-10:30 with:
        // 1. Same room R1 (Room conflict)
        // 2. Same instructor I1 (Instructor conflict)
        // 3. Course size 35 in room capacity 30 (Capacity conflict)
        // 4. Lab-only course in Classroom R1 (Room type conflict)
        Course labBigCourse = new Course("BIO999", "Advanced Lab", 40, RoomType.LABORATORY, "Biology", "01");

        ValidationResult result = conflictChecker.checkAllConflicts(
                labBigCourse, instructor1, room1, DayOfWeek.MONDAY,
                LocalTime.of(9, 30), LocalTime.of(10, 30),
                existingSchedules, null
        );

        assertFalse(result.isValid());
        assertEquals(4, result.getErrors().size(), "Must detect all 4 distinct conflicts");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.startsWith("Room conflict")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.startsWith("Instructor conflict")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.startsWith("Capacity conflict")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.startsWith("Room type conflict")));
    }

    @Test
    @DisplayName("Detects deactivated room conflict")
    void testDeactivatedRoomConflict() {
        room1.setActive(false);
        ValidationResult result = conflictChecker.checkAllConflicts(
                courseSmall, instructor1, room1, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0),
                existingSchedules, null
        );

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("deactivated and cannot be scheduled"));
    }

    @Test
    @DisplayName("Ignores excludeScheduleId during conflict checking")
    void testExcludeScheduleIdIgnored() {
        Schedule existing = new Schedule("S-001", courseSmall, instructor1, room1,
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        existingSchedules.add(existing);

        // Edit S-001 to same slot, excluding S-001
        ValidationResult result = conflictChecker.checkAllConflicts(
                courseSmall, instructor1, room1, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0),
                existingSchedules, "S-001"
        );

        assertTrue(result.isValid(), "Self-conflict must be ignored when excludeScheduleId is provided");
    }
}
