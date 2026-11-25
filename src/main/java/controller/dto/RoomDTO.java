package controller.dto;

import model.Room;
import model.RoomType;

/**
 * Data Transfer Object for Room entities in API requests and responses.
 */
public record RoomDTO(
        String roomId,
        String name,
        RoomType type,
        String location,
        int capacity,
        String description,
        Boolean active
) {
    public static RoomDTO fromEntity(Room room) {
        if (room == null) {
            return null;
        }
        return new RoomDTO(
                room.getRoomId(),
                room.getName(),
                room.getType(),
                room.getLocation(),
                room.getCapacity(),
                room.getDescription(),
                room.isActive()
        );
    }
}
