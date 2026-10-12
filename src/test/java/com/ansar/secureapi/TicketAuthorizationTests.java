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
class TicketAuthorizationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    String alice, mallory, admin;
    long aliceTicketId;

    @BeforeEach
    void setup() throws Exception {
        TestSupport.register(mvc, "alice", "AlicePassw0rd");
        TestSupport.register(mvc, "mallory", "MalloryPassw0rd");
        alice = TestSupport.bearer(TestSupport.login(mvc, om, "alice", "AlicePassw0rd"));
        mallory = TestSupport.bearer(TestSupport.login(mvc, om, "mallory", "MalloryPassw0rd"));
        admin = TestSupport.bearer(TestSupport.login(mvc, om, "admin", "AdminPassw0rd!"));
        String body = mvc.perform(post("/api/tickets").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Phishing email reported\",\"severity\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        aliceTicketId = om.readTree(body).get("id").asLong();
    }

    @Test
    void ownerCanReadOwnTicket() throws Exception {
        mvc.perform(get("/api/tickets/" + aliceTicketId).header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("alice"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void idorReadOfAnotherUsersTicketIsDenied() throws Exception {
        mvc.perform(get("/api/tickets/" + aliceTicketId).header("Authorization", mallory))
                .andExpect(status().isNotFound());
    }

    @Test
    void idorUpdateOfAnotherUsersTicketIsDenied() throws Exception {
        mvc.perform(put("/api/tickets/" + aliceTicketId).header("Authorization", mallory)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"pwned\",\"severity\":\"LOW\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/tickets/" + aliceTicketId).header("Authorization", alice))
                .andExpect(jsonPath("$.title").value("Phishing email reported"));
    }

    @Test
    void idorDeleteOfAnotherUsersTicketIsDenied() throws Exception {
        mvc.perform(delete("/api/tickets/" + aliceTicketId).header("Authorization", mallory))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/tickets/" + aliceTicketId).header("Authorization", alice))
                .andExpect(status().isOk());
    }

    @Test
    void listOnlyReturnsCallersOwnTickets() throws Exception {
        mvc.perform(get("/api/tickets").header("Authorization", mallory))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.owner == 'alice')]").isEmpty());
    }

    @Test
    void userCannotReachAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/tickets").header("Authorization", alice)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", alice)).andExpect(status().isForbidden());
    }

    @Test
    void adminCanSeeAllTicketsAndAnyTicket() throws Exception {
        mvc.perform(get("/api/admin/tickets").header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(get("/api/tickets/" + aliceTicketId).header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void ownerCanUpdateAndDelete() throws Exception {
        mvc.perform(put("/api/tickets/" + aliceTicketId).header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Phishing - triaged\",\"severity\":\"MEDIUM\",\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(delete("/api/tickets/" + aliceTicketId).header("Authorization", alice))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/tickets/" + aliceTicketId).header("Authorization", alice))
                .andExpect(status().isNotFound());
    }
}
