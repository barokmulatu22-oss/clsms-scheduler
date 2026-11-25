package controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One end-to-end walk through the demo, using only the HTTP API on a fresh app.
 * Steps: master data, schedules, view/search, conflicts, updates, cancel and rebook.
 */
@SpringBootTest(properties = "clsms.load-sample-data=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class DemoFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions send(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions sendPut(String url, String json) throws Exception {
        return mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String slot(String course, String teacher, String room, String day, String start, String end) {
        return "{\"courseCode\":\"" + course + "\",\"instructorId\":\"" + teacher + "\",\"roomId\":\"" + room
                + "\",\"day\":\"" + day + "\",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}";
    }

    /** Creates a booking, expects 201 and returns the new schedule ID. */
    private String createBooking(String body) throws Exception {
        String json = send("/api/schedules", body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.scheduleId");
    }

    @Test
    void fullDemoFlow() throws Exception {
        // ----- 1. Master data (on top of the sample data) -----
        send("/api/rooms", "{\"roomId\":\"C-301\",\"name\":\"Classroom 301\",\"type\":\"CLASSROOM\",\"location\":\"Annex\",\"capacity\":50}")
                .andExpect(status().isCreated());
        send("/api/rooms", "{\"roomId\":\"L-10\",\"name\":\"Lab 10\",\"type\":\"LABORATORY\",\"location\":\"Annex\",\"capacity\":40}")
                .andExpect(status().isCreated());
        send("/api/courses", "{\"courseCode\":\"CS201\",\"title\":\"dup\",\"expectedEnrollment\":5}")
                .andExpect(status().isConflict()); // sample course already exists
        send("/api/courses", "{\"courseCode\":\"NET101\",\"title\":\"Networks\",\"expectedEnrollment\":35,\"requiredRoomType\":\"LABORATORY\"}")
                .andExpect(status().isCreated());
        send("/api/instructors", "{\"instructorId\":\"I-010\",\"fullName\":\"Dr. Demo\"}")
                .andExpect(status().isCreated());
        mvc.perform(get("/api/reports/summary"))
                .andExpect(jsonPath("$.totalRooms").value(6))
                .andExpect(jsonPath("$.totalCourses").value(4))
                .andExpect(jsonPath("$.totalInstructors").value(4))
                .andExpect(jsonPath("$.totalSchedules").value(0));

        // ----- 2. Valid schedules -----
        String first = createBooking(slot("NET101", "I-010", "L-10", "MONDAY", "09:00", "10:00"));
        String second = createBooking(slot("MAT202", "I-003", "C-301", "MONDAY", "09:00", "11:00"));
        createBooking(slot("PHY210", "I-002", "L-01", "TUESDAY", "14:00", "16:00"));

        // ----- 3. View and search -----
        mvc.perform(get("/api/schedules")).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].day").value("MONDAY"))
                .andExpect(jsonPath("$[2].day").value("TUESDAY"));
        mvc.perform(get("/api/schedules?instructor=I-003")).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseCode").value("MAT202"));
        mvc.perform(get("/api/schedules?room=L-10")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/schedules?course=PHY210")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/schedules?day=MONDAY")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/schedules?day=MONDAY&time=10:30")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/reports/daily?day=MONDAY")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/reports/room/L-10")).andExpect(jsonPath("$.MONDAY", hasSize(1)));

        // ----- 4. Room and instructor conflicts are rejected, with details -----
        send("/api/schedules", slot("NET101", "I-001", "L-10", "MONDAY", "09:30", "10:30"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Room conflict")))
                .andExpect(jsonPath("$.errors[0]").value(containsString("L-10")))
                .andExpect(jsonPath("$.errors[0]").value(containsString("09:00")))
                .andExpect(jsonPath("$.errors[0]").value(containsString(first)));
        send("/api/schedules", slot("MAT202", "I-003", "C-205", "MONDAY", "10:00", "11:30"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Instructor conflict")))
                .andExpect(jsonPath("$.errors[0]").value(containsString(second)));
        mvc.perform(get("/api/schedules")).andExpect(jsonPath("$", hasSize(3))); // nothing was saved

        // ----- 5. A valid update, then a conflicting one that keeps the original -----
        sendPut("/api/schedules/" + first, slot("NET101", "I-010", "L-10", "WEDNESDAY", "09:00", "10:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.day").value("WEDNESDAY"));
        sendPut("/api/schedules/" + second, slot("MAT202", "I-003", "C-301", "TUESDAY", "15:00", "17:00"))
                .andExpect(status().isOk()); // instructor I-003 is free on Tuesday
        sendPut("/api/schedules/" + second, slot("MAT202", "I-002", "C-301", "TUESDAY", "15:00", "17:00"))
                .andExpect(status().isConflict()); // I-002 teaches PHY210 on Tuesday 14:00-16:00
        mvc.perform(get("/api/schedules/" + second))
                .andExpect(jsonPath("$.instructorId").value("I-003"))
                .andExpect(jsonPath("$.startTime").value("15:00"));

        // ----- 6. Availability, cancel, rebook -----
        mvc.perform(get("/api/availability?day=WEDNESDAY&start=09:00&end=10:00&type=LABORATORY&minCapacity=40"))
                .andExpect(jsonPath("$.length()").value(0)); // L-10 is taken, no other 40-seat lab
        mvc.perform(delete("/api/schedules/" + first)).andExpect(status().isOk());
        mvc.perform(get("/api/availability?day=WEDNESDAY&start=09:00&end=10:00&type=LABORATORY&minCapacity=40"))
                .andExpect(jsonPath("$[0].roomId").value("L-10"));
        createBooking(slot("NET101", "I-010", "L-10", "WEDNESDAY", "09:00", "10:00")); // rebook the freed slot

        mvc.perform(get("/api/reports/summary")).andExpect(jsonPath("$.totalSchedules").value(3));
    }
}

