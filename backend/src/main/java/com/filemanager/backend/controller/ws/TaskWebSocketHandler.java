package com.filemanager.backend.controller.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filemanager.domain.dto.TaskStatusDTO;
import com.filemanager.domain.service.TaskService;
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
public class TaskWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> taskMonitors = new ConcurrentHashMap<>();

    @Autowired
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String taskId = session.getUri().getPath().split("/")[3];
        sessions.put(taskId, session);

        // 启动任务监控
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        taskMonitors.put(taskId, executor);

        executor.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    TaskStatusDTO status = taskService.getTaskStatus(taskId);
                    String message = objectMapper.writeValueAsString(status);
                    session.sendMessage(new TextMessage(message));

                    // 如果任务完成，停止监控
                    String taskStatus = status.getStatus();
                    if (taskStatus.equals("COMPLETED") || taskStatus.equals("FAILED") || taskStatus.equals("CANCELLED")) {
                        executor.shutdown();
                        taskMonitors.remove(taskId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String taskId = session.getUri().getPath().split("/")[3];
        sessions.remove(taskId);

        // 停止任务监控
        ScheduledExecutorService executor = taskMonitors.remove(taskId);
        if (executor != null) {
            executor.shutdown();
        }
    }
}
