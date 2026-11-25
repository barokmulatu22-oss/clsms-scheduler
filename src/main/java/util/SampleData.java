package util;

import model.RoomType;
import service.CourseManager;
import service.InstructorManager;
import service.RoomManager;

/**
 * Loads standard baseline demo data from PRD Appendix A into the managers.
 */
public final class SampleData {

    private SampleData() {
        // Utility class
    }

    /**
     * Populates the room, course, and instructor managers with PRD Appendix A sample data.
     */
    public static void loadSampleData(RoomManager roomManager, CourseManager courseManager, InstructorManager instructorManager) {
        // Rooms from Appendix A
        roomManager.addRoom("C-101", "Classroom 101", RoomType.CLASSROOM, "Main Building - 1st Floor", 40, "Standard lecture classroom");
        roomManager.addRoom("C-205", "Classroom 205", RoomType.CLASSROOM, "Main Building - 2nd Floor", 60, "Large tiered lecture hall");
        roomManager.addRoom("L-01", "Laboratory 01", RoomType.LABORATORY, "Science Block", 24, "Science experimental laboratory");
        roomManager.addRoom("L-02", "Laboratory 02", RoomType.LABORATORY, "Engineering Block", 30, "Engineering computer laboratory");

        // Instructors from Appendix A
        instructorManager.addInstructor("I-001", "Dr. Example (CS)", "Computer Science", "cs.example@university.edu", "555-0101");
        instructorManager.addInstructor("I-002", "Dr. Example (Physics)", "Physics", "physics.example@university.edu", "555-0102");
        instructorManager.addInstructor("I-003", "Dr. Example (Math)", "Mathematics", "math.example@university.edu", "555-0103");

        // Courses from Appendix A
        courseManager.addCourse("CS201", "Object-Oriented Programming", 35, RoomType.LABORATORY, "Computer Science", "Section A");
        courseManager.addCourse("PHY210", "Modern Physics", 22, RoomType.LABORATORY, "Physics", "Section 01");
        courseManager.addCourse("MAT202", "Discrete Mathematics", 45, RoomType.CLASSROOM, "Mathematics", "Section 01");
    }
}
