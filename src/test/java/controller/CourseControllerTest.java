package controller;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseControllerTest extends ApiTestBase {

    private static final String NEW_COURSE =
            "{\"courseCode\":\"bio101\",\"title\":\"Biology\",\"expectedEnrollment\":30,"
                    + "\"requiredRoomType\":\"LABORATORY\",\"department\":\"Science\",\"section\":\"A\"}";

    @Test
    void createCourseThenListIt() throws Exception {
        postJson("/api/courses", NEW_COURSE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseCode").value("BIO101"));
        getJson("/api/courses").andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void duplicateCourseGives409() throws Exception {
        postJson("/api/courses", NEW_COURSE).andExpect(status().isCreated());
        postJson("/api/courses", NEW_COURSE).andExpect(status().isConflict());
    }

    @Test
    void badCourseGives400WithAllErrors() throws Exception {
        postJson("/api/courses", "{\"courseCode\":\"\",\"title\":\"\",\"expectedEnrollment\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(3)));
    }

    @Test
    void unknownCourseGives404() throws Exception {
        getJson("/api/courses/NOPE").andExpect(status().isNotFound());
    }

    @Test
    void searchByTitle() throws Exception {
        getJson("/api/courses?search=physics")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseCode").value("PHY210"));
    }

    @Test
    void updateCourse() throws Exception {
        putJson("/api/courses/MAT202",
                "{\"title\":\"Discrete Math II\",\"expectedEnrollment\":40,\"requiredRoomType\":\"CLASSROOM\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Discrete Math II"));
    }

    @Test
    void bookedCourseCannotBeDeleted() throws Exception {
        bookBaseline();
        deleteIt("/api/courses/PHY210")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Cannot delete")));
        getJson("/api/courses/PHY210").andExpect(status().isOk());
    }

    @Test
    void unbookedCourseCanBeDeleted() throws Exception {
        deleteIt("/api/courses/MAT202").andExpect(status().isOk());
        getJson("/api/courses/MAT202").andExpect(status().isNotFound());
    }
}
