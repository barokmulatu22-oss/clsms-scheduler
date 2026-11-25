package service;

import model.Room;
import model.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.ValidationResult;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoomManager Service Tests")
class RoomManagerTest {

    private RoomManager roomManager;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManager();
    }

    @Test
    @DisplayName("Successfully adds valid room and retrieves it")
    void testAddValidRoom() {
        ValidationResult result = roomManager.addRoom("C-101", "Lecture Hall 1", RoomType.CLASSROOM,
                "Main Building - 1st Floor", 40, "Projector equipped");

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());

        Optional<Room> roomOpt = roomManager.getRoomById("C-101");
        assertTrue(roomOpt.isPresent());
        assertEquals("Lecture Hall 1", roomOpt.get().getName());
        assertEquals(40, roomOpt.get().getCapacity());
        assertTrue(roomOpt.get().isActive());
    }

    @Test
    @DisplayName("Rejects duplicate room ID case-insensitively")
    void testAddDuplicateRoomId() {
        roomManager.addRoom("C-101", "Lecture Hall 1", RoomType.CLASSROOM, "Main", 40, null);

        ValidationResult duplicateResult = roomManager.addRoom("c-101", "Another Name", RoomType.LABORATORY, "Other", 30, null);

        assertFalse(duplicateResult.isValid());
        assertEquals(1, duplicateResult.getErrors().size());
        assertTrue(duplicateResult.getErrors().get(0).contains("already exists"));
        assertEquals(1, roomManager.getAllRooms().size());
    }

    @Test
    @DisplayName("Collects all validation errors on invalid room creation")
    void testAddInvalidRoomFields() {
        ValidationResult result = roomManager.addRoom("", "  ", null, "", 0, null);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 4);
        assertEquals(0, roomManager.getAllRooms().size());
    }

    @Test
    @DisplayName("Searches rooms by partial match on ID, name, location, and type")
    void testSearchRooms() {
        roomManager.addRoom("C-101", "Math Classroom", RoomType.CLASSROOM, "Science Wing", 35, null);
        roomManager.addRoom("L-01", "Physics Lab", RoomType.LABORATORY, "Science Wing", 25, null);
        roomManager.addRoom("C-205", "Auditorium", RoomType.CLASSROOM, "East Tower", 100, null);

        // Search by location
        List<Room> scienceRooms = roomManager.searchRooms("science");
        assertEquals(2, scienceRooms.size());

        // Search by type
        List<Room> labRooms = roomManager.searchRooms("lab");
        assertEquals(1, labRooms.size());
        assertEquals("L-01", labRooms.get(0).getRoomId());

        // Search by partial ID
        List<Room> cRooms = roomManager.searchRooms("C-");
        assertEquals(2, cRooms.size());

        // Blank search returns all
        assertEquals(3, roomManager.searchRooms("").size());
        assertEquals(3, roomManager.searchRooms(null).size());
    }

    @Test
    @DisplayName("Updates room successfully")
    void testUpdateRoomSuccess() {
        roomManager.addRoom("C-101", "Room 101", RoomType.CLASSROOM, "Building A", 30, null);

        ValidationResult result = roomManager.updateRoom("c-101", "New Room 101", RoomType.LABORATORY,
                "Building B", 50, "Updated description", false);

        assertTrue(result.isValid());
        Room updated = roomManager.getRoomById("C-101").orElseThrow();
        assertEquals("New Room 101", updated.getName());
        assertEquals(RoomType.LABORATORY, updated.getType());
        assertEquals("Building B", updated.getLocation());
        assertEquals(50, updated.getCapacity());
        assertEquals("Updated description", updated.getDescription());
        assertFalse(updated.isActive());
    }

    @Test
    @DisplayName("Failed update preserves the original room record unchanged")
    void testFailedUpdatePreservesOriginal() {
        roomManager.addRoom("C-101", "Original Name", RoomType.CLASSROOM, "Original Loc", 30, "Original Desc");

        ValidationResult result = roomManager.updateRoom("C-101", "   ", null, "", -10, "Ignored", false);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 3);

        Room original = roomManager.getRoomById("C-101").orElseThrow();
        assertEquals("Original Name", original.getName());
        assertEquals(RoomType.CLASSROOM, original.getType());
        assertEquals("Original Loc", original.getLocation());
        assertEquals(30, original.getCapacity());
        assertTrue(original.isActive());
    }

    @Test
    @DisplayName("Returns failure when updating non-existent room")
    void testUpdateRoomNotFound() {
        ValidationResult result = roomManager.updateRoom("X-999", "Name", RoomType.CLASSROOM, "Loc", 20, null, true);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("does not exist"));
    }

    @Test
    @DisplayName("Deletes unreferenced room permanently")
    void testDeleteUnreferencedRoom() {
        roomManager.addRoom("C-101", "Room 101", RoomType.CLASSROOM, "Loc", 30, null);
        roomManager.setScheduleReferenceChecker(id -> false);

        ValidationResult result = roomManager.deleteRoom("c-101");

        assertTrue(result.isValid());
        assertTrue(roomManager.getRoomById("C-101").isEmpty());
        assertEquals(0, roomManager.getAllRooms().size());
    }

    @Test
    @DisplayName("Deactivates referenced room instead of deleting it")
    void testDeleteReferencedRoomDeactivates() {
        roomManager.addRoom("C-101", "Room 101", RoomType.CLASSROOM, "Loc", 30, null);
        // Simulate room C-101 being referenced by an active schedule
        roomManager.setScheduleReferenceChecker(id -> "C-101".equalsIgnoreCase(id));

        ValidationResult result = roomManager.deleteRoom("c-101");

        assertTrue(result.isValid());
        Optional<Room> roomOpt = roomManager.getRoomById("C-101");
        assertTrue(roomOpt.isPresent());
        assertFalse(roomOpt.get().isActive(), "Referenced room must be deactivated");
        assertEquals(1, roomManager.getAllRooms().size());
    }

    @Test
    @DisplayName("Returns failure when deleting non-existent room")
    void testDeleteRoomNotFound() {
        ValidationResult result = roomManager.deleteRoom("UNKNOWN");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("does not exist"));
    }
}
