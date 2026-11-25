package controller;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Shared setup for controller tests. Each test gets a fresh app,
 * so the in-memory data always starts from SampleData.
 *
 * Sample data used by the tests:
 *  rooms C-101(40) C-205(60) L-01(24 lab) L-02(30 lab)
 *  instructors I-001 I-002 I-003
 *  courses CS201(35 lab) PHY210(22 lab) MAT202(45 classroom)
 */
@SpringBootTest(properties = "clsms.load-sample-data=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
abstract class ApiTestBase {

    @Autowired
    protected MockMvc mvc;

    protected ResultActions getJson(String url) throws Exception {
        return mvc.perform(get(url));
    }

    protected ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions putJson(String url, String body) throws Exception {
        return mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions deleteIt(String url) throws Exception {
        return mvc.perform(delete(url));
    }

    /** JSON body for a schedule request. */
    protected static String booking(String course, String instructor, String room,
                                    String day, String start, String end) {
        return String.format(
                "{\"courseCode\":\"%s\",\"instructorId\":\"%s\",\"roomId\":\"%s\","
                        + "\"day\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                course, instructor, room, day, start, end);
    }

    /** Books PHY210 / I-002 / L-01 on Monday 09:00-10:00 (valid on sample data). */
    protected void bookBaseline() throws Exception {
        postJson("/api/schedules", booking("PHY210", "I-002", "L-01", "MONDAY", "09:00", "10:00"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());
    }

    /** Adds a small course with no room-type requirement, so only time conflicts can fail. */
    protected void addFlexibleCourse(String code) throws Exception {
        postJson("/api/courses", "{\"courseCode\":\"" + code + "\",\"title\":\"Flexible\",\"expectedEnrollment\":10}")
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());
    }
}
