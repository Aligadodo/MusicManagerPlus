package com.filemanager.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/logs")
public class LogController {

    private static class LogEntry {
        private final long timestamp;
        private final String level;
        private final String message;
        private final String source;

        public LogEntry(String level, String message, String source) {
            this.timestamp = System.currentTimeMillis();
            this.level = level;
            this.message = message;
            this.source = source;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public String getSource() {
            return source;
        }
    }

    // 简单的内存日志存储
    private final List<LogEntry> logs = new ArrayList<>();
    private final int MAX_LOGS = 1000;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {
        try {
            List<Map<String, Object>> filteredLogs = new ArrayList<>();
            
            for (LogEntry entry : logs) {
                if (level != null && !entry.getLevel().equals(level)) {
                    continue;
                }
                if (source != null && !entry.getSource().equals(source)) {
                    continue;
                }
                
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("timestamp", entry.getTimestamp());
                logMap.put("level", entry.getLevel());
                logMap.put("message", entry.getMessage());
                logMap.put("source", entry.getSource());
                filteredLogs.add(logMap);
            }
            
            // 简单的分页处理
            int start = (page - 1) * size;
            int end = Math.min(start + size, filteredLogs.size());
            if (start < filteredLogs.size()) {
                filteredLogs = filteredLogs.subList(start, end);
            }
            
            return ResponseEntity.ok(filteredLogs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addLog(@RequestBody Map<String, Object> logEntry) {
        try {
            String level = (String) logEntry.getOrDefault("level", "INFO");
            String message = (String) logEntry.get("message");
            String source = (String) logEntry.getOrDefault("source", "api");
            
            if (message == null) {
                return ResponseEntity.badRequest().body(null);
            }
            
            logs.add(new LogEntry(level, message, source));
            
            // 保持日志数量在限制内
            if (logs.size() > MAX_LOGS) {
                logs.remove(0);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "日志添加成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearLogs() {
        try {
            logs.clear();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "日志已清空");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getLogCount() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("count", logs.size());
            result.put("maxCount", MAX_LOGS);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}