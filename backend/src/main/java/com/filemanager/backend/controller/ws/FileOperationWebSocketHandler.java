package com.filemanager.backend.controller.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class FileOperationWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> operationMonitors = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String operationId = session.getUri().getPath().split("/")[3];
        sessions.put(operationId, session);

        // 启动文件操作监控
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        operationMonitors.put(operationId, executor);

        executor.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    Map<String, Object> operationData = new java.util.HashMap<>();
                    operationData.put("operationId", operationId);
                    operationData.put("operation", "scan");
                    operationData.put("progress", 0.6);
                    operationData.put("currentFile", "file.txt");
                    operationData.put("totalFiles", 10);
                    operationData.put("completedFiles", 6);
                    operationData.put("timestamp", System.currentTimeMillis());

                    String message = objectMapper.writeValueAsString(operationData);
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String operationId = session.getUri().getPath().split("/")[3];
        sessions.remove(operationId);

        // 停止操作监控
        ScheduledExecutorService executor = operationMonitors.remove(operationId);
        if (executor != null) {
            executor.shutdown();
        }
    }
}
