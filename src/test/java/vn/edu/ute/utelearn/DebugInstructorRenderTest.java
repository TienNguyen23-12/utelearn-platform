package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class DebugInstructorRenderTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    void testRenderCourses() throws Exception {
        mockMvc.perform(get("/instructor/courses"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    void testRenderCohorts() throws Exception {
        mockMvc.perform(get("/instructor/cohorts")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    void testRenderAssets() throws Exception {
        mockMvc.perform(get("/instructor/assets")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    void testRenderCourseGradebook() throws Exception {
        mockMvc.perform(get("/instructor/courses/1/gradebook")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testRenderAdminInstructorViews() throws Exception {
        mockMvc.perform(get("/instructor/courses")).andExpect(status().isOk());
        mockMvc.perform(get("/instructor/cohorts")).andExpect(status().isOk());
        mockMvc.perform(get("/instructor/assets")).andExpect(status().isOk());
        mockMvc.perform(get("/instructor/courses/1/gradebook")).andExpect(status().isOk());
    }
}
