package com.filemanager.plugin;

import com.filemanager.domain.entity.ChangeRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, Object> attributes = new HashMap<>();
    
    // 策略执行相关的上下文数据
    private List<ChangeRecord> inputRecords = new ArrayList<>();
    private List<File> rootDirs = new ArrayList<>();
    
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
     * 设置属性
     * @param key 属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * 获取属性
     * @param key 属性键
     * @return 属性值
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * 获取属性
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        Object value = attributes.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * 开始计时
     */
    public void startTimer() {
        setAttribute("timerStart", System.currentTimeMillis());
    }
    
    /**
     * 停止计时并返回耗时
     * @return 耗时（毫秒）
     */
    public long stopTimer() {
        Long startTime = getAttribute("timerStart", 0L);
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
    
    /**
     * 获取输入记录列表
     * @return 输入记录列表
     */
    public List<ChangeRecord> getInputRecords() {
        return inputRecords;
    }
    
    /**
     * 设置输入记录列表
     * @param inputRecords 输入记录列表
     */
    public void setInputRecords(List<ChangeRecord> inputRecords) {
        this.inputRecords = inputRecords != null ? inputRecords : new ArrayList<>();
    }
    
    /**
     * 添加输入记录
     * @param record 输入记录
     */
    public void addInputRecord(ChangeRecord record) {
        if (inputRecords == null) {
            inputRecords = new ArrayList<>();
        }
        inputRecords.add(record);
    }
    
    /**
     * 获取根目录列表
     * @return 根目录列表
     */
    public List<File> getRootDirs() {
        return rootDirs;
    }
    
    /**
     * 设置根目录列表
     * @param rootDirs 根目录列表
     */
    public void setRootDirs(List<File> rootDirs) {
        this.rootDirs = rootDirs != null ? rootDirs : new ArrayList<>();
    }
    
    /**
     * 添加根目录
     * @param rootDir 根目录
     */
    public void addRootDir(File rootDir) {
        if (rootDirs == null) {
            rootDirs = new ArrayList<>();
        }
        rootDirs.add(rootDir);
    }
    
}
