package com.ansar.secureapi;

import com.ansar.secureapi.security.LoginAttemptService;
import com.ansar.secureapi.repo.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository users;
    @Autowired LoginAttemptService attempts;

    @BeforeEach
    void reset() { attempts.reset(); }

    @Test
    void registerThenLoginReturnsJwt() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice_auth\",\"password\":\"CorrectHorse1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice_auth\",\"password\":\"CorrectHorse1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        TestSupport.register(mvc, "hash_user", "CorrectHorse1");
        String hash = users.findByUsername("hash_user").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2a$12$").doesNotContain("CorrectHorse1");
    }

    @Test
    void registerCannotEscalateRoleViaBody() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sneaky\",\"password\":\"CorrectHorse1\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void duplicateUsernameIsConflict() throws Exception {
        TestSupport.register(mvc, "dupe_user", "CorrectHorse1");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dupe_user\",\"password\":\"CorrectHorse1\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordAndUnknownUserGiveSameGeneric401() throws Exception {
        TestSupport.register(mvc, "bob_auth", "CorrectHorse1");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob_auth\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody_here\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void accountLocksAfterRepeatedFailuresEvenWithCorrectPassword() throws Exception {
        TestSupport.register(mvc, "lock_me", "CorrectHorse1");
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"lock_me\",\"password\":\"bad-guess-" + i + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lock_me\",\"password\":\"CorrectHorse1\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIs401() throws Exception {
        TestSupport.register(mvc, "tamper_user", "CorrectHorse1");
        String token = TestSupport.login(mvc, om, "tamper_user", "CorrectHorse1");
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        mvc.perform(get("/api/tickets").header("Authorization", TestSupport.bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unsignedAlgNoneTokenIs401() throws Exception {
        TestSupport.register(mvc, "none_user", "CorrectHorse1");
        var enc = java.util.Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = enc.encodeToString("{\"sub\":\"none_user\",\"iss\":\"spring-secure-api\",\"role\":\"ADMIN\"}".getBytes());
        mvc.perform(get("/api/tickets").header("Authorization", "Bearer " + header + "." + payload + "."))
                .andExpect(status().isUnauthorized());
    }
}
