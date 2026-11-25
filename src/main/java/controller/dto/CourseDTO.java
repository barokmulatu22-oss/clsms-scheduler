package controller.dto;

import model.Course;
import model.RoomType;

/**
 * Data Transfer Object for Course entities in API requests and responses.
 */
public record CourseDTO(
        String courseCode,
        String title,
        int expectedEnrollment,
        RoomType requiredRoomType,
        String department,
        String section
) {
    public static CourseDTO fromEntity(Course course) {
        if (course == null) {
            return null;
        }
        return new CourseDTO(
                course.getCourseCode(),
                course.getTitle(),
                course.getExpectedEnrollment(),
                course.getRequiredRoomType(),
                course.getDepartment(),
                course.getSection()
        );
    }
}
