package com.filemanager.backend.controller;

import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;
import com.filemanager.domain.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
    }

    @Test
    public void testCreateTask_Success() throws Exception {
        // 准备测试数据
        String requestBody = "{\"strategyId\":\"test-strategy\",\"filePaths\":[\"/path/to/file1\",\"/path/to/file2\"],\"taskName\":\"Test Task\",\"description\":\"Test Task Description\"}";

        // 模拟服务返回
        when(taskService.createTask(any(TaskRequestDTO.class))).thenReturn("task-123");

        // 执行测试
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value("task-123"));
    }

    @Test
    public void testExecuteTask_Success() throws Exception {
        // 模拟服务返回
        when(taskService.executeTask("task-123")).thenReturn(true);

        // 执行测试
        mockMvc.perform(post("/api/tasks/task-123/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testExecuteTask_Failure() throws Exception {
        // 模拟服务返回
        when(taskService.executeTask("task-123")).thenReturn(false);

        // 执行测试
        mockMvc.perform(post("/api/tasks/task-123/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testGetTaskStatus() throws Exception {
        // 准备测试数据
        TaskStatusDTO statusDTO = new TaskStatusDTO();
        statusDTO.setTaskId("task-123");
        statusDTO.setStatus("RUNNING");
        statusDTO.setProgress(50);

        // 模拟服务返回
        when(taskService.getTaskStatus("task-123")).thenReturn(statusDTO);

        // 执行测试
        mockMvc.perform(get("/api/tasks/task-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value("task-123"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.progress").value(50));
    }

    @Test
    public void testGetTasks() throws Exception {
        // 准备测试数据
        List<TaskStatusDTO> tasks = new ArrayList<>();
        TaskStatusDTO task1 = new TaskStatusDTO();
        task1.setTaskId("task-123");
        task1.setStatus("COMPLETED");
        tasks.add(task1);

        // 模拟服务返回
        when(taskService.getTasks(null, 1, 20)).thenReturn(tasks);

        // 执行测试
        mockMvc.perform(get("/api/tasks")
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value("task-123"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    public void testCancelTask_Success() throws Exception {
        // 模拟服务返回
        when(taskService.cancelTask("task-123")).thenReturn(true);

        // 执行测试
        mockMvc.perform(post("/api/tasks/task-123/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testCancelTask_Failure() throws Exception {
        // 模拟服务返回
        when(taskService.cancelTask("task-123")).thenReturn(false);

        // 执行测试
        mockMvc.perform(post("/api/tasks/task-123/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testDeleteTask_Success() throws Exception {
        // 模拟服务返回
        when(taskService.deleteTask("task-123")).thenReturn(true);

        // 执行测试
        mockMvc.perform(delete("/api/tasks/task-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testDeleteTask_Failure() throws Exception {
        // 模拟服务返回
        when(taskService.deleteTask("task-123")).thenReturn(false);

        // 执行测试
        mockMvc.perform(delete("/api/tasks/task-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
