package com.aiassistant.service;

import com.aiassistant.persistence.entity.DeviceInfoEntity;
import com.aiassistant.persistence.entity.UserProfileEntity;
import com.aiassistant.persistence.repository.DeviceInfoRepository;
import com.aiassistant.persistence.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DeviceManager {
    private final DeviceInfoRepository deviceInfoRepository;
    private final UserProfileRepository userProfileRepository;

    public DeviceManager(DeviceInfoRepository deviceInfoRepository, UserProfileRepository userProfileRepository) {
        this.deviceInfoRepository = deviceInfoRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public RegisteredDevice register(String deviceId, String name, String type, String ipAddress) {
        String normalizedDeviceId = normalize(deviceId, "deviceId");
        String normalizedName = normalize(name, "name");
        String normalizedType = normalize(type, "type");
        String normalizedIp = normalize(ipAddress, "ipAddress");
        Instant now = Instant.now();
        userProfileRepository.findById(normalizedDeviceId)
                .orElseGet(() -> userProfileRepository.save(new UserProfileEntity(normalizedDeviceId, normalizedDeviceId, now)));
        DeviceInfoEntity saved = deviceInfoRepository.save(new DeviceInfoEntity(
                normalizedDeviceId,
                normalizedName,
                normalizedType,
                normalizedIp,
                now,
                true,
                normalizedDeviceId
        ));
        return toRegisteredDevice(saved);
    }

    public List<RegisteredDevice> listActiveDevices() {
        return deviceInfoRepository.findByActiveTrueOrderByNameAscDeviceIdAsc().stream()
                .map(this::toRegisteredDevice)
                .toList();
    }

    public boolean isRegistered(String deviceId) {
        return deviceId != null && deviceInfoRepository.existsById(deviceId.trim());
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private RegisteredDevice toRegisteredDevice(DeviceInfoEntity entity) {
        return new RegisteredDevice(
                entity.getDeviceId(),
                entity.getName(),
                entity.getType(),
                entity.getIpAddress(),
                entity.getLastSeenAt(),
                entity.isActive()
        );
    }

    public record RegisteredDevice(
            String deviceId,
            String name,
            String type,
            String ipAddress,
            Instant lastSeenAt,
            boolean active
    ) {
    }
}
