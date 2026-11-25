package controller;

import controller.dto.ErrorResponse;
import controller.dto.ScheduleResponseDTO;
import model.Schedule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.CourseManager;
import service.InstructorManager;
import service.ReportManager;
import service.RoomManager;
import util.TimeUtil;

import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for generating daily, room, and instructor reports under /api/reports.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportManager reportManager;
    private final RoomManager roomManager;
    private final InstructorManager instructorManager;

    public ReportController(ReportManager reportManager, RoomManager roomManager, InstructorManager instructorManager) {
        this.reportManager = reportManager;
        this.roomManager = roomManager;
        this.instructorManager = instructorManager;
    }

    @GetMapping("/daily")
    public ResponseEntity<?> getDailyReport(@RequestParam(required = false) String day) {
        if (day == null || day.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Parameter 'day' is required."));
        }
        DayOfWeek dayOfWeek;
        try {
            dayOfWeek = TimeUtil.parseDay(day);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        }

        List<Schedule> dailySchedules = reportManager.getDailyTimetable(dayOfWeek);
        List<ScheduleResponseDTO> dtos = dailySchedules.stream().map(ScheduleResponseDTO::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/room/{id}")
    public ResponseEntity<?> getRoomReport(@PathVariable String id) {
        if (roomManager.getRoomById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Room with ID '" + id + "' not found."));
        }

        Map<DayOfWeek, List<Schedule>> timetable = reportManager.getRoomTimetable(id);
        Map<String, List<ScheduleResponseDTO>> response = new LinkedHashMap<>();
        for (Map.Entry<DayOfWeek, List<Schedule>> entry : timetable.entrySet()) {
            response.put(
                    entry.getKey().name(),
                    entry.getValue().stream().map(ScheduleResponseDTO::fromEntity).toList()
            );
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/instructor/{id}")
    public ResponseEntity<?> getInstructorReport(@PathVariable String id) {
        if (instructorManager.getInstructorById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Instructor with ID '" + id + "' not found."));
        }

        Map<DayOfWeek, List<Schedule>> timetable = reportManager.getInstructorTimetable(id);
        Map<String, List<ScheduleResponseDTO>> response = new LinkedHashMap<>();
        for (Map.Entry<DayOfWeek, List<Schedule>> entry : timetable.entrySet()) {
            response.put(
                    entry.getKey().name(),
                    entry.getValue().stream().map(ScheduleResponseDTO::fromEntity).toList()
            );
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        return ResponseEntity.ok(reportManager.getSystemSummary());
    }
}
