package com.filemanager.backend.controller;

import com.filemanager.backend.config.ConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/thread-pool")
public class ThreadPoolController {

    @Autowired
    private ConfigManager configManager;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getThreadPoolConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("previewThreads", configManager.getConfig(ConfigManager.KEY_PREVIEW_THREADS, Integer.class));
            config.put("executionThreads", configManager.getConfig(ConfigManager.KEY_EXECUTION_THREADS, Integer.class));
            config.put("threadPoolMode", configManager.getConfig(ConfigManager.KEY_THREAD_POOL_MODE, String.class));
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

            if (!configManager.validateConfig(ConfigManager.KEY_PREVIEW_THREADS, threads)) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "线程数必须在1-16之间");
                return ResponseEntity.badRequest().body(errorResult);
            }

            configManager.setConfig(ConfigManager.KEY_PREVIEW_THREADS, threads);
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

            if (!configManager.validateConfig(ConfigManager.KEY_EXECUTION_THREADS, threads)) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "线程数必须在1-12之间");
                return ResponseEntity.badRequest().body(errorResult);
            }

            configManager.setConfig(ConfigManager.KEY_EXECUTION_THREADS, threads);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "执行线程数设置成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/mode")
    public ResponseEntity<Map<String, Object>> setThreadPoolMode(@RequestBody Map<String, Object> request) {
        try {
            String mode = (String) request.get("mode");
            if (mode == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "线程池模式不能为空");
                return ResponseEntity.badRequest().body(errorResult);
            }

            if (!configManager.validateConfig(ConfigManager.KEY_THREAD_POOL_MODE, mode)) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "线程池模式必须是 GLOBAL 或 ROOT_PATH");
                return ResponseEntity.badRequest().body(errorResult);
            }

            configManager.setConfig(ConfigManager.KEY_THREAD_POOL_MODE, mode);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "线程池模式设置成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
