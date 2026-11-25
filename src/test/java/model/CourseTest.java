package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Course Entity Tests")
class CourseTest {

    @Test
    @DisplayName("Successfully creates valid Course with all fields")
    void testValidCourseCreation() {
        Course course = new Course("CS201", "Object-Oriented Programming", 35,
                RoomType.LABORATORY, "Computer Science", "Section A");

        assertEquals("CS201", course.getCourseCode());
        assertEquals("Object-Oriented Programming", course.getTitle());
        assertEquals(35, course.getExpectedEnrollment());
        assertEquals(RoomType.LABORATORY, course.getRequiredRoomType());
        assertEquals("Computer Science", course.getDepartment());
        assertEquals("Section A", course.getSection());
        assertTrue(course.toString().contains("CS201"));
    }

    @Test
    @DisplayName("Successfully creates Course with optional fields null or empty")
    void testValidCourseWithOptionalFieldsNull() {
        Course course = new Course("  mat202  ", " Discrete Mathematics ", 45,
                null, null, null);

        assertEquals("MAT202", course.getCourseCode());
        assertEquals("Discrete Mathematics", course.getTitle());
        assertEquals(45, course.getExpectedEnrollment());
        assertNull(course.getRequiredRoomType());
        assertEquals("", course.getDepartment());
        assertEquals("", course.getSection());
    }

    @Test
    @DisplayName("Rejects null or blank course code")
    void testRejectsNullOrBlankCourseCode() {
        assertThrows(IllegalArgumentException.class, () ->
                new Course(null, "OOP", 30, null, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Course("   ", "OOP", 30, null, null, null));
    }

    @Test
    @DisplayName("Rejects null or blank title")
    void testRejectsNullOrBlankTitle() {
        assertThrows(IllegalArgumentException.class, () ->
                new Course("CS201", null, 30, null, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Course("CS201", "   ", 30, null, null, null));
    }

    @Test
    @DisplayName("Rejects zero or negative expected enrollment")
    void testRejectsZeroOrNegativeEnrollment() {
        assertThrows(IllegalArgumentException.class, () ->
                new Course("CS201", "OOP", 0, null, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Course("CS201", "OOP", -15, null, null, null));
    }

    @Test
    @DisplayName("Setters update valid values and reject invalid inputs")
    void testSetterValidations() {
        Course course = new Course("CS201", "OOP", 30, null, null, null);

        course.setTitle("Advanced Programming");
        assertEquals("Advanced Programming", course.getTitle());
        assertThrows(IllegalArgumentException.class, () -> course.setTitle(null));
        assertThrows(IllegalArgumentException.class, () -> course.setTitle("   "));

        course.setExpectedEnrollment(50);
        assertEquals(50, course.getExpectedEnrollment());
        assertThrows(IllegalArgumentException.class, () -> course.setExpectedEnrollment(0));
        assertThrows(IllegalArgumentException.class, () -> course.setExpectedEnrollment(-1));

        course.setRequiredRoomType(RoomType.CLASSROOM);
        assertEquals(RoomType.CLASSROOM, course.getRequiredRoomType());
        course.setRequiredRoomType(null);
        assertNull(course.getRequiredRoomType());

        course.setDepartment("Mathematics");
        assertEquals("Mathematics", course.getDepartment());

        course.setSection("Section B");
        assertEquals("Section B", course.getSection());
    }

    @Test
    @DisplayName("Courses with same code are equal regardless of other fields")
    void testEqualsAndHashCode() {
        Course c1 = new Course("CS201", "Title 1", 30, null, null, null);
        Course c2 = new Course("CS201", "Title 2", 40, RoomType.LABORATORY, "Dept", "Sec");
        Course c3 = new Course("CS202", "Title 1", 30, null, null, null);

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1, c3);
    }
}
