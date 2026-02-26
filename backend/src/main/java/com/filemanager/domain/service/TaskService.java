package com.filemanager.domain.service;

import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;

import java.util.List;

/**
 * 任务服务接口
 */
public interface TaskService {
    /**
     * 创建任务
     * @param request 任务请求DTO
     * @return 任务ID
     */
    String createTask(TaskRequestDTO request);

    /**
     * 执行任务
     * @param taskId 任务ID
     * @return 是否执行成功
     */
    boolean executeTask(String taskId);

    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态DTO
     */
    TaskStatusDTO getTaskStatus(String taskId);

    /**
     * 获取任务列表
     * @param status 状态过滤
     * @param page 页码
     * @param size 每页大小
     * @return 任务状态DTO列表
     */
    List<TaskStatusDTO> getTasks(String status, int page, int size);

    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    boolean cancelTask(String taskId);

    /**
     * 删除任务
     * @param taskId 任务ID
     * @return 是否删除成功
     */
    boolean deleteTask(String taskId);

    /**
     * 获取任务执行结果
     * @param taskId 任务ID
     * @return 变更记录列表
     */
    List<com.filemanager.domain.entity.ChangeRecord> getTaskResults(String taskId);

    /**
     * 检查是否有任务正在运行
     * @return 是否有任务正在运行
     */
    boolean isTaskRunning();

    /**
     * 删除全部任务
     * @return 删除的任务数量
     */
    int clearAllTasks();
}
