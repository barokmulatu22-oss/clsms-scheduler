package controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With default settings the app starts with no data at all. */
@SpringBootTest(properties = "clsms.load-sample-data=false")
@AutoConfigureMockMvc
class EmptyStartTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void appStartsEmptyByDefault() throws Exception {
        mvc.perform(get("/api/rooms")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/courses")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/instructors")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/schedules")).andExpect(jsonPath("$.length()").value(0));
    }
}
