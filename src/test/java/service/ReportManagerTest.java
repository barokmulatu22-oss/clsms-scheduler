package service;

import model.Course;
import model.Instructor;
import model.Room;
import model.RoomType;
import model.Schedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReportManager Service Tests")
class ReportManagerTest {

    private RoomManager roomManager;
    private CourseManager courseManager;
    private InstructorManager instructorManager;
    private ScheduleManager scheduleManager;
    private ReportManager reportManager;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManager();
        courseManager = new CourseManager();
        instructorManager = new InstructorManager();
        ConflictChecker conflictChecker = new ConflictChecker();
        scheduleManager = new ScheduleManager(conflictChecker, roomManager, courseManager, instructorManager);
        reportManager = new ReportManager(scheduleManager, roomManager, courseManager, instructorManager);

        // Add rooms
        roomManager.addRoom("C-101", "Room 101", RoomType.CLASSROOM, "Main", 40, null);
        roomManager.addRoom("L-01", "Lab 01", RoomType.LABORATORY, "Science", 25, null);

        // Add courses
        courseManager.addCourse("CS101", "Intro CS", 30, null, "CS", "01");
        courseManager.addCourse("CS201", "OOP Java", 20, RoomType.LABORATORY, "CS", "01");

        // Add instructors
        instructorManager.addInstructor("I-001", "Dr. Alice", "CS", "alice@univ.edu", null);
        instructorManager.addInstructor("I-002", "Dr. Bob", "CS", "bob@univ.edu", null);

        // Add bookings
        // Monday 14:00 - 15:00
        scheduleManager.createSchedule("CS201", "I-001", "L-01", DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));
        // Monday 09:00 - 10:00 (added later, should sort first chronologically)
        scheduleManager.createSchedule("CS101", "I-001", "C-101", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        // Tuesday 10:00 - 11:00
        scheduleManager.createSchedule("CS101", "I-002", "C-101", DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));
    }

    @Test
    @DisplayName("getDailyTimetable returns schedules for day sorted by start time")
    void testDailyTimetable() {
        List<Schedule> mondaySchedules = reportManager.getDailyTimetable(DayOfWeek.MONDAY);

        assertEquals(2, mondaySchedules.size());
        assertEquals(LocalTime.of(9, 0), mondaySchedules.get(0).getStartTime(), "09:00 booking must come first");
        assertEquals(LocalTime.of(14, 0), mondaySchedules.get(1).getStartTime(), "14:00 booking must come second");

        List<Schedule> fridaySchedules = reportManager.getDailyTimetable(DayOfWeek.FRIDAY);
        assertTrue(fridaySchedules.isEmpty());
    }

    @Test
    @DisplayName("getRoomTimetable returns schedules for room grouped by DayOfWeek")
    void testRoomTimetableGroupedByDay() {
        Map<DayOfWeek, List<Schedule>> timetable = reportManager.getRoomTimetable("C-101");

        assertEquals(2, timetable.size());
        assertTrue(timetable.containsKey(DayOfWeek.MONDAY));
        assertTrue(timetable.containsKey(DayOfWeek.TUESDAY));

        List<Schedule> mondayList = timetable.get(DayOfWeek.MONDAY);
        assertEquals(1, mondayList.size());
        assertEquals("CS101", mondayList.get(0).getCourse().getCourseCode());

        // Invalid or empty room ID returns empty map
        assertTrue(reportManager.getRoomTimetable("NON-EXISTENT").isEmpty());
    }

    @Test
    @DisplayName("getInstructorTimetable returns schedules for instructor grouped by DayOfWeek")
    void testInstructorTimetableGroupedByDay() {
        Map<DayOfWeek, List<Schedule>> timetable = reportManager.getInstructorTimetable("I-001");

        assertEquals(1, timetable.size());
        assertTrue(timetable.containsKey(DayOfWeek.MONDAY));

        List<Schedule> mondayList = timetable.get(DayOfWeek.MONDAY);
        assertEquals(2, mondayList.size());
        // Must be sorted by start time: 09:00 then 14:00
        assertEquals(LocalTime.of(9, 0), mondayList.get(0).getStartTime());
        assertEquals(LocalTime.of(14, 0), mondayList.get(1).getStartTime());

        // Invalid instructor returns empty map
        assertTrue(reportManager.getInstructorTimetable("NON-EXISTENT").isEmpty());
    }

    @Test
    @DisplayName("getSystemSummary returns accurate counts for all entities")
    void testSystemSummary() {
        Map<String, Object> summary = reportManager.getSystemSummary();

        assertEquals(2, summary.get("totalRooms"));
        assertEquals(2L, summary.get("activeRooms"));
        assertEquals(1L, summary.get("totalClassrooms"));
        assertEquals(1L, summary.get("totalLaboratories"));
        assertEquals(2, summary.get("totalCourses"));
        assertEquals(2, summary.get("totalInstructors"));
        assertEquals(3, summary.get("totalSchedules"));
    }
}
