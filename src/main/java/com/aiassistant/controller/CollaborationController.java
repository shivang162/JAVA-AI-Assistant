package com.aiassistant.controller;

import com.aiassistant.service.DeviceManager;
import com.aiassistant.service.GroupChatManager;
import com.aiassistant.service.VideoSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class CollaborationController {
    private final DeviceManager deviceManager;
    private final VideoSessionManager videoSessionManager;
    private final GroupChatManager groupChatManager;

    public CollaborationController(
            DeviceManager deviceManager,
            VideoSessionManager videoSessionManager,
            GroupChatManager groupChatManager
    ) {
        this.deviceManager = deviceManager;
        this.videoSessionManager = videoSessionManager;
        this.groupChatManager = groupChatManager;
    }

    @PostMapping("/device/register")
    public ResponseEntity<?> registerDevice(@RequestBody DeviceRegistrationRequest request, HttpServletRequest servletRequest) {
        try {
            String ipAddress = request == null ? null : request.ipAddress();
            if (ipAddress == null || ipAddress.isBlank()) {
                ipAddress = servletRequest == null ? null : servletRequest.getRemoteAddr();
            }
            return ResponseEntity.ok(deviceManager.register(
                    request == null ? null : request.deviceId(),
                    request == null ? null : request.name(),
                    request == null ? null : request.type(),
                    ipAddress
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/devices")
    public List<DeviceManager.RegisteredDevice> activeDevices() {
        return deviceManager.listActiveDevices();
    }

    @PostMapping("/video/start")
    public ResponseEntity<?> startVideo(@RequestBody StartVideoRequest request) {
        try {
            String hostDeviceId = request == null ? null : request.hostDeviceId();
            String sessionId = request == null ? null : request.sessionId();
            List<String> participantDeviceIds = request == null ? null : request.participantDeviceIds();
            if (!deviceManager.isRegistered(hostDeviceId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Host device must be registered first."));
            }
            return ResponseEntity.ok(videoSessionManager.startSession(
                    sessionId,
                    hostDeviceId,
                    participantDeviceIds
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/video/join")
    public ResponseEntity<?> joinVideo(@RequestBody JoinVideoRequest request) {
        try {
            String deviceId = request == null ? null : request.deviceId();
            String sessionId = request == null ? null : request.sessionId();
            if (!deviceManager.isRegistered(deviceId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Device must be registered first."));
            }
            return ResponseEntity.ok(videoSessionManager.joinSession(
                    sessionId,
                    deviceId
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/video/end")
    public ResponseEntity<?> endVideo(@RequestBody EndVideoRequest request) {
        try {
            return ResponseEntity.ok(videoSessionManager.endSession(
                    request == null ? null : request.sessionId()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/group-chat/create")
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest request) {
        try {
            String creatorDeviceId = request == null ? null : request.creatorDeviceId();
            String groupId = request == null ? null : request.groupId();
            String name = request == null ? null : request.name();
            List<String> invitedDeviceIds = request == null ? null : request.invitedDeviceIds();
            if (!deviceManager.isRegistered(creatorDeviceId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Creator device must be registered first."));
            }
            return ResponseEntity.ok(groupChatManager.createGroup(
                    groupId,
                    name,
                    creatorDeviceId,
                    invitedDeviceIds
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/group-chat/join")
    public ResponseEntity<?> joinGroup(@RequestBody JoinGroupRequest request) {
        try {
            String deviceId = request == null ? null : request.deviceId();
            String groupId = request == null ? null : request.groupId();
            if (!deviceManager.isRegistered(deviceId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Device must be registered first."));
            }
            return ResponseEntity.ok(groupChatManager.joinGroup(
                    groupId,
                    deviceId
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/group-chat/send")
    public ResponseEntity<?> sendGroupMessage(@RequestBody SendGroupMessageRequest request) {
        try {
            return ResponseEntity.ok(groupChatManager.sendMessage(
                    request == null ? null : request.groupId(),
                    request == null ? null : request.deviceId(),
                    request == null ? null : request.message()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/group-chat/{groupId}/messages")
    public ResponseEntity<?> getGroupMessages(@PathVariable("groupId") String groupId) {
        try {
            return ResponseEntity.ok(groupChatManager.getMessages(groupId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    public record DeviceRegistrationRequest(String deviceId, String name, String type, String ipAddress) {
    }

    public record StartVideoRequest(String sessionId, String hostDeviceId, List<String> participantDeviceIds) {
    }

    public record JoinVideoRequest(String sessionId, String deviceId) {
    }

    public record EndVideoRequest(String sessionId) {
    }

    public record CreateGroupRequest(String groupId, String name, String creatorDeviceId, List<String> invitedDeviceIds) {
    }

    public record JoinGroupRequest(String groupId, String deviceId) {
    }

    public record SendGroupMessageRequest(String groupId, String deviceId, String message) {
    }
}
