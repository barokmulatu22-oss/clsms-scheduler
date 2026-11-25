package controller;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerTest extends ApiTestBase {

    @Test
    void dailyTimetableIsInTimeOrder() throws Exception {
        addFlexibleCourse("GEN100");
        postJson("/api/schedules", booking("GEN100", "I-003", "C-101", "MONDAY", "13:00", "14:00"));
        bookBaseline();

        getJson("/api/reports/daily?day=monday")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].startTime").value("09:00"))
                .andExpect(jsonPath("$[1].startTime").value("13:00"));
    }

    @Test
    void dailyNeedsAValidDay() throws Exception {
        getJson("/api/reports/daily").andExpect(status().isBadRequest());
        getJson("/api/reports/daily?day=funday").andExpect(status().isBadRequest());
    }

    @Test
    void roomTimetableIsGroupedByDay() throws Exception {
        bookBaseline();
        getJson("/api/reports/room/L-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.MONDAY", hasSize(1)))
                .andExpect(jsonPath("$.MONDAY[0].courseCode").value("PHY210"));
    }

    @Test
    void instructorTimetableIsGroupedByDay() throws Exception {
        bookBaseline();
        getJson("/api/reports/instructor/I-002")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.MONDAY", hasSize(1)));
    }

    @Test
    void unknownRoomOrInstructorGives404() throws Exception {
        getJson("/api/reports/room/NOPE").andExpect(status().isNotFound());
        getJson("/api/reports/instructor/NOPE").andExpect(status().isNotFound());
    }

    @Test
    void summaryCountsEverything() throws Exception {
        bookBaseline();
        getJson("/api/reports/summary")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms").value(4))
                .andExpect(jsonPath("$.totalCourses").value(3))
                .andExpect(jsonPath("$.totalInstructors").value(3))
                .andExpect(jsonPath("$.totalSchedules").value(1));
    }

    @Test
    void unknownApiPathGivesJsonError404() throws Exception {
        getJson("/api/nothing-here")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors", hasSize(1)));
    }
}
