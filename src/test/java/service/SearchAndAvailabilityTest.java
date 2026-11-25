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

@DisplayName("Search and Room Availability Service Tests")
class SearchAndAvailabilityTest {

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

        // Populate baseline rooms
        roomManager.addRoom("C-101", "Classroom 101", RoomType.CLASSROOM, "Main Building", 30, null);
        roomManager.addRoom("C-205", "Classroom 205", RoomType.CLASSROOM, "Main Building", 60, null);
        roomManager.addRoom("L-01", "Lab 01", RoomType.LABORATORY, "Science Block", 24, null);
        roomManager.addRoom("L-02", "Lab 02", RoomType.LABORATORY, "Engineering Block", 40, null);

        // Populate baseline courses
        courseManager.addCourse("CS101", "Intro Programming", 20, null, "CS", "01");
        courseManager.addCourse("CS201", "OOP Java", 35, RoomType.LABORATORY, "CS", "01");
        courseManager.addCourse("MAT101", "Calculus", 25, null, "Math", "01");

        // Populate baseline instructors
        instructorManager.addInstructor("I-001", "Dr. Alice", "CS", "alice@univ.edu", null);
        instructorManager.addInstructor("I-002", "Dr. Bob", "Math", "bob@univ.edu", null);

        // Create sample schedules
        // S1: Monday 09:00 - 10:00, CS101, I-001 in C-101
        scheduleManager.createSchedule("CS101", "I-001", "C-101", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        // S2: Monday 14:00 - 16:00, CS201, I-001 in L-02
        scheduleManager.createSchedule("CS201", "I-001", "L-02", DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0));
        // S3: Wednesday 10:00 - 11:30, MAT101, I-002 in C-205
        scheduleManager.createSchedule("MAT101", "I-002", "C-205", DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(11, 30));
    }

    @Test
    @DisplayName("Filter by roomId alone")
    void testFilterRoomAlone() {
        List<Schedule> results = scheduleManager.searchSchedules("C-101", null, null, null, null);
        assertEquals(1, results.size());
        assertEquals("C-101", results.get(0).getRoom().getRoomId());
    }

    @Test
    @DisplayName("Filter by courseCode alone")
    void testFilterCourseAlone() {
        List<Schedule> results = scheduleManager.searchSchedules(null, "CS201", null, null, null);
        assertEquals(1, results.size());
        assertEquals("CS201", results.get(0).getCourse().getCourseCode());
    }

    @Test
    @DisplayName("Filter by instructorId alone")
    void testFilterInstructorAlone() {
        List<Schedule> results = scheduleManager.searchSchedules(null, null, "I-001", null, null);
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Filter by day alone")
    void testFilterDayAlone() {
        List<Schedule> mondayResults = scheduleManager.searchSchedules(null, null, null, DayOfWeek.MONDAY, null);
        assertEquals(2, mondayResults.size());

        List<Schedule> wedResults = scheduleManager.searchSchedules(null, null, null, DayOfWeek.WEDNESDAY, null);
        assertEquals(1, wedResults.size());
    }

    @Test
    @DisplayName("Filter by time alone (matching active sessions)")
    void testFilterTimeAlone() {
        // At 09:30, only S1 (09:00-10:00) is active
        List<Schedule> at0930 = scheduleManager.searchSchedules(null, null, null, null, LocalTime.of(9, 30));
        assertEquals(1, at0930.size());
        assertEquals("CS101", at0930.get(0).getCourse().getCourseCode());

        // At 14:30, only S2 (14:00-16:00) is active
        List<Schedule> at1430 = scheduleManager.searchSchedules(null, null, null, null, LocalTime.of(14, 30));
        assertEquals(1, at1430.size());
        assertEquals("CS201", at1430.get(0).getCourse().getCourseCode());
    }

    @Test
    @DisplayName("Combined search filters work together")
    void testCombinedFilters() {
        List<Schedule> results = scheduleManager.searchSchedules("C-101", "CS101", "I-001", DayOfWeek.MONDAY, LocalTime.of(9, 15));
        assertEquals(1, results.size());

        // Mismatched combined filter returns empty
        List<Schedule> noMatch = scheduleManager.searchSchedules("C-101", "CS101", "I-001", DayOfWeek.TUESDAY, LocalTime.of(9, 15));
        assertEquals(0, noMatch.size());
    }

    @Test
    @DisplayName("Search returns empty list when no schedules match")
    void testSearchNoResults() {
        List<Schedule> results = scheduleManager.searchSchedules("NON-EXISTENT", null, null, null, null);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Search results are sorted chronologically by day then start time")
    void testSearchSortOrder() {
        List<Schedule> all = scheduleManager.searchSchedules(null, null, null, null, null);
        assertEquals(3, all.size());
        // Mon 09:00 first, Mon 14:00 second, Wed 10:00 third
        assertEquals(DayOfWeek.MONDAY, all.get(0).getDay());
        assertEquals(LocalTime.of(9, 0), all.get(0).getStartTime());

        assertEquals(DayOfWeek.MONDAY, all.get(1).getDay());
        assertEquals(LocalTime.of(14, 0), all.get(1).getStartTime());

        assertEquals(DayOfWeek.WEDNESDAY, all.get(2).getDay());
        assertEquals(LocalTime.of(10, 0), all.get(2).getStartTime());
    }

    @Test
    @DisplayName("findAvailableRooms: Booked room is excluded from available results")
    void testFindAvailableRoomsBookedRoomExcluded() {
        // C-101 is booked on Monday 09:00-10:00
        // Querying Monday 09:30-10:30 must exclude C-101
        List<Room> available = scheduleManager.findAvailableRooms(
                DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(10, 30), null, null);

        assertFalse(available.stream().anyMatch(r -> r.getRoomId().equals("C-101")));
        assertTrue(available.stream().anyMatch(r -> r.getRoomId().equals("C-205")));
        assertTrue(available.stream().anyMatch(r -> r.getRoomId().equals("L-01")));
        assertTrue(available.stream().anyMatch(r -> r.getRoomId().equals("L-02")));
    }

    @Test
    @DisplayName("findAvailableRooms: Back-to-back room is INCLUDED (not blocked)")
    void testFindAvailableRoomsBackToBackIncluded() {
        // C-101 is booked on Monday 09:00-10:00
        // Querying Monday 10:00-11:00 must INCLUDE C-101
        List<Room> available = scheduleManager.findAvailableRooms(
                DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), null, null);

        assertTrue(available.stream().anyMatch(r -> r.getRoomId().equals("C-101")),
                "Back-to-back slot (10:00-11:00) must include room booked 09:00-10:00");
    }

    @Test
    @DisplayName("findAvailableRooms: Filters by minimum capacity")
    void testFindAvailableRoomsCapacityFilter() {
        // Min capacity 35: C-101 (30) and L-01 (24) excluded; C-205 (60) and L-02 (40) included
        List<Room> available = scheduleManager.findAvailableRooms(
                DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 35, null);

        assertEquals(2, available.size());
        assertTrue(available.stream().anyMatch(r -> r.getRoomId().equals("C-205")));
        assertTrue(available.stream().anyMatch(r -> r.getRoomId().equals("L-02")));
    }

    @Test
    @DisplayName("findAvailableRooms: Filters by room type")
    void testFindAvailableRoomsTypeFilter() {
        // Required type: LABORATORY
        List<Room> available = scheduleManager.findAvailableRooms(
                DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, RoomType.LABORATORY);

        assertEquals(2, available.size());
        assertTrue(available.stream().allMatch(r -> r.getType() == RoomType.LABORATORY));
    }

    @Test
    @DisplayName("findAvailableRooms: Deactivated rooms are strictly excluded")
    void testFindAvailableRoomsDeactivatedExcluded() {
        roomManager.getRoomById("C-205").orElseThrow().setActive(false);

        List<Room> available = scheduleManager.findAvailableRooms(
                DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, null);

        assertFalse(available.stream().anyMatch(r -> r.getRoomId().equals("C-205")));
    }

    @Test
    @DisplayName("findAvailableRooms: Room becomes free after schedule cancellation")
    void testFindAvailableRoomsFreeAfterCancellation() {
        // C-101 is booked on Monday 09:00-10:00
        Schedule s1 = scheduleManager.searchSchedules("C-101", null, null, DayOfWeek.MONDAY, null).get(0);

        // Verify C-101 is NOT available
        List<Room> beforeCancel = scheduleManager.findAvailableRooms(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, null);
        assertFalse(beforeCancel.stream().anyMatch(r -> r.getRoomId().equals("C-101")));

        // Cancel the booking
        ValidationResult cancelResult = scheduleManager.cancelSchedule(s1.getScheduleId());
        assertTrue(cancelResult.isValid());

        // Now C-101 MUST be available
        List<Room> afterCancel = scheduleManager.findAvailableRooms(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, null);
        assertTrue(afterCancel.stream().anyMatch(r -> r.getRoomId().equals("C-101")),
                "Room must become available immediately after schedule cancellation");
    }
}
