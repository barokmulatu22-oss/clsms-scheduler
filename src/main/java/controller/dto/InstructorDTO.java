package controller.dto;

import model.Instructor;

/**
 * Data Transfer Object for Instructor entities in API requests and responses.
 */
public record InstructorDTO(
        String instructorId,
        String fullName,
        String department,
        String email,
        String phone
) {
    public static InstructorDTO fromEntity(Instructor instructor) {
        if (instructor == null) {
            return null;
        }
        return new InstructorDTO(
                instructor.getInstructorId(),
                instructor.getFullName(),
                instructor.getDepartment(),
                instructor.getEmail(),
                instructor.getPhone()
        );
    }
}
