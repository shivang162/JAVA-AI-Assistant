package com.aiassistant.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupChatWebSocketHandler.class);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void broadcast(String payload) {
        TextMessage message = new TextMessage(payload);
        sessions.removeIf(session -> !session.isOpen());
        Set<WebSocketSession> failedSessions = new HashSet<>();
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(message);
            } catch (IOException exception) {
                LOGGER.warn("Failed to deliver websocket group message to session {}", session.getId(), exception);
                failedSessions.add(session);
            }
        }
        sessions.removeAll(failedSessions);
    }
}
