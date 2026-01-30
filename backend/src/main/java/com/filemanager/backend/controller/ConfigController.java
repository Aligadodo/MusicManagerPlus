package com.filemanager.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    // 简单的内存配置存储
    private final Map<String, Object> configStore = new HashMap<>();

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        try {
            return ResponseEntity.ok(configStore);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<Object> getConfigValue(@PathVariable String key) {
        try {
            Object value = configStore.get(key);
            return ResponseEntity.ok(value);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> config) {
        try {
            configStore.putAll(config);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "配置更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{key}")
    public ResponseEntity<Map<String, Object>> updateConfigValue(@PathVariable String key, @RequestBody Object value) {
        try {
            configStore.put(key, value);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "配置项更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, Object>> deleteConfigValue(@PathVariable String key) {
        try {
            configStore.remove(key);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "配置项删除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearConfig() {
        try {
            configStore.clear();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "配置已清空");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}