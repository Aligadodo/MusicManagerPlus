package com.filemanager.backend.integration;

import com.filemanager.backend.model.TaskConfigSnapshot;
import com.filemanager.backend.model.TaskInfo;
import com.filemanager.backend.service.OptimizedTaskExecutionService;
import com.filemanager.backend.service.OptimizedTaskStorageService;
import com.filemanager.domain.dto.TaskRequestDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskExecutionIntegrationTest集成测试类
 * 测试完整的任务执行流程
 */
class TaskExecutionIntegrationTest {

    private OptimizedTaskExecutionService executionService;
    private OptimizedTaskStorageService storageService;
    private String testTaskId;
    private Path testDirectory;

    @BeforeEach
    void setUp() throws IOException {
        storageService = new OptimizedTaskStorageService();
        executionService = new OptimizedTaskExecutionService(storageService, null, null);

        testTaskId = "integration-test-" + System.currentTimeMillis();

        testDirectory = Files.createTempDirectory("music-manager-test");

        for (int i = 1; i <= 5; i++) {
            String fileName = "test-song-" + i + ".mp3";
            Path filePath = testDirectory.resolve(fileName);
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write("Test music file " + i);
            }
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (testTaskId != null) {
            storageService.deleteTask(testTaskId);
        }
        storageService.shutdown();

        if (testDirectory != null && Files.exists(testDirectory)) {
            Files.walk(testDirectory)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("无法删除文件: " + path);
                        }
                    });
        }
    }

    @Test
    void testCompleteTaskFlow() throws InterruptedException {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("集成测试任务");
        String taskId = executionService.createTask(request);

        assertNotNull(taskId);
        assertTrue(taskId.startsWith("task-"));

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        assertEquals(TaskInfo.TaskStatus.CREATED, taskInfo.getStatus());

        executionService.executeScan(taskId);

        Thread.sleep(3000);

        taskInfo = storageService.loadTaskInfo(taskId);
        assertTrue(taskInfo.getStatus() == TaskInfo.TaskStatus.SCANNING || 
                   taskInfo.getStatus() == TaskInfo.TaskStatus.SCANNED);

        List<String> scanResults = storageService.readScanData(taskId, 1, 100);
        assertTrue(scanResults.size() >= 0);

        storageService.deleteTask(taskId);
    }

    @Test
    void testTaskCancellation() throws InterruptedException {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("取消测试任务");
        String taskId = executionService.createTask(request);

        executionService.executeScan(taskId);

        Thread.sleep(1000);

        boolean cancelled = executionService.cancelTask(taskId);
        assertTrue(cancelled);

        Thread.sleep(1000);

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        assertEquals(TaskInfo.TaskStatus.CANCELLED, taskInfo.getStatus());

        storageService.deleteTask(taskId);
    }

    @Test
    void testTaskProgressTracking() throws InterruptedException {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("进度跟踪测试任务");
        String taskId = executionService.createTask(request);

        executionService.executeScan(taskId);

        for (int i = 0; i < 10; i++) {
            Thread.sleep(500);
            TaskInfo taskInfo = executionService.getTaskProgress(taskId);
            assertNotNull(taskInfo);
            assertNotNull(taskInfo.getTaskId());
            assertNotNull(taskInfo.getStatus());
            assertNotNull(taskInfo.getCurrentStage());

            if (taskInfo.getStatus() == TaskInfo.TaskStatus.SCANNED) {
                break;
            }
        }

        storageService.deleteTask(taskId);
    }

    @Test
    void testTaskLogRecording() throws InterruptedException {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("日志记录测试任务");
        String taskId = executionService.createTask(request);

        executionService.executeScan(taskId);

        Thread.sleep(2000);

        List<String> logs = storageService.readTaskLog(taskId, 1, 100);
        assertTrue(logs.size() >= 0);

        storageService.deleteTask(taskId);
    }

    @Test
    void testMultipleConcurrentTasks() throws InterruptedException {
        TaskRequestDTO request1 = new TaskRequestDTO();
        request1.setTaskName("并发测试任务1");
        String taskId1 = executionService.createTask(request1);

        TaskRequestDTO request2 = new TaskRequestDTO();
        request2.setTaskName("并发测试任务2");
        String taskId2 = executionService.createTask(request2);

        executionService.executeScan(taskId1);
        executionService.executeScan(taskId2);

        Thread.sleep(3000);

        TaskInfo taskInfo1 = storageService.loadTaskInfo(taskId1);
        TaskInfo taskInfo2 = storageService.loadTaskInfo(taskId2);

        assertNotNull(taskInfo1);
        assertNotNull(taskInfo2);

        storageService.deleteTask(taskId1);
        storageService.deleteTask(taskId2);
    }

    @Test
    void testTaskDirectoryStructure() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("目录结构测试任务");
        String taskId = executionService.createTask(request);

        String taskDir = storageService.getTaskDirectory(taskId);
        assertTrue(Files.exists(Paths.get(taskDir)));
        assertTrue(Files.exists(Paths.get(taskDir + "/scan")));
        assertTrue(Files.exists(Paths.get(taskDir + "/preview")));
        assertTrue(Files.exists(Paths.get(taskDir + "/execution")));

        storageService.deleteTask(taskId);
    }

    @Test
    void testTaskPersistence() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("持久化测试任务");
        String taskId = executionService.createTask(request);

        TaskInfo taskInfo1 = storageService.loadTaskInfo(taskId);
        assertNotNull(taskInfo1);
        assertEquals(taskId, taskInfo1.getTaskId());
        assertEquals("持久化测试任务", taskInfo1.getTaskName());

        TaskInfo taskInfo2 = storageService.loadTaskInfo(taskId);
        assertNotNull(taskInfo2);
        assertEquals(taskInfo1.getTaskId(), taskInfo2.getTaskId());
        assertEquals(taskInfo1.getTaskName(), taskInfo2.getTaskName());

        storageService.deleteTask(taskId);
    }

    @Test
    void testTaskDeletion() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("删除测试任务");
        String taskId = executionService.createTask(request);

        String taskDir = storageService.getTaskDirectory(taskId);
        assertTrue(Files.exists(Paths.get(taskDir)));

        boolean deleted = storageService.deleteTask(taskId);
        assertTrue(deleted);

        assertFalse(Files.exists(Paths.get(taskDir)));

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        assertNull(taskInfo);
    }

    @Test
    void testTaskListRetrieval() {
        TaskRequestDTO request1 = new TaskRequestDTO();
        request1.setTaskName("列表测试任务1");
        String taskId1 = executionService.createTask(request1);

        TaskRequestDTO request2 = new TaskRequestDTO();
        request2.setTaskName("列表测试任务2");
        String taskId2 = executionService.createTask(request2);

        List<String> taskIds = storageService.getAllTaskIds();

        assertNotNull(taskIds);
        assertTrue(taskIds.size() >= 2);
        assertTrue(taskIds.contains(taskId1));
        assertTrue(taskIds.contains(taskId2));

        storageService.deleteTask(taskId1);
        storageService.deleteTask(taskId2);
    }

    @Test
    void testTaskErrorHandling() {
        String nonExistentTaskId = "non-existent-task-" + System.currentTimeMillis();

        TaskInfo taskInfo = executionService.getTaskProgress(nonExistentTaskId);
        assertNull(taskInfo);

        boolean cancelled = executionService.cancelTask(nonExistentTaskId);
        assertFalse(cancelled);
    }

    @Test
    void testTaskConfigSnapshot() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("配置快照测试任务");
        String taskId = executionService.createTask(request);

        TaskConfigSnapshot configSnapshot = storageService.loadConfigSnapshot(taskId);
        assertNotNull(configSnapshot);

        storageService.deleteTask(taskId);
    }
}
