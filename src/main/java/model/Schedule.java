package model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Represents a weekly recurring booking linking a Course, Instructor, Room, Day, and Time interval.
 */
public class Schedule {

    private final String scheduleId;
    private Course course;
    private Instructor instructor;
    private Room room;
    private DayOfWeek day;
    private LocalTime startTime;
    private LocalTime endTime;

    /**
     * Constructs a Schedule with all references and time interval validated.
     *
     * @param scheduleId Unique auto-generated schedule ID (cannot be null or blank)
     * @param course     Scheduled Course (cannot be null)
     * @param instructor Assigned Instructor (cannot be null)
     * @param room       Assigned Room (cannot be null)
     * @param day        Day of week (cannot be null)
     * @param startTime  Start time (cannot be null)
     * @param endTime    End time (cannot be null and must be after startTime)
     * @throws IllegalArgumentException if any field is null, blank, or if endTime <= startTime
     */
    public Schedule(String scheduleId, Course course, Instructor instructor,
                    Room room, DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        if (scheduleId == null || scheduleId.isBlank()) {
            throw new IllegalArgumentException("Schedule ID cannot be null or blank.");
        }
        this.scheduleId = scheduleId.trim();
        setCourse(course);
        setInstructor(instructor);
        setRoom(room);
        setDay(day);
        setTimeInterval(startTime, endTime);
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course cannot be null in Schedule.");
        }
        this.course = course;
    }

    public Instructor getInstructor() {
        return instructor;
    }

    public void setInstructor(Instructor instructor) {
        if (instructor == null) {
            throw new IllegalArgumentException("Instructor cannot be null in Schedule.");
        }
        this.instructor = instructor;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room cannot be null in Schedule.");
        }
        this.room = room;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public void setDay(DayOfWeek day) {
        if (day == null) {
            throw new IllegalArgumentException("Day cannot be null in Schedule.");
        }
        this.day = day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Updates the start and end time interval atomically with validation.
     *
     * @param startTime Start time (cannot be null)
     * @param endTime   End time (cannot be null, must be after startTime)
     * @throws IllegalArgumentException if times are null or endTime <= startTime
     */
    public void setTimeInterval(LocalTime startTime, LocalTime endTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null in Schedule.");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("End time cannot be null in Schedule.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    String.format("End time (%s) must be strictly after start time (%s).", endTime, startTime)
            );
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Schedule other)) return false;
        return Objects.equals(scheduleId, other.scheduleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduleId);
    }

    @Override
    public String toString() {
        return String.format("Schedule[id=%s, course=%s, instructor=%s, room=%s, day=%s, time=%s-%s]",
                scheduleId,
                course.getCourseCode(),
                instructor.getInstructorId(),
                room.getRoomId(),
                day,
                startTime,
                endTime);
    }
}
