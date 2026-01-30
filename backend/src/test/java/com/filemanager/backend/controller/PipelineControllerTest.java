package com.filemanager.backend.controller;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.PluginService;
import com.filemanager.domain.service.TaskService;
import com.filemanager.domain.dto.TaskRequestDTO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PipelineControllerTest {

    @Mock
    private PluginService pluginService;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private PipelineController pipelineController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetPipeline_Success() {
        ResponseEntity<List<Map<String, Object>>> response = pipelineController.getPipeline();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testUpdatePipeline_Success() {
        List<Map<String, Object>> pipeline = new ArrayList<>();
        Map<String, Object> plugin = new HashMap<>();
        plugin.put("pluginId", "file-cleanup");
        plugin.put("name", "文件清理插件");
        plugin.put("config", new HashMap<>());
        pipeline.add(plugin);

        ResponseEntity<Map<String, Object>> response = pipelineController.updatePipeline(pipeline);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("流水线更新成功", response.getBody().get("message"));
    }

    @Test
    public void testUpdatePipeline_EmptyPipeline() {
        List<Map<String, Object>> pipeline = new ArrayList<>();

        ResponseEntity<Map<String, Object>> response = pipelineController.updatePipeline(pipeline);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("流水线更新成功", response.getBody().get("message"));
    }

    @Test
    public void testAnalyzePipeline_Success() {
        Map<String, Object> request = new HashMap<>();
        List<String> sourceDirectories = new ArrayList<>();
        sourceDirectories.add("/test/path");
        request.put("sourceDirectories", sourceDirectories);
        
        List<Map<String, Object>> pipeline = new ArrayList<>();
        Map<String, Object> plugin = new HashMap<>();
        plugin.put("pluginId", "file-cleanup");
        plugin.put("config", new HashMap<>());
        pipeline.add(plugin);
        request.put("pipeline", pipeline);

        List<ChangeRecord> mockChanges = new ArrayList<>();
        ChangeRecord change = new ChangeRecord();
        change.setId("1");
        change.setOriginalName("test.txt");
        change.setNewName("renamed.txt");
        change.setStatus(ChangeRecord.ExecStatus.PENDING);
        mockChanges.add(change);

        when(pluginService.previewPlugin(anyString(), anyList(), any(PluginConfigDTO.class)))
            .thenReturn(mockChanges);

        ResponseEntity<List<ChangeRecord>> response = pipelineController.analyzePipeline(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("test.txt", response.getBody().get(0).getOriginalName());
        assertEquals("renamed.txt", response.getBody().get(0).getNewName());
    }

    @Test
    public void testAnalyzePipeline_MultiplePlugins() {
        Map<String, Object> request = new HashMap<>();
        List<String> sourceDirectories = new ArrayList<>();
        sourceDirectories.add("/test/path");
        request.put("sourceDirectories", sourceDirectories);
        
        List<Map<String, Object>> pipeline = new ArrayList<>();
        Map<String, Object> plugin1 = new HashMap<>();
        plugin1.put("pluginId", "file-cleanup");
        plugin1.put("config", new HashMap<>());
        pipeline.add(plugin1);
        
        Map<String, Object> plugin2 = new HashMap<>();
        plugin2.put("pluginId", "file-rename");
        plugin2.put("config", new HashMap<>());
        pipeline.add(plugin2);
        request.put("pipeline", pipeline);

        List<ChangeRecord> mockChanges1 = new ArrayList<>();
        ChangeRecord change1 = new ChangeRecord();
        change1.setId("1");
        change1.setOriginalName("test.txt");
        change1.setNewName("renamed.txt");
        change1.setStatus(ChangeRecord.ExecStatus.PENDING);
        mockChanges1.add(change1);

        List<ChangeRecord> mockChanges2 = new ArrayList<>();
        ChangeRecord change2 = new ChangeRecord();
        change2.setId("2");
        change2.setOriginalName("test2.txt");
        change2.setNewName("moved.txt");
        change2.setStatus(ChangeRecord.ExecStatus.PENDING);
        mockChanges2.add(change2);

        when(pluginService.previewPlugin(eq("file-cleanup"), anyList(), any(PluginConfigDTO.class)))
            .thenReturn(mockChanges1);
        when(pluginService.previewPlugin(eq("file-rename"), anyList(), any(PluginConfigDTO.class)))
            .thenReturn(mockChanges2);

        ResponseEntity<List<ChangeRecord>> response = pipelineController.analyzePipeline(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    public void testExecutePipeline_Success() {
        Map<String, Object> request = new HashMap<>();
        List<String> sourceDirectories = new ArrayList<>();
        sourceDirectories.add("/test/path");
        request.put("sourceDirectories", sourceDirectories);
        
        List<Map<String, Object>> pipeline = new ArrayList<>();
        Map<String, Object> plugin = new HashMap<>();
        plugin.put("pluginId", "file-cleanup");
        plugin.put("config", new HashMap<>());
        pipeline.add(plugin);
        request.put("pipeline", pipeline);

        when(taskService.createTask(any(TaskRequestDTO.class))).thenReturn("task-123");

        ResponseEntity<Map<String, Object>> response = pipelineController.executePipeline(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("task-123", response.getBody().get("taskId"));
        
        verify(taskService, times(1)).createTask(any(TaskRequestDTO.class));
        verify(taskService, times(1)).executeTask("task-123");
    }

    @Test
    public void testExecutePipeline_EmptyPipeline() {
        Map<String, Object> request = new HashMap<>();
        List<String> sourceDirectories = new ArrayList<>();
        sourceDirectories.add("/test/path");
        request.put("sourceDirectories", sourceDirectories);
        
        List<Map<String, Object>> pipeline = new ArrayList<>();
        request.put("pipeline", pipeline);

        ResponseEntity<Map<String, Object>> response = pipelineController.executePipeline(request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("流水线不能为空", response.getBody().get("message"));
    }

    @Test
    public void testGetPipeline_AfterUpdate() {
        List<Map<String, Object>> pipeline = new ArrayList<>();
        Map<String, Object> plugin = new HashMap<>();
        plugin.put("pluginId", "file-cleanup");
        plugin.put("name", "文件清理插件");
        plugin.put("config", new HashMap<>());
        pipeline.add(plugin);

        pipelineController.updatePipeline(pipeline);
        
        ResponseEntity<List<Map<String, Object>>> response = pipelineController.getPipeline();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("file-cleanup", response.getBody().get(0).get("pluginId"));
    }
}
