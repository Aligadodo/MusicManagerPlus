package com.filemanager.backend.controller;

import com.filemanager.backend.logging.UnifiedLogger;
import com.filemanager.domain.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private LogService logService;

    @PostMapping("/frontend-error")
    public ResponseEntity<Map<String, Object>> logFrontendError(@RequestBody Map<String, Object> request) {
        try {
            String action = (String) request.getOrDefault("action", "unknown");
            String message = (String) request.getOrDefault("message", "");
            String stackTrace = (String) request.getOrDefault("stackTrace", "");
            String url = (String) request.getOrDefault("url", "");
            String userAgent = (String) request.getOrDefault("userAgent", "");

            StringBuilder logMessage = new StringBuilder();
            logMessage.append("URL: ").append(url);
            if (userAgent != null && !userAgent.isEmpty()) {
                logMessage.append(", UserAgent: ").append(userAgent);
            }
            logMessage.append(", Message: ").append(message);
            if (stackTrace != null && !stackTrace.isEmpty()) {
                logMessage.append("\nStackTrace: ").append(stackTrace);
            }

            UnifiedLogger.frontendError(action, logMessage.toString(), null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Frontend error logged successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            UnifiedLogger.error("LOG_CONTROLLER", "Failed to log frontend error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to log frontend error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<Map<String, Object>>> getLogFiles() {
        try {
            List<Map<String, Object>> logFiles = logService.getLogFiles();
            return ResponseEntity.ok(logFiles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/entries")
    public ResponseEntity<Map<String, Object>> getLogEntries(
            @RequestParam String fileName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            Map<String, Object> result = logService.getLogEntries(fileName, keyword, page, size);
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get log entries: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Map<String, Object>> downloadLogFile(@PathVariable String fileName) {
        try {
            Map<String, Object> result = logService.downloadLogFile(fileName);
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to download log file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearOldLogs(@RequestBody Map<String, Object> request) {
        try {
            int days = ((Number) request.getOrDefault("days", 7)).intValue();
            Map<String, Object> result = logService.clearOldLogs(days);
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to clear old logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}