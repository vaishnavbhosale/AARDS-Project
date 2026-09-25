package com.aards.analytics;

import com.aards.auth.dto.LoginRequest;
import com.aards.auth.service.AuthService;
import com.aards.department.DepartmentRepository;
import com.aards.session.AcademicSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Checks dashboard endpoints: filters need login, dashboard needs params + login.
@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private AcademicSessionRepository sessionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private String adminToken() {
        return authService.login(LoginRequest.builder()
                .username("admin").password("admin123").build()).getToken();
    }

    @Test
    void filtersNeedsLoginAndReturnsSessions() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/filters"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/dashboard/filters")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(hasKey("sessions")));
    }

    @Test
    void dashboardReturns200WithValidParams() throws Exception {
        Long sessionId = sessionRepository.findAll().get(0).getId();
        Long deptId = departmentRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + adminToken())
                        .param("sessionId", String.valueOf(sessionId))
                        .param("departmentId", String.valueOf(deptId))
                        .param("year", "2")
                        .param("semester", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cards").exists());
    }
}
