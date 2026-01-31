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
@RequestMapping("/source-directories")
public class SourceDirectoryController {

    private static class SourceDirectory {
        private final String path;
        private int threadCount;

        public SourceDirectory(String path, int threadCount) {
            this.path = path;
            this.threadCount = threadCount;
        }

        public String getPath() {
            return path;
        }

        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }
    }

    private final List<SourceDirectory> sourceDirectories = new ArrayList<>();

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getSourceDirectories() {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            for (SourceDirectory dir : sourceDirectories) {
                Map<String, Object> dirMap = new HashMap<>();
                dirMap.put("path", dir.getPath());
                dirMap.put("threadCount", dir.getThreadCount());
                result.add(dirMap);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addSourceDirectory(@RequestBody Map<String, Object> request) {
        try {
            String path = (String) request.get("path");
            int threadCount = (Integer) request.getOrDefault("threadCount", 4);

            // 检查路径是否已存在
            for (SourceDirectory dir : sourceDirectories) {
                if (dir.getPath().equals(path)) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("message", "路径已存在");
                    return ResponseEntity.badRequest().body(errorResult);
                }
            }

            sourceDirectories.add(new SourceDirectory(path, threadCount));
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "源目录添加成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> removeSourceDirectory(@PathVariable String id) {
        try {
            boolean removed = sourceDirectories.removeIf(dir -> dir.getPath().equals(id));
            if (!removed) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "源目录不存在");
                return ResponseEntity.badRequest().body(errorResult);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "源目录移除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearSourceDirectories() {
        try {
            sourceDirectories.clear();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "源目录清空成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{id}/threads")
    public ResponseEntity<Map<String, Object>> updateThreadCount(@PathVariable String id, @RequestBody Map<String, Object> request) {
        try {
            Integer threadCount = (Integer) request.get("threadCount");
            if (threadCount == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "线程数不能为空");
                return ResponseEntity.badRequest().body(errorResult);
            }

            for (SourceDirectory dir : sourceDirectories) {
                if (dir.getPath().equals(id)) {
                    dir.setThreadCount(threadCount);
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "线程数更新成功");
                    return ResponseEntity.ok(result);
                }
            }

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "源目录不存在");
            return ResponseEntity.badRequest().body(errorResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
