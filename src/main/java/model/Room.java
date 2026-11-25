package model;

import java.util.Objects;

/**
 * Represents a physical classroom or laboratory facility.
 */
public class Room {

    private final String roomId;
    private String name;
    private RoomType type;
    private String location;
    private int capacity;
    private String description;
    private boolean active;

    /**
     * Constructs a new Room with all attributes specified.
     *
     * @param roomId      Unique room identifier (cannot be null or blank)
     * @param name        Room name or number (cannot be null or blank)
     * @param type        Room classification (cannot be null)
     * @param location    Building and floor location (cannot be null or blank)
     * @param capacity    Seating/occupancy capacity (must be greater than 0)
     * @param description Optional description or equipment notes
     * @param active      Operational status of the room
     * @throws IllegalArgumentException if any required argument is invalid
     */
    public Room(String roomId, String name, RoomType type, String location, int capacity, String description, boolean active) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("Room ID cannot be null or blank.");
        }
        this.roomId = roomId.trim().toUpperCase();
        setName(name);
        setType(type);
        setLocation(location);
        setCapacity(capacity);
        setDescription(description);
        this.active = active;
    }

    /**
     * Constructs an active Room by default.
     */
    public Room(String roomId, String name, RoomType type, String location, int capacity, String description) {
        this(roomId, name, type, location, capacity, description, true);
    }

    public String getRoomId() {
        return roomId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name cannot be null or blank.");
        }
        this.name = name.trim();
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        if (type == null) {
            throw new IllegalArgumentException("Room type cannot be null.");
        }
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Room location cannot be null or blank.");
        }
        this.location = location.trim();
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Room capacity must be greater than zero. Provided: " + capacity);
        }
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = (description == null || description.isBlank()) ? "" : description.trim();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room other)) return false;
        return Objects.equals(roomId, other.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId);
    }

    @Override
    public String toString() {
        return String.format("Room[id=%s, name=%s, type=%s, location=%s, capacity=%d, active=%b%s]",
                roomId, name, type, location, capacity, active,
                description.isEmpty() ? "" : ", desc=" + description);
    }
}
