package com.ansar.secureapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    String token;

    @BeforeEach
    void setup() throws Exception {
        TestSupport.register(mvc, "val_user", "ValidPassw0rd");
        token = TestSupport.bearer(TestSupport.login(mvc, om, "val_user", "ValidPassw0rd"));
    }

    @Test
    void shortPasswordRejectedWithFieldError() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"shorty\",\"password\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void invalidUsernameCharactersRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"<script>\",\"password\":\"LongEnough123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void blankTitleAndMissingSeverityRejected() throws Exception {
        mvc.perform(post("/api/tickets").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.severity").exists());
    }

    @Test
    void invalidEnumValueIsMalformedRequestNotServerError() throws Exception {
        mvc.perform(post("/api/tickets").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"severity\":\"APOCALYPTIC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request"));
    }

    @Test
    void nonNumericIdIsBadRequest() throws Exception {
        mvc.perform(get("/api/tickets/abc").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overlongTitleRejected() throws Exception {
        mvc.perform(post("/api/tickets").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(121) + "\",\"severity\":\"LOW\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }
}
