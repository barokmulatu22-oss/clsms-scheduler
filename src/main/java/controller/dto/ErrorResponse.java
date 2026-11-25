package controller.dto;

import java.util.List;

/**
 * Standard error response DTO adhering to the API specification:
 * {"errors": ["message 1", "message 2"]}
 */
public record ErrorResponse(List<String> errors) {

    public ErrorResponse(String singleError) {
        this(List.of(singleError));
    }
}
