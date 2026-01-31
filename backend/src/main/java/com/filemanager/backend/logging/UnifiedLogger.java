package com.filemanager.backend.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UnifiedLogger {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedLogger.class);

    private static final String FRONTEND = "FRONTEND";
    private static final String BACKEND = "BACKEND";
    private static final String PLUGIN = "PLUGIN";
    private static final String API = "API";

    public static void info(String source, String message) {
        logger.info("[{}] {}", source, message);
    }

    public static void warn(String source, String message) {
        logger.warn("[{}] {}", source, message);
    }

    public static void error(String source, String message) {
        logger.error("[{}] {}", source, message);
    }

    public static void error(String source, String message, Throwable throwable) {
        logger.error("[{}] {}", source, message, throwable);
    }

    public static void debug(String source, String message) {
        logger.debug("[{}] {}", source, message);
    }

    public static void apiCall(String method, String endpoint, String message) {
        logger.info("[API] {} {} - {}", method, endpoint, message);
    }

    public static void apiError(String method, String endpoint, String message, Throwable throwable) {
        logger.error("[API] {} {} - ERROR: {}", method, endpoint, message, throwable);
    }

    public static void pluginExecution(String pluginName, String operation, String message) {
        logger.info("[PLUGIN] {} - {} - {}", pluginName, operation, message);
    }

    public static void pluginError(String pluginName, String operation, String message, Throwable throwable) {
        logger.error("[PLUGIN] {} - {} - ERROR: {}", pluginName, operation, message, throwable);
    }

    public static void frontendAction(String action, String message) {
        logger.info("[FRONTEND] {} - {}", action, message);
    }

    public static void frontendError(String action, String message, Throwable throwable) {
        logger.error("[FRONTEND] {} - ERROR: {}", action, message, throwable);
    }

    public static void backendOperation(String operation, String message) {
        logger.info("[BACKEND] {} - {}", operation, message);
    }

    public static void backendError(String operation, String message, Throwable throwable) {
        logger.error("[BACKEND] {} - ERROR: {}", operation, message, throwable);
    }
}
