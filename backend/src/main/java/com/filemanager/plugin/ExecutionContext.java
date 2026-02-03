package com.filemanager.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 插件执行上下文
 * 提供插件执行过程中的环境支持，如日志记录、进度跟踪等
 */
public class ExecutionContext {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionContext.class);
    
    private String pluginId;
    private String executionId;
    private long startTime;
    private long totalFiles;
    private long processedFiles;
    private boolean cancelled;
    
    public ExecutionContext() {
        this.executionId = "exec-" + System.currentTimeMillis() + "-" + Math.random();
        this.startTime = System.currentTimeMillis();
        this.totalFiles = 0;
        this.processedFiles = 0;
        this.cancelled = false;
    }
    
    public ExecutionContext(String pluginId) {
        this();
        this.pluginId = pluginId;
    }
    
    /**
     * 记录信息日志
     * @param message 日志信息
     */
    public void logInfo(String message) {
        logger.info("[Plugin: {}] {}", pluginId, message);
    }
    
    /**
     * 记录错误日志
     * @param message 错误信息
     */
    public void logError(String message) {
        logger.error("[Plugin: {}] {}", pluginId, message);
    }
    
    /**
     * 记录警告日志
     * @param message 警告信息
     */
    public void logWarn(String message) {
        logger.warn("[Plugin: {}] {}", pluginId, message);
    }
    
    /**
     * 记录调试日志
     * @param message 调试信息
     */
    public void logDebug(String message) {
        logger.debug("[Plugin: {}] {}", pluginId, message);
    }
    
    /**
     * 更新进度
     * @param processedFiles 已处理文件数
     * @param totalFiles 总文件数
     */
    public void updateProgress(long processedFiles, long totalFiles) {
        this.processedFiles = processedFiles;
        this.totalFiles = totalFiles;
        double progress = totalFiles > 0 ? (double) processedFiles / totalFiles * 100 : 0;
        logDebug("Progress: " + processedFiles + "/" + totalFiles + " (" + String.format("%.2f", progress) + "%)");
    }
    
    /**
     * 获取当前进度
     * @return 当前进度百分比
     */
    public double getProgress() {
        return totalFiles > 0 ? (double) processedFiles / totalFiles * 100 : 0;
    }
    
    /**
     * 取消执行
     */
    public void cancel() {
        this.cancelled = true;
        logInfo("Execution cancelled");
    }
    
    /**
     * 检查是否已取消
     * @return 是否已取消
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * 获取执行时间
     * @return 执行时间（毫秒）
     */
    public long getExecutionTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 获取插件ID
     * @return 插件ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * 设置插件ID
     * @param pluginId 插件ID
     */
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }
    
    /**
     * 获取执行ID
     * @return 执行ID
     */
    public String getExecutionId() {
        return executionId;
    }
    
    /**
     * 获取开始时间
     * @return 开始时间
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * 获取总文件数
     * @return 总文件数
     */
    public long getTotalFiles() {
        return totalFiles;
    }
    
    /**
     * 设置总文件数
     * @param totalFiles 总文件数
     */
    public void setTotalFiles(long totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    /**
     * 获取已处理文件数
     * @return 已处理文件数
     */
    public long getProcessedFiles() {
        return processedFiles;
    }
    
    /**
     * 设置已处理文件数
     * @param processedFiles 已处理文件数
     */
    public void setProcessedFiles(long processedFiles) {
        this.processedFiles = processedFiles;
    }
    
}
