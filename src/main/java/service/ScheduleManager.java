package service;

import model.Course;
import model.Instructor;
import model.Room;
import model.Schedule;
import util.ValidationResult;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the creation, modification, cancellation, and retrieval of schedule bookings.
 * Enforces business constraints by delegating to ConflictChecker before mutating data.
 * Coordinates referential integrity checks with RoomManager, CourseManager, and InstructorManager.
 */
public class ScheduleManager {

    private final List<Schedule> scheduleList = new ArrayList<>();
    private final Map<String, Schedule> scheduleMap = new HashMap<>();
    private final ConflictChecker conflictChecker;
    private final RoomManager roomManager;
    private final CourseManager courseManager;
    private final InstructorManager instructorManager;
    private int nextSequence = 1;

    /**
     * Constructs ScheduleManager and binds reference checkers to entity managers.
     */
    public ScheduleManager(ConflictChecker conflictChecker, RoomManager roomManager,
                           CourseManager courseManager, InstructorManager instructorManager) {
        this.conflictChecker = conflictChecker != null ? conflictChecker : new ConflictChecker();
        this.roomManager = roomManager;
        this.courseManager = courseManager;
        this.instructorManager = instructorManager;

        // Connect reference checkers to entity managers so their delete operations know if an entity is booked
        if (this.roomManager != null) {
            this.roomManager.setScheduleReferenceChecker(this::isRoomReferenced);
        }
        if (this.courseManager != null) {
            this.courseManager.setScheduleReferenceChecker(this::isCourseReferenced);
        }
        if (this.instructorManager != null) {
            this.instructorManager.setScheduleReferenceChecker(this::isInstructorReferenced);
        }
    }

    /**
     * Validates input, entity existence, and conflict constraints before saving a new schedule.
     * Saves nothing if validation fails.
     */
    public ValidationResult createSchedule(String courseCode, String instructorId, String roomId,
                                           DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        ValidationResult inputResult = validateInputs(courseCode, instructorId, roomId, day, startTime, endTime);
        if (!inputResult.isValid()) {
            return inputResult;
        }

        Course course = courseManager.getCourseByCode(courseCode).orElse(null);
        Instructor instructor = instructorManager.getInstructorById(instructorId).orElse(null);
        Room room = roomManager.getRoomById(roomId).orElse(null);

        ValidationResult entityResult = validateEntityExistence(courseCode, course, instructorId, instructor, roomId, room);
        if (!entityResult.isValid()) {
            return entityResult;
        }

        // Run conflict and suitability checks against existing active schedules
        ValidationResult conflictResult = conflictChecker.checkAllConflicts(course, instructor, room, day, startTime, endTime, scheduleList, null);
        if (!conflictResult.isValid()) {
            return conflictResult;
        }

        // Generate ID like S-001, S-002, etc.
        String scheduleId = generateNextScheduleId();
        Schedule schedule = new Schedule(scheduleId, course, instructor, room, day, startTime, endTime);

        scheduleList.add(schedule);
        scheduleMap.put(schedule.getScheduleId(), schedule);
        return ValidationResult.success();
    }

    /**
     * Re-validates a proposed schedule update, ignoring the schedule's own ID during conflict checks.
     * Leaves the original schedule unchanged if validation fails.
     */
    public ValidationResult updateSchedule(String scheduleId, String courseCode, String instructorId, String roomId,
                                           DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        Optional<Schedule> existingOpt = getScheduleById(scheduleId);
        if (existingOpt.isEmpty()) {
            return ValidationResult.failure("Schedule with ID '" + scheduleId + "' does not exist.");
        }

        ValidationResult inputResult = validateInputs(courseCode, instructorId, roomId, day, startTime, endTime);
        if (!inputResult.isValid()) {
            return inputResult;
        }

        Course course = courseManager.getCourseByCode(courseCode).orElse(null);
        Instructor instructor = instructorManager.getInstructorById(instructorId).orElse(null);
        Room room = roomManager.getRoomById(roomId).orElse(null);

        ValidationResult entityResult = validateEntityExistence(courseCode, course, instructorId, instructor, roomId, room);
        if (!entityResult.isValid()) {
            return entityResult;
        }

        // Conflict check excluding the schedule being edited
        ValidationResult conflictResult = conflictChecker.checkAllConflicts(course, instructor, room, day, startTime, endTime, scheduleList, scheduleId);
        if (!conflictResult.isValid()) {
            return conflictResult;
        }

        // Atomically update fields on original record
        Schedule existing = existingOpt.get();
        existing.setCourse(course);
        existing.setInstructor(instructor);
        existing.setRoom(room);
        existing.setDay(day);
        existing.setTimeInterval(startTime, endTime);

        return ValidationResult.success();
    }

    /**
     * Cancels and permanently removes a schedule booking.
     */
    public ValidationResult cancelSchedule(String scheduleId) {
        Optional<Schedule> scheduleOpt = getScheduleById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            return ValidationResult.failure("Schedule with ID '" + scheduleId + "' does not exist.");
        }

        Schedule schedule = scheduleOpt.get();
        scheduleList.remove(schedule);
        scheduleMap.remove(schedule.getScheduleId());
        return ValidationResult.success();
    }

    /**
     * Retrieves a schedule by its unique scheduleId.
     */
    public Optional<Schedule> getScheduleById(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(scheduleMap.get(scheduleId.trim().toUpperCase()));
    }

    /**
     * Returns all active schedules sorted chronologically by day of week then start time.
     */
    public List<Schedule> getAllSchedules() {
        List<Schedule> sorted = new ArrayList<>(scheduleList);
        sorted.sort(Comparator.comparing(Schedule::getDay).thenComparing(Schedule::getStartTime));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Filters active schedules by any combination of criteria: roomId, courseCode, instructorId, day, and time.
     * Empty or null filters are ignored.
     * Results are sorted chronologically by day of week then start time.
     */
    public List<Schedule> searchSchedules(String roomId, String courseCode, String instructorId,
                                          DayOfWeek day, LocalTime time) {
        String filterRoom = (roomId != null && !roomId.isBlank()) ? roomId.trim() : null;
        String filterCourse = (courseCode != null && !courseCode.isBlank()) ? courseCode.trim() : null;
        String filterInstructor = (instructorId != null && !instructorId.isBlank()) ? instructorId.trim() : null;

        List<Schedule> matches = new ArrayList<>();
        for (Schedule s : scheduleList) {
            if (filterRoom != null && !s.getRoom().getRoomId().equalsIgnoreCase(filterRoom)) {
                continue;
            }
            if (filterCourse != null && !s.getCourse().getCourseCode().equalsIgnoreCase(filterCourse)) {
                continue;
            }
            if (filterInstructor != null && !s.getInstructor().getInstructorId().equalsIgnoreCase(filterInstructor)) {
                continue;
            }
            if (day != null && s.getDay() != day) {
                continue;
            }
            if (time != null && (time.isBefore(s.getStartTime()) || !time.isBefore(s.getEndTime()))) {
                continue;
            }
            matches.add(s);
        }

        matches.sort(Comparator.comparing(Schedule::getDay).thenComparing(Schedule::getStartTime));
        return Collections.unmodifiableList(matches);
    }

    /**
     * Finds active rooms that are free throughout the requested day and time interval,
     * meeting optional minimum capacity and room type criteria.
     * Uses TimeUtil.overlaps() so back-to-back bookings do not block availability.
     * Deactivated rooms are excluded.
     */
    public List<Room> findAvailableRooms(DayOfWeek day, LocalTime startTime, LocalTime endTime,
                                         Integer minCapacity, model.RoomType requiredType) {
        if (day == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Day, start time, and end time cannot be null for availability search.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(String.format("End time (%s) must be strictly after start time (%s).", endTime, startTime));
        }

        List<Room> allRooms = (roomManager != null) ? roomManager.getAllRooms() : Collections.emptyList();
        List<Room> available = new ArrayList<>();

        for (Room room : allRooms) {
            // Exclude deactivated rooms
            if (!room.isActive()) {
                continue;
            }
            // Check capacity filter
            if (minCapacity != null && room.getCapacity() < minCapacity) {
                continue;
            }
            // Check room type filter
            if (requiredType != null && room.getType() != requiredType) {
                continue;
            }

            // Check if room has an overlapping booking that day
            boolean isBooked = false;
            for (Schedule s : scheduleList) {
                if (s.getDay() == day && s.getRoom().equals(room)) {
                    if (util.TimeUtil.overlaps(startTime, endTime, s.getStartTime(), s.getEndTime())) {
                        isBooked = true;
                        break;
                    }
                }
            }

            if (!isBooked) {
                available.add(room);
            }
        }

        return Collections.unmodifiableList(available);
    }

    /**
     * Checks if a room ID is referenced by any active schedule.
     */
    public boolean isRoomReferenced(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return false;
        }
        String id = roomId.trim().toUpperCase();
        for (Schedule s : scheduleList) {
            if (s.getRoom().getRoomId().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a course code is referenced by any active schedule.
     */
    public boolean isCourseReferenced(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return false;
        }
        String code = courseCode.trim().toUpperCase();
        for (Schedule s : scheduleList) {
            if (s.getCourse().getCourseCode().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an instructor ID is referenced by any active schedule.
     */
    public boolean isInstructorReferenced(String instructorId) {
        if (instructorId == null || instructorId.isBlank()) {
            return false;
        }
        String id = instructorId.trim().toUpperCase();
        for (Schedule s : scheduleList) {
            if (s.getInstructor().getInstructorId().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private ValidationResult validateInputs(String courseCode, String instructorId, String roomId,
                                            DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        ValidationResult result = new ValidationResult();
        if (courseCode == null || courseCode.isBlank()) {
            result.addError("Course code cannot be null or blank.");
        }
        if (instructorId == null || instructorId.isBlank()) {
            result.addError("Instructor ID cannot be null or blank.");
        }
        if (roomId == null || roomId.isBlank()) {
            result.addError("Room ID cannot be null or blank.");
        }
        if (day == null) {
            result.addError("Day of week cannot be null.");
        }
        if (startTime == null) {
            result.addError("Start time cannot be null.");
        }
        if (endTime == null) {
            result.addError("End time cannot be null.");
        }
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            result.addError(String.format("End time (%s) must be strictly after start time (%s).", endTime, startTime));
        }
        return result;
    }

    private ValidationResult validateEntityExistence(String courseCode, Course course,
                                                     String instructorId, Instructor instructor,
                                                     String roomId, Room room) {
        ValidationResult result = new ValidationResult();
        if (course == null && courseCode != null && !courseCode.isBlank()) {
            result.addError("Course with code '" + courseCode.trim().toUpperCase() + "' does not exist.");
        }
        if (instructor == null && instructorId != null && !instructorId.isBlank()) {
            result.addError("Instructor with ID '" + instructorId.trim().toUpperCase() + "' does not exist.");
        }
        if (room == null && roomId != null && !roomId.isBlank()) {
            result.addError("Room with ID '" + roomId.trim().toUpperCase() + "' does not exist.");
        }
        return result;
    }

    private synchronized String generateNextScheduleId() {
        String id;
        do {
            id = String.format("S-%03d", nextSequence++);
        } while (scheduleMap.containsKey(id));
        return id;
    }
}
