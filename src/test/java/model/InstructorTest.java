package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Instructor Entity Tests")
class InstructorTest {

    @Test
    @DisplayName("Successfully creates valid Instructor with all fields")
    void testValidInstructorCreation() {
        Instructor instructor = new Instructor("I-001", "Dr. Jane Doe", "Computer Science",
                "jane.doe@university.edu", "555-123-4567");

        assertEquals("I-001", instructor.getInstructorId());
        assertEquals("Dr. Jane Doe", instructor.getFullName());
        assertEquals("Computer Science", instructor.getDepartment());
        assertEquals("jane.doe@university.edu", instructor.getEmail());
        assertEquals("555-123-4567", instructor.getPhone());
        assertTrue(instructor.toString().contains("I-001"));
    }

    @Test
    @DisplayName("Successfully creates Instructor with optional fields null or empty")
    void testValidInstructorWithOptionalFieldsNull() {
        Instructor instructor = new Instructor("  i-002  ", " Dr. John Smith ",
                null, null, null);

        assertEquals("I-002", instructor.getInstructorId());
        assertEquals("Dr. John Smith", instructor.getFullName());
        assertEquals("", instructor.getDepartment());
        assertEquals("", instructor.getEmail());
        assertEquals("", instructor.getPhone());
    }

    @Test
    @DisplayName("Rejects null or blank instructor ID")
    void testRejectsNullOrBlankInstructorId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instructor(null, "Dr. Smith", null, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Instructor("   ", "Dr. Smith", null, null, null));
    }

    @Test
    @DisplayName("Rejects null or blank full name")
    void testRejectsNullOrBlankFullName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instructor("I-001", null, null, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Instructor("I-001", "   ", null, null, null));
    }

    @Test
    @DisplayName("Setters update valid values and reject invalid inputs")
    void testSetterValidations() {
        Instructor instructor = new Instructor("I-001", "Dr. Jane Doe", null, null, null);

        instructor.setFullName("Prof. Jane Doe");
        assertEquals("Prof. Jane Doe", instructor.getFullName());
        assertThrows(IllegalArgumentException.class, () -> instructor.setFullName(null));
        assertThrows(IllegalArgumentException.class, () -> instructor.setFullName("   "));

        instructor.setDepartment("Physics");
        assertEquals("Physics", instructor.getDepartment());

        instructor.setEmail("prof.doe@univ.edu");
        assertEquals("prof.doe@univ.edu", instructor.getEmail());

        instructor.setPhone("555-987-6543");
        assertEquals("555-987-6543", instructor.getPhone());
    }

    @Test
    @DisplayName("Instructors with same ID are equal regardless of other fields")
    void testEqualsAndHashCode() {
        Instructor i1 = new Instructor("I-001", "Name 1", "Dept 1", null, null);
        Instructor i2 = new Instructor("I-001", "Name 2", "Dept 2", "mail", "phone");
        Instructor i3 = new Instructor("I-002", "Name 1", "Dept 1", null, null);

        assertEquals(i1, i2);
        assertEquals(i1.hashCode(), i2.hashCode());
        assertNotEquals(i1, i3);
    }
}
