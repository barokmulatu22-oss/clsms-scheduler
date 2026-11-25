package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Room Entity Tests")
class RoomTest {

    @Test
    @DisplayName("Successfully creates valid Room with all fields")
    void testValidRoomCreation() {
        Room room = new Room("C-101", "Lecture Hall 1", RoomType.CLASSROOM, "Main Building - 1st Floor", 40, "Projector equipped", true);

        assertEquals("C-101", room.getRoomId());
        assertEquals("Lecture Hall 1", room.getName());
        assertEquals(RoomType.CLASSROOM, room.getType());
        assertEquals("Main Building - 1st Floor", room.getLocation());
        assertEquals(40, room.getCapacity());
        assertEquals("Projector equipped", room.getDescription());
        assertTrue(room.isActive());
        assertTrue(room.toString().contains("C-101"));
    }

    @Test
    @DisplayName("Successfully creates Room with default active=true and trims inputs")
    void testValidRoomDefaultActiveAndTrimming() {
        Room room = new Room("  l-01  ", " Lab 1 ", RoomType.LABORATORY, " Science Block ", 24, null);

        assertEquals("L-01", room.getRoomId());
        assertEquals("Lab 1", room.getName());
        assertEquals(RoomType.LABORATORY, room.getType());
        assertEquals("Science Block", room.getLocation());
        assertEquals(24, room.getCapacity());
        assertEquals("", room.getDescription());
        assertTrue(room.isActive());
    }

    @Test
    @DisplayName("Rejects null or blank room ID")
    void testRejectsNullOrBlankRoomId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Room(null, "Room 101", RoomType.CLASSROOM, "Building A", 30, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Room("   ", "Room 101", RoomType.CLASSROOM, "Building A", 30, null));
    }

    @Test
    @DisplayName("Rejects null or blank room name")
    void testRejectsNullOrBlankRoomName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Room("C-101", null, RoomType.CLASSROOM, "Building A", 30, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Room("C-101", "   ", RoomType.CLASSROOM, "Building A", 30, null));
    }

    @Test
    @DisplayName("Rejects null room type")
    void testRejectsNullRoomType() {
        assertThrows(IllegalArgumentException.class, () ->
                new Room("C-101", "Room 101", null, "Building A", 30, null));
    }

    @Test
    @DisplayName("Rejects null or blank location")
    void testRejectsNullOrBlankLocation() {
        assertThrows(IllegalArgumentException.class, () ->
                new Room("C-101", "Room 101", RoomType.CLASSROOM, null, 30, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Room("C-101", "Room 101", RoomType.CLASSROOM, "   ", 30, null));
    }

    @Test
    @DisplayName("Rejects zero or negative capacity in constructor")
    void testRejectsZeroOrNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () ->
                new Room("C-101", "Room 101", RoomType.CLASSROOM, "Building A", 0, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Room("C-101", "Room 101", RoomType.CLASSROOM, "Building A", -10, null));
    }

    @Test
    @DisplayName("Setters update valid values and reject invalid inputs")
    void testSetterValidations() {
        Room room = new Room("C-101", "Room 101", RoomType.CLASSROOM, "Building A", 30, null);

        room.setName("Updated Room Name");
        assertEquals("Updated Room Name", room.getName());
        assertThrows(IllegalArgumentException.class, () -> room.setName(null));
        assertThrows(IllegalArgumentException.class, () -> room.setName("  "));

        room.setType(RoomType.LABORATORY);
        assertEquals(RoomType.LABORATORY, room.getType());
        assertThrows(IllegalArgumentException.class, () -> room.setType(null));

        room.setLocation("Building B - 2nd Floor");
        assertEquals("Building B - 2nd Floor", room.getLocation());
        assertThrows(IllegalArgumentException.class, () -> room.setLocation(null));
        assertThrows(IllegalArgumentException.class, () -> room.setLocation("  "));

        room.setCapacity(50);
        assertEquals(50, room.getCapacity());
        assertThrows(IllegalArgumentException.class, () -> room.setCapacity(0));
        assertThrows(IllegalArgumentException.class, () -> room.setCapacity(-5));

        room.setDescription("New description");
        assertEquals("New description", room.getDescription());

        room.setActive(false);
        assertFalse(room.isActive());
    }

    @Test
    @DisplayName("Rooms with same ID are equal regardless of other fields")
    void testEqualsAndHashCode() {
        Room room1 = new Room("C-101", "Name 1", RoomType.CLASSROOM, "Loc 1", 30, null);
        Room room2 = new Room("C-101", "Name 2", RoomType.LABORATORY, "Loc 2", 60, null);
        Room room3 = new Room("C-102", "Name 1", RoomType.CLASSROOM, "Loc 1", 30, null);

        assertEquals(room1, room2);
        assertEquals(room1.hashCode(), room2.hashCode());
        assertNotEquals(room1, room3);
    }
}
