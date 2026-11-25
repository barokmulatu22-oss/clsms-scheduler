package service;

import model.Course;
import model.RoomType;
import util.ValidationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the collection of academic courses and requirements.
 * Implements in-memory storage using an ArrayList for ordered display
 * and a HashMap for efficient O(1) courseCode lookups.
 */
public class CourseManager {

    private final List<Course> courseList = new ArrayList<>();
    private final Map<String, Course> courseMap = new HashMap<>();
    private ScheduleReferenceChecker scheduleReferenceChecker;

    /**
     * Injects a reference checker to verify active schedule references before deletion.
     */
    public void setScheduleReferenceChecker(ScheduleReferenceChecker scheduleReferenceChecker) {
        this.scheduleReferenceChecker = scheduleReferenceChecker;
    }

    /**
     * Adds a new Course to the system if all fields are valid and code is unique.
     */
    public ValidationResult addCourse(String courseCode, String title, int expectedEnrollment,
                                      RoomType requiredRoomType, String department, String section) {
        ValidationResult result = new ValidationResult();

        if (courseCode == null || courseCode.isBlank()) {
            result.addError("Course code cannot be null or blank.");
        } else if (!util.IdRules.isValid(courseCode)) {
            result.addError(util.IdRules.message("Course code"));
        } else if (courseMap.containsKey(courseCode.trim().toUpperCase())) {
            result.addError("A course with code '" + courseCode.trim().toUpperCase() + "' already exists.");
        }

        if (title == null || title.isBlank()) {
            result.addError("Course title cannot be null or blank.");
        }
        if (expectedEnrollment <= 0) {
            result.addError("Expected enrollment must be greater than zero. Provided: " + expectedEnrollment);
        }

        if (!result.isValid()) {
            return result;
        }

        Course course = new Course(courseCode, title, expectedEnrollment, requiredRoomType, department, section);
        courseList.add(course);
        courseMap.put(course.getCourseCode(), course);
        return ValidationResult.success();
    }

    /**
     * Retrieves a course by unique course code (case-insensitive).
     */
    public Optional<Course> getCourseByCode(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(courseMap.get(courseCode.trim().toUpperCase()));
    }

    /**
     * Returns an unmodifiable list of all registered courses in insertion order.
     */
    public List<Course> getAllCourses() {
        return Collections.unmodifiableList(new ArrayList<>(courseList));
    }

    /**
     * Searches courses by case-insensitive partial match on code, title, or department.
     */
    public List<Course> searchCourses(String query) {
        if (query == null || query.isBlank()) {
            return getAllCourses();
        }
        String q = query.trim().toLowerCase();
        List<Course> matches = new ArrayList<>();
        for (Course c : courseList) {
            if (c.getCourseCode().toLowerCase().contains(q)
                    || c.getTitle().toLowerCase().contains(q)
                    || c.getDepartment().toLowerCase().contains(q)) {
                matches.add(c);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    /**
     * Updates an existing Course atomically. If validation fails, the original record remains unchanged.
     */
    public ValidationResult updateCourse(String courseCode, String title, int expectedEnrollment,
                                         RoomType requiredRoomType, String department, String section) {
        Optional<Course> courseOpt = getCourseByCode(courseCode);
        if (courseOpt.isEmpty()) {
            return ValidationResult.failure("Course with code '" + courseCode + "' does not exist.");
        }

        ValidationResult result = new ValidationResult();
        if (title == null || title.isBlank()) {
            result.addError("Course title cannot be null or blank.");
        }
        if (expectedEnrollment <= 0) {
            result.addError("Expected enrollment must be greater than zero. Provided: " + expectedEnrollment);
        }

        if (!result.isValid()) {
            return result;
        }

        Course course = courseOpt.get();
        course.setTitle(title);
        course.setExpectedEnrollment(expectedEnrollment);
        course.setRequiredRoomType(requiredRoomType);
        course.setDepartment(department);
        course.setSection(section);
        return ValidationResult.success();
    }

    /**
     * Deletes a course if unreferenced; blocks deletion and returns an explanatory error if referenced.
     */
    public ValidationResult deleteCourse(String courseCode) {
        Optional<Course> courseOpt = getCourseByCode(courseCode);
        if (courseOpt.isEmpty()) {
            return ValidationResult.failure("Course with code '" + courseCode + "' does not exist.");
        }

        Course course = courseOpt.get();
        if (isReferenced(course.getCourseCode())) {
            return ValidationResult.failure("Cannot delete Course '" + course.getCourseCode()
                    + "': It is referenced by active schedule(s). Cancel associated bookings first.");
        }

        courseList.remove(course);
        courseMap.remove(course.getCourseCode());
        return ValidationResult.success();
    }

    private boolean isReferenced(String code) {
        return scheduleReferenceChecker != null && scheduleReferenceChecker.isReferenced(code);
    }
}
