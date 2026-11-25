package controller.dto;

/**
 * Data Transfer Object for creating or updating schedule bookings.
 */
public record ScheduleRequestDTO(
        String courseCode,
        String instructorId,
        String roomId,
        String day,
        String startTime,
        String endTime
) {
}
