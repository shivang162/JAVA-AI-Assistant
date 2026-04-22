package com.aiassistant.service;

import com.aiassistant.persistence.entity.GroupChatEntity;
import com.aiassistant.persistence.entity.GroupChatMessageEntity;
import com.aiassistant.persistence.repository.GroupChatMessageRepository;
import com.aiassistant.persistence.repository.GroupChatRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupChatManager {
    private final GroupChatRepository groupChatRepository;
    private final GroupChatMessageRepository groupChatMessageRepository;

    public GroupChatManager(GroupChatRepository groupChatRepository, GroupChatMessageRepository groupChatMessageRepository) {
        this.groupChatRepository = groupChatRepository;
        this.groupChatMessageRepository = groupChatMessageRepository;
    }

    public synchronized GroupChat createGroup(String requestedGroupId, String name, String creatorDeviceId, List<String> invitedDeviceIds) {
        String groupId = requestedGroupId == null || requestedGroupId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedGroupId.trim();
        String normalizedCreator = normalize(creatorDeviceId, "creatorDeviceId");
        String normalizedName = normalize(name, "name");
        if (groupChatRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Group chat already exists.");
        }
        Set<String> memberDeviceIds = new LinkedHashSet<>();
        memberDeviceIds.add(normalizedCreator);
        if (invitedDeviceIds != null) {
            invitedDeviceIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .forEach(memberDeviceIds::add);
        }
        GroupChatEntity group = groupChatRepository.save(new GroupChatEntity(groupId, normalizedName, serializeMembers(memberDeviceIds), Instant.now()));
        return toView(group);
    }

    public synchronized GroupChat joinGroup(String groupId, String deviceId) {
        GroupChatEntity group = requireGroup(groupId);
        Set<String> members = deserializeMembers(group.getMemberDeviceIds());
        members.add(normalize(deviceId, "deviceId"));
        group.setMemberDeviceIds(serializeMembers(members));
        GroupChatEntity updated = groupChatRepository.save(group);
        return toView(updated);
    }

    public synchronized GroupMessage sendMessage(String groupId, String senderDeviceId, String message) {
        GroupChatEntity group = requireGroup(groupId);
        String sender = normalize(senderDeviceId, "deviceId");
        Set<String> members = deserializeMembers(group.getMemberDeviceIds());
        if (!members.contains(sender)) {
            throw new IllegalArgumentException("Device is not part of this group.");
        }
        GroupChatMessageEntity saved = groupChatMessageRepository.save(
                new GroupChatMessageEntity(group.getGroupId(), sender, normalize(message, "message"), Instant.now())
        );
        return new GroupMessage(saved.getSenderDeviceId(), saved.getContent(), saved.getSentAt());
    }

    public synchronized List<GroupMessage> getMessages(String groupId) {
        String normalizedGroupId = normalize(groupId, "groupId");
        if (!groupChatRepository.existsById(normalizedGroupId)) {
            throw new NoSuchElementException("Group chat was not found.");
        }
        return groupChatMessageRepository.findByGroupIdOrderBySentAtAsc(normalizedGroupId).stream()
                .map(message -> new GroupMessage(
                        message.getSenderDeviceId(),
                        message.getContent(),
                        message.getSentAt()
                ))
                .toList();
    }

    private GroupChatEntity requireGroup(String groupId) {
        return groupChatRepository.findById(normalize(groupId, "groupId"))
                .orElseThrow(() -> new NoSuchElementException("Group chat was not found."));
    }

    private String serializeMembers(Set<String> members) {
        return String.join(",", members);
    }

    private Set<String> deserializeMembers(String serializedMembers) {
        if (serializedMembers == null || serializedMembers.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(serializedMembers.split(","))
                .map(String::trim)
                .filter(member -> !member.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private GroupChat toView(GroupChatEntity group) {
        if (group == null) {
            throw new NoSuchElementException("Group chat was not found.");
        }
        return new GroupChat(
                group.getGroupId(),
                group.getName(),
                List.copyOf(deserializeMembers(group.getMemberDeviceIds())),
                group.getCreatedAt()
        );
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
}
