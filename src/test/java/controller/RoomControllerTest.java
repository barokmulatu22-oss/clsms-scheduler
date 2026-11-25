package controller;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomControllerTest extends ApiTestBase {

    private static final String NEW_ROOM =
            "{\"roomId\":\"c-300\",\"name\":\"Classroom 300\",\"type\":\"CLASSROOM\","
                    + "\"location\":\"Annex\",\"capacity\":50,\"description\":\"\"}";

    @Test
    void createRoomThenItAppearsInList() throws Exception {
        postJson("/api/rooms", NEW_ROOM)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value("C-300"))
                .andExpect(jsonPath("$.active").value(true));

        getJson("/api/rooms").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)));
        getJson("/api/rooms/C-300").andExpect(status().isOk());
    }

    @Test
    void duplicateRoomGives409() throws Exception {
        postJson("/api/rooms", NEW_ROOM).andExpect(status().isCreated());
        postJson("/api/rooms", NEW_ROOM)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("A room with ID 'C-300' already exists."));
    }

    @Test
    void badRoomListsEveryProblemWith400() throws Exception {
        postJson("/api/rooms", "{\"roomId\":\"X-1\",\"name\":\"\",\"type\":\"CLASSROOM\",\"location\":\"\",\"capacity\":-5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(3)));
    }

    @Test
    void unknownRoomTypeGives400() throws Exception {
        postJson("/api/rooms", "{\"roomId\":\"X-1\",\"name\":\"A\",\"type\":\"HALL\",\"location\":\"B\",\"capacity\":5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("Invalid value 'HALL' for field 'type'."));
    }

    @Test
    void brokenJsonGives400() throws Exception {
        postJson("/api/rooms", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)));
    }

    @Test
    void unknownRoomGives404() throws Exception {
        getJson("/api/rooms/NOPE").andExpect(status().isNotFound());
        deleteIt("/api/rooms/NOPE").andExpect(status().isNotFound());
    }

    @Test
    void searchFindsRoomsByText() throws Exception {
        getJson("/api/rooms?search=laboratory")
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].roomId", hasItem("L-01")));
    }

    @Test
    void updateChangesRoomAndBadUpdateKeepsOriginal() throws Exception {
        putJson("/api/rooms/C-101",
                "{\"name\":\"Renamed\",\"type\":\"CLASSROOM\",\"location\":\"Main\",\"capacity\":45}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));

        putJson("/api/rooms/C-101",
                "{\"name\":\"Broken\",\"type\":\"CLASSROOM\",\"location\":\"Main\",\"capacity\":0}")
                .andExpect(status().isBadRequest());
        getJson("/api/rooms/C-101").andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void deletingBookedRoomDeactivatesIt() throws Exception {
        bookBaseline();
        deleteIt("/api/rooms/L-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("deactivated")));
        getJson("/api/rooms/L-01").andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deletingUnbookedRoomRemovesIt() throws Exception {
        deleteIt("/api/rooms/C-205").andExpect(status().isOk());
        getJson("/api/rooms/C-205").andExpect(status().isNotFound());
    }
}
