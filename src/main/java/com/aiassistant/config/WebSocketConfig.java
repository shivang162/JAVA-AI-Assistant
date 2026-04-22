package com.aiassistant.config;

import com.aiassistant.websocket.GroupChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final GroupChatWebSocketHandler groupChatWebSocketHandler;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(
            GroupChatWebSocketHandler groupChatWebSocketHandler,
            @Value("${app.websocket.allowed-origin-patterns:http://localhost:8080}") String allowedOriginPatterns
    ) {
        this.groupChatWebSocketHandler = groupChatWebSocketHandler;
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(groupChatWebSocketHandler, "/ws/group-chat")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}
