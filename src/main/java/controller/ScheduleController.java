package controller;

import controller.dto.ErrorResponse;
import controller.dto.ScheduleRequestDTO;
import controller.dto.ScheduleResponseDTO;
import model.Schedule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.ScheduleManager;
import util.TimeUtil;
import util.ValidationResult;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing Schedule bookings under /api/schedules.
 */
@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleManager scheduleManager;

    public ScheduleController(ScheduleManager scheduleManager) {
        this.scheduleManager = scheduleManager;
    }

    @GetMapping
    public List<ScheduleResponseDTO> getSchedules(
            @RequestParam(required = false) String room,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String instructor,
            @RequestParam(required = false) String day,
            @RequestParam(required = false) String time) {

        boolean hasFilter = (room != null && !room.isBlank())
                || (course != null && !course.isBlank())
                || (instructor != null && !instructor.isBlank())
                || (day != null && !day.isBlank())
                || (time != null && !time.isBlank());

        if (hasFilter) {
            DayOfWeek dayOfWeek = (day != null && !day.isBlank()) ? TimeUtil.parseDay(day) : null;
            LocalTime localTime = (time != null && !time.isBlank()) ? TimeUtil.parseTime(time) : null;
            return scheduleManager.searchSchedules(room, course, instructor, dayOfWeek, localTime)
                    .stream()
                    .map(ScheduleResponseDTO::fromEntity)
                    .toList();
        }

        return scheduleManager.getAllSchedules()
                .stream()
                .map(ScheduleResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getScheduleById(@PathVariable String id) {
        Optional<Schedule> scheduleOpt = scheduleManager.getScheduleById(id);
        if (scheduleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Schedule with ID '" + id + "' not found."));
        }
        return ResponseEntity.ok(ScheduleResponseDTO.fromEntity(scheduleOpt.get()));
    }

    @PostMapping
    public ResponseEntity<?> createSchedule(@RequestBody ScheduleRequestDTO dto) {
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Request body cannot be null."));
        }

        ValidationResult inputResult = validateScheduleRequest(dto);
        if (!inputResult.isValid()) {
            return ApiResponseHelper.toErrorResponse(inputResult);
        }

        DayOfWeek day = TimeUtil.parseDay(dto.day());
        LocalTime startTime = TimeUtil.parseTime(dto.startTime());
        LocalTime endTime = TimeUtil.parseTime(dto.endTime());

        ValidationResult result = scheduleManager.createSchedule(
                dto.courseCode(),
                dto.instructorId(),
                dto.roomId(),
                day,
                startTime,
                endTime
        );

        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }

        Schedule created = scheduleManager.getAllSchedules().stream()
                .filter(s -> s.getRoom().getRoomId().equalsIgnoreCase(dto.roomId().trim())
                        && s.getCourse().getCourseCode().equalsIgnoreCase(dto.courseCode().trim())
                        && s.getInstructor().getInstructorId().equalsIgnoreCase(dto.instructorId().trim())
                        && s.getDay() == day
                        && s.getStartTime().equals(startTime)
                        && s.getEndTime().equals(endTime))
                .findFirst()
                .orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(ScheduleResponseDTO.fromEntity(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSchedule(@PathVariable String id, @RequestBody ScheduleRequestDTO dto) {
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Request body cannot be null."));
        }

        Optional<Schedule> scheduleOpt = scheduleManager.getScheduleById(id);
        if (scheduleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Schedule with ID '" + id + "' not found."));
        }

        ValidationResult inputResult = validateScheduleRequest(dto);
        if (!inputResult.isValid()) {
            return ApiResponseHelper.toErrorResponse(inputResult);
        }

        DayOfWeek day = TimeUtil.parseDay(dto.day());
        LocalTime startTime = TimeUtil.parseTime(dto.startTime());
        LocalTime endTime = TimeUtil.parseTime(dto.endTime());

        ValidationResult result = scheduleManager.updateSchedule(
                id,
                dto.courseCode(),
                dto.instructorId(),
                dto.roomId(),
                day,
                startTime,
                endTime
        );

        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }

        Schedule updated = scheduleManager.getScheduleById(id).orElseThrow();
        return ResponseEntity.ok(ScheduleResponseDTO.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelSchedule(@PathVariable String id) {
        Optional<Schedule> scheduleOpt = scheduleManager.getScheduleById(id);
        if (scheduleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Schedule with ID '" + id + "' not found."));
        }

        ValidationResult result = scheduleManager.cancelSchedule(id);
        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }

        return ResponseEntity.ok(Map.of("message", "Schedule '" + id.toUpperCase() + "' was cancelled successfully."));
    }

    private ValidationResult validateScheduleRequest(ScheduleRequestDTO dto) {
        ValidationResult errors = new ValidationResult();
        if (dto.courseCode() == null || dto.courseCode().isBlank()) {
            errors.addError("Course code cannot be null or blank.");
        }
        if (dto.instructorId() == null || dto.instructorId().isBlank()) {
            errors.addError("Instructor ID cannot be null or blank.");
        }
        if (dto.roomId() == null || dto.roomId().isBlank()) {
            errors.addError("Room ID cannot be null or blank.");
        }

        DayOfWeek day = null;
        if (dto.day() == null || dto.day().isBlank()) {
            errors.addError("Day of week cannot be null or blank.");
        } else {
            try {
                day = TimeUtil.parseDay(dto.day());
            } catch (IllegalArgumentException e) {
                errors.addError(e.getMessage());
            }
        }

        LocalTime startTime = null;
        if (dto.startTime() == null || dto.startTime().isBlank()) {
            errors.addError("Start time cannot be null or blank.");
        } else {
            try {
                startTime = TimeUtil.parseTime(dto.startTime());
            } catch (IllegalArgumentException e) {
                errors.addError(e.getMessage());
            }
        }

        LocalTime endTime = null;
        if (dto.endTime() == null || dto.endTime().isBlank()) {
            errors.addError("End time cannot be null or blank.");
        } else {
            try {
                endTime = TimeUtil.parseTime(dto.endTime());
            } catch (IllegalArgumentException e) {
                errors.addError(e.getMessage());
            }
        }

        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            errors.addError(String.format("End time (%s) must be strictly after start time (%s).", endTime, startTime));
        }

        return errors;
    }
}
