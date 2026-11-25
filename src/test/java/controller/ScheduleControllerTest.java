package controller;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScheduleControllerTest extends ApiTestBase {

    @Test
    void validScheduleIsCreated() throws Exception {
        postJson("/api/schedules", booking("PHY210", "I-002", "L-01", "MONDAY", "09:00", "10:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleId").value("S-001"))
                .andExpect(jsonPath("$.roomType").value("LABORATORY"))
                .andExpect(jsonPath("$.startTime").value("09:00"));
        getJson("/api/schedules").andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void roomConflictGives409AndListsEveryProblem() throws Exception {
        bookBaseline();
        // MAT202: wrong room type, too many students, and L-01 is busy at 09:30
        postJson("/api/schedules", booking("MAT202", "I-003", "L-01", "MONDAY", "09:30", "10:30"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[*]", org.hamcrest.Matchers.hasItem(containsString("Room conflict"))))
                .andExpect(jsonPath("$.errors[*]", org.hamcrest.Matchers.hasItem(containsString("Capacity conflict"))))
                .andExpect(jsonPath("$.errors[*]", org.hamcrest.Matchers.hasItem(containsString("Room type conflict"))));
        getJson("/api/schedules").andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void instructorConflictIsReported() throws Exception {
        bookBaseline();
        addFlexibleCourse("GEN100");
        postJson("/api/schedules", booking("GEN100", "I-002", "C-101", "MONDAY", "09:30", "10:30"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value(containsString("Instructor conflict")));
    }

    @Test
    void backToBackBookingIsAllowed() throws Exception {
        bookBaseline();
        addFlexibleCourse("GEN100");
        postJson("/api/schedules", booking("GEN100", "I-002", "L-01", "MONDAY", "10:00", "11:00"))
                .andExpect(status().isCreated());
    }

    @Test
    void conflictingUpdateKeepsTheOriginal() throws Exception {
        bookBaseline();
        addFlexibleCourse("GEN100");
        postJson("/api/schedules", booking("GEN100", "I-003", "L-01", "MONDAY", "10:00", "11:00"))
                .andExpect(jsonPath("$.scheduleId").value("S-002"));

        putJson("/api/schedules/S-002", booking("GEN100", "I-003", "L-01", "MONDAY", "09:30", "10:30"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Room conflict")));

        getJson("/api/schedules/S-002")
                .andExpect(jsonPath("$.startTime").value("10:00"))
                .andExpect(jsonPath("$.endTime").value("11:00"));
    }

    @Test
    void validUpdateChangesTheBooking() throws Exception {
        bookBaseline();
        // Same slot again is fine: a booking never conflicts with itself
        putJson("/api/schedules/S-001", booking("PHY210", "I-002", "L-01", "TUESDAY", "09:00", "10:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.day").value("TUESDAY"));
    }

    @Test
    void cancellingFreesTheSlot() throws Exception {
        bookBaseline();
        deleteIt("/api/schedules/S-001").andExpect(status().isOk());
        getJson("/api/schedules").andExpect(jsonPath("$", hasSize(0)));
        postJson("/api/schedules", booking("PHY210", "I-002", "L-01", "MONDAY", "09:00", "10:00"))
                .andExpect(status().isCreated());
    }

    @Test
    void badInputGives400WithEveryMessage() throws Exception {
        postJson("/api/schedules", "{\"day\":\"FUNDAY\",\"startTime\":\"25:99\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(6)));
    }

    @Test
    void endBeforeStartGives400() throws Exception {
        postJson("/api/schedules", booking("PHY210", "I-002", "L-01", "MONDAY", "10:00", "09:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(containsString("End time")));
    }

    @Test
    void unknownIdsGive404() throws Exception {
        postJson("/api/schedules", booking("NOPE", "I-002", "L-01", "MONDAY", "09:00", "10:00"))
                .andExpect(status().isNotFound());
        getJson("/api/schedules/S-999").andExpect(status().isNotFound());
        putJson("/api/schedules/S-999", booking("PHY210", "I-002", "L-01", "MONDAY", "09:00", "10:00"))
                .andExpect(status().isNotFound());
        deleteIt("/api/schedules/S-999").andExpect(status().isNotFound());
    }

    @Test
    void filtersNarrowTheList() throws Exception {
        bookBaseline();
        addFlexibleCourse("GEN100");
        postJson("/api/schedules", booking("GEN100", "I-003", "C-101", "TUESDAY", "13:00", "14:00"))
                .andExpect(status().isCreated());

        getJson("/api/schedules").andExpect(jsonPath("$", hasSize(2)));
        getJson("/api/schedules?room=L-01").andExpect(jsonPath("$", hasSize(1)));
        getJson("/api/schedules?instructor=I-003").andExpect(jsonPath("$[0].courseCode").value("GEN100"));
        getJson("/api/schedules?course=PHY210").andExpect(jsonPath("$", hasSize(1)));
        getJson("/api/schedules?day=tuesday").andExpect(jsonPath("$", hasSize(1)));
        getJson("/api/schedules?day=MONDAY&time=09:30").andExpect(jsonPath("$", hasSize(1)));
        getJson("/api/schedules?day=MONDAY&time=10:00").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void badFilterValueGives400NotACrash() throws Exception {
        getJson("/api/schedules?time=banana")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)));
        getJson("/api/schedules?day=funday").andExpect(status().isBadRequest());
    }
}
