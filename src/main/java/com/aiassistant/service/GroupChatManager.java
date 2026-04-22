package com.aiassistant.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GroupChatManager {
    private final Map<String, GroupChatState> groups = new ConcurrentHashMap<>();

    public synchronized GroupChat createGroup(String requestedGroupId, String name, String creatorDeviceId, List<String> invitedDeviceIds) {
        String groupId = requestedGroupId == null || requestedGroupId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedGroupId.trim();
        String normalizedCreator = normalize(creatorDeviceId, "creatorDeviceId");
        GroupChatState state = new GroupChatState(groupId, normalize(name, "name"));
        state.memberDeviceIds.add(normalizedCreator);
        if (invitedDeviceIds != null) {
            invitedDeviceIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .forEach(state.memberDeviceIds::add);
        }
        GroupChatState existing = groups.putIfAbsent(groupId, state);
        if (existing != null) {
            throw new IllegalArgumentException("Group chat already exists.");
        }
        return state.toView();
    }

    public synchronized GroupChat joinGroup(String groupId, String deviceId) {
        GroupChatState state = requireGroup(groupId);
        state.memberDeviceIds.add(normalize(deviceId, "deviceId"));
        return state.toView();
    }

    public synchronized GroupMessage sendMessage(String groupId, String senderDeviceId, String message) {
        GroupChatState state = requireGroup(groupId);
        String sender = normalize(senderDeviceId, "deviceId");
        if (!state.memberDeviceIds.contains(sender)) {
            throw new IllegalArgumentException("Device is not part of this group.");
        }
        GroupMessage groupMessage = new GroupMessage(sender, normalize(message, "message"), Instant.now());
        state.messages.add(groupMessage);
        return groupMessage;
    }

    public synchronized List<GroupMessage> getMessages(String groupId) {
        GroupChatState state = requireGroup(groupId);
        return List.copyOf(state.messages);
    }

    private GroupChatState requireGroup(String groupId) {
        GroupChatState state = groups.get(normalize(groupId, "groupId"));
        if (state == null) {
            throw new NoSuchElementException("Group chat was not found.");
        }
        return state;
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    public record GroupChat(
            String groupId,
            String name,
            List<String> memberDeviceIds,
            Instant createdAt
    ) {
    }

    public record GroupMessage(
            String senderDeviceId,
            String content,
            Instant sentAt
    ) {
    }

    private static final class GroupChatState {
        private final String groupId;
        private final String name;
        private final Instant createdAt;
        private final Set<String> memberDeviceIds = new LinkedHashSet<>();
        private final List<GroupMessage> messages = new ArrayList<>();

        private GroupChatState(String groupId, String name) {
            this.groupId = groupId;
            this.name = name;
            this.createdAt = Instant.now();
        }

        private GroupChat toView() {
            return new GroupChat(groupId, name, List.copyOf(memberDeviceIds), createdAt);
        }
    }
}
