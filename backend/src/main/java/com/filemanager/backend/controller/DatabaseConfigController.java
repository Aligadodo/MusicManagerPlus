package com.filemanager.backend.controller;

import com.filemanager.backend.entity.ConfigSnapshotPO;
import com.filemanager.backend.entity.ConfigTemplatePO;
import com.filemanager.backend.entity.SystemConfigPO;
import com.filemanager.backend.service.ConfigSnapshotService;
import com.filemanager.backend.service.ConfigTemplateService;
import com.filemanager.backend.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/database/config")
@CrossOrigin(origins = "*")
public class DatabaseConfigController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigController.class);

    @Autowired
    private ConfigSnapshotService configSnapshotService;

    @Autowired
    private ConfigTemplateService configTemplateService;

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("DatabaseConfigController工作正常");
    }

    @GetMapping("/snapshots")
    public ResponseEntity<Map<String, Object>> getSnapshots(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ConfigSnapshotPO> snapshots;
            long total;
            
            if (type != null && !type.isEmpty()) {
                snapshots = configSnapshotService.getSnapshotsByType(type);
                total = snapshots.size();
            } else {
                snapshots = configSnapshotService.getSnapshotsByPage(page, size);
                total = configSnapshotService.getTotalSnapshotCount();
            }
            
            response.put("success", true);
            response.put("data", snapshots);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取配置快照列表失败", e);
            response.put("success", false);
            response.put("message", "获取配置快照列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/snapshots/{snapshotId}")
    public ResponseEntity<Map<String, Object>> getSnapshot(@PathVariable String snapshotId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ConfigSnapshotPO snapshot = configSnapshotService.getSnapshotById(snapshotId);
            if (snapshot == null) {
                response.put("success", false);
                response.put("message", "配置快照不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("data", snapshot);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取配置快照详情失败: " + snapshotId, e);
            response.put("success", false);
            response.put("message", "获取配置快照详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/snapshots")
    public ResponseEntity<Map<String, Object>> createSnapshot(@RequestBody ConfigSnapshotPO snapshot) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ConfigSnapshotPO createdSnapshot = configSnapshotService.createSnapshot(snapshot);
            
            response.put("success", true);
            response.put("data", createdSnapshot);
            response.put("message", "配置快照已创建");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建配置快照失败", e);
            response.put("success", false);
            response.put("message", "创建配置快照失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/snapshots/{snapshotId}")
    public ResponseEntity<Map<String, Object>> updateSnapshot(
            @PathVariable String snapshotId,
            @RequestBody ConfigSnapshotPO snapshot) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            snapshot.setSnapshotId(snapshotId);
            ConfigSnapshotPO updatedSnapshot = configSnapshotService.updateSnapshot(snapshot);
            
            response.put("success", true);
            response.put("data", updatedSnapshot);
            response.put("message", "配置快照已更新");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新配置快照失败: " + snapshotId, e);
            response.put("success", false);
            response.put("message", "更新配置快照失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/snapshots/{snapshotId}")
    public ResponseEntity<Map<String, Object>> deleteSnapshot(@PathVariable String snapshotId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = configSnapshotService.deleteSnapshot(snapshotId);
            
            if (success) {
                response.put("success", true);
                response.put("message", "配置快照已删除");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "删除配置快照失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            logger.error("删除配置快照失败: " + snapshotId, e);
            response.put("success", false);
            response.put("message", "删除配置快照失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ConfigTemplatePO> templates;
            long total;
            
            if (category != null && !category.isEmpty()) {
                templates = configTemplateService.getTemplatesByCategory(category);
                total = templates.size();
            } else {
                templates = configTemplateService.getTemplatesByPage(page, size);
                total = configTemplateService.getTotalTemplateCount();
            }
            
            response.put("success", true);
            response.put("data", templates);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取配置模板列表失败", e);
            response.put("success", false);
            response.put("message", "获取配置模板列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/templates/{templateId}")
    public ResponseEntity<Map<String, Object>> getTemplate(@PathVariable String templateId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ConfigTemplatePO template = configTemplateService.getTemplateById(templateId);
            if (template == null) {
                response.put("success", false);
                response.put("message", "配置模板不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("data", template);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取配置模板详情失败: " + templateId, e);
            response.put("success", false);
            response.put("message", "获取配置模板详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/templates")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody ConfigTemplatePO template) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ConfigTemplatePO createdTemplate = configTemplateService.createTemplate(template);
            
            response.put("success", true);
            response.put("data", createdTemplate);
            response.put("message", "配置模板已创建");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建配置模板失败", e);
            response.put("success", false);
            response.put("message", "创建配置模板失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/templates/{templateId}")
    public ResponseEntity<Map<String, Object>> updateTemplate(
            @PathVariable String templateId,
            @RequestBody ConfigTemplatePO template) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            template.setTemplateId(templateId);
            ConfigTemplatePO updatedTemplate = configTemplateService.updateTemplate(template);
            
            response.put("success", true);
            response.put("data", updatedTemplate);
            response.put("message", "配置模板已更新");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新配置模板失败: " + templateId, e);
            response.put("success", false);
            response.put("message", "更新配置模板失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable String templateId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = configTemplateService.deleteTemplate(templateId);
            
            if (success) {
                response.put("success", true);
                response.put("message", "配置模板已删除");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "删除配置模板失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            logger.error("删除配置模板失败: " + templateId, e);
            response.put("success", false);
            response.put("message", "删除配置模板失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> getSystemConfigs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<SystemConfigPO> configs;
            long total;
            
            if (category != null && !category.isEmpty()) {
                configs = systemConfigService.getConfigsByCategory(category);
                total = configs.size();
            } else {
                configs = systemConfigService.getConfigsByPage(page, size);
                total = systemConfigService.getTotalConfigCount();
            }
            
            response.put("success", true);
            response.put("data", configs);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取系统配置列表失败", e);
            response.put("success", false);
            response.put("message", "获取系统配置列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/system/{configKey}")
    public ResponseEntity<Map<String, Object>> getSystemConfig(@PathVariable String configKey) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            SystemConfigPO config = systemConfigService.getConfigByKey(configKey);
            if (config == null) {
                response.put("success", false);
                response.put("message", "系统配置不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("data", config);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取系统配置详情失败: " + configKey, e);
            response.put("success", false);
            response.put("message", "获取系统配置详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/system")
    public ResponseEntity<Map<String, Object>> createSystemConfig(@RequestBody SystemConfigPO config) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            SystemConfigPO createdConfig = systemConfigService.createConfig(config);
            
            response.put("success", true);
            response.put("data", createdConfig);
            response.put("message", "系统配置已创建");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建系统配置失败", e);
            response.put("success", false);
            response.put("message", "创建系统配置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/system/{configKey}")
    public ResponseEntity<Map<String, Object>> updateSystemConfig(
            @PathVariable String configKey,
            @RequestBody SystemConfigPO config) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            config.setConfigKey(configKey);
            SystemConfigPO updatedConfig = systemConfigService.updateConfig(config);
            
            response.put("success", true);
            response.put("data", updatedConfig);
            response.put("message", "系统配置已更新");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新系统配置失败: " + configKey, e);
            response.put("success", false);
            response.put("message", "更新系统配置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/system/{configKey}")
    public ResponseEntity<Map<String, Object>> deleteSystemConfig(@PathVariable String configKey) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = systemConfigService.deleteConfigByKey(configKey);
            
            if (success) {
                response.put("success", true);
                response.put("message", "系统配置已删除");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "删除系统配置失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            logger.error("删除系统配置失败: " + configKey, e);
            response.put("success", false);
            response.put("message", "删除系统配置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
