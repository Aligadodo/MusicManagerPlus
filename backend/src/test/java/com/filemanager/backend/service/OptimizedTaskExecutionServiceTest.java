package com.filemanager.backend.service;

import com.filemanager.backend.model.TaskConfigSnapshot;
import com.filemanager.backend.model.TaskInfo;
import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.service.StrategyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * OptimizedTaskExecutionService测试类
 * 测试任务执行服务的各项功能
 */
class OptimizedTaskExecutionServiceTest {

    @Mock
    private StrategyService strategyService;

    private OptimizedTaskStorageService storageService;
    private OptimizedTaskExecutionService executionService;
    private WebSocketMessageService webSocketService;
    private String testTaskId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        storageService = new OptimizedTaskStorageService();
        webSocketService = new WebSocketMessageService(null);
        executionService = new OptimizedTaskExecutionService(storageService, strategyService, webSocketService);
        testTaskId = "test-task-" + System.currentTimeMillis();
    }

    @AfterEach
    void tearDown() {
        if (testTaskId != null) {
            storageService.deleteTask(testTaskId);
        }
        storageService.shutdown();
    }

    @Test
    void testCreateTask() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");

        String taskId = executionService.createTask(request);

        assertNotNull(taskId);
        assertTrue(taskId.startsWith("task-"));

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        assertNotNull(taskInfo);
        assertEquals("测试任务", taskInfo.getTaskName());
        assertEquals(TaskInfo.TaskStatus.CREATED, taskInfo.getStatus());
        assertEquals("CREATED", taskInfo.getCurrentStage());
    }

    @Test
    void testCreateTaskWithEmptyName() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("");

        String taskId = executionService.createTask(request);

        assertNotNull(taskId);

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        assertNotNull(taskInfo);
    }

    @Test
    void testExecuteScan() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        executionService.executeScan(taskId);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        assertNotNull(taskInfo);
        assertTrue(taskInfo.getStatus() == TaskInfo.TaskStatus.SCANNING || 
                   taskInfo.getStatus() == TaskInfo.TaskStatus.SCANNED);
    }

    @Test
    void testExecutePreviewWithoutScan() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        assertThrows(IllegalStateException.class, () -> {
            executionService.executePreview(taskId);
        });
    }

    @Test
    void testExecuteWithoutPreview() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        assertThrows(IllegalStateException.class, () -> {
            executionService.executeTask(taskId);
        });
    }

    @Test
    void testCancelTask() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        executionService.executeScan(taskId);

        boolean cancelled = executionService.cancelTask(taskId);

        assertTrue(cancelled);
    }

    @Test
    void testCancelNonExistentTask() {
        boolean cancelled = executionService.cancelTask("non-existent-task");

        assertFalse(cancelled);
    }

    @Test
    void testGetTaskProgress() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        TaskInfo taskInfo = executionService.getTaskProgress(taskId);

        assertNotNull(taskInfo);
        assertEquals(taskId, taskInfo.getTaskId());
        assertEquals("测试任务", taskInfo.getTaskName());
    }

    @Test
    void testGetTaskProgressForNonExistentTask() {
        TaskInfo taskInfo = executionService.getTaskProgress("non-existent-task");

        assertNull(taskInfo);
    }

    @Test
    void testExecuteSelected() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        List<String> selectedRecordIds = Arrays.asList("record-001", "record-002");

        assertThrows(IllegalStateException.class, () -> {
            executionService.executeSelected(taskId, selectedRecordIds);
        });
    }

    @Test
    void testRetryFailed() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        assertThrows(IllegalStateException.class, () -> {
            executionService.retryFailed(taskId);
        });
    }

    @Test
    void testTaskDirectoryStructure() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        String taskDir = storageService.getTaskDirectory(taskId);
        assertTrue(Files.exists(Paths.get(taskDir)));
        assertTrue(Files.exists(Paths.get(taskDir + "/scan")));
        assertTrue(Files.exists(Paths.get(taskDir + "/preview")));
        assertTrue(Files.exists(Paths.get(taskDir + "/execution")));
    }

    @Test
    void testConfigSnapshotSaved() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        TaskConfigSnapshot configSnapshot = storageService.loadConfigSnapshot(taskId);

        assertNotNull(configSnapshot);
    }

    @Test
    void testTaskInfoSaved() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);

        assertNotNull(taskInfo);
        assertEquals(taskId, taskInfo.getTaskId());
        assertEquals("测试任务", taskInfo.getTaskName());
        assertNotNull(taskInfo.getCreatedAt());
        assertEquals(TaskInfo.TaskStatus.CREATED, taskInfo.getStatus());
        assertEquals("CREATED", taskInfo.getCurrentStage());
    }

    @Test
    void testMultipleTasks() {
        TaskRequestDTO request1 = new TaskRequestDTO();
        request1.setTaskName("测试任务1");
        String taskId1 = executionService.createTask(request1);

        TaskRequestDTO request2 = new TaskRequestDTO();
        request2.setTaskName("测试任务2");
        String taskId2 = executionService.createTask(request2);

        TaskInfo taskInfo1 = storageService.loadTaskInfo(taskId1);
        TaskInfo taskInfo2 = storageService.loadTaskInfo(taskId2);

        assertNotNull(taskInfo1);
        assertNotNull(taskInfo2);
        assertNotEquals(taskId1, taskId2);
        assertEquals("测试任务1", taskInfo1.getTaskName());
        assertEquals("测试任务2", taskInfo2.getTaskName());

        storageService.deleteTask(taskId2);
    }

    @Test
    void testDeleteTask() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("测试任务");
        String taskId = executionService.createTask(request);

        boolean deleted = storageService.deleteTask(taskId);

        assertTrue(deleted);

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        assertNull(taskInfo);

        String taskDir = storageService.getTaskDirectory(taskId);
        assertFalse(Files.exists(Paths.get(taskDir)));
    }
}
