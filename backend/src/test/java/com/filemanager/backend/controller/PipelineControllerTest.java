package com.filemanager.backend.controller;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
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
    private StrategyService strategyService;

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
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("strategyId", "rename");
        strategy.put("name", "重命名策略");
        strategy.put("config", new HashMap<>());
        pipeline.add(strategy);

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
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("strategyId", "rename");
        strategy.put("config", new HashMap<>());
        pipeline.add(strategy);
        request.put("pipeline", pipeline);

        List<ChangeRecord> mockChanges = new ArrayList<>();
        ChangeRecord change = new ChangeRecord();
        change.setId("1");
        change.setOriginalName("test.txt");
        change.setNewName("renamed.txt");
        change.setStatus(ChangeRecord.ExecStatus.PENDING);
        mockChanges.add(change);

        when(strategyService.analyzeFiles(anyString(), anyList(), any(StrategyConfigDTO.class)))
            .thenReturn(mockChanges);

        ResponseEntity<List<ChangeRecord>> response = pipelineController.analyzePipeline(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("test.txt", response.getBody().get(0).getOriginalName());
        assertEquals("renamed.txt", response.getBody().get(0).getNewName());
    }

    @Test
    public void testAnalyzePipeline_MultipleStrategies() {
        Map<String, Object> request = new HashMap<>();
        List<String> sourceDirectories = new ArrayList<>();
        sourceDirectories.add("/test/path");
        request.put("sourceDirectories", sourceDirectories);
        
        List<Map<String, Object>> pipeline = new ArrayList<>();
        Map<String, Object> strategy1 = new HashMap<>();
        strategy1.put("strategyId", "rename");
        strategy1.put("config", new HashMap<>());
        pipeline.add(strategy1);
        
        Map<String, Object> strategy2 = new HashMap<>();
        strategy2.put("strategyId", "move");
        strategy2.put("config", new HashMap<>());
        pipeline.add(strategy2);
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

        when(strategyService.analyzeFiles(eq("rename"), anyList(), any(StrategyConfigDTO.class)))
            .thenReturn(mockChanges1);
        when(strategyService.analyzeFiles(eq("move"), anyList(), any(StrategyConfigDTO.class)))
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
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("strategyId", "rename");
        strategy.put("config", new HashMap<>());
        pipeline.add(strategy);
        request.put("pipeline", pipeline);

        when(taskService.createTask(any(TaskRequestDTO.class))).thenReturn("task-123");
        doNothing().when(taskService).executeTask("task-123");

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

        when(taskService.createTask(any(TaskRequestDTO.class))).thenReturn("task-456");
        doNothing().when(taskService).executeTask("task-456");

        ResponseEntity<Map<String, Object>> response = pipelineController.executePipeline(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("task-456", response.getBody().get("taskId"));
    }

    @Test
    public void testGetPipeline_AfterUpdate() {
        List<Map<String, Object>> pipeline = new ArrayList<>();
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("strategyId", "rename");
        strategy.put("name", "重命名策略");
        strategy.put("config", new HashMap<>());
        pipeline.add(strategy);

        pipelineController.updatePipeline(pipeline);
        
        ResponseEntity<List<Map<String, Object>>> response = pipelineController.getPipeline();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("rename", response.getBody().get(0).get("strategyId"));
    }
}
