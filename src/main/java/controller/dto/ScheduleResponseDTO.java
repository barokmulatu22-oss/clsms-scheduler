package controller.dto;

import model.RoomType;
import model.Schedule;
import util.TimeUtil;

/**
 * Data Transfer Object for schedule bookings returned by the API.
 */
public record ScheduleResponseDTO(
        String scheduleId,
        String courseCode,
        String courseTitle,
        String instructorId,
        String instructorName,
        String roomId,
        String roomName,
        RoomType roomType,
        String day,
        String startTime,
        String endTime
) {
    public static ScheduleResponseDTO fromEntity(Schedule schedule) {
        if (schedule == null) {
            return null;
        }
        return new ScheduleResponseDTO(
                schedule.getScheduleId(),
                schedule.getCourse().getCourseCode(),
                schedule.getCourse().getTitle(),
                schedule.getInstructor().getInstructorId(),
                schedule.getInstructor().getFullName(),
                schedule.getRoom().getRoomId(),
                schedule.getRoom().getName(),
                schedule.getRoom().getType(),
                schedule.getDay().name(),
                TimeUtil.formatTime(schedule.getStartTime()),
                TimeUtil.formatTime(schedule.getEndTime())
        );
    }
}
