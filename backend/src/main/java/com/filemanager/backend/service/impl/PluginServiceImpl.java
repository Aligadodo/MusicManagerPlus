package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.PluginInfoDTO;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.PluginService;
import com.filemanager.plugin.IPlugin;
import com.filemanager.plugin.PluginRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PluginServiceImpl implements PluginService {

    private final Map<String, PluginConfigDTO> pluginConfigs = new ConcurrentHashMap<>();

    @Autowired
    private PluginRegistry pluginRegistry;

    @Override
    public List<PluginInfoDTO> getAvailablePlugins() {
        List<PluginInfoDTO> plugins = new ArrayList<>();
        for (IPlugin plugin : pluginRegistry.getAvailablePlugins()) {
            PluginInfoDTO info = new PluginInfoDTO();
            info.setId(plugin.getId());
            info.setName(plugin.getName());
            info.setDescription(plugin.getDescription());
            info.setVersion(plugin.getVersion());
            info.setEnabled(true);
            plugins.add(info);
        }
        return plugins;
    }

    @Override
    public PluginInfoDTO getPluginInfo(String pluginId) {
        IPlugin plugin = pluginRegistry.getPlugin(pluginId);
        if (plugin == null) {
            return null;
        }
        PluginInfoDTO info = new PluginInfoDTO();
        info.setId(plugin.getId());
        info.setName(plugin.getName());
        info.setDescription(plugin.getDescription());
        info.setVersion(plugin.getVersion());
        info.setEnabled(true);
        return info;
    }

    @Override
    public PluginConfigDTO getPluginConfig(String pluginId) {
        IPlugin plugin = pluginRegistry.getPlugin(pluginId);
        if (plugin == null) {
            return null;
        }
        
        PluginConfigDTO config = pluginConfigs.get(pluginId);
        if (config == null) {
            config = plugin.getDefaultConfig();
            pluginConfigs.put(pluginId, config);
        }
        return config;
    }

    @Override
    public boolean updatePluginConfig(String pluginId, PluginConfigDTO config) {
        IPlugin plugin = pluginRegistry.getPlugin(pluginId);
        if (plugin == null) {
            return false;
        }
        pluginConfigs.put(pluginId, config);
        return true;
    }

    @Override
    public List<ChangeRecord> previewPlugin(String pluginId, List<String> filePaths, PluginConfigDTO config) {
        IPlugin plugin = pluginRegistry.getPlugin(pluginId);
        if (plugin == null) {
            return new ArrayList<>();
        }
        
        return plugin.preview(filePaths, config, new com.filemanager.plugin.ExecutionContext());
    }

    @Override
    public List<ChangeRecord> executePlugin(String pluginId, List<String> filePaths, PluginConfigDTO config) {
        IPlugin plugin = pluginRegistry.getPlugin(pluginId);
        if (plugin == null) {
            return new ArrayList<>();
        }
        
        return plugin.execute(filePaths, config, new com.filemanager.plugin.ExecutionContext());
    }

    @Override
    public boolean reloadPlugins() {
        pluginRegistry.reloadPlugins();
        return true;
    }
}
