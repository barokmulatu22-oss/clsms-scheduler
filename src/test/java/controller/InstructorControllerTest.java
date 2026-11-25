package controller;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InstructorControllerTest extends ApiTestBase {

    private static final String NEW_INSTRUCTOR =
            "{\"instructorId\":\"i-010\",\"fullName\":\"Dr. Nora\",\"department\":\"Chemistry\"}";

    @Test
    void createInstructorThenListIt() throws Exception {
        postJson("/api/instructors", NEW_INSTRUCTOR)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instructorId").value("I-010"));
        getJson("/api/instructors").andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void duplicateInstructorGives409() throws Exception {
        postJson("/api/instructors", NEW_INSTRUCTOR).andExpect(status().isCreated());
        postJson("/api/instructors", NEW_INSTRUCTOR).andExpect(status().isConflict());
    }

    @Test
    void blankNameGives400() throws Exception {
        postJson("/api/instructors", "{\"instructorId\":\"I-011\",\"fullName\":\" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)));
    }

    @Test
    void unknownInstructorGives404() throws Exception {
        getJson("/api/instructors/NOPE").andExpect(status().isNotFound());
        putJson("/api/instructors/NOPE", "{\"fullName\":\"X\"}").andExpect(status().isNotFound());
    }

    @Test
    void searchByDepartment() throws Exception {
        getJson("/api/instructors?search=physics")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].instructorId").value("I-002"));
    }

    @Test
    void updateInstructor() throws Exception {
        putJson("/api/instructors/I-001", "{\"fullName\":\"Dr. Renamed\",\"department\":\"CS\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Dr. Renamed"));
    }

    @Test
    void assignedInstructorCannotBeDeleted() throws Exception {
        bookBaseline();
        deleteIt("/api/instructors/I-002")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(containsString("Cannot delete")));
    }

    @Test
    void freeInstructorCanBeDeleted() throws Exception {
        deleteIt("/api/instructors/I-003").andExpect(status().isOk());
        getJson("/api/instructors/I-003").andExpect(status().isNotFound());
    }
}
