package com.filemanager.backend.integration;

import com.filemanager.backend.entity.TaskInfoPO;
import com.filemanager.backend.entity.ChangeRecordPO;
import com.filemanager.backend.service.TaskInfoService;
import com.filemanager.backend.service.ChangeRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseIntegrationTest {

    @Autowired
    private TaskInfoService taskInfoService;

    @Autowired
    private ChangeRecordService changeRecordService;

    private TaskInfoPO testTask;
    private String testTaskId;

    @BeforeEach
    void setUp() {
        testTaskId = "integration-test-task-" + System.currentTimeMillis();
        
        testTask = new TaskInfoPO();
        testTask.setTaskId(testTaskId);
        testTask.setTaskName("集成测试任务");
        testTask.setStatus("CREATED");
        testTask.setCurrentStage("CREATED");
        
        taskInfoService.createTask(testTask);
    }

    @AfterEach
    void tearDown() {
        if (testTaskId != null) {
            changeRecordService.deleteRecordsByTaskId(testTaskId);
            taskInfoService.deleteTask(testTaskId);
        }
    }

    @Test
    void testCreateAndRetrieveTask() {
        TaskInfoPO retrievedTask = taskInfoService.getTaskById(testTaskId);
        
        assertNotNull(retrievedTask);
        assertEquals(testTaskId, retrievedTask.getTaskId());
        assertEquals("集成测试任务", retrievedTask.getTaskName());
        assertEquals("CREATED", retrievedTask.getStatus());
    }

    @Test
    void testUpdateTaskStatus() {
        boolean updated = taskInfoService.updateTaskStatus(testTaskId, "SCANNING");
        
        assertTrue(updated);
        
        TaskInfoPO retrievedTask = taskInfoService.getTaskById(testTaskId);
        assertEquals("SCANNING", retrievedTask.getStatus());
    }

    @Test
    void testUpdateTaskProgress() {
        boolean updated = taskInfoService.updateTaskProgress(testTaskId, 50);
        
        assertTrue(updated);
        
        TaskInfoPO retrievedTask = taskInfoService.getTaskById(testTaskId);
        assertEquals(50.0, retrievedTask.getOverallProgress());
    }

    @Test
    void testCreateAndRetrieveChangeRecord() {
        ChangeRecordPO changeRecord = new ChangeRecordPO();
        changeRecord.setTaskId(testTaskId);
        changeRecord.setOriginalName("test.mp3");
        changeRecord.setNewName("new_test.mp3");
        changeRecord.setFilePath("/old/path/test.mp3");
        changeRecord.setNewPath("/new/path/new_test.mp3");
        changeRecord.setOperationType("RENAME");
        changeRecord.setStatus("PENDING");
        changeRecord.setChanged(true);
        
        ChangeRecordPO createdRecord = changeRecordService.createRecord(changeRecord);
        
        assertNotNull(createdRecord);
        assertNotNull(createdRecord.getId());
        
        List<ChangeRecordPO> records = changeRecordService.getRecordsByTaskId(testTaskId);
        
        assertFalse(records.isEmpty());
        assertEquals(1, records.size());
        assertEquals("test.mp3", records.get(0).getOriginalName());
    }

    @Test
    void testUpdateChangeRecordStatus() {
        ChangeRecordPO changeRecord = new ChangeRecordPO();
        changeRecord.setTaskId(testTaskId);
        changeRecord.setOriginalName("test.mp3");
        changeRecord.setNewName("new_test.mp3");
        changeRecord.setOperationType("RENAME");
        changeRecord.setStatus("PENDING");
        
        ChangeRecordPO createdRecord = changeRecordService.createRecord(changeRecord);
        createdRecord.setStatus("SUCCESS");
        
        ChangeRecordPO updatedRecord = changeRecordService.updateRecord(createdRecord);
        
        assertNotNull(updatedRecord);
        assertEquals("SUCCESS", updatedRecord.getStatus());
    }

    @Test
    void testDeleteChangeRecord() {
        ChangeRecordPO changeRecord = new ChangeRecordPO();
        changeRecord.setTaskId(testTaskId);
        changeRecord.setOriginalName("test.mp3");
        changeRecord.setOperationType("RENAME");
        changeRecord.setStatus("PENDING");
        
        ChangeRecordPO createdRecord = changeRecordService.createRecord(changeRecord);
        
        boolean deleted = changeRecordService.deleteRecord(createdRecord.getId());
        
        assertTrue(deleted);
        
        List<ChangeRecordPO> records = changeRecordService.getRecordsByTaskId(testTaskId);
        assertTrue(records.isEmpty());
    }

    @Test
    void testGetChangeRecordsByStatus() {
        ChangeRecordPO record1 = new ChangeRecordPO();
        record1.setTaskId(testTaskId);
        record1.setOriginalName("test1.mp3");
        record1.setOperationType("RENAME");
        record1.setStatus("SUCCESS");
        changeRecordService.createRecord(record1);
        
        ChangeRecordPO record2 = new ChangeRecordPO();
        record2.setTaskId(testTaskId);
        record2.setOriginalName("test2.mp3");
        record2.setOperationType("RENAME");
        record2.setStatus("PENDING");
        changeRecordService.createRecord(record2);
        
        List<ChangeRecordPO> successRecords = changeRecordService.getRecordsByStatus("SUCCESS");
        
        assertNotNull(successRecords);
        assertEquals(1, successRecords.size());
        assertEquals("SUCCESS", successRecords.get(0).getStatus());
    }

    @Test
    void testGetChangeRecordsByOperationType() {
        ChangeRecordPO record1 = new ChangeRecordPO();
        record1.setTaskId(testTaskId);
        record1.setOriginalName("test1.mp3");
        record1.setOperationType("RENAME");
        record1.setStatus("PENDING");
        changeRecordService.createRecord(record1);
        
        ChangeRecordPO record2 = new ChangeRecordPO();
        record2.setTaskId(testTaskId);
        record2.setOriginalName("test2.mp3");
        record2.setOperationType("MOVE");
        record2.setStatus("PENDING");
        changeRecordService.createRecord(record2);
        
        List<ChangeRecordPO> renameRecords = changeRecordService.getRecordsByOperationType("RENAME");
        
        assertNotNull(renameRecords);
        assertEquals(1, renameRecords.size());
        assertEquals("RENAME", renameRecords.get(0).getOperationType());
    }

    @Test
    void testSearchChangeRecordsByKeyword() {
        ChangeRecordPO record1 = new ChangeRecordPO();
        record1.setTaskId(testTaskId);
        record1.setOriginalName("test_song.mp3");
        record1.setOperationType("RENAME");
        record1.setStatus("PENDING");
        changeRecordService.createRecord(record1);
        
        ChangeRecordPO record2 = new ChangeRecordPO();
        record2.setTaskId(testTaskId);
        record2.setOriginalName("other_song.mp3");
        record2.setOperationType("RENAME");
        record2.setStatus("PENDING");
        changeRecordService.createRecord(record2);
        
        List<ChangeRecordPO> searchResults = changeRecordService.searchRecords("test", null, 1, 10);
        
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());
        assertTrue(searchResults.get(0).getOriginalName().contains("test"));
    }

    @Test
    void testGetChangeRecordsByPage() {
        for (int i = 1; i <= 15; i++) {
            ChangeRecordPO record = new ChangeRecordPO();
            record.setTaskId(testTaskId);
            record.setOriginalName("test" + i + ".mp3");
            record.setOperationType("RENAME");
            record.setStatus("PENDING");
            changeRecordService.createRecord(record);
        }
        
        List<ChangeRecordPO> page1 = changeRecordService.getRecordsByPage(
            testTaskId, null, null, null, null, null, "created_at", "DESC", 1, 10
        );
        
        assertEquals(10, page1.size());
        
        List<ChangeRecordPO> page2 = changeRecordService.getRecordsByPage(
            testTaskId, null, null, null, null, null, "created_at", "DESC", 2, 10
        );
        
        assertEquals(5, page2.size());
    }

    @Test
    void testCountChangeRecords() {
        for (int i = 1; i <= 5; i++) {
            ChangeRecordPO record = new ChangeRecordPO();
            record.setTaskId(testTaskId);
            record.setOriginalName("test" + i + ".mp3");
            record.setOperationType("RENAME");
            record.setStatus("PENDING");
            changeRecordService.createRecord(record);
        }
        
        long totalCount = changeRecordService.getTotalRecordCount();
        assertTrue(totalCount >= 5);
        
        long taskCount = changeRecordService.getRecordCountByTaskId(testTaskId);
        assertEquals(5, taskCount);
    }

    @Test
    void testGetTasksByPage() {
        for (int i = 1; i <= 5; i++) {
            TaskInfoPO task = new TaskInfoPO();
            task.setTaskId("test-task-" + System.currentTimeMillis() + "-" + i);
            task.setTaskName("测试任务" + i);
            task.setStatus("CREATED");
            taskInfoService.createTask(task);
        }
        
        List<TaskInfoPO> tasks = taskInfoService.getTasksByPage(1, 10);
        
        assertNotNull(tasks);
        assertTrue(tasks.size() >= 5);
    }

    @Test
    void testSearchTasksByKeyword() {
        TaskInfoPO task1 = new TaskInfoPO();
        task1.setTaskId("search-task-1-" + System.currentTimeMillis());
        task1.setTaskName("搜索测试任务");
        task1.setStatus("CREATED");
        taskInfoService.createTask(task1);
        
        TaskInfoPO task2 = new TaskInfoPO();
        task2.setTaskId("search-task-2-" + System.currentTimeMillis());
        task2.setTaskName("普通任务");
        task2.setStatus("CREATED");
        taskInfoService.createTask(task2);
        
        List<TaskInfoPO> searchResults = taskInfoService.searchTasks("搜索", 1, 10);
        
        assertNotNull(searchResults);
        assertTrue(searchResults.stream().anyMatch(t -> t.getTaskName().contains("搜索")));
    }

    @Test
    void testGetTotalTaskCount() {
        long initialCount = taskInfoService.getTotalTaskCount();
        
        TaskInfoPO newTask = new TaskInfoPO();
        newTask.setTaskId("count-test-task-" + System.currentTimeMillis());
        newTask.setTaskName("计数测试任务");
        newTask.setStatus("CREATED");
        taskInfoService.createTask(newTask);
        
        long newCount = taskInfoService.getTotalTaskCount();
        
        assertEquals(initialCount + 1, newCount);
    }

    @Test
    void testGetTaskCountByStatus() {
        TaskInfoPO task1 = new TaskInfoPO();
        task1.setTaskId("status-task-1-" + System.currentTimeMillis());
        task1.setTaskName("状态测试任务1");
        task1.setStatus("SCANNING");
        taskInfoService.createTask(task1);
        
        TaskInfoPO task2 = new TaskInfoPO();
        task2.setTaskId("status-task-2-" + System.currentTimeMillis());
        task2.setTaskName("状态测试任务2");
        task2.setStatus("SCANNING");
        taskInfoService.createTask(task2);
        
        long scanningCount = taskInfoService.getTaskCountByStatus("SCANNING");
        
        assertTrue(scanningCount >= 2);
    }

    @Test
    void testDeleteTaskWithChangeRecords() {
        ChangeRecordPO record1 = new ChangeRecordPO();
        record1.setTaskId(testTaskId);
        record1.setOriginalName("test1.mp3");
        record1.setOperationType("RENAME");
        record1.setStatus("PENDING");
        changeRecordService.createRecord(record1);
        
        ChangeRecordPO record2 = new ChangeRecordPO();
        record2.setTaskId(testTaskId);
        record2.setOriginalName("test2.mp3");
        record2.setOperationType("MOVE");
        record2.setStatus("PENDING");
        changeRecordService.createRecord(record2);
        
        boolean taskDeleted = taskInfoService.deleteTask(testTaskId);
        
        assertTrue(taskDeleted);
        
        TaskInfoPO deletedTask = taskInfoService.getTaskById(testTaskId);
        assertNull(deletedTask);
        
        List<ChangeRecordPO> remainingRecords = changeRecordService.getRecordsByTaskId(testTaskId);
        assertTrue(remainingRecords.isEmpty());
    }
}
