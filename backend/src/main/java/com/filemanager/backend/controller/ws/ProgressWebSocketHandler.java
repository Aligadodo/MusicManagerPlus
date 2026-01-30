package com.filemanager.backend.controller.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ProgressWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        executor.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> progressData = new java.util.HashMap<>();
                progressData.put("totalTasks", 10);
                progressData.put("completedTasks", 3);
                progressData.put("runningTasks", 2);
                progressData.put("failedTasks", 0);
                progressData.put("totalProgress", 0.3);
                progressData.put("timestamp", System.currentTimeMillis());

                String message = objectMapper.writeValueAsString(progressData);
                
                for (WebSocketSession session : sessions.values()) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
    }
}
