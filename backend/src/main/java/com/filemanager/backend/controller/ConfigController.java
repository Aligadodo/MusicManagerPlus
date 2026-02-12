package com.filemanager.backend.controller;

import com.filemanager.backend.config.ConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigManager configManager;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        try {
            Map<String, Object> flatConfig = configManager.getAllConfig();
            Map<String, Object> nestedConfig = convertToNestedStructure(flatConfig);
            return ResponseEntity.ok(nestedConfig);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<Object> getConfigValue(@PathVariable String key) {
        try {
            if ("appearance".equals(key)) {
                Map<String, Object> flatConfig = configManager.getAllConfig();
                Map<String, Object> appearanceConfig = new HashMap<>();
                Object themeConfig = flatConfig.get(ConfigManager.KEY_THEME_CONFIG);
                if (themeConfig instanceof Map) {
                    appearanceConfig.putAll((Map<String, Object>) themeConfig);
                }
                return ResponseEntity.ok(appearanceConfig);
            } else if ("globalSettings".equals(key)) {
                Map<String, Object> flatConfig = configManager.getAllConfig();
                Map<String, Object> globalSettings = new HashMap<>();
                globalSettings.put("previewThreads", flatConfig.get(ConfigManager.KEY_PREVIEW_THREADS));
                globalSettings.put("executionThreads", flatConfig.get(ConfigManager.KEY_EXECUTION_THREADS));
                globalSettings.put("threadPoolMode", flatConfig.get(ConfigManager.KEY_THREAD_POOL_MODE));
                globalSettings.put("autoRefresh", flatConfig.get(ConfigManager.KEY_AUTO_REFRESH));
                globalSettings.put("previewLimit", flatConfig.get(ConfigManager.KEY_PREVIEW_LIMIT));
                globalSettings.put("recursionMode", flatConfig.get(ConfigManager.KEY_RECURSION_MODE));
                globalSettings.put("recursionDepth", flatConfig.get(ConfigManager.KEY_RECURSION_DEPTH));
                globalSettings.put("minRecursionDepth", flatConfig.get(ConfigManager.KEY_MIN_RECURSION_DEPTH));
                globalSettings.put("maxRecursionDepth", flatConfig.get(ConfigManager.KEY_MAX_RECURSION_DEPTH));
                globalSettings.put("scanFilterList", flatConfig.get(ConfigManager.KEY_SCAN_FILTER_LIST));
                globalSettings.put("fileTypeTree", flatConfig.get(ConfigManager.KEY_FILE_TYPE_TREE));
                return ResponseEntity.ok(globalSettings);
            } else if ("themePresets".equals(key)) {
                Object themePresets = configManager.getConfig(ConfigManager.KEY_THEME_PRESETS, Object.class);
                return ResponseEntity.ok(themePresets);
            } else {
                Object value = configManager.getConfig(key, Object.class);
                return ResponseEntity.ok(value);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> config) {
        try {
            Map<String, Object> flatConfig = convertToFlatStructure(config);
            
            for (Map.Entry<String, Object> entry : flatConfig.entrySet()) {
                if (!configManager.validateConfig(entry.getKey(), entry.getValue())) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("message", "配置值无效: " + entry.getKey());
                    return ResponseEntity.badRequest().body(errorResult);
                }
            }

            configManager.updateConfig(flatConfig);
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
            if ("appearance".equals(key) && value instanceof Map) {
                Map<String, Object> appearanceConfig = (Map<String, Object>) value;
                configManager.setConfig(ConfigManager.KEY_THEME_CONFIG, appearanceConfig);
            } else if ("globalSettings".equals(key) && value instanceof Map) {
                Map<String, Object> globalSettings = (Map<String, Object>) value;
                if (globalSettings.containsKey("previewThreads")) {
                    configManager.setConfig(ConfigManager.KEY_PREVIEW_THREADS, globalSettings.get("previewThreads"));
                }
                if (globalSettings.containsKey("executionThreads")) {
                    configManager.setConfig(ConfigManager.KEY_EXECUTION_THREADS, globalSettings.get("executionThreads"));
                }
                if (globalSettings.containsKey("threadPoolMode")) {
                    configManager.setConfig(ConfigManager.KEY_THREAD_POOL_MODE, globalSettings.get("threadPoolMode"));
                }
                if (globalSettings.containsKey("autoRefresh")) {
                    configManager.setConfig(ConfigManager.KEY_AUTO_REFRESH, globalSettings.get("autoRefresh"));
                }
                if (globalSettings.containsKey("previewLimit")) {
                    configManager.setConfig(ConfigManager.KEY_PREVIEW_LIMIT, globalSettings.get("previewLimit"));
                }
                if (globalSettings.containsKey("recursionMode")) {
                    configManager.setConfig(ConfigManager.KEY_RECURSION_MODE, globalSettings.get("recursionMode"));
                }
                if (globalSettings.containsKey("recursionDepth")) {
                    configManager.setConfig(ConfigManager.KEY_RECURSION_DEPTH, globalSettings.get("recursionDepth"));
                }
                if (globalSettings.containsKey("minRecursionDepth")) {
                    configManager.setConfig(ConfigManager.KEY_MIN_RECURSION_DEPTH, globalSettings.get("minRecursionDepth"));
                }
                if (globalSettings.containsKey("maxRecursionDepth")) {
                    configManager.setConfig(ConfigManager.KEY_MAX_RECURSION_DEPTH, globalSettings.get("maxRecursionDepth"));
                }
                if (globalSettings.containsKey("scanFilterList")) {
                    configManager.setConfig(ConfigManager.KEY_SCAN_FILTER_LIST, globalSettings.get("scanFilterList"));
                }
                if (globalSettings.containsKey("fileTypeTree")) {
                    configManager.setConfig(ConfigManager.KEY_FILE_TYPE_TREE, globalSettings.get("fileTypeTree"));
                }
            } else if ("themePresets".equals(key) && value instanceof List) {
                configManager.setConfig(ConfigManager.KEY_THEME_PRESETS, value);
            } else {
                if (!configManager.validateConfig(key, value)) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("message", "配置值无效");
                    return ResponseEntity.badRequest().body(errorResult);
                }
                configManager.setConfig(key, value);
            }
            
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

    private Map<String, Object> convertToNestedStructure(Map<String, Object> flatConfig) {
        Map<String, Object> nestedConfig = new HashMap<>();
        
        Map<String, Object> appearance = new HashMap<>();
        Object themeConfig = flatConfig.get(ConfigManager.KEY_THEME_CONFIG);
        if (themeConfig instanceof Map) {
            appearance.putAll((Map<String, Object>) themeConfig);
        }
        nestedConfig.put("appearance", appearance);
        
        Map<String, Object> globalSettings = new HashMap<>();
        globalSettings.put("previewThreads", flatConfig.get(ConfigManager.KEY_PREVIEW_THREADS));
        globalSettings.put("executionThreads", flatConfig.get(ConfigManager.KEY_EXECUTION_THREADS));
        globalSettings.put("threadPoolMode", flatConfig.get(ConfigManager.KEY_THREAD_POOL_MODE));
        globalSettings.put("autoRefresh", flatConfig.get(ConfigManager.KEY_AUTO_REFRESH));
        globalSettings.put("previewLimit", flatConfig.get(ConfigManager.KEY_PREVIEW_LIMIT));
        globalSettings.put("recursionMode", flatConfig.get(ConfigManager.KEY_RECURSION_MODE));
        globalSettings.put("recursionDepth", flatConfig.get(ConfigManager.KEY_RECURSION_DEPTH));
        globalSettings.put("minRecursionDepth", flatConfig.get(ConfigManager.KEY_MIN_RECURSION_DEPTH));
        globalSettings.put("maxRecursionDepth", flatConfig.get(ConfigManager.KEY_MAX_RECURSION_DEPTH));
        globalSettings.put("scanFilterList", flatConfig.get(ConfigManager.KEY_SCAN_FILTER_LIST));
        globalSettings.put("fileTypeTree", flatConfig.get(ConfigManager.KEY_FILE_TYPE_TREE));
        nestedConfig.put("globalSettings", globalSettings);
        
        Map<String, Object> pluginConfigs = new HashMap<>();
        nestedConfig.put("pluginConfigs", pluginConfigs);
        
        Object themePresets = flatConfig.get(ConfigManager.KEY_THEME_PRESETS);
        if (themePresets != null) {
            nestedConfig.put("themePresets", themePresets);
        }
        
        return nestedConfig;
    }

    private Map<String, Object> convertToFlatStructure(Map<String, Object> nestedConfig) {
        Map<String, Object> flatConfig = new HashMap<>();
        
        if (nestedConfig.containsKey("appearance")) {
            Object appearance = nestedConfig.get("appearance");
            if (appearance instanceof Map) {
                flatConfig.put(ConfigManager.KEY_THEME_CONFIG, appearance);
            }
        }
        
        if (nestedConfig.containsKey("globalSettings")) {
            Object globalSettings = nestedConfig.get("globalSettings");
            if (globalSettings instanceof Map) {
                Map<String, Object> settings = (Map<String, Object>) globalSettings;
                if (settings.containsKey("previewThreads")) {
                    flatConfig.put(ConfigManager.KEY_PREVIEW_THREADS, settings.get("previewThreads"));
                }
                if (settings.containsKey("executionThreads")) {
                    flatConfig.put(ConfigManager.KEY_EXECUTION_THREADS, settings.get("executionThreads"));
                }
                if (settings.containsKey("threadPoolMode")) {
                    flatConfig.put(ConfigManager.KEY_THREAD_POOL_MODE, settings.get("threadPoolMode"));
                }
                if (settings.containsKey("autoRefresh")) {
                    flatConfig.put(ConfigManager.KEY_AUTO_REFRESH, settings.get("autoRefresh"));
                }
                if (settings.containsKey("previewLimit")) {
                    flatConfig.put(ConfigManager.KEY_PREVIEW_LIMIT, settings.get("previewLimit"));
                }
                if (settings.containsKey("recursionMode")) {
                    flatConfig.put(ConfigManager.KEY_RECURSION_MODE, settings.get("recursionMode"));
                }
                if (settings.containsKey("recursionDepth")) {
                    flatConfig.put(ConfigManager.KEY_RECURSION_DEPTH, settings.get("recursionDepth"));
                }
                if (settings.containsKey("minRecursionDepth")) {
                    flatConfig.put(ConfigManager.KEY_MIN_RECURSION_DEPTH, settings.get("minRecursionDepth"));
                }
                if (settings.containsKey("maxRecursionDepth")) {
                    flatConfig.put(ConfigManager.KEY_MAX_RECURSION_DEPTH, settings.get("maxRecursionDepth"));
                }
                if (settings.containsKey("scanFilterList")) {
                    flatConfig.put(ConfigManager.KEY_SCAN_FILTER_LIST, settings.get("scanFilterList"));
                }
                if (settings.containsKey("fileTypeTree")) {
                    flatConfig.put(ConfigManager.KEY_FILE_TYPE_TREE, settings.get("fileTypeTree"));
                }
            }
        }
        
        if (nestedConfig.containsKey("pluginConfigs")) {
            Object pluginConfigs = nestedConfig.get("pluginConfigs");
            if (pluginConfigs instanceof Map) {
                flatConfig.putAll((Map<String, Object>) pluginConfigs);
            }
        }
        
        return flatConfig;
    }
}