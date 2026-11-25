package model;

import java.util.Objects;

/**
 * Represents an academic instructor or faculty member.
 */
public class Instructor {

    private final String instructorId;
    private String fullName;
    private String department;
    private String email;
    private String phone;

    /**
     * Constructs an Instructor with all details specified.
     *
     * @param instructorId Unique instructor ID (cannot be null or blank)
     * @param fullName     Instructor's full name (cannot be null or blank)
     * @param department   Academic department (optional)
     * @param email        Contact email address (optional)
     * @param phone        Contact phone number (optional)
     * @throws IllegalArgumentException if instructorId or fullName is null or blank
     */
    public Instructor(String instructorId, String fullName, String department, String email, String phone) {
        if (instructorId == null || instructorId.isBlank()) {
            throw new IllegalArgumentException("Instructor ID cannot be null or blank.");
        }
        this.instructorId = instructorId.trim().toUpperCase();
        setFullName(fullName);
        setDepartment(department);
        setEmail(email);
        setPhone(phone);
    }

    public String getInstructorId() {
        return instructorId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Instructor full name cannot be null or blank.");
        }
        this.fullName = fullName.trim();
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = (department == null || department.isBlank()) ? "" : department.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = (email == null || email.isBlank()) ? "" : email.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = (phone == null || phone.isBlank()) ? "" : phone.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Instructor other)) return false;
        return Objects.equals(instructorId, other.instructorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instructorId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Instructor[id=").append(instructorId)
                .append(", name=").append(fullName);
        if (!department.isEmpty()) {
            sb.append(", dept=").append(department);
        }
        if (!email.isEmpty()) {
            sb.append(", email=").append(email);
        }
        if (!phone.isEmpty()) {
            sb.append(", phone=").append(phone);
        }
        sb.append("]");
        return sb.toString();
    }
}
