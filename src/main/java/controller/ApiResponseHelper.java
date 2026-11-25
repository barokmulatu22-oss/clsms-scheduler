package controller;

import controller.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import util.ValidationResult;

import java.util.List;

/**
 * Utility helper to convert ValidationResult objects to standardized HTTP error responses.
 */
public final class ApiResponseHelper {

    private ApiResponseHelper() {
        // Utility class
    }

    public static ResponseEntity<ErrorResponse> toErrorResponse(ValidationResult result) {
        List<String> errors = result.getErrors();
        HttpStatus status = determineStatus(errors);
        return ResponseEntity.status(status).body(new ErrorResponse(errors));
    }

    public static HttpStatus determineStatus(List<String> errors) {
        for (String err : errors) {
            String lower = err.toLowerCase();
            if (lower.contains("does not exist") || lower.contains("not found")) {
                return HttpStatus.NOT_FOUND;
            }
            if (lower.contains("duplicate") || lower.contains("already exists")
                    || lower.contains("conflict") || lower.contains("referenced by active schedule")
                    || lower.contains("cannot delete") || lower.contains("cannot be scheduled")) {
                return HttpStatus.CONFLICT;
            }
        }
        return HttpStatus.BAD_REQUEST;
    }
}
