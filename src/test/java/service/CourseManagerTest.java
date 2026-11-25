package service;

import model.Course;
import model.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.ValidationResult;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CourseManager Service Tests")
class CourseManagerTest {

    private CourseManager courseManager;

    @BeforeEach
    void setUp() {
        courseManager = new CourseManager();
    }

    @Test
    @DisplayName("Successfully adds valid course and retrieves it")
    void testAddValidCourse() {
        ValidationResult result = courseManager.addCourse("CS201", "Object-Oriented Programming", 35,
                RoomType.LABORATORY, "Computer Science", "Section A");

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());

        Optional<Course> courseOpt = courseManager.getCourseByCode("CS201");
        assertTrue(courseOpt.isPresent());
        assertEquals("Object-Oriented Programming", courseOpt.get().getTitle());
        assertEquals(35, courseOpt.get().getExpectedEnrollment());
        assertEquals(RoomType.LABORATORY, courseOpt.get().getRequiredRoomType());
    }

    @Test
    @DisplayName("Rejects duplicate course code case-insensitively")
    void testAddDuplicateCourseCode() {
        courseManager.addCourse("CS201", "OOP 1", 30, null, null, null);

        ValidationResult duplicateResult = courseManager.addCourse("cs201", "OOP 2", 40, null, null, null);

        assertFalse(duplicateResult.isValid());
        assertEquals(1, duplicateResult.getErrors().size());
        assertTrue(duplicateResult.getErrors().get(0).contains("already exists"));
        assertEquals(1, courseManager.getAllCourses().size());
    }

    @Test
    @DisplayName("Collects all validation errors on invalid course creation")
    void testAddInvalidCourseFields() {
        ValidationResult result = courseManager.addCourse("", "  ", -5, null, null, null);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 3);
        assertEquals(0, courseManager.getAllCourses().size());
    }

    @Test
    @DisplayName("Searches courses by partial match on code, title, and department")
    void testSearchCourses() {
        courseManager.addCourse("CS201", "OOP with Java", 35, null, "Computer Science", null);
        courseManager.addCourse("CS301", "Algorithms", 30, null, "Computer Science", null);
        courseManager.addCourse("PHY101", "General Physics", 25, null, "Physics", null);

        // Search by title
        List<Course> javaCourses = courseManager.searchCourses("java");
        assertEquals(1, javaCourses.size());
        assertEquals("CS201", javaCourses.get(0).getCourseCode());

        // Search by department
        List<Course> csCourses = courseManager.searchCourses("computer");
        assertEquals(2, csCourses.size());

        // Search by code
        List<Course> cs2 = courseManager.searchCourses("CS2");
        assertEquals(1, cs2.size());
        assertEquals("CS201", cs2.get(0).getCourseCode());

        // Search by title
        List<Course> algo = courseManager.searchCourses("Algorithms");
        assertEquals(1, algo.size());
        assertEquals("CS301", algo.get(0).getCourseCode());

        // Empty query returns all
        assertEquals(3, courseManager.searchCourses("").size());
        assertEquals(3, courseManager.searchCourses(null).size());
    }

    @Test
    @DisplayName("Updates course successfully")
    void testUpdateCourseSuccess() {
        courseManager.addCourse("CS201", "OOP", 30, null, "CS", "01");

        ValidationResult result = courseManager.updateCourse("cs201", "Advanced Java OOP", 45,
                RoomType.LABORATORY, "Informatics", "02");

        assertTrue(result.isValid());
        Course updated = courseManager.getCourseByCode("CS201").orElseThrow();
        assertEquals("Advanced Java OOP", updated.getTitle());
        assertEquals(45, updated.getExpectedEnrollment());
        assertEquals(RoomType.LABORATORY, updated.getRequiredRoomType());
        assertEquals("Informatics", updated.getDepartment());
        assertEquals("02", updated.getSection());
    }

    @Test
    @DisplayName("Failed update preserves the original course record unchanged")
    void testFailedUpdatePreservesOriginal() {
        courseManager.addCourse("CS201", "Original Title", 30, null, "Dept", "Sec");

        ValidationResult result = courseManager.updateCourse("CS201", "   ", -10, null, "New", "Sec");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 2);

        Course original = courseManager.getCourseByCode("CS201").orElseThrow();
        assertEquals("Original Title", original.getTitle());
        assertEquals(30, original.getExpectedEnrollment());
    }

    @Test
    @DisplayName("Returns failure when updating non-existent course")
    void testUpdateCourseNotFound() {
        ValidationResult result = courseManager.updateCourse("UNKNOWN", "Title", 30, null, null, null);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("does not exist"));
    }

    @Test
    @DisplayName("Deletes unreferenced course permanently")
    void testDeleteUnreferencedCourse() {
        courseManager.addCourse("CS201", "OOP", 30, null, null, null);
        courseManager.setScheduleReferenceChecker(id -> false);

        ValidationResult result = courseManager.deleteCourse("cs201");

        assertTrue(result.isValid());
        assertTrue(courseManager.getCourseByCode("CS201").isEmpty());
        assertEquals(0, courseManager.getAllCourses().size());
    }

    @Test
    @DisplayName("Blocks deletion of course referenced by active schedule and explains why")
    void testDeleteReferencedCourseBlocked() {
        courseManager.addCourse("CS201", "OOP", 30, null, null, null);
        // Simulate CS201 being referenced by a schedule
        courseManager.setScheduleReferenceChecker(code -> "CS201".equalsIgnoreCase(code));

        ValidationResult result = courseManager.deleteCourse("cs201");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Cannot delete Course 'CS201'"));
        assertTrue(result.getErrors().get(0).contains("referenced by active schedule"));
        assertTrue(courseManager.getCourseByCode("CS201").isPresent(), "Course must still exist");
        assertEquals(1, courseManager.getAllCourses().size());
    }

    @Test
    @DisplayName("Returns failure when deleting non-existent course")
    void testDeleteCourseNotFound() {
        ValidationResult result = courseManager.deleteCourse("UNKNOWN");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("does not exist"));
    }
}
