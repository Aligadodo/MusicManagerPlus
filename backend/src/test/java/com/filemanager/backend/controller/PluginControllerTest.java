package com.filemanager.backend.controller;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginInfoDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.PluginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PluginControllerTest {

    @Mock
    private PluginService pluginService;

    @InjectMocks
    private PluginController pluginController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetPlugins_Success() {
        List<PluginInfoDTO> mockPlugins = new ArrayList<>();
        PluginInfoDTO plugin = new PluginInfoDTO();
        plugin.setId("test-plugin");
        plugin.setName("Test Plugin");
        plugin.setDescription("A test plugin");
        plugin.setVersion("1.0.0");
        plugin.setEnabled(true);
        mockPlugins.add(plugin);

        when(pluginService.getAvailablePlugins()).thenReturn(mockPlugins);

        ResponseEntity<List<PluginInfoDTO>> response = pluginController.getPlugins();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("test-plugin", response.getBody().get(0).getId());
        assertEquals("Test Plugin", response.getBody().get(0).getName());
    }

    @Test
    public void testGetPlugins_Empty() {
        when(pluginService.getAvailablePlugins()).thenReturn(new ArrayList<>());

        ResponseEntity<List<PluginInfoDTO>> response = pluginController.getPlugins();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testGetPluginInfo_Success() {
        PluginInfoDTO mockPlugin = new PluginInfoDTO();
        mockPlugin.setId("test-plugin");
        mockPlugin.setName("Test Plugin");
        mockPlugin.setDescription("A test plugin");
        mockPlugin.setVersion("1.0.0");
        mockPlugin.setEnabled(true);

        when(pluginService.getPluginInfo("test-plugin")).thenReturn(mockPlugin);

        ResponseEntity<PluginInfoDTO> response = pluginController.getPluginInfo("test-plugin");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test-plugin", response.getBody().getId());
        assertEquals("Test Plugin", response.getBody().getName());
    }

    @Test
    public void testGetPluginInfo_NotFound() {
        when(pluginService.getPluginInfo("nonexistent")).thenReturn(null);

        ResponseEntity<PluginInfoDTO> response = pluginController.getPluginInfo("nonexistent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testGetPluginConfig_Success() {
        PluginConfigDTO mockConfig = new PluginConfigDTO();
        mockConfig.setConfigMap(new HashMap<>());

        when(pluginService.getPluginConfig("test-plugin")).thenReturn(mockConfig);

        ResponseEntity<PluginConfigDTO> response = pluginController.getPluginConfig("test-plugin");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testUpdatePluginConfig_Success() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setConfigMap(new HashMap<>());

        when(pluginService.updatePluginConfig(eq("test-plugin"), any(PluginConfigDTO.class)))
            .thenReturn(true);

        ResponseEntity<PluginConfigDTO> response = pluginController.updatePluginConfig("test-plugin", config);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(pluginService, times(1)).updatePluginConfig(eq("test-plugin"), any(PluginConfigDTO.class));
    }

    @Test
    public void testPreviewPlugin_Success() {
        Map<String, Object> request = new HashMap<>();
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/test/file1.txt");
        filePaths.add("/test/file2.txt");
        request.put("filePaths", filePaths);

        PluginConfigDTO config = new PluginConfigDTO();
        config.setConfigMap(new HashMap<>());
        request.put("config", config);

        List<ChangeRecord> mockChanges = new ArrayList<>();
        ChangeRecord change = new ChangeRecord();
        change.setId("1");
        change.setOriginalName("file1.txt");
        change.setNewName("renamed1.txt");
        change.setStatus("PENDING");
        mockChanges.add(change);

        when(pluginService.previewPlugin(eq("test-plugin"), eq(filePaths), any(PluginConfigDTO.class)))
            .thenReturn(mockChanges);

        ResponseEntity<List<ChangeRecord>> response = pluginController.previewPlugin("test-plugin", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("file1.txt", response.getBody().get(0).getOriginalName());
    }

    @Test
    public void testExecutePlugin_Success() {
        Map<String, Object> request = new HashMap<>();
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/test/file1.txt");
        request.put("filePaths", filePaths);

        PluginConfigDTO config = new PluginConfigDTO();
        config.setConfigMap(new HashMap<>());
        request.put("config", config);

        List<ChangeRecord> mockChanges = new ArrayList<>();
        ChangeRecord change = new ChangeRecord();
        change.setId("1");
        change.setOriginalName("file1.txt");
        change.setNewName("processed1.txt");
        change.setStatus("SUCCESS");
        mockChanges.add(change);

        when(pluginService.executePlugin(eq("test-plugin"), eq(filePaths), any(PluginConfigDTO.class)))
            .thenReturn(mockChanges);

        ResponseEntity<List<ChangeRecord>> response = pluginController.executePlugin("test-plugin", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("SUCCESS", response.getBody().get(0).getStatus());
    }

    @Test
    public void testReloadPlugins_Success() {
        when(pluginService.reloadPlugins()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = pluginController.reloadPlugins();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("插件重载成功", response.getBody().get("message"));

        verify(pluginService, times(1)).reloadPlugins();
    }

    @Test
    public void testReloadPlugins_Failure() {
        when(pluginService.reloadPlugins()).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = pluginController.reloadPlugins();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
    }
}
