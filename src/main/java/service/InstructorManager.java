package service;

import model.Instructor;
import util.ValidationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the collection of academic instructors.
 * Implements in-memory storage using an ArrayList for ordered display
 * and a HashMap for efficient O(1) instructorId lookups.
 */
public class InstructorManager {

    private final List<Instructor> instructorList = new ArrayList<>();
    private final Map<String, Instructor> instructorMap = new HashMap<>();
    private ScheduleReferenceChecker scheduleReferenceChecker;

    /**
     * Injects a reference checker to verify active schedule references before deletion.
     */
    public void setScheduleReferenceChecker(ScheduleReferenceChecker scheduleReferenceChecker) {
        this.scheduleReferenceChecker = scheduleReferenceChecker;
    }

    /**
     * Adds a new Instructor to the system if all fields are valid and ID is unique.
     */
    public ValidationResult addInstructor(String instructorId, String fullName,
                                          String department, String email, String phone) {
        ValidationResult result = new ValidationResult();

        if (instructorId == null || instructorId.isBlank()) {
            result.addError("Instructor ID cannot be null or blank.");
        } else if (!util.IdRules.isValid(instructorId)) {
            result.addError(util.IdRules.message("Instructor ID"));
        } else if (instructorMap.containsKey(instructorId.trim().toUpperCase())) {
            result.addError("An instructor with ID '" + instructorId.trim().toUpperCase() + "' already exists.");
        }

        if (fullName == null || fullName.isBlank()) {
            result.addError("Instructor full name cannot be null or blank.");
        }

        if (!result.isValid()) {
            return result;
        }

        Instructor instructor = new Instructor(instructorId, fullName, department, email, phone);
        instructorList.add(instructor);
        instructorMap.put(instructor.getInstructorId(), instructor);
        return ValidationResult.success();
    }

    /**
     * Retrieves an instructor by unique instructor ID (case-insensitive).
     */
    public Optional<Instructor> getInstructorById(String instructorId) {
        if (instructorId == null || instructorId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(instructorMap.get(instructorId.trim().toUpperCase()));
    }

    /**
     * Returns an unmodifiable list of all registered instructors in insertion order.
     */
    public List<Instructor> getAllInstructors() {
        return Collections.unmodifiableList(new ArrayList<>(instructorList));
    }

    /**
     * Searches instructors by case-insensitive partial match on ID, name, or department.
     */
    public List<Instructor> searchInstructors(String query) {
        if (query == null || query.isBlank()) {
            return getAllInstructors();
        }
        String q = query.trim().toLowerCase();
        List<Instructor> matches = new ArrayList<>();
        for (Instructor i : instructorList) {
            if (i.getInstructorId().toLowerCase().contains(q)
                    || i.getFullName().toLowerCase().contains(q)
                    || i.getDepartment().toLowerCase().contains(q)) {
                matches.add(i);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    /**
     * Updates an existing Instructor atomically. If validation fails, the original record remains unchanged.
     */
    public ValidationResult updateInstructor(String instructorId, String fullName,
                                             String department, String email, String phone) {
        Optional<Instructor> instructorOpt = getInstructorById(instructorId);
        if (instructorOpt.isEmpty()) {
            return ValidationResult.failure("Instructor with ID '" + instructorId + "' does not exist.");
        }

        ValidationResult result = new ValidationResult();
        if (fullName == null || fullName.isBlank()) {
            result.addError("Instructor full name cannot be null or blank.");
        }

        if (!result.isValid()) {
            return result;
        }

        Instructor instructor = instructorOpt.get();
        instructor.setFullName(fullName);
        instructor.setDepartment(department);
        instructor.setEmail(email);
        instructor.setPhone(phone);
        return ValidationResult.success();
    }

    /**
     * Deletes an instructor if unreferenced; blocks deletion and returns an explanatory error if referenced.
     */
    public ValidationResult deleteInstructor(String instructorId) {
        Optional<Instructor> instructorOpt = getInstructorById(instructorId);
        if (instructorOpt.isEmpty()) {
            return ValidationResult.failure("Instructor with ID '" + instructorId + "' does not exist.");
        }

        Instructor instructor = instructorOpt.get();
        if (isReferenced(instructor.getInstructorId())) {
            return ValidationResult.failure("Cannot delete Instructor '" + instructor.getInstructorId()
                    + "': Instructor is assigned to active schedule(s). Reassign or cancel associated bookings first.");
        }

        instructorList.remove(instructor);
        instructorMap.remove(instructor.getInstructorId());
        return ValidationResult.success();
    }

    private boolean isReferenced(String id) {
        return scheduleReferenceChecker != null && scheduleReferenceChecker.isReferenced(id);
    }
}
