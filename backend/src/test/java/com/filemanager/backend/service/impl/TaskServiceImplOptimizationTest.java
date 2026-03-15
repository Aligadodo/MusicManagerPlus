package com.filemanager.backend.service.impl;

import com.filemanager.domain.service.TaskService;
import com.filemanager.backend.storage.DatabaseTaskStorage;
import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.enums.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskServiceImpl优化测试类
 * 测试任务管理服务的优化功能
 */
@SpringBootTest
class TaskServiceImplOptimizationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private DatabaseTaskStorage taskStorage;

    private List<String> testTaskIds;

    @BeforeEach
    void setUp() {
        testTaskIds = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        for (String taskId : testTaskIds) {
            try {
                taskService.deleteTask(taskId);
            } catch (Exception e) {
                // 忽略删除错误
            }
        }
    }

    /**
     * 测试任务ID生成使用UUID
     */
    @Test
    void testTaskIdGeneration() {
        // 创建多个任务
        for (int i = 0; i < 10; i++) {
            TaskRequestDTO request = new TaskRequestDTO();
            request.setTaskName("测试任务-" + i);
            
            TaskRequestDTO.SourceDirectoryDTO sourceDir = new TaskRequestDTO.SourceDirectoryDTO();
            sourceDir.setPath(System.getProperty("java.io.tmpdir"));
            sourceDir.setRecursive(true);
            request.setSourceDirectories(Arrays.asList(sourceDir));

            String taskId = taskService.createTask(request);
            testTaskIds.add(taskId);

            // 验证任务ID格式是否为UUID
            assertTrue(taskId.startsWith("task-"), "任务ID应该以'task-'前缀开头");
            String uuidPart = taskId.substring(5); // 去掉"task-"前缀
            try {
                UUID.fromString(uuidPart);
            } catch (IllegalArgumentException e) {
                fail("任务ID不是有效的UUID格式: " + uuidPart);
            }
        }

        // 验证任务ID是否唯一
        assertEquals(testTaskIds.size(), testTaskIds.stream().distinct().count(), "任务ID应该唯一");
    }

    /**
     * 测试任务状态使用枚举类型
     */
    @Test
    void testTaskStatusManagement() {
        // 创建任务
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("状态管理测试任务");
        
        TaskRequestDTO.SourceDirectoryDTO sourceDir = new TaskRequestDTO.SourceDirectoryDTO();
        sourceDir.setPath(System.getProperty("java.io.tmpdir"));
        sourceDir.setRecursive(true);
        request.setSourceDirectories(Arrays.asList(sourceDir));

        String taskId = taskService.createTask(request);
        testTaskIds.add(taskId);

        // 验证初始状态
        com.filemanager.domain.dto.TaskStatusDTO status = taskService.getTaskStatus(taskId);
        assertEquals(com.filemanager.backend.model.TaskInfo.TaskStatus.CREATED.name(), status.getStatus(), "初始状态应该是CREATED");

        // 执行任务
        boolean executed = taskService.executeTask(taskId);
        assertTrue(executed, "任务执行应该成功");

        // 等待一段时间后检查状态
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        // 验证执行状态
        status = taskService.getTaskStatus(taskId);
        assertTrue(status.getStatus().equals(com.filemanager.backend.model.TaskInfo.TaskStatus.EXECUTING.name()) || 
                   status.getStatus().equals(com.filemanager.backend.model.TaskInfo.TaskStatus.COMPLETED.name()) ||
                   status.getStatus().equals(com.filemanager.backend.model.TaskInfo.TaskStatus.FAILED.name()), 
                   "任务状态应该是EXECUTING、COMPLETED或FAILED");

        // 取消任务
        boolean cancelled = taskService.cancelTask(taskId);
        assertTrue(cancelled, "任务取消应该成功");

        // 验证取消状态
        status = taskService.getTaskStatus(taskId);
        assertEquals(com.filemanager.backend.model.TaskInfo.TaskStatus.CANCELLED.name(), status.getStatus(), "取消后状态应该是CANCELLED");
    }

    /**
     * 测试任务持久化到数据库
     */
    @Test
    void testTaskPersistence() {
        // 创建任务
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("持久化测试任务");
        
        TaskRequestDTO.SourceDirectoryDTO sourceDir = new TaskRequestDTO.SourceDirectoryDTO();
        sourceDir.setPath(System.getProperty("java.io.tmpdir"));
        sourceDir.setRecursive(true);
        request.setSourceDirectories(Arrays.asList(sourceDir));

        String taskId = taskService.createTask(request);
        testTaskIds.add(taskId);

        // 验证任务是否持久化到数据库
        com.filemanager.backend.model.TaskInfo taskInfo = taskStorage.loadTaskInfo(taskId);
        assertNotNull(taskInfo, "任务应该被持久化到数据库");
        assertEquals(taskId, taskInfo.getTaskId(), "任务ID应该一致");
        assertEquals("持久化测试任务", taskInfo.getTaskName(), "任务名称应该一致");

        // 执行任务
        taskService.executeTask(taskId);

        // 等待一段时间后检查数据库中的状态
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        // 验证数据库中的状态是否更新
        taskInfo = taskStorage.loadTaskInfo(taskId);
        assertNotNull(taskInfo, "任务应该仍然存在于数据库中");
        // 状态应该是EXECUTING、COMPLETED或FAILED
        assertTrue(taskInfo.getStatus().name().equals(com.filemanager.backend.model.TaskInfo.TaskStatus.EXECUTING.name()) || 
                   taskInfo.getStatus().name().equals(com.filemanager.backend.model.TaskInfo.TaskStatus.COMPLETED.name()) ||
                   taskInfo.getStatus().name().equals(com.filemanager.backend.model.TaskInfo.TaskStatus.FAILED.name()));
    }

    /**
     * 测试异常处理机制
     */
    @Test
    void testExceptionHandling() {
        // 测试无效的任务ID
        assertThrows(IllegalArgumentException.class, () -> {
            taskService.executeTask("invalid-task-id");
        }, "应该抛出IllegalArgumentException");

        // 测试空的任务请求
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("异常测试任务");
        // 不设置源目录

        String taskId = taskService.createTask(request);
        testTaskIds.add(taskId);

        // 执行任务，应该处理异常
        boolean executed = taskService.executeTask(taskId);
        assertTrue(executed, "任务执行应该成功启动");

        // 等待一段时间后检查状态
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        // 验证任务状态是否为失败
        com.filemanager.domain.dto.TaskStatusDTO status = taskService.getTaskStatus(taskId);
        assertEquals(com.filemanager.backend.model.TaskInfo.TaskStatus.FAILED.name(), status.getStatus(), "任务状态应该是FAILED");
        assertNotNull(status.getMessage(), "错误信息应该不为空");
    }

    /**
     * 测试任务执行超时控制
     */
    @Test
    void testTaskTimeoutControl() {
        // 创建任务，设置较短的超时时间
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("超时测试任务");
        
        TaskRequestDTO.SourceDirectoryDTO sourceDir = new TaskRequestDTO.SourceDirectoryDTO();
        sourceDir.setPath(System.getProperty("java.io.tmpdir"));
        sourceDir.setRecursive(true);
        request.setSourceDirectories(Arrays.asList(sourceDir));

        // 设置全局设置，包含超时时间
        TaskRequestDTO.GlobalSettingsDTO globalSettings = new TaskRequestDTO.GlobalSettingsDTO();
        globalSettings.setTimeout(1000L); // 1秒超时
        request.setGlobalSettings(globalSettings);

        String taskId = taskService.createTask(request);
        testTaskIds.add(taskId);

        // 执行任务
        boolean executed = taskService.executeTask(taskId);
        assertTrue(executed, "任务执行应该成功启动");

        // 等待超时时间
        try {
            Thread.sleep(2000); // 等待2秒，确保超时
        } catch (InterruptedException e) {
            fail("测试被中断");
        }

        // 验证任务状态是否为失败
        com.filemanager.domain.dto.TaskStatusDTO status = taskService.getTaskStatus(taskId);
        assertEquals(com.filemanager.backend.model.TaskInfo.TaskStatus.FAILED.name(), status.getStatus(), "任务状态应该是FAILED");
        assertNotNull(status.getMessage(), "错误信息应该不为空");
    }

    /**
     * 测试并发任务执行
     */
    @Test
    void testConcurrentTaskExecution() throws InterruptedException {
        int taskCount = 5;
        CountDownLatch latch = new CountDownLatch(taskCount);

        // 并发创建并执行任务
        for (int i = 0; i < taskCount; i++) {
            new Thread(() -> {
                try {
                    TaskRequestDTO request = new TaskRequestDTO();
                    request.setTaskName("并发测试任务-" + Thread.currentThread().getId());
                    
                    TaskRequestDTO.SourceDirectoryDTO sourceDir = new TaskRequestDTO.SourceDirectoryDTO();
                    sourceDir.setPath(System.getProperty("java.io.tmpdir"));
                    sourceDir.setRecursive(true);
                    request.setSourceDirectories(Arrays.asList(sourceDir));

                    String taskId = taskService.createTask(request);
                    testTaskIds.add(taskId);

                    // 执行任务
                    taskService.executeTask(taskId);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 等待所有任务完成
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertTrue(completed, "所有任务应该在60秒内完成");

        // 验证所有任务都已创建
        assertEquals(taskCount, testTaskIds.size(), "应该创建了" + taskCount + "个任务");

        // 验证所有任务都有状态
        for (String taskId : testTaskIds) {
            com.filemanager.domain.dto.TaskStatusDTO status = taskService.getTaskStatus(taskId);
            assertNotNull(status, "任务状态应该不为空");
            assertNotNull(status.getStatus(), "任务状态值应该不为空");
        }
    }
}
