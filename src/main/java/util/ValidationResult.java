package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the outcome of a validation or business operation.
 * Collects all error messages rather than failing fast.
 */
public class ValidationResult {

    private boolean valid;
    private final List<String> errors;

    public ValidationResult() {
        this.valid = true;
        this.errors = new ArrayList<>();
    }

    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors != null ? errors : Collections.emptyList());
    }

    /**
     * Creates a successful ValidationResult with no errors.
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    /**
     * Creates a failed ValidationResult with a list of error messages.
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    /**
     * Creates a failed ValidationResult with a single error message.
     */
    public static ValidationResult failure(String singleError) {
        ValidationResult result = new ValidationResult(false, new ArrayList<>());
        result.addError(singleError);
        return result;
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void addError(String error) {
        if (error != null && !error.isBlank()) {
            this.errors.add(error.trim());
            this.valid = false;
        }
    }

    public void addErrors(List<String> newErrors) {
        if (newErrors != null) {
            for (String err : newErrors) {
                addError(err);
            }
        }
    }

    /**
     * Formats all error messages joined by semicolons.
     */
    public String getFormattedErrors() {
        return String.join("; ", errors);
    }

    @Override
    public String toString() {
        return valid ? "ValidationResult[SUCCESS]" : "ValidationResult[FAILURE: " + getFormattedErrors() + "]";
    }
}
