package controller;

import controller.dto.ErrorResponse;
import controller.dto.RoomDTO;
import model.Room;
import model.RoomType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.ScheduleManager;
import util.TimeUtil;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for finding available rooms under /api/availability.
 */
@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final ScheduleManager scheduleManager;

    public AvailabilityController(ScheduleManager scheduleManager) {
        this.scheduleManager = scheduleManager;
    }

    @GetMapping
    public ResponseEntity<?> findAvailableRooms(
            @RequestParam(required = false) String day,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String type) {

        List<String> errors = new ArrayList<>();
        if (day == null || day.isBlank()) {
            errors.add("Parameter 'day' is required.");
        }
        if (start == null || start.isBlank()) {
            errors.add("Parameter 'start' is required.");
        }
        if (end == null || end.isBlank()) {
            errors.add("Parameter 'end' is required.");
        }

        DayOfWeek dayOfWeek = null;
        if (day != null && !day.isBlank()) {
            try {
                dayOfWeek = TimeUtil.parseDay(day);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }

        LocalTime startTime = null;
        if (start != null && !start.isBlank()) {
            try {
                startTime = TimeUtil.parseTime(start);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }

        LocalTime endTime = null;
        if (end != null && !end.isBlank()) {
            try {
                endTime = TimeUtil.parseTime(end);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }

        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            errors.add("End time must be after start time.");
        }

        RoomType roomType = null;
        if (type != null && !type.isBlank()) {
            try {
                roomType = RoomType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("Invalid room type '" + type + "'. Allowed values: CLASSROOM, LABORATORY");
            }
        }

        if (!errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(errors));
        }

        List<Room> availableRooms = scheduleManager.findAvailableRooms(dayOfWeek, startTime, endTime, minCapacity, roomType);
        return ResponseEntity.ok(availableRooms.stream().map(RoomDTO::fromEntity).toList());
    }
}
