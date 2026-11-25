package service;

import model.Course;
import model.Instructor;
import model.Room;
import model.Schedule;
import util.TimeUtil;
import util.ValidationResult;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Service that validates proposed bookings against conflict rules:
 * - Room conflict (same room, same day, intersecting time interval)
 * - Instructor conflict (same instructor, same day, intersecting time interval)
 * - Capacity conflict (course enrollment exceeds room capacity)
 * - Room type conflict (course requires laboratory but room is classroom, or vice versa)
 *
 * Stateless and does not own or modify any domain data.
 * Calls TimeUtil.overlaps() for every interval intersection calculation.
 */
public class ConflictChecker {

    /**
     * Checks all conflict rules for a proposed booking against the existing schedule list.
     * Collects and returns ALL detected errors together.
     *
     * @param course            Course to be scheduled
     * @param instructor        Instructor assigned to teach
     * @param room              Room where class will be held
     * @param day               Day of the week
     * @param startTime         Session start time
     * @param endTime           Session end time
     * @param existingSchedules Current active schedules
     * @param excludeScheduleId Schedule ID to ignore during conflict checks (for updates), or null for new schedules
     * @return ValidationResult containing all detected conflict errors, or success if none
     */
    public ValidationResult checkAllConflicts(Course course, Instructor instructor, Room room,
                                             DayOfWeek day, LocalTime startTime, LocalTime endTime,
                                             List<Schedule> existingSchedules, String excludeScheduleId) {
        ValidationResult result = new ValidationResult();

        // 1. Check room operational status
        if (room != null && !room.isActive()) {
            result.addError("Room conflict: Room '" + room.getRoomId() + "' is deactivated and cannot be scheduled.");
        }

        // 2. Check room capacity suitability
        ValidationResult capacityResult = checkCapacity(room, course);
        if (!capacityResult.isValid()) {
            result.addErrors(capacityResult.getErrors());
        }

        // 3. Check room type compatibility
        ValidationResult typeResult = checkRoomTypeSuitability(room, course);
        if (!typeResult.isValid()) {
            result.addErrors(typeResult.getErrors());
        }

        // 4. Check timetable room overlap
        ValidationResult roomResult = checkRoomConflict(room, day, startTime, endTime, existingSchedules, excludeScheduleId);
        if (!roomResult.isValid()) {
            result.addErrors(roomResult.getErrors());
        }

        // 5. Check timetable instructor overlap
        ValidationResult instructorResult = checkInstructorConflict(instructor, day, startTime, endTime, existingSchedules, excludeScheduleId);
        if (!instructorResult.isValid()) {
            result.addErrors(instructorResult.getErrors());
        }

        return result;
    }

    /**
     * Convenience method taking a candidate Schedule object.
     */
    public ValidationResult checkAllConflicts(Schedule candidate, List<Schedule> existingSchedules, String excludeScheduleId) {
        if (candidate == null) {
            return ValidationResult.failure("Candidate schedule cannot be null.");
        }
        return checkAllConflicts(
                candidate.getCourse(),
                candidate.getInstructor(),
                candidate.getRoom(),
                candidate.getDay(),
                candidate.getStartTime(),
                candidate.getEndTime(),
                existingSchedules,
                excludeScheduleId != null ? excludeScheduleId : candidate.getScheduleId()
        );
    }

    /**
     * Checks if another active booking uses the same room on the same day during an overlapping interval.
     */
    public ValidationResult checkRoomConflict(Room room, DayOfWeek day, LocalTime startTime, LocalTime endTime,
                                              List<Schedule> existingSchedules, String excludeScheduleId) {
        ValidationResult result = new ValidationResult();
        if (room == null || day == null || startTime == null || endTime == null || existingSchedules == null) {
            return result;
        }

        for (Schedule existing : existingSchedules) {
            if (excludeScheduleId != null && excludeScheduleId.equalsIgnoreCase(existing.getScheduleId())) {
                continue;
            }

            if (existing.getDay() == day && existing.getRoom().equals(room)) {
                if (TimeUtil.overlaps(startTime, endTime, existing.getStartTime(), existing.getEndTime())) {
                    result.addError(String.format(
                            "Room conflict: Room '%s' is already booked on %s from %s to %s for course %s (Schedule ID: %s).",
                            room.getRoomId(),
                            day,
                            TimeUtil.formatTime(existing.getStartTime()),
                            TimeUtil.formatTime(existing.getEndTime()),
                            existing.getCourse().getCourseCode(),
                            existing.getScheduleId()
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Checks if another active booking assigns the same instructor on the same day during an overlapping interval.
     */
    public ValidationResult checkInstructorConflict(Instructor instructor, DayOfWeek day, LocalTime startTime, LocalTime endTime,
                                                    List<Schedule> existingSchedules, String excludeScheduleId) {
        ValidationResult result = new ValidationResult();
        if (instructor == null || day == null || startTime == null || endTime == null || existingSchedules == null) {
            return result;
        }

        for (Schedule existing : existingSchedules) {
            if (excludeScheduleId != null && excludeScheduleId.equalsIgnoreCase(existing.getScheduleId())) {
                continue;
            }

            if (existing.getDay() == day && existing.getInstructor().equals(instructor)) {
                if (TimeUtil.overlaps(startTime, endTime, existing.getStartTime(), existing.getEndTime())) {
                    result.addError(String.format(
                            "Instructor conflict: Instructor '%s' (%s) is already scheduled on %s from %s to %s in room %s for course %s (Schedule ID: %s).",
                            instructor.getFullName(),
                            instructor.getInstructorId(),
                            day,
                            TimeUtil.formatTime(existing.getStartTime()),
                            TimeUtil.formatTime(existing.getEndTime()),
                            existing.getRoom().getRoomId(),
                            existing.getCourse().getCourseCode(),
                            existing.getScheduleId()
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Validates that room capacity is greater than or equal to course expected enrollment.
     */
    public ValidationResult checkCapacity(Room room, Course course) {
        ValidationResult result = new ValidationResult();
        if (room == null || course == null) {
            return result;
        }

        if (course.getExpectedEnrollment() > room.getCapacity()) {
            result.addError(String.format(
                    "Capacity conflict: Course enrollment (%d) exceeds room capacity (%d) for room '%s'.",
                    course.getExpectedEnrollment(),
                    room.getCapacity(),
                    room.getRoomId()
            ));
        }
        return result;
    }

    /**
     * Validates that room type matches the course's required room type (if specified).
     */
    public ValidationResult checkRoomTypeSuitability(Room room, Course course) {
        ValidationResult result = new ValidationResult();
        if (room == null || course == null || course.getRequiredRoomType() == null) {
            return result; // No type restriction
        }

        if (room.getType() != course.getRequiredRoomType()) {
            result.addError(String.format(
                    "Room type conflict: Course '%s' requires a %s, but room '%s' is a %s.",
                    course.getCourseCode(),
                    course.getRequiredRoomType(),
                    room.getRoomId(),
                    room.getType()
            ));
        }
        return result;
    }
}
