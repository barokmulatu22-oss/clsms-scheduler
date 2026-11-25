package service;

import model.Room;
import model.RoomType;
import util.ValidationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the collection of academic rooms and laboratories.
 * Implements in-memory storage using an ArrayList for ordered display
 * and a HashMap for efficient O(1) ID lookups.
 */
public class RoomManager {

    private final List<Room> roomList = new ArrayList<>();
    private final Map<String, Room> roomMap = new HashMap<>();
    private ScheduleReferenceChecker scheduleReferenceChecker;

    /**
     * Injects a reference checker to verify active schedule references before deletion.
     */
    public void setScheduleReferenceChecker(ScheduleReferenceChecker scheduleReferenceChecker) {
        this.scheduleReferenceChecker = scheduleReferenceChecker;
    }

    /**
     * Adds a new Room to the system if all fields are valid and ID is unique.
     */
    public ValidationResult addRoom(String roomId, String name, RoomType type,
                                    String location, int capacity, String description) {
        ValidationResult result = new ValidationResult();

        if (roomId == null || roomId.isBlank()) {
            result.addError("Room ID cannot be null or blank.");
        } else if (!util.IdRules.isValid(roomId)) {
            result.addError(util.IdRules.message("Room ID"));
        } else if (roomMap.containsKey(roomId.trim().toUpperCase())) {
            result.addError("A room with ID '" + roomId.trim().toUpperCase() + "' already exists.");
        }

        if (name == null || name.isBlank()) {
            result.addError("Room name cannot be null or blank.");
        }
        if (type == null) {
            result.addError("Room type cannot be null.");
        }
        if (location == null || location.isBlank()) {
            result.addError("Room location cannot be null or blank.");
        }
        if (capacity <= 0) {
            result.addError("Room capacity must be greater than zero. Provided: " + capacity);
        }

        if (!result.isValid()) {
            return result;
        }

        Room room = new Room(roomId, name, type, location, capacity, description, true);
        roomList.add(room);
        roomMap.put(room.getRoomId(), room);
        return ValidationResult.success();
    }

    /**
     * Retrieves a room by unique room ID (case-insensitive).
     */
    public Optional<Room> getRoomById(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(roomMap.get(roomId.trim().toUpperCase()));
    }

    /**
     * Returns an unmodifiable list of all registered rooms in insertion order.
     */
    public List<Room> getAllRooms() {
        return Collections.unmodifiableList(new ArrayList<>(roomList));
    }

    /**
     * Searches rooms by case-insensitive partial match on ID, name, or location.
     */
    public List<Room> searchRooms(String query) {
        if (query == null || query.isBlank()) {
            return getAllRooms();
        }
        String q = query.trim().toLowerCase();
        List<Room> matches = new ArrayList<>();
        for (Room r : roomList) {
            if (r.getRoomId().toLowerCase().contains(q)
                    || r.getName().toLowerCase().contains(q)
                    || r.getLocation().toLowerCase().contains(q)
                    || r.getType().name().toLowerCase().contains(q)) {
                matches.add(r);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    /**
     * Updates an existing Room atomically. If validation fails, the original record remains unchanged.
     */
    public ValidationResult updateRoom(String roomId, String name, RoomType type,
                                       String location, int capacity, String description, boolean active) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return ValidationResult.failure("Room with ID '" + roomId + "' does not exist.");
        }

        ValidationResult result = new ValidationResult();
        if (name == null || name.isBlank()) {
            result.addError("Room name cannot be null or blank.");
        }
        if (type == null) {
            result.addError("Room type cannot be null.");
        }
        if (location == null || location.isBlank()) {
            result.addError("Room location cannot be null or blank.");
        }
        if (capacity <= 0) {
            result.addError("Room capacity must be greater than zero. Provided: " + capacity);
        }

        if (!result.isValid()) {
            return result;
        }

        Room room = roomOpt.get();
        room.setName(name);
        room.setType(type);
        room.setLocation(location);
        room.setCapacity(capacity);
        room.setDescription(description);
        room.setActive(active);
        return ValidationResult.success();
    }

    /**
     * Deletes a room if unreferenced, or deactivates it if referenced by an active schedule.
     */
    public ValidationResult deleteRoom(String roomId) {
        Optional<Room> roomOpt = getRoomById(roomId);
        if (roomOpt.isEmpty()) {
            return ValidationResult.failure("Room with ID '" + roomId + "' does not exist.");
        }

        Room room = roomOpt.get();
        if (isReferenced(room.getRoomId())) {
            room.setActive(false);
            return ValidationResult.success();
        }

        roomList.remove(room);
        roomMap.remove(room.getRoomId());
        return ValidationResult.success();
    }

    private boolean isReferenced(String id) {
        return scheduleReferenceChecker != null && scheduleReferenceChecker.isReferenced(id);
    }
}
