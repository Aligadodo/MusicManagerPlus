package com.filemanager.backend.integration;

import com.filemanager.backend.model.TaskConfigSnapshot;
import com.filemanager.backend.model.TaskInfo;
import com.filemanager.backend.service.TaskExecutionService;
import com.filemanager.backend.service.TaskStorageService;
import com.filemanager.backend.service.ConfigSnapshotService;
import com.filemanager.domain.dto.TaskRequestDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskExecutionIntegrationTestDB集成测试类
 * 测试数据库存储模式下的完整任务执行流程
 */
@SpringBootTest
@ActiveProfiles("test-db")
class TaskExecutionIntegrationTestDB {

    @Autowired
    private TaskExecutionService executionService;

    @Autowired
    private TaskStorageService storageService;

    @Autowired
    private ConfigSnapshotService configSnapshotService;

    private String testTaskId;
    private Path testDirectory;

    @BeforeEach
    void setUp() throws IOException {
        testTaskId = "integration-test-db-" + System.currentTimeMillis();

        testDirectory = Files.createTempDirectory("music-manager-test-db");

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
        request.setTaskName("数据库模式集成测试任务");
        
        TaskRequestDTO.SourceDirectoryDTO sourceDir = new TaskRequestDTO.SourceDirectoryDTO();
        sourceDir.setPath(testDirectory.toString());
        sourceDir.setRecursive(true);
        request.setSourceDirectories(java.util.Arrays.asList(sourceDir));
        
        String taskId = executionService.createTask(request);

        assertNotNull(taskId);
        assertTrue(taskId.startsWith("task-"));

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        assertEquals(TaskInfo.TaskStatus.CREATED, taskInfo.getStatus());
        assertNotNull(taskInfo.getTaskId());
        assertNotNull(taskInfo.getTaskName());
        assertTrue(taskInfo.getCreatedAt() > 0);
        assertTrue(taskInfo.getUpdatedAt() > 0);
    }

    @Test
    void testTaskPersistence() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("持久化测试任务");
        String taskId = executionService.createTask(request);

        TaskInfo originalTask = storageService.loadTaskInfo(taskId);
        assertNotNull(originalTask);

        TaskInfo loadedTask = storageService.loadTaskInfo(taskId);
        assertEquals(originalTask.getTaskId(), loadedTask.getTaskId());
        assertEquals(originalTask.getTaskName(), loadedTask.getTaskName());
        assertEquals(originalTask.getStatus(), loadedTask.getStatus());
    }

    @Test
    void testTaskDeletion() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("删除测试任务");
        String taskId = executionService.createTask(request);

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        assertNotNull(taskInfo);

        boolean deleted = storageService.deleteTask(taskId);
        assertTrue(deleted);

        TaskInfo deletedTask = storageService.loadTaskInfo(taskId);
        assertNull(deletedTask);
    }

    @Test
    void testTaskListRetrieval() {
        TaskRequestDTO request1 = new TaskRequestDTO();
        request1.setTaskName("任务1");
        TaskRequestDTO request2 = new TaskRequestDTO();
        request2.setTaskName("任务2");
        TaskRequestDTO request3 = new TaskRequestDTO();
        request3.setTaskName("任务3");

        String taskId1 = executionService.createTask(request1);
        String taskId2 = executionService.createTask(request2);
        String taskId3 = executionService.createTask(request3);

        List<String> taskIds = storageService.getAllTaskIds();
        assertTrue(taskIds.size() >= 3);

        assertTrue(taskIds.contains(taskId1));
        assertTrue(taskIds.contains(taskId2));
        assertTrue(taskIds.contains(taskId3));
    }

    @Test
    void testTaskConfigSnapshot() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("配置快照测试任务");
        String taskId = executionService.createTask(request);

        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        assertNotNull(taskInfo.getConfigSnapshotId());

        TaskConfigSnapshot snapshot = configSnapshotService.getSnapshot(taskInfo.getConfigSnapshotId());
        assertNotNull(snapshot);
        assertNotNull(snapshot.getSourceDirectories());
        assertNotNull(snapshot.getPipelineConfig());
        assertNotNull(snapshot.getGlobalSettings());
    }
}
