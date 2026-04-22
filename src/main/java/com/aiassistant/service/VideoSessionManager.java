package com.aiassistant.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VideoSessionManager {
    private static final String CONNECTION_MODE = "server-relay";
    private final Map<String, VideoSessionState> sessions = new ConcurrentHashMap<>();

    public synchronized VideoSession startSession(String requestedSessionId, String hostDeviceId, List<String> initialParticipantDeviceIds) {
        String normalizedHost = normalize(hostDeviceId, "hostDeviceId");
        String sessionId = requestedSessionId == null || requestedSessionId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedSessionId.trim();

        VideoSessionState state = new VideoSessionState(sessionId, normalizedHost);
        state.participantDeviceIds.add(normalizedHost);
        if (initialParticipantDeviceIds != null) {
            initialParticipantDeviceIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .forEach(state.participantDeviceIds::add);
        }
        VideoSessionState existing = sessions.putIfAbsent(sessionId, state);
        if (existing != null && existing.active) {
            throw new IllegalArgumentException("Video session already exists.");
        }
        if (existing != null && !existing.active) {
            sessions.put(sessionId, state);
        }
        return state.toView();
    }

    public synchronized VideoSession joinSession(String sessionId, String deviceId) {
        VideoSessionState state = requireActiveSession(sessionId);
        state.participantDeviceIds.add(normalize(deviceId, "deviceId"));
        return state.toView();
    }

    public synchronized VideoSession endSession(String sessionId) {
        VideoSessionState state = sessions.get(normalize(sessionId, "sessionId"));
        if (state == null) {
            throw new NoSuchElementException("Video session was not found.");
        }
        state.active = false;
        return state.toView();
    }

    private VideoSessionState requireActiveSession(String sessionId) {
        VideoSessionState state = sessions.get(normalize(sessionId, "sessionId"));
        if (state == null || !state.active) {
            throw new NoSuchElementException("Active video session was not found.");
        }
        return state;
    }

    private String normalize(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    public record VideoSession(
            String sessionId,
            String hostDeviceId,
            List<String> participantDeviceIds,
            String connectionMode,
            Instant startedAt,
            boolean active
    ) {
    }

    private static final class VideoSessionState {
        private final String sessionId;
        private final String hostDeviceId;
        private final Instant startedAt;
        private final Set<String> participantDeviceIds = new LinkedHashSet<>();
        private volatile boolean active = true;

        private VideoSessionState(String sessionId, String hostDeviceId) {
            this.sessionId = sessionId;
            this.hostDeviceId = hostDeviceId;
            this.startedAt = Instant.now();
        }

        private VideoSession toView() {
            return new VideoSession(
                    sessionId,
                    hostDeviceId,
                    List.copyOf(participantDeviceIds),
                    CONNECTION_MODE,
                    startedAt,
                    active
            );
        }
    }
}
