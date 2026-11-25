package model;

import java.util.Objects;

/**
 * Represents an academic course offering with enrollment and facility requirements.
 */
public class Course {

    private final String courseCode;
    private String title;
    private int expectedEnrollment;
    private RoomType requiredRoomType;
    private String department;
    private String section;

    /**
     * Constructs a Course with all details specified.
     *
     * @param courseCode         Unique course identifier (cannot be null or blank)
     * @param title              Course title (cannot be null or blank)
     * @param expectedEnrollment Number of students expected (must be greater than 0)
     * @param requiredRoomType   Specific room type required, or null if any room is suitable
     * @param department         Academic department (optional)
     * @param section            Section identifier (optional)
     * @throws IllegalArgumentException if required fields are null, blank, or invalid
     */
    public Course(String courseCode, String title, int expectedEnrollment,
                  RoomType requiredRoomType, String department, String section) {
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("Course code cannot be null or blank.");
        }
        this.courseCode = courseCode.trim().toUpperCase();
        setTitle(title);
        setExpectedEnrollment(expectedEnrollment);
        setRequiredRoomType(requiredRoomType);
        setDepartment(department);
        setSection(section);
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Course title cannot be null or blank.");
        }
        this.title = title.trim();
    }

    public int getExpectedEnrollment() {
        return expectedEnrollment;
    }

    public void setExpectedEnrollment(int expectedEnrollment) {
        if (expectedEnrollment <= 0) {
            throw new IllegalArgumentException("Expected enrollment must be greater than zero. Provided: " + expectedEnrollment);
        }
        this.expectedEnrollment = expectedEnrollment;
    }

    public RoomType getRequiredRoomType() {
        return requiredRoomType;
    }

    public void setRequiredRoomType(RoomType requiredRoomType) {
        this.requiredRoomType = requiredRoomType;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = (department == null || department.isBlank()) ? "" : department.trim();
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = (section == null || section.isBlank()) ? "" : section.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course other)) return false;
        return Objects.equals(courseCode, other.courseCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseCode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Course[code=").append(courseCode)
                .append(", title=").append(title)
                .append(", enrollment=").append(expectedEnrollment);
        if (requiredRoomType != null) {
            sb.append(", requiredType=").append(requiredRoomType);
        }
        if (!department.isEmpty()) {
            sb.append(", dept=").append(department);
        }
        if (!section.isEmpty()) {
            sb.append(", section=").append(section);
        }
        sb.append("]");
        return sb.toString();
    }
}
