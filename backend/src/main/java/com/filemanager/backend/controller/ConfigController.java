package com.filemanager.backend.controller;

import com.filemanager.backend.config.ConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigManager configManager;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        try {
            Map<String, Object> config = configManager.getAllConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<Object> getConfigValue(@PathVariable String key) {
        try {
            Object value = configManager.getConfig(key, Object.class);
            return ResponseEntity.ok(value);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> config) {
        try {
            // 验证配置
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                if (!configManager.validateConfig(entry.getKey(), entry.getValue())) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("message", "配置值无效: " + entry.getKey());
                    return ResponseEntity.badRequest().body(errorResult);
                }
            }

            configManager.updateConfig(config);
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
            if (!configManager.validateConfig(key, value)) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "配置值无效");
                return ResponseEntity.badRequest().body(errorResult);
            }

            configManager.setConfig(key, value);
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
            // 从配置缓存中移除
            Map<String, Object> config = configManager.getAllConfig();
            if (config.containsKey(key)) {
                config.remove(key);
                configManager.updateConfig(config);
            }
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
            configManager.resetConfig();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "配置已重置为默认值");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}