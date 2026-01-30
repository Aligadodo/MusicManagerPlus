package com.filemanager.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new com.filemanager.backend.controller.ws.TaskWebSocketHandler(), "/ws/tasks/**")
                .setAllowedOrigins("*")
                .withSockJS();

        registry.addHandler(new com.filemanager.backend.controller.ws.ProgressWebSocketHandler(), "/ws/progress")
                .setAllowedOrigins("*")
                .withSockJS();

        registry.addHandler(new com.filemanager.backend.controller.ws.FileOperationWebSocketHandler(), "/ws/files/**")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
