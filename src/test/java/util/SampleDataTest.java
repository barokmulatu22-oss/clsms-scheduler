package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.CourseManager;
import service.InstructorManager;
import service.RoomManager;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SampleData Utility Tests")
class SampleDataTest {

    @Test
    @DisplayName("Loads Appendix A rooms, courses, and instructors accurately")
    void testLoadSampleData() {
        RoomManager roomManager = new RoomManager();
        CourseManager courseManager = new CourseManager();
        InstructorManager instructorManager = new InstructorManager();

        SampleData.loadSampleData(roomManager, courseManager, instructorManager);

        assertEquals(4, roomManager.getAllRooms().size());
        assertTrue(roomManager.getRoomById("C-101").isPresent());
        assertTrue(roomManager.getRoomById("C-205").isPresent());
        assertTrue(roomManager.getRoomById("L-01").isPresent());
        assertTrue(roomManager.getRoomById("L-02").isPresent());

        assertEquals(3, courseManager.getAllCourses().size());
        assertTrue(courseManager.getCourseByCode("CS201").isPresent());
        assertTrue(courseManager.getCourseByCode("PHY210").isPresent());
        assertTrue(courseManager.getCourseByCode("MAT202").isPresent());

        assertEquals(3, instructorManager.getAllInstructors().size());
        assertTrue(instructorManager.getInstructorById("I-001").isPresent());
        assertTrue(instructorManager.getInstructorById("I-002").isPresent());
        assertTrue(instructorManager.getInstructorById("I-003").isPresent());
    }
}
