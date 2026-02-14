package com.filemanager.backend.service;

import com.filemanager.backend.model.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket消息服务
 * 用于发送任务进度实时更新
 */
@Service
public class WebSocketMessageService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageService.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketMessageService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 发送任务进度更新
     */
    public void sendTaskProgress(String taskId, String currentStage, int progress, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("currentStage", currentStage);
        data.put("progress", progress);
        data.put("message", message);

        sendMessage(taskId, "TASK_PROGRESS", data);
        logger.debug("[WebSocket] 发送任务进度更新: {} - {}%", taskId, progress);
    }

    /**
     * 发送阶段状态更新
     */
    public void sendStageStatus(String taskId, String stage, String status, long startTime, 
                                int processedCount, int totalCount) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("stage", stage);
        data.put("status", status);
        data.put("startTime", startTime);
        data.put("processedCount", processedCount);
        data.put("totalCount", totalCount);

        sendMessage(taskId, "STAGE_STATUS", data);
        logger.debug("[WebSocket] 发送阶段状态更新: {} - {} - {}/{}", taskId, stage, processedCount, totalCount);
    }

    /**
     * 发送任务完成消息
     */
    public void sendTaskCompleted(String taskId, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("status", "COMPLETED");
        data.put("message", message);

        sendMessage(taskId, "TASK_COMPLETED", data);
        logger.info("[WebSocket] 发送任务完成消息: {}", taskId);
    }

    /**
     * 发送任务失败消息
     */
    public void sendTaskFailed(String taskId, String message, String errorCode, String errorMessage) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("status", "FAILED");
        data.put("message", message);

        Map<String, Object> error = new HashMap<>();
        error.put("code", errorCode);
        error.put("message", errorMessage);
        data.put("error", error);

        sendMessage(taskId, "TASK_FAILED", data);
        logger.error("[WebSocket] 发送任务失败消息: {} - {}", taskId, errorMessage);
    }

    /**
     * 发送任务取消消息
     */
    public void sendTaskCancelled(String taskId, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("status", "CANCELLED");
        data.put("message", message);

        sendMessage(taskId, "TASK_CANCELLED", data);
        logger.info("[WebSocket] 发送任务取消消息: {}", taskId);
    }

    /**
     * 发送任务信息更新
     */
    public void sendTaskInfoUpdate(String taskId, TaskInfo taskInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("status", taskInfo.getStatus().name());
        data.put("currentStage", taskInfo.getCurrentStage());
        data.put("progress", taskInfo.getOverallProgress());
        data.put("message", taskInfo.getMessage());

        sendMessage(taskId, "TASK_INFO_UPDATE", data);
        logger.debug("[WebSocket] 发送任务信息更新: {}", taskId);
    }

    /**
     * 发送自定义消息
     */
    public void sendCustomMessage(String taskId, String messageType, Map<String, Object> data) {
        sendMessage(taskId, messageType, data);
        logger.debug("[WebSocket] 发送自定义消息: {} - {}", taskId, messageType);
    }

    /**
     * 发送消息到指定主题
     */
    private void sendMessage(String taskId, String messageType, Map<String, Object> data) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", messageType);
        message.put("data", data);
        message.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/tasks/" + taskId;
        messagingTemplate.convertAndSend(destination, message);
    }
}
