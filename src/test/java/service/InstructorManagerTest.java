package service;

import model.Instructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.ValidationResult;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InstructorManager Service Tests")
class InstructorManagerTest {

    private InstructorManager instructorManager;

    @BeforeEach
    void setUp() {
        instructorManager = new InstructorManager();
    }

    @Test
    @DisplayName("Successfully adds valid instructor and retrieves it")
    void testAddValidInstructor() {
        ValidationResult result = instructorManager.addInstructor("I-001", "Dr. Jane Doe",
                "Computer Science", "jane@univ.edu", "555-0100");

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());

        Optional<Instructor> instOpt = instructorManager.getInstructorById("I-001");
        assertTrue(instOpt.isPresent());
        assertEquals("Dr. Jane Doe", instOpt.get().getFullName());
        assertEquals("Computer Science", instOpt.get().getDepartment());
    }

    @Test
    @DisplayName("Rejects duplicate instructor ID case-insensitively")
    void testAddDuplicateInstructorId() {
        instructorManager.addInstructor("I-001", "Dr. Jane Doe", "CS", null, null);

        ValidationResult duplicateResult = instructorManager.addInstructor("i-001", "Another Name", "Physics", null, null);

        assertFalse(duplicateResult.isValid());
        assertEquals(1, duplicateResult.getErrors().size());
        assertTrue(duplicateResult.getErrors().get(0).contains("already exists"));
        assertEquals(1, instructorManager.getAllInstructors().size());
    }

    @Test
    @DisplayName("Collects all validation errors on invalid instructor creation")
    void testAddInvalidInstructorFields() {
        ValidationResult result = instructorManager.addInstructor("", "  ", null, null, null);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 2);
        assertEquals(0, instructorManager.getAllInstructors().size());
    }

    @Test
    @DisplayName("Searches instructors by partial match on ID, name, or department")
    void testSearchInstructors() {
        instructorManager.addInstructor("I-001", "Dr. Alan Turing", "Mathematics", null, null);
        instructorManager.addInstructor("I-002", "Dr. Ada Lovelace", "Computer Science", null, null);
        instructorManager.addInstructor("I-003", "Prof. Richard Feynman", "Physics", null, null);

        // Search by name
        List<Instructor> ada = instructorManager.searchInstructors("ada");
        assertEquals(1, ada.size());
        assertEquals("I-002", ada.get(0).getInstructorId());

        // Search by department
        List<Instructor> math = instructorManager.searchInstructors("math");
        assertEquals(1, math.size());
        assertEquals("I-001", math.get(0).getInstructorId());

        // Search by partial ID
        List<Instructor> iPrefix = instructorManager.searchInstructors("I-00");
        assertEquals(3, iPrefix.size());

        // Empty query returns all
        assertEquals(3, instructorManager.searchInstructors("").size());
        assertEquals(3, instructorManager.searchInstructors(null).size());
    }

    @Test
    @DisplayName("Updates instructor successfully")
    void testUpdateInstructorSuccess() {
        instructorManager.addInstructor("I-001", "Dr. Jane Doe", "CS", "old@univ.edu", "111");

        ValidationResult result = instructorManager.updateInstructor("i-001", "Prof. Jane Doe",
                "Data Science", "new@univ.edu", "222");

        assertTrue(result.isValid());
        Instructor updated = instructorManager.getInstructorById("I-001").orElseThrow();
        assertEquals("Prof. Jane Doe", updated.getFullName());
        assertEquals("Data Science", updated.getDepartment());
        assertEquals("new@univ.edu", updated.getEmail());
        assertEquals("222", updated.getPhone());
    }

    @Test
    @DisplayName("Failed update preserves the original instructor record unchanged")
    void testFailedUpdatePreservesOriginal() {
        instructorManager.addInstructor("I-001", "Original Name", "Original Dept", "mail", "phone");

        ValidationResult result = instructorManager.updateInstructor("I-001", "   ", "New Dept", "mail", "phone");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());

        Instructor original = instructorManager.getInstructorById("I-001").orElseThrow();
        assertEquals("Original Name", original.getFullName());
    }

    @Test
    @DisplayName("Returns failure when updating non-existent instructor")
    void testUpdateInstructorNotFound() {
        ValidationResult result = instructorManager.updateInstructor("UNKNOWN", "Name", null, null, null);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("does not exist"));
    }

    @Test
    @DisplayName("Deletes unreferenced instructor permanently")
    void testDeleteUnreferencedInstructor() {
        instructorManager.addInstructor("I-001", "Dr. Jane", "CS", null, null);
        instructorManager.setScheduleReferenceChecker(id -> false);

        ValidationResult result = instructorManager.deleteInstructor("i-001");

        assertTrue(result.isValid());
        assertTrue(instructorManager.getInstructorById("I-001").isEmpty());
        assertEquals(0, instructorManager.getAllInstructors().size());
    }

    @Test
    @DisplayName("Blocks deletion of instructor referenced by active schedule and explains why")
    void testDeleteReferencedInstructorBlocked() {
        instructorManager.addInstructor("I-001", "Dr. Jane", "CS", null, null);
        // Simulate instructor referenced by active schedule
        instructorManager.setScheduleReferenceChecker(id -> "I-001".equalsIgnoreCase(id));

        ValidationResult result = instructorManager.deleteInstructor("i-001");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Cannot delete Instructor 'I-001'"));
        assertTrue(result.getErrors().get(0).contains("assigned to active schedule"));
        assertTrue(instructorManager.getInstructorById("I-001").isPresent(), "Instructor must still exist");
        assertEquals(1, instructorManager.getAllInstructors().size());
    }

    @Test
    @DisplayName("Returns failure when deleting non-existent instructor")
    void testDeleteInstructorNotFound() {
        ValidationResult result = instructorManager.deleteInstructor("UNKNOWN");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("does not exist"));
    }
}
