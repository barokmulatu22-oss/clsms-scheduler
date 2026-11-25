package controller;

import controller.dto.CourseDTO;
import controller.dto.ErrorResponse;
import model.Course;
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
import service.CourseManager;
import util.ValidationResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing Course resources under /api/courses.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseManager courseManager;

    public CourseController(CourseManager courseManager) {
        this.courseManager = courseManager;
    }

    @GetMapping
    public List<CourseDTO> getAllCourses(@RequestParam(required = false) String search) {
        List<Course> courses = (search != null && !search.isBlank())
                ? courseManager.searchCourses(search)
                : courseManager.getAllCourses();
        return courses.stream().map(CourseDTO::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable String id) {
        Optional<Course> courseOpt = courseManager.getCourseByCode(id);
        if (courseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Course with code '" + id + "' not found."));
        }
        return ResponseEntity.ok(CourseDTO.fromEntity(courseOpt.get()));
    }

    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody CourseDTO dto) {
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Request body cannot be null."));
        }
        ValidationResult result = courseManager.addCourse(
                dto.courseCode(),
                dto.title(),
                dto.expectedEnrollment(),
                dto.requiredRoomType(),
                dto.department(),
                dto.section()
        );
        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }
        Course created = courseManager.getCourseByCode(dto.courseCode()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(CourseDTO.fromEntity(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable String id, @RequestBody CourseDTO dto) {
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Request body cannot be null."));
        }
        Optional<Course> courseOpt = courseManager.getCourseByCode(id);
        if (courseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Course with code '" + id + "' not found."));
        }
        ValidationResult result = courseManager.updateCourse(
                id,
                dto.title(),
                dto.expectedEnrollment(),
                dto.requiredRoomType(),
                dto.department(),
                dto.section()
        );
        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }
        Course updated = courseManager.getCourseByCode(id).orElseThrow();
        return ResponseEntity.ok(CourseDTO.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable String id) {
        Optional<Course> courseOpt = courseManager.getCourseByCode(id);
        if (courseOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Course with code '" + id + "' not found."));
        }
        ValidationResult result = courseManager.deleteCourse(id);
        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }
        return ResponseEntity.ok(Map.of("message", "Course '" + id.toUpperCase() + "' was deleted successfully."));
    }
}
