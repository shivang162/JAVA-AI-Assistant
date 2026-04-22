package com.aiassistant.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void chatEndpointReturnsAssistantResponse() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").isNotEmpty());
    }

    @Test
    void historyEndpointReturnsPersistedMessages() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"persist me\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.role == 'user' && @.content == 'persist me')]").exists());
    }

    @Test
    void commandEndpointHandlesHelp() throws Exception {
        mockMvc.perform(post("/api/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"/help\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handled").value(true))
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("/help")));
    }
}
