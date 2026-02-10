package com.filemanager.domain.service;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginInfoDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.io.File;
import java.util.List;

/**
 * 插件服务接口
 */
public interface PluginService {
    /**
     * 预览插件执行结果
     * @param pluginId 插件ID
     * @param sourceDirectories 源目录列表
     * @param config 插件配置
     * @return 变更记录列表
     */
    List<ChangeRecord> previewPlugin(String pluginId, List<String> sourceDirectories, PluginConfigDTO config);

    /**
     * 预览插件执行结果（带前置条件）
     * @param pluginId 插件ID
     * @param sourceDirectories 源目录列表
     * @param config 插件配置
     * @param preconditionGroups 前置条件组
     * @return 变更记录列表
     */
    List<ChangeRecord> previewPlugin(String pluginId, List<String> sourceDirectories, PluginConfigDTO config, List<PreconditionGroupDTO> preconditionGroups);

    /**
     * 分析单个文件（新接口，推荐使用）
     * @param pluginId 插件ID
     * @param currentRecord 当前文件的变更记录
     * @param inputRecords 输入记录列表
     * @param rootDirs 根目录列表
     * @param config 插件配置
     * @param preconditionGroups 前置条件组
     * @return 变更记录列表
     */
    List<ChangeRecord> analyzePlugin(String pluginId, ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, List<File> rootDirs, 
        PluginConfigDTO config, List<PreconditionGroupDTO> preconditionGroups);

    /**
     * 执行插件
     * @param pluginId 插件ID
     * @param sourceDirectories 源目录列表
     * @param config 插件配置
     * @return 变更记录列表
     */
    List<ChangeRecord> executePlugin(String pluginId, List<String> sourceDirectories, PluginConfigDTO config);

    /**
     * 执行插件（带前置条件）
     * @param pluginId 插件ID
     * @param sourceDirectories 源目录列表
     * @param config 插件配置
     * @param preconditionGroups 前置条件组
     * @return 变更记录列表
     */
    List<ChangeRecord> executePlugin(String pluginId, List<String> sourceDirectories, PluginConfigDTO config, List<PreconditionGroupDTO> preconditionGroups);

    /**
     * 获取可用插件列表
     * @return 插件信息列表
     */
    List<PluginInfoDTO> getAvailablePlugins();

    /**
     * 获取插件信息
     * @param pluginId 插件ID
     * @return 插件信息
     */
    PluginInfoDTO getPluginInfo(String pluginId);

    /**
     * 获取插件配置
     * @param pluginId 插件ID
     * @return 插件配置
     */
    PluginConfigDTO getPluginConfig(String pluginId);

    /**
     * 更新插件配置
     * @param pluginId 插件ID
     * @param config 插件配置
     * @return 是否更新成功
     */
    boolean updatePluginConfig(String pluginId, PluginConfigDTO config);

    /**
     * 重载插件
     * @return 是否成功
     */
    boolean reloadPlugins();

    /**
     * 获取内部插件列表
     * @return 插件信息列表
     */
    List<PluginInfoDTO> getInternalPlugins();

    /**
     * 获取外部插件列表
     * @return 插件信息列表
     */
    List<PluginInfoDTO> getExternalPlugins();

    /**
     * 扫描外部插件
     * @param pluginDir 插件目录
     * @return 插件JAR文件列表
     */
    List<String> scanExternalPlugins(String pluginDir);

    /**
     * 加载外部插件
     * @param pluginDir 插件目录
     */
    void loadExternalPlugins(String pluginDir);

    /**
     * 重载外部插件
     */
    void reloadExternalPlugins();
}
