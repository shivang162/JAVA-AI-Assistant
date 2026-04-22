package com.aiassistant.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "group_chats")
public class GroupChatEntity {
    @Id
    @Column(nullable = false, length = 128)
    private String groupId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 4096)
    private String memberDeviceIds;

    @Column(nullable = false)
    private Instant createdAt;

    protected GroupChatEntity() {
    }

    public GroupChatEntity(String groupId, String name, String memberDeviceIds, Instant createdAt) {
        this.groupId = groupId;
        this.name = name;
        this.memberDeviceIds = memberDeviceIds;
        this.createdAt = createdAt;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public String getMemberDeviceIds() {
        return memberDeviceIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setMemberDeviceIds(String memberDeviceIds) {
        this.memberDeviceIds = memberDeviceIds;
    }
}
