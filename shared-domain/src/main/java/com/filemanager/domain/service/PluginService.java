package com.filemanager.domain.service;

import com.filemanager.domain.dto.PluginInfoDTO;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;

public interface PluginService {
    List<PluginInfoDTO> getAvailablePlugins();
    PluginInfoDTO getPluginInfo(String pluginId);
    PluginConfigDTO getPluginConfig(String pluginId);
    boolean updatePluginConfig(String pluginId, PluginConfigDTO config);
    List<ChangeRecord> executePlugin(String pluginId, List<String> filePaths, PluginConfigDTO config);
    boolean reloadPlugins();
}
