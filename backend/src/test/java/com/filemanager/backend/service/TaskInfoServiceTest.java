package com.filemanager.backend.service;

import com.filemanager.backend.entity.TaskInfoPO;
import com.filemanager.backend.mapper.TaskInfoMapper;
import com.filemanager.backend.service.impl.TaskInfoServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskInfoServiceTest {

    @Mock
    private TaskInfoMapper taskInfoMapper;

    @InjectMocks
    private TaskInfoServiceImpl taskInfoService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testCreateTask() {
        TaskInfoPO taskInfo = new TaskInfoPO();
        taskInfo.setTaskId("test-task-001");
        taskInfo.setTaskName("测试任务");
        taskInfo.setStatus("CREATED");

        when(taskInfoMapper.insert(any(TaskInfoPO.class))).thenReturn(1);

        TaskInfoPO result = taskInfoService.createTask(taskInfo);

        assertNotNull(result);
        assertEquals("test-task-001", result.getTaskId());
        verify(taskInfoMapper, times(1)).insert(any(TaskInfoPO.class));
    }

    @Test
    void testGetTaskById() {
        TaskInfoPO taskInfo = new TaskInfoPO();
        taskInfo.setTaskId("test-task-001");
        taskInfo.setTaskName("测试任务");

        when(taskInfoMapper.selectByTaskId("test-task-001")).thenReturn(taskInfo);

        TaskInfoPO result = taskInfoService.getTaskById("test-task-001");

        assertNotNull(result);
        assertEquals("test-task-001", result.getTaskId());
        assertEquals("测试任务", result.getTaskName());
        verify(taskInfoMapper, times(1)).selectByTaskId("test-task-001");
    }

    @Test
    void testGetTaskByIdNotFound() {
        when(taskInfoMapper.selectByTaskId("non-existent-task")).thenReturn(null);

        TaskInfoPO result = taskInfoService.getTaskById("non-existent-task");

        assertNull(result);
        verify(taskInfoMapper, times(1)).selectByTaskId("non-existent-task");
    }

    @Test
    void testUpdateTask() {
        TaskInfoPO taskInfo = new TaskInfoPO();
        taskInfo.setTaskId("test-task-001");
        taskInfo.setTaskName("更新后的任务名称");
        taskInfo.setStatus("SCANNING");

        when(taskInfoMapper.update(any(TaskInfoPO.class))).thenReturn(1);

        TaskInfoPO result = taskInfoService.updateTask(taskInfo);

        assertNotNull(result);
        verify(taskInfoMapper, times(1)).update(any(TaskInfoPO.class));
    }

    @Test
    void testUpdateTaskStatus() {
        when(taskInfoMapper.updateStatus("test-task-001", "SCANNING")).thenReturn(1);

        boolean result = taskInfoService.updateTaskStatus("test-task-001", "SCANNING");

        assertTrue(result);
        verify(taskInfoMapper, times(1)).updateStatus("test-task-001", "SCANNING");
    }

    @Test
    void testDeleteTask() {
        when(taskInfoMapper.deleteByTaskId("test-task-001")).thenReturn(1);

        boolean result = taskInfoService.deleteTask("test-task-001");

        assertTrue(result);
        verify(taskInfoMapper, times(1)).deleteByTaskId("test-task-001");
    }

    @Test
    void testGetAllTasks() {
        TaskInfoPO task1 = new TaskInfoPO();
        task1.setTaskId("test-task-001");
        task1.setTaskName("测试任务1");

        TaskInfoPO task2 = new TaskInfoPO();
        task2.setTaskId("test-task-002");
        task2.setTaskName("测试任务2");

        List<TaskInfoPO> tasks = Arrays.asList(task1, task2);

        when(taskInfoMapper.selectAll()).thenReturn(tasks);

        List<TaskInfoPO> result = taskInfoService.getAllTasks();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("test-task-001", result.get(0).getTaskId());
        assertEquals("test-task-002", result.get(1).getTaskId());
        verify(taskInfoMapper, times(1)).selectAll();
    }

    @Test
    void testGetTasksByStatus() {
        TaskInfoPO task1 = new TaskInfoPO();
        task1.setTaskId("test-task-001");
        task1.setTaskName("测试任务1");
        task1.setStatus("SCANNING");

        TaskInfoPO task2 = new TaskInfoPO();
        task2.setTaskId("test-task-002");
        task2.setTaskName("测试任务2");
        task2.setStatus("SCANNING");

        List<TaskInfoPO> tasks = Arrays.asList(task1, task2);

        when(taskInfoMapper.selectByStatus("SCANNING")).thenReturn(tasks);

        List<TaskInfoPO> result = taskInfoService.getTasksByStatus("SCANNING");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> "SCANNING".equals(t.getStatus())));
        verify(taskInfoMapper, times(1)).selectByStatus("SCANNING");
    }

    @Test
    void testGetTasksByStatusEmpty() {
        when(taskInfoMapper.selectByStatus("COMPLETED")).thenReturn(Arrays.asList());

        List<TaskInfoPO> result = taskInfoService.getTasksByStatus("COMPLETED");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskInfoMapper, times(1)).selectByStatus("COMPLETED");
    }

    @Test
    void testGetTasksByPage() {
        TaskInfoPO task1 = new TaskInfoPO();
        task1.setTaskId("test-task-001");
        task1.setTaskName("测试任务1");

        TaskInfoPO task2 = new TaskInfoPO();
        task2.setTaskId("test-task-002");
        task2.setTaskName("测试任务2");

        List<TaskInfoPO> tasks = Arrays.asList(task1, task2);

        when(taskInfoMapper.selectByPage(any(), any(), any(), any(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(tasks);

        List<TaskInfoPO> result = taskInfoService.getTasksByPage(1, 10);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(taskInfoMapper, times(1)).selectByPage(any(), any(), any(), any(), anyString(), anyString(), eq(0), eq(10));
    }

    @Test
    void testSearchTasks() {
        TaskInfoPO task1 = new TaskInfoPO();
        task1.setTaskId("test-task-001");
        task1.setTaskName("测试任务关键词");

        List<TaskInfoPO> tasks = Arrays.asList(task1);

        when(taskInfoMapper.selectByPage(any(), any(), any(), anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(tasks);

        List<TaskInfoPO> result = taskInfoService.searchTasks("关键词", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getTaskName().contains("关键词"));
        verify(taskInfoMapper, times(1)).selectByPage(any(), any(), any(), eq("关键词"), anyString(), anyString(), eq(0), eq(10));
    }

    @Test
    void testGetTotalTaskCount() {
        when(taskInfoMapper.countByPage(null, null, null, null)).thenReturn(5);

        long result = taskInfoService.getTotalTaskCount();

        assertEquals(5L, result);
        verify(taskInfoMapper, times(1)).countByPage(null, null, null, null);
    }

    @Test
    void testGetTaskCountByStatus() {
        when(taskInfoMapper.countByPage("SCANNING", null, null, null)).thenReturn(3);

        long result = taskInfoService.getTaskCountByStatus("SCANNING");

        assertEquals(3L, result);
        verify(taskInfoMapper, times(1)).countByPage("SCANNING", null, null, null);
    }

    @Test
    void testUpdateTaskProgress() {
        when(taskInfoMapper.updateProgress("test-task-001", 50.0)).thenReturn(1);

        boolean result = taskInfoService.updateTaskProgress("test-task-001", 50);

        assertTrue(result);
        verify(taskInfoMapper, times(1)).updateProgress("test-task-001", 50.0);
    }

    @Test
    void testIncrementProcessedFiles() {
        when(taskInfoMapper.updateMessage("test-task-001", "Processed files incremented")).thenReturn(1);

        boolean result = taskInfoService.incrementProcessedFiles("test-task-001");

        assertTrue(result);
        verify(taskInfoMapper, times(1)).updateMessage("test-task-001", "Processed files incremented");
    }

    @Test
    void testIncrementSuccessCount() {
        when(taskInfoMapper.updateMessage("test-task-001", "Success count incremented")).thenReturn(1);

        boolean result = taskInfoService.incrementSuccessCount("test-task-001");

        assertTrue(result);
        verify(taskInfoMapper, times(1)).updateMessage("test-task-001", "Success count incremented");
    }

    @Test
    void testIncrementFailedCount() {
        when(taskInfoMapper.updateMessage("test-task-001", "Failed count incremented")).thenReturn(1);

        boolean result = taskInfoService.incrementFailedCount("test-task-001");

        assertTrue(result);
        verify(taskInfoMapper, times(1)).updateMessage("test-task-001", "Failed count incremented");
    }

    @Test
    void testIncrementSkippedCount() {
        when(taskInfoMapper.updateMessage("test-task-001", "Skipped count incremented")).thenReturn(1);

        boolean result = taskInfoService.incrementSkippedCount("test-task-001");

        assertTrue(result);
        verify(taskInfoMapper, times(1)).updateMessage("test-task-001", "Skipped count incremented");
    }
}
