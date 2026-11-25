package controller;

import controller.dto.ErrorResponse;
import controller.dto.RoomDTO;
import model.Room;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.RoomManager;
import util.ValidationResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing Room resources under /api/rooms.
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomManager roomManager;

    public RoomController(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @GetMapping
    public List<RoomDTO> getAllRooms(@RequestParam(required = false) String search) {
        List<Room> rooms = (search != null && !search.isBlank())
                ? roomManager.searchRooms(search)
                : roomManager.getAllRooms();
        return rooms.stream().map(RoomDTO::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoomById(@PathVariable String id) {
        Optional<Room> roomOpt = roomManager.getRoomById(id);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Room with ID '" + id + "' not found."));
        }
        return ResponseEntity.ok(RoomDTO.fromEntity(roomOpt.get()));
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody RoomDTO dto) {
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Request body cannot be null."));
        }
        ValidationResult result = roomManager.addRoom(
                dto.roomId(),
                dto.name(),
                dto.type(),
                dto.location(),
                dto.capacity(),
                dto.description()
        );
        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }
        Room created = roomManager.getRoomById(dto.roomId()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(RoomDTO.fromEntity(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable String id, @RequestBody RoomDTO dto) {
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Request body cannot be null."));
        }
        Optional<Room> roomOpt = roomManager.getRoomById(id);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Room with ID '" + id + "' not found."));
        }
        boolean active = (dto.active() != null) ? dto.active() : roomOpt.get().isActive();
        ValidationResult result = roomManager.updateRoom(
                id,
                dto.name(),
                dto.type(),
                dto.location(),
                dto.capacity(),
                dto.description(),
                active
        );
        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }
        Room updated = roomManager.getRoomById(id).orElseThrow();
        return ResponseEntity.ok(RoomDTO.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable String id) {
        Optional<Room> roomOpt = roomManager.getRoomById(id);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Room with ID '" + id + "' not found."));
        }
        ValidationResult result = roomManager.deleteRoom(id);
        if (!result.isValid()) {
            return ApiResponseHelper.toErrorResponse(result);
        }
        if (roomManager.getRoomById(id).isPresent()) {
            return ResponseEntity.ok(Map.of("message", "Room '" + id.toUpperCase() + "' has active bookings and was deactivated instead of removed."));
        } else {
            return ResponseEntity.ok(Map.of("message", "Room '" + id.toUpperCase() + "' was deleted successfully."));
        }
    }
}
