package controller;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import controller.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every unexpected failure into our {"errors": [...]} format,
 * so bad input never produces a stack trace or an HTML error page.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Bad day/time text that reached a TimeUtil parser, or any other rejected value. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadValue(IllegalArgumentException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Broken JSON, empty body, or a value of the wrong type (e.g. room type "HALL"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            String field = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            return respond(HttpStatus.BAD_REQUEST,
                    "Invalid value '" + ife.getValue() + "' for field '" + field + "'.");
        }
        return respond(HttpStatus.BAD_REQUEST,
                "Request body is missing or is not valid JSON.");
    }

    /** Body sent as text/form instead of JSON: a client mistake, not a server error. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleWrongContentType(HttpMediaTypeNotSupportedException e) {
        return respond(HttpStatus.BAD_REQUEST, "Request body must be JSON (Content-Type: application/json).");
    }

    /** Query or path value of the wrong type (e.g. minCapacity=abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleWrongType(MethodArgumentTypeMismatchException e) {
        return respond(HttpStatus.BAD_REQUEST,
                "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnknownPath(NoResourceFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "Resource not found: /" + e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleWrongMethod(HttpRequestMethodNotSupportedException e) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method " + e.getMethod() + " is not allowed here.");
    }

    /** Last safety net: never leak internals, never crash. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error. Please try again.");
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
