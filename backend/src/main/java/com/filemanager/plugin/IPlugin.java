package com.filemanager.plugin;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;
import java.util.Map;

/**
 * 插件接口
 * 所有插件都需要实现此接口
 */
public interface IPlugin {

    /**
     * 获取插件ID
     * @return 插件ID
     */
    String getId();

    /**
     * 获取插件名称
     * @return 插件名称
     */
    String getName();

    /**
     * 获取插件描述
     * @return 插件描述
     */
    String getDescription();

    /**
     * 获取插件版本
     * @return 插件版本
     */
    String getVersion();

    /**
     * 获取插件参数列表
     * @return 插件参数列表
     */
    List<Map<String, Object>> getParameters();

    /**
     * 获取默认配置
     * @return 默认配置
     */
    PluginConfigDTO getDefaultConfig();

    /**
     * 预览插件执行结果
     * @param filePaths 文件路径列表
     * @param config 插件配置
     * @param context 执行上下文
     * @return 变更记录列表
     */
    List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);

    /**
     * 执行插件
     * @param filePaths 文件路径列表
     * @param config 插件配置
     * @param context 执行上下文
     * @return 变更记录列表
     */
    List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);

}
