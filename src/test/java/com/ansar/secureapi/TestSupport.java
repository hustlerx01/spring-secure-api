package com.ansar.secureapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

final class TestSupport {
    private TestSupport() {}

    static void register(MockMvc mvc, String user, String pass) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"));
    }

    static String login(MockMvc mvc, ObjectMapper om, String user, String pass) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                .andReturn().getResponse().getContentAsString();
        JsonNode n = om.readTree(body);
        return n.get("accessToken").asText();
    }

    static String bearer(String token) { return "Bearer " + token; }
}
