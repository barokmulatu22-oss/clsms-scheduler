package controller;

import controller.dto.ErrorResponse;
import controller.dto.InstructorDTO;
import model.Instructor;
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
import service.InstructorManager;
import util.ValidationResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing Instructor resources under /api/instructors.
 */
@RestController
@RequestMapping("/api/instructors")
public class InstructorController {

    private final InstructorManager instructorManager;

    public InstructorController(InstructorManager instructorManager) {
        this.instructorManager = instructorManager;
    }

    @GetMapping
    public List<InstructorDTO> getAllInstructors(@RequestParam(required = false) String search) {
        List<Instructor> instructors = (search != null && !search.isBlank())
                ? instructorManager.searchInstructors(search)
                : instructorManager.getAllInstructors();
        return instructors.stream().map(InstructorDTO::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInstructorById(@PathVariable String id) {
        Optional<Instructor> instructorOpt = instructorManager.getInstructorById(id);
        if (instructorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Instructor with ID '" + id + "' not found."));
        }
        return ResponseEntity.ok(InstructorDTO.fromEntity(instructorOpt.get()));
    }

    @PostMapping
    public ResponseEntity<?> createInstructor(@RequestBody InstructorDTO dto) {
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Request body cannot be null."));
        }
        ValidationResult result = instructorManager.addInstructor(
                dto.instructorId(),
                dto.fullName(),
                dto.department(),
                dto.email(),
                dto.phone()
        );
        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }
        Instructor created = instructorManager.getInstructorById(dto.instructorId()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(InstructorDTO.fromEntity(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateInstructor(@PathVariable String id, @RequestBody InstructorDTO dto) {
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Request body cannot be null."));
        }
        Optional<Instructor> instructorOpt = instructorManager.getInstructorById(id);
        if (instructorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Instructor with ID '" + id + "' not found."));
        }
        ValidationResult result = instructorManager.updateInstructor(
                id,
                dto.fullName(),
                dto.department(),
                dto.email(),
                dto.phone()
        );
        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }
        Instructor updated = instructorManager.getInstructorById(id).orElseThrow();
        return ResponseEntity.ok(InstructorDTO.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInstructor(@PathVariable String id) {
        Optional<Instructor> instructorOpt = instructorManager.getInstructorById(id);
        if (instructorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Instructor with ID '" + id + "' not found."));
        }
        ValidationResult result = instructorManager.deleteInstructor(id);
        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }
        return ResponseEntity.ok(Map.of("message", "Instructor '" + id.toUpperCase() + "' was deleted successfully."));
    }
}
