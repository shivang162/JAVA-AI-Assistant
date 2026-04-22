package com.aiassistant.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "group_chat_messages")
public class GroupChatMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String groupId;

    @Column(nullable = false, length = 128)
    private String senderDeviceId;

    @Column(nullable = false, length = 4096)
    private String content;

    @Column(nullable = false)
    private Instant sentAt;

    protected GroupChatMessageEntity() {
    }

    public GroupChatMessageEntity(String groupId, String senderDeviceId, String content, Instant sentAt) {
        this.groupId = groupId;
        this.senderDeviceId = senderDeviceId;
        this.content = content;
        this.sentAt = sentAt;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getSenderDeviceId() {
        return senderDeviceId;
    }

    public String getContent() {
        return content;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
