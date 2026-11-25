package controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PRD section 13 test strategy, checked through the real API.
 * Every test builds its own rooms/instructors/courses (unique IDs),
 * so tests do not disturb each other and share one running app.
 */
@SpringBootTest(properties = "clsms.load-sample-data=true")
@AutoConfigureMockMvc
class AcceptanceCriteriaTest {

    @Autowired
    private MockMvc mvc;

    private static final AtomicInteger COUNTER = new AtomicInteger();

    // ---------- helpers ----------

    private ResultActions send(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String newId(String prefix) {
        return prefix + "-" + COUNTER.incrementAndGet();
    }

    private String addRoom(int capacity, String type) throws Exception {
        String id = newId("R");
        send("/api/rooms", "{\"roomId\":\"" + id + "\",\"name\":\"Room " + id + "\",\"type\":\"" + type
                + "\",\"location\":\"Test wing\",\"capacity\":" + capacity + "}").andExpect(status().isCreated());
        return id;
    }

    private String addInstructor() throws Exception {
        String id = newId("I9");
        send("/api/instructors", "{\"instructorId\":\"" + id + "\",\"fullName\":\"Teacher " + id + "\"}")
                .andExpect(status().isCreated());
        return id;
    }

    /** Course with the given size and optional required type ("null" means any type). */
    private String addCourse(int students, String requiredType) throws Exception {
        String code = newId("C9");
        String type = requiredType == null ? "null" : "\"" + requiredType + "\"";
        send("/api/courses", "{\"courseCode\":\"" + code + "\",\"title\":\"Test course\",\"expectedEnrollment\":"
                + students + ",\"requiredRoomType\":" + type + "}").andExpect(status().isCreated());
        return code;
    }

    private ResultActions book(String course, String instructor, String room, String day, String start, String end)
            throws Exception {
        return send("/api/schedules", "{\"courseCode\":\"" + course + "\",\"instructorId\":\"" + instructor
                + "\",\"roomId\":\"" + room + "\",\"day\":\"" + day + "\",\"startTime\":\"" + start
                + "\",\"endTime\":\"" + end + "\"}");
    }

    // ---------- interval overlap (exact, partial, containment, equal, back-to-back) ----------

    @Test
    void roomOverlapRulesAcrossTheApi() throws Exception {
        String room = addRoom(30, "CLASSROOM");
        String course = addCourse(10, null);
        book(course, addInstructor(), room, "MONDAY", "10:00", "12:00").andExpect(status().isCreated());

        // every kind of overlap is rejected
        book(course, addInstructor(), room, "MONDAY", "10:00", "12:00").andExpect(status().isConflict()); // equal
        book(course, addInstructor(), room, "MONDAY", "09:00", "11:00").andExpect(status().isConflict()); // overlaps the start
        book(course, addInstructor(), room, "MONDAY", "11:00", "13:00").andExpect(status().isConflict()); // overlaps the end
        book(course, addInstructor(), room, "MONDAY", "10:30", "11:30").andExpect(status().isConflict()); // inside
        book(course, addInstructor(), room, "MONDAY", "09:00", "13:00").andExpect(status().isConflict()); // around

        // back-to-back is allowed on both sides
        book(course, addInstructor(), room, "MONDAY", "08:00", "10:00").andExpect(status().isCreated());
        book(course, addInstructor(), room, "MONDAY", "12:00", "14:00").andExpect(status().isCreated());
    }

    @Test
    void sameRoomOnAnotherDayOrAnotherRoomIsFine() throws Exception {
        String room = addRoom(30, "CLASSROOM");
        String otherRoom = addRoom(30, "CLASSROOM");
        String course = addCourse(10, null);
        book(course, addInstructor(), room, "MONDAY", "10:00", "12:00").andExpect(status().isCreated());

        book(course, addInstructor(), room, "TUESDAY", "10:00", "12:00").andExpect(status().isCreated());
        book(course, addInstructor(), otherRoom, "MONDAY", "10:00", "12:00").andExpect(status().isCreated());
    }

    // ---------- instructor conflicts ----------

    @Test
    void instructorOverlapRulesAcrossTheApi() throws Exception {
        String teacher = addInstructor();
        String course = addCourse(10, null);
        book(course, teacher, addRoom(30, "CLASSROOM"), "WEDNESDAY", "10:00", "12:00").andExpect(status().isCreated());

        // same teacher, same day, overlapping time, different room: rejected
        book(course, teacher, addRoom(30, "CLASSROOM"), "WEDNESDAY", "11:00", "13:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Instructor conflict")));
        // same teacher, back-to-back: allowed
        book(course, teacher, addRoom(30, "CLASSROOM"), "WEDNESDAY", "12:00", "13:00").andExpect(status().isCreated());
        // same teacher, other day: allowed
        book(course, teacher, addRoom(30, "CLASSROOM"), "THURSDAY", "10:00", "12:00").andExpect(status().isCreated());
        // other teacher, same time: allowed
        book(course, addInstructor(), addRoom(30, "CLASSROOM"), "WEDNESDAY", "10:00", "12:00")
                .andExpect(status().isCreated());
    }

    // ---------- capacity (equal, below, over) and room type ----------

    @Test
    void capacityRuleAtEqualBelowAndOver() throws Exception {
        String room = addRoom(30, "CLASSROOM");
        book(addCourse(30, null), addInstructor(), room, "FRIDAY", "08:00", "09:00").andExpect(status().isCreated()); // equal
        book(addCourse(10, null), addInstructor(), room, "FRIDAY", "09:00", "10:00").andExpect(status().isCreated()); // below
        book(addCourse(31, null), addInstructor(), room, "FRIDAY", "10:00", "11:00")                                   // over
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Capacity conflict")));
    }

    @Test
    void courseThatNeedsALabIsRejectedInAClassroom() throws Exception {
        String classroom = addRoom(50, "CLASSROOM");
        String lab = addRoom(50, "LABORATORY");
        String labCourse = addCourse(20, "LABORATORY");
        book(labCourse, addInstructor(), classroom, "FRIDAY", "13:00", "14:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Room type conflict")));
        book(labCourse, addInstructor(), lab, "FRIDAY", "13:00", "14:00").andExpect(status().isCreated());
    }

    // ---------- availability against known bookings ----------

    @Test
    void availabilityMatchesKnownBookings() throws Exception {
        String room = addRoom(77, "LABORATORY"); // capacity 77 is unique, so the filter finds only this room
        book(addCourse(10, null), addInstructor(), room, "SATURDAY", "10:00", "11:00").andExpect(status().isCreated());

        mvc.perform(get("/api/availability?day=SATURDAY&start=10:30&end=11:30&minCapacity=77"))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/availability?day=SATURDAY&start=11:00&end=12:00&minCapacity=77"))
                .andExpect(jsonPath("$[0].roomId").value(room));
        mvc.perform(get("/api/availability?day=SUNDAY&start=10:30&end=11:30&minCapacity=77"))
                .andExpect(jsonPath("$[0].roomId").value(room));
    }

    // ---------- bad input never crashes the server ----------

    @Test
    void textBodyInsteadOfJsonGives400Not500() throws Exception {
        mvc.perform(post("/api/rooms").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").exists());
    }

    @Test
    void jsonArrayOrWrongTypesGive400() throws Exception {
        send("/api/rooms", "[]").andExpect(status().isBadRequest());
        send("/api/rooms", "{\"roomId\":\"X-1\",\"name\":\"A\",\"type\":\"CLASSROOM\",\"location\":\"B\",\"capacity\":\"many\"}")
                .andExpect(status().isBadRequest());
        send("/api/schedules", "{\"day\":[1,2]}").andExpect(status().isBadRequest());
    }

    @Test
    void fractionalCapacityIsRejectedInsteadOfRounded() throws Exception {
        send("/api/rooms", "{\"roomId\":\"F-1\",\"name\":\"A\",\"type\":\"CLASSROOM\",\"location\":\"B\",\"capacity\":2.5}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void idsWithUrlCharactersAreRejected() throws Exception {
        for (String badId : new String[]{"A/B", "A B", "A?B", "A#B", "A%41"}) {
            send("/api/rooms", "{\"roomId\":\"" + badId + "\",\"name\":\"A\",\"type\":\"CLASSROOM\",\"location\":\"B\",\"capacity\":5}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value(containsString("letters, digits")));
            send("/api/courses", "{\"courseCode\":\"" + badId + "\",\"title\":\"A\",\"expectedEnrollment\":5}")
                    .andExpect(status().isBadRequest());
            send("/api/instructors", "{\"instructorId\":\"" + badId + "\",\"fullName\":\"A\"}")
                    .andExpect(status().isBadRequest());
        }
    }
}
