package com.filemanager.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/thread-pool")
public class ThreadPoolController {

    private final AtomicInteger previewThreads = new AtomicInteger(4);
    private final AtomicInteger executionThreads = new AtomicInteger(8);

    @GetMapping
    public ResponseEntity<Map<String, Object>> getThreadPoolConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("previewThreads", previewThreads.get());
            config.put("executionThreads", executionThreads.get());
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/preview")
    public ResponseEntity<Map<String, Object>> setPreviewThreads(@RequestBody Map<String, Object> request) {
        try {
            Integer threads = (Integer) request.get("threads");
            if (threads == null || threads <= 0) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "线程数必须大于0");
                return ResponseEntity.badRequest().body(errorResult);
            }

            previewThreads.set(threads);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "预览线程数设置成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/execution")
    public ResponseEntity<Map<String, Object>> setExecutionThreads(@RequestBody Map<String, Object> request) {
        try {
            Integer threads = (Integer) request.get("threads");
            if (threads == null || threads <= 0) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "线程数必须大于0");
                return ResponseEntity.badRequest().body(errorResult);
            }

            executionThreads.set(threads);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "执行线程数设置成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
