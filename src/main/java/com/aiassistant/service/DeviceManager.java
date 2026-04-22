package com.aiassistant.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceManager {
    private final Map<String, RegisteredDevice> devices = new ConcurrentHashMap<>();

    public RegisteredDevice register(String deviceId, String name, String type, String ipAddress) {
        String normalizedDeviceId = normalize(deviceId, "deviceId");
        String normalizedName = normalize(name, "name");
        String normalizedType = normalize(type, "type");
        String normalizedIp = normalize(ipAddress, "ipAddress");
        RegisteredDevice device = new RegisteredDevice(
                normalizedDeviceId,
                normalizedName,
                normalizedType,
                normalizedIp,
                Instant.now(),
                true
        );
        devices.put(normalizedDeviceId, device);
        return device;
    }

    public List<RegisteredDevice> listActiveDevices() {
        return devices.values().stream()
                .filter(RegisteredDevice::active)
                .sorted(Comparator.comparing(RegisteredDevice::name).thenComparing(RegisteredDevice::deviceId))
                .toList();
    }

    public boolean isRegistered(String deviceId) {
        return deviceId != null && devices.containsKey(deviceId.trim());
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
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
