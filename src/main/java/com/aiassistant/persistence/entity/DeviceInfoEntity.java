package com.aiassistant.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "device_info")
public class DeviceInfoEntity {
    @Id
    @Column(nullable = false, length = 128)
    private String deviceId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(nullable = false, length = 64)
    private String ipAddress;

    @Column(nullable = false)
    private Instant lastSeenAt;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, length = 128)
    private String userId;

    protected DeviceInfoEntity() {
    }

    public DeviceInfoEntity(
            String deviceId,
            String name,
            String type,
            String ipAddress,
            Instant lastSeenAt,
            boolean active,
            String userId
    ) {
        this.deviceId = deviceId;
        this.name = name;
        this.type = type;
        this.ipAddress = ipAddress;
        this.lastSeenAt = lastSeenAt;
        this.active = active;
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public boolean isActive() {
        return active;
    }

    public String getUserId() {
        return userId;
    }
}
