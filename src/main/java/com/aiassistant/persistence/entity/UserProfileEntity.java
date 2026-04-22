package com.aiassistant.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {
    @Id
    @Column(nullable = false, length = 128)
    private String userId;

    @Column(nullable = false, length = 128)
    private String displayName;

    @Column(nullable = false)
    private Instant createdAt;

    protected UserProfileEntity() {
    }

    public UserProfileEntity(String userId, String displayName, Instant createdAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
