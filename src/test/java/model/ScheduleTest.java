package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Schedule Entity Tests")
class ScheduleTest {

    private Room sampleRoom;
    private Course sampleCourse;
    private Instructor sampleInstructor;

    @BeforeEach
    void setUp() {
        sampleRoom = new Room("C-101", "Lecture Hall 1", RoomType.CLASSROOM, "Main Building", 40, null, true);
        sampleCourse = new Course("CS201", "OOP", 35, RoomType.CLASSROOM, "CS", "01");
        sampleInstructor = new Instructor("I-001", "Dr. Jane Doe", "CS", "jane@univ.edu", "555-0100");
    }

    @Test
    @DisplayName("Successfully creates valid Schedule")
    void testValidScheduleCreation() {
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);

        Schedule schedule = new Schedule("S-001", sampleCourse, sampleInstructor, sampleRoom, DayOfWeek.MONDAY, start, end);

        assertEquals("S-001", schedule.getScheduleId());
        assertEquals(sampleCourse, schedule.getCourse());
        assertEquals(sampleInstructor, schedule.getInstructor());
        assertEquals(sampleRoom, schedule.getRoom());
        assertEquals(DayOfWeek.MONDAY, schedule.getDay());
        assertEquals(start, schedule.getStartTime());
        assertEquals(end, schedule.getEndTime());
        assertTrue(schedule.toString().contains("S-001"));
        assertTrue(schedule.toString().contains("CS201"));
    }

    @Test
    @DisplayName("Rejects null or blank schedule ID")
    void testRejectsNullOrBlankScheduleId() {
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);

        assertThrows(IllegalArgumentException.class, () ->
                new Schedule(null, sampleCourse, sampleInstructor, sampleRoom, DayOfWeek.MONDAY, start, end));

        assertThrows(IllegalArgumentException.class, () ->
                new Schedule("   ", sampleCourse, sampleInstructor, sampleRoom, DayOfWeek.MONDAY, start, end));
    }

    @Test
    @DisplayName("Rejects null course, instructor, or room")
    void testRejectsNullEntities() {
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);

        assertThrows(IllegalArgumentException.class, () ->
                new Schedule("S-001", null, sampleInstructor, sampleRoom, DayOfWeek.MONDAY, start, end));

        assertThrows(IllegalArgumentException.class, () ->
                new Schedule("S-001", sampleCourse, null, sampleRoom, DayOfWeek.MONDAY, start, end));

        assertThrows(IllegalArgumentException.class, () ->
                new Schedule("S-001", sampleCourse, sampleInstructor, null, DayOfWeek.MONDAY, start, end));
    }

    @Test
    @DisplayName("Rejects null day of week")
    void testRejectsNullDay() {
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);

        assertThrows(IllegalArgumentException.class, () ->
                new Schedule("S-001", sampleCourse, sampleInstructor, sampleRoom, null, start, end));
    }

    @Test
    @DisplayName("Rejects null start or end time")
    void testRejectsNullTimes() {
        LocalTime validTime = LocalTime.of(9, 0);

        assertThrows(IllegalArgumentException.class, () ->
                new Schedule("S-001", sampleCourse, sampleInstructor, sampleRoom, DayOfWeek.MONDAY, null, validTime));

        assertThrows(IllegalArgumentException.class, () ->
                new Schedule("S-001", sampleCourse, sampleInstructor, sampleRoom, DayOfWeek.MONDAY, validTime, null));
    }

    @Test
    @DisplayName("Rejects end time that is equal to start time")
    void testRejectsEqualStartAndEndTime() {
        LocalTime time = LocalTime.of(10, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new Schedule("S-001", sampleCourse, sampleInstructor, sampleRoom, DayOfWeek.MONDAY, time, time));
        assertTrue(ex.getMessage().contains("must be strictly after"));
    }

    @Test
    @DisplayName("Rejects end time that is earlier than start time")
    void testRejectsEndTimeBeforeStartTime() {
        LocalTime start = LocalTime.of(11, 0);
        LocalTime end = LocalTime.of(10, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new Schedule("S-001", sampleCourse, sampleInstructor, sampleRoom, DayOfWeek.MONDAY, start, end));
        assertTrue(ex.getMessage().contains("must be strictly after"));
    }

    @Test
    @DisplayName("Setters update valid values and reject invalid inputs")
    void testSetterValidations() {
        Schedule schedule = new Schedule("S-001", sampleCourse, sampleInstructor, sampleRoom,
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        Room newRoom = new Room("L-01", "Lab 1", RoomType.LABORATORY, "Science Block", 24, null);
        schedule.setRoom(newRoom);
        assertEquals(newRoom, schedule.getRoom());
        assertThrows(IllegalArgumentException.class, () -> schedule.setRoom(null));

        Course newCourse = new Course("PHY210", "Physics", 20, null, null, null);
        schedule.setCourse(newCourse);
        assertEquals(newCourse, schedule.getCourse());
        assertThrows(IllegalArgumentException.class, () -> schedule.setCourse(null));

        Instructor newInstructor = new Instructor("I-002", "Dr. Bob", null, null, null);
        schedule.setInstructor(newInstructor);
        assertEquals(newInstructor, schedule.getInstructor());
        assertThrows(IllegalArgumentException.class, () -> schedule.setInstructor(null));

        schedule.setDay(DayOfWeek.FRIDAY);
        assertEquals(DayOfWeek.FRIDAY, schedule.getDay());
        assertThrows(IllegalArgumentException.class, () -> schedule.setDay(null));

        // Time interval setter validations
        schedule.setTimeInterval(LocalTime.of(14, 0), LocalTime.of(16, 0));
        assertEquals(LocalTime.of(14, 0), schedule.getStartTime());
        assertEquals(LocalTime.of(16, 0), schedule.getEndTime());

        assertThrows(IllegalArgumentException.class, () ->
                schedule.setTimeInterval(null, LocalTime.of(16, 0)));
        assertThrows(IllegalArgumentException.class, () ->
                schedule.setTimeInterval(LocalTime.of(14, 0), null));
        assertThrows(IllegalArgumentException.class, () ->
                schedule.setTimeInterval(LocalTime.of(15, 0), LocalTime.of(14, 0)));
        assertThrows(IllegalArgumentException.class, () ->
                schedule.setTimeInterval(LocalTime.of(15, 0), LocalTime.of(15, 0)));
    }

    @Test
    @DisplayName("Schedules with same ID are equal regardless of other fields")
    void testEqualsAndHashCode() {
        Schedule s1 = new Schedule("S-001", sampleCourse, sampleInstructor, sampleRoom,
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Schedule s2 = new Schedule("S-001", sampleCourse, sampleInstructor, sampleRoom,
                DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));
        Schedule s3 = new Schedule("S-002", sampleCourse, sampleInstructor, sampleRoom,
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1, s3);
    }
}
