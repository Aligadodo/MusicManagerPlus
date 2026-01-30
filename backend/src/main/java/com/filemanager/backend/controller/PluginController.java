package com.filemanager.backend.controller;

import com.filemanager.domain.dto.PluginInfoDTO;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.PluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plugins")
public class PluginController {

    @Autowired
    private PluginService pluginService;

    @GetMapping
    public ResponseEntity<List<PluginInfoDTO>> getPlugins() {
        try {
            List<PluginInfoDTO> plugins = pluginService.getAvailablePlugins();
            return ResponseEntity.ok(plugins);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PluginInfoDTO> getPluginInfo(@PathVariable String id) {
        try {
            PluginInfoDTO plugin = pluginService.getPluginInfo(id);
            return ResponseEntity.ok(plugin);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{id}/config")
    public ResponseEntity<PluginConfigDTO> getPluginConfig(@PathVariable String id) {
        try {
            PluginConfigDTO config = pluginService.getPluginConfig(id);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/{id}/config")
    public ResponseEntity<Map<String, Object>> updatePluginConfig(@PathVariable String id, @RequestBody PluginConfigDTO config) {
        try {
            boolean success = pluginService.updatePluginConfig(id, config);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", "配置更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<List<ChangeRecord>> executePlugin(@PathVariable String id, @RequestBody Map<String, Object> request) {
        try {
            List<String> files = (List<String>) request.get("files");
            PluginConfigDTO config = (PluginConfigDTO) request.get("config");

            List<ChangeRecord> changes = pluginService.executePlugin(id, files, config);
            return ResponseEntity.ok(changes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadPlugins() {
        try {
            boolean success = pluginService.reloadPlugins();
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", "插件重载成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
