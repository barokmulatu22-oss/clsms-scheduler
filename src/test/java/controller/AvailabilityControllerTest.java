package controller;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityControllerTest extends ApiTestBase {

    @Test
    void allRoomsFreeWhenNothingIsBooked() throws Exception {
        getJson("/api/availability?day=MONDAY&start=09:00&end=10:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void bookedRoomIsLeftOut() throws Exception {
        bookBaseline();
        getJson("/api/availability?day=MONDAY&start=09:30&end=10:30")
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].roomId", not(hasItem("L-01"))));
    }

    @Test
    void backToBackTimeStillShowsTheRoom() throws Exception {
        bookBaseline();
        getJson("/api/availability?day=MONDAY&start=10:00&end=11:00")
                .andExpect(jsonPath("$[*].roomId", hasItem("L-01")));
    }

    @Test
    void capacityAndTypeFiltersWork() throws Exception {
        getJson("/api/availability?day=MONDAY&start=09:00&end=10:00&minCapacity=50")
                .andExpect(jsonPath("$[*].roomId", contains("C-205")));
        getJson("/api/availability?day=MONDAY&start=09:00&end=10:00&type=laboratory")
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void deactivatedRoomIsNotOffered() throws Exception {
        bookBaseline();
        deleteIt("/api/rooms/L-01").andExpect(status().isOk());
        getJson("/api/availability?day=FRIDAY&start=09:00&end=10:00")
                .andExpect(jsonPath("$[*].roomId", not(hasItem("L-01"))));
    }

    @Test
    void missingParametersListedTogether() throws Exception {
        getJson("/api/availability")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(3)));
    }

    @Test
    void badValuesGive400() throws Exception {
        getJson("/api/availability?day=MONDAY&start=10:00&end=09:00").andExpect(status().isBadRequest());
        getJson("/api/availability?day=MONDAY&start=09:00&end=10:00&type=HALL").andExpect(status().isBadRequest());
        getJson("/api/availability?day=MONDAY&start=09:00&end=10:00&minCapacity=abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)));
    }
}
