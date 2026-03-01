package com.filemanager.backend.service;

import com.filemanager.backend.entity.TaskExecutionLog;
import com.filemanager.backend.mapper.TaskExecutionLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskExecutionLogService {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionLogService.class);

    @Autowired
    private TaskExecutionLogMapper logMapper;

    public void addLog(String taskId, String logLevel, String logType, String message) {
        addLog(taskId, logLevel, logType, message, null);
    }

    public void addLog(String taskId, String logLevel, String logType, String message, String details) {
        TaskExecutionLog log = new TaskExecutionLog(taskId, logLevel, logType, message);
        log.setDetails(details);
        try {
            logMapper.insert(log);
        } catch (Exception e) {
            logger.error("Failed to insert task execution log: taskId={}, message={}", taskId, message, e);
        }
    }

    public void info(String taskId, String logType, String message) {
        addLog(taskId, "INFO", logType, message);
        logger.info("[Task-{}] [{}] {}", taskId, logType, message);
    }

    public void warn(String taskId, String logType, String message) {
        addLog(taskId, "WARN", logType, message);
        logger.warn("[Task-{}] [{}] {}", taskId, logType, message);
    }

    public void error(String taskId, String logType, String message) {
        addLog(taskId, "ERROR", logType, message);
        logger.error("[Task-{}] [{}] {}", taskId, logType, message);
    }

    public void error(String taskId, String logType, String message, Throwable t) {
        addLog(taskId, "ERROR", logType, message, t.getMessage());
        logger.error("[Task-{}] [{}] {}", taskId, logType, message, t);
    }

    public void debug(String taskId, String logType, String message) {
        addLog(taskId, "DEBUG", logType, message);
        logger.debug("[Task-{}] [{}] {}", taskId, logType, message);
    }

    public List<TaskExecutionLog> getLogs(String taskId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return logMapper.selectByTaskId(taskId, pageSize, offset);
    }

    public List<TaskExecutionLog> getLogsByLevel(String taskId, String logLevel, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return logMapper.selectByTaskIdAndLevel(taskId, logLevel, pageSize, offset);
    }

    public List<TaskExecutionLog> getLogsByType(String taskId, String logType, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return logMapper.selectByTaskIdAndType(taskId, logType, pageSize, offset);
    }

    public List<TaskExecutionLog> getNewLogs(String taskId, Long since) {
        return logMapper.selectNewLogs(taskId, since);
    }

    public int getLogCount(String taskId) {
        return logMapper.countByTaskId(taskId);
    }

    public void deleteLogs(String taskId) {
        logMapper.deleteByTaskId(taskId);
    }
}
