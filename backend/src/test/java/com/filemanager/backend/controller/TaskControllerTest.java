package com.filemanager.backend.controller;

import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;
import com.filemanager.domain.service.TaskService;
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

public class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateTask_Success() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setStrategyId("test-strategy");
        request.setTaskName("Test Task");
        request.setDescription("A test task");

        when(taskService.createTask(any(TaskRequestDTO.class))).thenReturn("task-123");

        ResponseEntity<Map<String, String>> response = taskController.createTask(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("task-123", response.getBody().get("taskId"));

        verify(taskService, times(1)).createTask(any(TaskRequestDTO.class));
    }

    @Test
    public void testGetTaskStatus_Success() {
        TaskStatusDTO mockStatus = new TaskStatusDTO();
        mockStatus.setTaskId("task-123");
        mockStatus.setStatus(TaskStatusDTO.TaskStatus.RUNNING);
        mockStatus.setProgress(0.5);
        mockStatus.setMessage("Task is running");

        when(taskService.getTaskStatus("task-123")).thenReturn(mockStatus);

        ResponseEntity<TaskStatusDTO> response = taskController.getTaskStatus("task-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("task-123", response.getBody().getTaskId());
        assertEquals(TaskStatusDTO.TaskStatus.RUNNING, response.getBody().getStatus());
        assertEquals(0.5, response.getBody().getProgress());
    }

    @Test
    public void testGetTaskStatus_NotFound() {
        when(taskService.getTaskStatus("nonexistent"))
            .thenThrow(new IllegalArgumentException("Task not found"));

        ResponseEntity<TaskStatusDTO> response = taskController.getTaskStatus("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testGetTasks_Success() {
        List<TaskStatusDTO> mockTasks = new ArrayList<>();
        TaskStatusDTO task1 = new TaskStatusDTO();
        task1.setTaskId("task-1");
        task1.setStatus(TaskStatusDTO.TaskStatus.PENDING);
        mockTasks.add(task1);

        TaskStatusDTO task2 = new TaskStatusDTO();
        task2.setTaskId("task-2");
        task2.setStatus(TaskStatusDTO.TaskStatus.SUCCESS);
        mockTasks.add(task2);

        when(taskService.getTasks(null, 1, 20)).thenReturn(mockTasks);

        ResponseEntity<List<TaskStatusDTO>> response = taskController.getTasks(null, 1, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    public void testGetTasks_WithStatusFilter() {
        List<TaskStatusDTO> mockTasks = new ArrayList<>();
        TaskStatusDTO task = new TaskStatusDTO();
        task.setTaskId("task-1");
        task.setStatus(TaskStatusDTO.TaskStatus.RUNNING);
        mockTasks.add(task);

        when(taskService.getTasks("RUNNING", 1, 20)).thenReturn(mockTasks);

        ResponseEntity<List<TaskStatusDTO>> response = taskController.getTasks("RUNNING", 1, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(TaskStatusDTO.TaskStatus.RUNNING, response.getBody().get(0).getStatus());
    }

    @Test
    public void testExecuteTask_Success() {
        when(taskService.executeTask("task-123")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = taskController.executeTask("task-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("任务开始执行", response.getBody().get("message"));

        verify(taskService, times(1)).executeTask("task-123");
    }

    @Test
    public void testCancelTask_Success() {
        when(taskService.cancelTask("task-123")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = taskController.cancelTask("task-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("任务已取消", response.getBody().get("message"));

        verify(taskService, times(1)).cancelTask("task-123");
    }

    @Test
    public void testCancelTask_NotFound() {
        when(taskService.cancelTask("nonexistent")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = taskController.cancelTask("nonexistent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
    }

    @Test
    public void testDeleteTask_Success() {
        when(taskService.deleteTask("task-123")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = taskController.deleteTask("task-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("任务已删除", response.getBody().get("message"));

        verify(taskService, times(1)).deleteTask("task-123");
    }

    @Test
    public void testDeleteTask_NotFound() {
        when(taskService.deleteTask("nonexistent")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = taskController.deleteTask("nonexistent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
    }
}
