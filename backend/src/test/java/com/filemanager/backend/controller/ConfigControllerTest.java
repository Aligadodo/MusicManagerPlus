package com.filemanager.backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigControllerTest {

    @InjectMocks
    private ConfigController configController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetConfig_Empty() {
        ResponseEntity<Map<String, Object>> response = configController.getConfig();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testGetConfigValue_Success() {
        Map<String, Object> config = new HashMap<>();
        config.put("testKey", "testValue");
        configController.updateConfig(config);

        ResponseEntity<Object> response = configController.getConfigValue("testKey");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testValue", response.getBody());
    }

    @Test
    public void testGetConfigValue_NotFound() {
        ResponseEntity<Object> response = configController.getConfigValue("nonexistent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testUpdateConfig_Success() {
        Map<String, Object> config = new HashMap<>();
        config.put("key1", "value1");
        config.put("key2", 123);
        config.put("key3", true);

        ResponseEntity<Map<String, Object>> response = configController.updateConfig(config);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("配置更新成功", response.getBody().get("message"));

        ResponseEntity<Map<String, Object>> getResponse = configController.getConfig();
        assertEquals("value1", getResponse.getBody().get("key1"));
        assertEquals(123, getResponse.getBody().get("key2"));
        assertEquals(true, getResponse.getBody().get("key3"));
    }

    @Test
    public void testUpdateConfigValue_Success() {
        ResponseEntity<Map<String, Object>> response = configController.updateConfigValue("testKey", "testValue");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("配置项更新成功", response.getBody().get("message"));

        ResponseEntity<Object> getResponse = configController.getConfigValue("testKey");
        assertEquals("testValue", getResponse.getBody());
    }

    @Test
    public void testDeleteConfigValue_Success() {
        Map<String, Object> config = new HashMap<>();
        config.put("key1", "value1");
        config.put("key2", "value2");
        configController.updateConfig(config);

        ResponseEntity<Map<String, Object>> response = configController.deleteConfigValue("key1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("配置项删除成功", response.getBody().get("message"));

        ResponseEntity<Map<String, Object>> getResponse = configController.getConfig();
        assertFalse(getResponse.getBody().containsKey("key1"));
        assertTrue(getResponse.getBody().containsKey("key2"));
    }

    @Test
    public void testClearConfig_Success() {
        Map<String, Object> config = new HashMap<>();
        config.put("key1", "value1");
        config.put("key2", "value2");
        configController.updateConfig(config);

        ResponseEntity<Map<String, Object>> response = configController.clearConfig();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("配置已清空", response.getBody().get("message"));

        ResponseEntity<Map<String, Object>> getResponse = configController.getConfig();
        assertTrue(getResponse.getBody().isEmpty());
    }
}
