package com.filemanager.backend.controller;

import com.filemanager.backend.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ConfigControllerTest {

    @Mock
    private ConfigManager configManager;

    @InjectMocks
    private ConfigController configController;

    private final Map<String, Object> configCache = new HashMap<>();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        configCache.put(ConfigManager.KEY_PREVIEW_THREADS, 4);
        configCache.put(ConfigManager.KEY_EXECUTION_THREADS, 8);
        configCache.put(ConfigManager.KEY_THREAD_POOL_MODE, "GLOBAL");
        configCache.put(ConfigManager.KEY_AUTO_REFRESH, true);
        configCache.put(ConfigManager.KEY_PREVIEW_LIMIT, 100);
        configCache.put(ConfigManager.KEY_RECURSION_MODE, "AUTO");
        configCache.put(ConfigManager.KEY_RECURSION_DEPTH, 10);
        configCache.put(ConfigManager.KEY_MIN_RECURSION_DEPTH, 1);
        configCache.put(ConfigManager.KEY_MAX_RECURSION_DEPTH, 20);
        configCache.put(ConfigManager.KEY_SCAN_FILTER_LIST, new HashMap<>());
        configCache.put(ConfigManager.KEY_FILE_TYPE_TREE, new HashMap<>());
        configCache.put(ConfigManager.KEY_THEME_CONFIG, new HashMap<>());
        configCache.put(ConfigManager.KEY_THEME_PRESETS, new HashMap<>());
        
        when(configManager.getAllConfig()).thenReturn(new HashMap<>(configCache));
        when(configManager.getConfig(anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (configCache.containsKey(key)) {
                return configCache.get(key);
            } else {
                throw new RuntimeException("Config not found: " + key);
            }
        });
        when(configManager.validateConfig(anyString(), any())).thenReturn(true);
        doAnswer(invocation -> {
            Map<String, Object> updates = invocation.getArgument(0);
            configCache.putAll(updates);
            return null;
        }).when(configManager).updateConfig(anyMap());
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            configCache.put(key, value);
            return null;
        }).when(configManager).setConfig(anyString(), any());
        doAnswer(invocation -> {
            configCache.clear();
            return null;
        }).when(configManager).resetConfig();
    }

    @Test
    public void testGetConfig_Empty() {
        ResponseEntity<Map<String, Object>> response = configController.getConfig();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("appearance"));
        assertTrue(response.getBody().containsKey("globalSettings"));
        assertTrue(response.getBody().containsKey("pluginConfigs"));
    }

    @Test
    public void testGetConfigValue_Success() {
        ResponseEntity<Object> response = configController.getConfigValue("appearance");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
    }

    @Test
    public void testGetConfigValue_NotFound() {
        ResponseEntity<Object> response = configController.getConfigValue("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testUpdateConfig_Success() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> globalSettings = new HashMap<>();
        globalSettings.put("previewThreads", 5);
        globalSettings.put("executionThreads", 10);
        config.put("globalSettings", globalSettings);

        ResponseEntity<Map<String, Object>> response = configController.updateConfig(config);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("配置更新成功", response.getBody().get("message"));
    }

    @Test
    public void testUpdateConfigValue_Success() {
        Map<String, Object> value = new HashMap<>();
        value.put("previewThreads", 5);
        value.put("executionThreads", 10);

        ResponseEntity<Map<String, Object>> response = configController.updateConfigValue("globalSettings", value);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("配置项更新成功", response.getBody().get("message"));
    }

    @Test
    public void testDeleteConfigValue_Success() {
        configCache.put("testKey", "testValue");
        when(configManager.getAllConfig()).thenReturn(new HashMap<>(configCache));

        ResponseEntity<Map<String, Object>> response = configController.deleteConfigValue("testKey");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("配置项删除成功", response.getBody().get("message"));
    }

    @Test
    public void testClearConfig_Success() {
        ResponseEntity<Map<String, Object>> response = configController.clearConfig();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("配置已重置为默认值", response.getBody().get("message"));
    }
}
