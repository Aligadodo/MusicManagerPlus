package com.filemanager.domain.enums;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    /**
     * 准备就绪 - 任务已创建，等待开始执行
     */
    READY("准备就绪"),
    
    /**
     * 预览中 - 正在进行预览分析
     */
    PREVIEWING("预览中"),
    
    /**
     * 预览完成 - 预览分析已完成
     */
    PREVIEW_COMPLETED("预览完成"),
    
    /**
     * 预览失败 - 预览分析失败
     */
    PREVIEW_FAILED("预览失败"),
    
    /**
     * 执行中 - 正在执行任务
     */
    EXECUTING("执行中"),
    
    /**
     * 执行完成 - 任务执行完成
     */
    EXECUTION_COMPLETED("执行完成"),
    
    /**
     * 执行失败 - 任务执行失败
     */
    EXECUTION_FAILED("执行失败"),
    
    /**
     * 已中止 - 任务被用户中止
     */
    CANCELLED("已中止");
    
    private final String description;
    
    TaskStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isCompleted() {
        return this == PREVIEW_COMPLETED || this == EXECUTION_COMPLETED;
    }
    
    public boolean isFailed() {
        return this == PREVIEW_FAILED || this == EXECUTION_FAILED;
    }
    
    public boolean isRunning() {
        return this == PREVIEWING || this == EXECUTING;
    }
    
    public boolean canTransitionTo(TaskStatus target) {
        switch (this) {
            case READY:
                return target == PREVIEWING || target == CANCELLED;
            case PREVIEWING:
                return target == PREVIEW_COMPLETED || target == PREVIEW_FAILED || target == CANCELLED;
            case PREVIEW_COMPLETED:
                return target == EXECUTING || target == CANCELLED;
            case PREVIEW_FAILED:
                return target == PREVIEWING || target == CANCELLED;
            case EXECUTING:
                return target == EXECUTION_COMPLETED || target == EXECUTION_FAILED || target == CANCELLED;
            case EXECUTION_COMPLETED:
                return target == PREVIEWING || target == CANCELLED;
            case EXECUTION_FAILED:
                return target == PREVIEWING || target == CANCELLED;
            case CANCELLED:
                return target == PREVIEWING;
            default:
                return false;
        }
    }
}