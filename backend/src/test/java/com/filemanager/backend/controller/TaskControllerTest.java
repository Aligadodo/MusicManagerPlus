package com.filemanager.backend.controller;

import com.filemanager.backend.model.TaskConfigSnapshot;
import com.filemanager.backend.model.TaskInfo;
import com.filemanager.backend.service.OptimizedTaskExecutionService;
import com.filemanager.backend.service.OptimizedTaskStorageService;
import com.filemanager.domain.dto.TaskRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TaskController测试类
 * 测试任务管理API的各项功能
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OptimizedTaskExecutionService executionService;

    @Autowired
    private OptimizedTaskStorageService storageService;

    private String testTaskId;

    @BeforeEach
    void setUp() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        testTaskId = executionService.createTask(request);
    }

    @AfterEach
    void tearDown() {
        if (testTaskId != null) {
            storageService.deleteTask(testTaskId);
        }
        storageService.shutdown();
    }

    @Test
    void testCreateTask() throws Exception {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("新任务");

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").exists())
                .andExpect(jsonPath("$.data.taskName").value("新任务"))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void testGetTaskList() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testGetTaskInfo() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(testTaskId))
                .andExpect(jsonPath("$.data.taskName").value("测试任务"))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void testGetTaskInfoNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}", "non-existent-task"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCancelTask() throws Exception {
        executionService.executeScan(testTaskId);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        mockMvc.perform(post("/api/tasks/{taskId}/cancel", testTaskId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteTask() throws Exception {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("待删除任务");
        String taskId = executionService.createTask(request);

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.message").value("任务已删除"));

        storageService.deleteTask(taskId);
    }

    @Test
    void testExecuteScan() throws Exception {
        mockMvc.perform(post("/api/tasks/{taskId}/scan", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(testTaskId))
                .andExpect(jsonPath("$.message").value("扫描已开始"));
    }

    @Test
    void testExecutePreview() throws Exception {
        executionService.executeScan(testTaskId);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        mockMvc.perform(post("/api/tasks/{taskId}/preview", testTaskId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testExecuteTask() throws Exception {
        mockMvc.perform(post("/api/tasks/{taskId}/execute", testTaskId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetScanResults() throws Exception {
        executionService.executeScan(testTaskId);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        mockMvc.perform(get("/api/tasks/{taskId}/scan/files", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    void testGetScanStatistics() throws Exception {
        executionService.executeScan(testTaskId);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        mockMvc.perform(get("/api/tasks/{taskId}/scan/statistics", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetPreviewResults() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/preview/records", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    void testGetPreviewStatistics() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/preview/statistics", testTaskId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetExecutionHistory() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/execution/history", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testGetExecutionResults() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/execution/1/records", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    void testGetExecutionStatistics() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/execution/1/statistics", testTaskId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetTaskLogs() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/logs", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    void testExecuteSelected() throws Exception {
        List<String> selectedRecordIds = Arrays.asList("record-001", "record-002");
        Map<String, Object> params = new HashMap<>();
        params.put("executeAll", false);
        params.put("selectedRecordIds", selectedRecordIds);

        mockMvc.perform(post("/api/tasks/{taskId}/execute", testTaskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testRetryFailed() throws Exception {
        mockMvc.perform(post("/api/tasks/{taskId}/retry", testTaskId))
                .andExpect(status().isInternalServerError());
    }
}
