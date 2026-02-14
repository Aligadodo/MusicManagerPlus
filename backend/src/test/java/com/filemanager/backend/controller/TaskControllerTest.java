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
import java.util.List;

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
                .andExpect(jsonPath("$.taskId").exists())
                .andExpect(jsonPath("$.taskName").value("新任务"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void testGetTaskList() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testGetTaskInfo() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.taskName").value("测试任务"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void testGetTaskInfoNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}", "non-existent-task"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetTaskProgress() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/progress", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.currentStage").value("CREATED"));
    }

    @Test
    void testCancelTask() throws Exception {
        mockMvc.perform(post("/api/tasks/{taskId}/cancel", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testDeleteTask() throws Exception {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("待删除任务");
        String taskId = executionService.createTask(request);

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.message").value("任务已删除"));

        storageService.deleteTask(taskId);
    }

    @Test
    void testExecuteScan() throws Exception {
        mockMvc.perform(post("/api/tasks/{taskId}/scan", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.message").value("文件扫描已开始"));
    }

    @Test
    void testExecutePreview() throws Exception {
        executionService.executeScan(testTaskId);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        mockMvc.perform(post("/api/tasks/{taskId}/preview", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.message").value("预览分析已开始"));
    }

    @Test
    void testExecuteTask() throws Exception {
        executionService.executeScan(testTaskId);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        executionService.executePreview(testTaskId);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        mockMvc.perform(post("/api/tasks/{taskId}/execute", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.message").value("任务执行已开始"));
    }

    @Test
    void testGetScanResults() throws Exception {
        executionService.executeScan(testTaskId);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        mockMvc.perform(get("/api/tasks/{taskId}/scan/results", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.files").isArray());
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
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.totalFiles").exists())
                .andExpect(jsonPath("$.totalSize").exists());
    }

    @Test
    void testGetPreviewResults() throws Exception {
        executionService.executeScan(testTaskId);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        executionService.executePreview(testTaskId);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        mockMvc.perform(get("/api/tasks/{taskId}/preview/results", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.changes").isArray());
    }

    @Test
    void testGetPreviewStatistics() throws Exception {
        executionService.executeScan(testTaskId);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        executionService.executePreview(testTaskId);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        mockMvc.perform(get("/api/tasks/{taskId}/preview/statistics", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.totalChanges").exists());
    }

    @Test
    void testGetExecutionHistory() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/execution/history", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.executions").isArray());
    }

    @Test
    void testGetExecutionResults() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/execution/results", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.records").isArray());
    }

    @Test
    void testGetExecutionStatistics() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/execution/statistics", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.totalRecords").exists());
    }

    @Test
    void testGetTaskLogs() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/logs", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.logs").isArray());
    }

    @Test
    void testExecuteSelected() throws Exception {
        List<String> selectedRecordIds = Arrays.asList("record-001", "record-002");

        mockMvc.perform(post("/api/tasks/{taskId}/execute/selected", testTaskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(selectedRecordIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.message").value("选中记录执行已开始"));
    }

    @Test
    void testRetryFailed() throws Exception {
        mockMvc.perform(post("/api/tasks/{taskId}/retry", testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.message").value("失败记录重试已开始"));
    }

    @Test
    void testExportScanResults() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/scan/export", testTaskId))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void testExportPreviewResults() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/preview/export", testTaskId))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void testExportExecutionResults() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/execution/export", testTaskId))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void testExportTaskLogs() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/logs/export", testTaskId))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }
}
