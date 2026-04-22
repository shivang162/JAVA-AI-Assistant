package com.aiassistant.config;

import com.aiassistant.websocket.GroupChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final GroupChatWebSocketHandler groupChatWebSocketHandler;

    public WebSocketConfig(GroupChatWebSocketHandler groupChatWebSocketHandler) {
        this.groupChatWebSocketHandler = groupChatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(groupChatWebSocketHandler, "/ws/group-chat")
                .setAllowedOriginPatterns("*");
    }
}
