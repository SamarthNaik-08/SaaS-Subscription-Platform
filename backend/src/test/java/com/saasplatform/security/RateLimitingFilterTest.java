package com.saasplatform.security;

import com.saasplatform.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rateLimitingFilterReturns429WhenThresholdExceeded() throws Exception {
        // Change password endpoint limit is 5 requests per minute
        // Simulating 7 rapid requests to trigger HTTP 429 Too Many Requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/users/me/change-password")
                    .contentType("application/json")
                    .content("{\"currentPassword\":\"p1\",\"newPassword\":\"p2\"}"));
        }

        // 6th or 7th request should be rate limited and return HTTP 429
        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .contentType("application/json")
                        .content("{\"currentPassword\":\"p1\",\"newPassword\":\"p2\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
