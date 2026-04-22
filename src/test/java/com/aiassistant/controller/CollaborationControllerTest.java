package com.aiassistant.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CollaborationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerDeviceAndListActiveDevices() throws Exception {
        String deviceId = "device-" + UUID.randomUUID();
        mockMvc.perform(post("/api/device/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"" + deviceId + "\",\"name\":\"My Laptop\",\"type\":\"desktop\",\"ipAddress\":\"127.0.0.1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(deviceId));

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].deviceId", hasItem(deviceId)));
    }

    @Test
    void startJoinAndEndVideoSession() throws Exception {
        String hostDeviceId = "host-" + UUID.randomUUID();
        String guestDeviceId = "guest-" + UUID.randomUUID();
        String sessionId = "video-" + UUID.randomUUID();
        registerDevice(hostDeviceId, "Host Device");
        registerDevice(guestDeviceId, "Guest Device");

        mockMvc.perform(post("/api/video/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"" + sessionId + "\",\"hostDeviceId\":\"" + hostDeviceId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.participantDeviceIds", hasItem(hostDeviceId)));

        mockMvc.perform(post("/api/video/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"" + sessionId + "\",\"deviceId\":\"" + guestDeviceId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantDeviceIds", hasItem(guestDeviceId)));

        mockMvc.perform(post("/api/video/end")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"" + sessionId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void createJoinAndSendGroupMessage() throws Exception {
        String creatorDeviceId = "creator-" + UUID.randomUUID();
        String memberDeviceId = "member-" + UUID.randomUUID();
        String groupId = "group-" + UUID.randomUUID();
        registerDevice(creatorDeviceId, "Creator");
        registerDevice(memberDeviceId, "Member");

        mockMvc.perform(post("/api/group-chat/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + groupId + "\",\"name\":\"Team Room\",\"creatorDeviceId\":\"" + creatorDeviceId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId));

        mockMvc.perform(post("/api/group-chat/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + groupId + "\",\"deviceId\":\"" + memberDeviceId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberDeviceIds", hasItem(memberDeviceId)));

        mockMvc.perform(post("/api/group-chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + groupId + "\",\"deviceId\":\"" + memberDeviceId + "\",\"message\":\"Hello group\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello group"));

        mockMvc.perform(get("/api/group-chat/" + groupId + "/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hello group"));
    }

    private void registerDevice(String deviceId, String name) throws Exception {
        mockMvc.perform(post("/api/device/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"" + deviceId + "\",\"name\":\"" + name + "\",\"type\":\"mobile\",\"ipAddress\":\"127.0.0.1\"}"))
                .andExpect(status().isOk());
    }
}
