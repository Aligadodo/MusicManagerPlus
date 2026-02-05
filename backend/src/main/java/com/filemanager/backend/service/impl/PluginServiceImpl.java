package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.PluginInfoDTO;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.PluginService;
import com.filemanager.plugin.IPlugin;
import com.filemanager.plugin.PluginRegistry;
import com.filemanager.plugin.util.PreconditionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    public List<ChangeRecord> previewPlugin(String pluginId, List<String> filePaths, PluginConfigDTO config, List<PreconditionGroupDTO> preconditionGroups) {
        IPlugin plugin = pluginRegistry.getPlugin(pluginId);
        if (plugin == null) {
            return new ArrayList<>();
        }

        List<String> filteredFilePaths = filterFilesByPreconditions(filePaths, preconditionGroups);
        return plugin.preview(filteredFilePaths, config, new com.filemanager.plugin.ExecutionContext());
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
    public List<ChangeRecord> executePlugin(String pluginId, List<String> filePaths, PluginConfigDTO config, List<PreconditionGroupDTO> preconditionGroups) {
        IPlugin plugin = pluginRegistry.getPlugin(pluginId);
        if (plugin == null) {
            return new ArrayList<>();
        }

        List<String> filteredFilePaths = filterFilesByPreconditions(filePaths, preconditionGroups);
        return plugin.execute(filteredFilePaths, config, new com.filemanager.plugin.ExecutionContext());
    }

    private List<String> filterFilesByPreconditions(List<String> filePaths, List<PreconditionGroupDTO> preconditionGroups) {
        if (preconditionGroups == null || preconditionGroups.isEmpty()) {
            return filePaths;
        }

        return filePaths.stream()
            .filter(filePath -> {
                File file = new File(filePath);
                return PreconditionEvaluator.evaluate(file, preconditionGroups);
            })
            .collect(Collectors.toList());
    }

    @Override
    public boolean reloadPlugins() {
        pluginRegistry.reloadPlugins();
        return true;
    }

    @Override
    public List<PluginInfoDTO> getInternalPlugins() {
        List<PluginInfoDTO> plugins = new ArrayList<>();
        for (IPlugin plugin : pluginRegistry.getInternalPlugins()) {
            PluginInfoDTO info = new PluginInfoDTO();
            info.setId(plugin.getId());
            info.setName(plugin.getName());
            info.setDescription(plugin.getDescription());
            info.setVersion(plugin.getVersion());
            info.setEnabled(true);
            info.setInternal(true);
            plugins.add(info);
        }
        return plugins;
    }

    @Override
    public List<PluginInfoDTO> getExternalPlugins() {
        List<PluginInfoDTO> plugins = new ArrayList<>();
        for (IPlugin plugin : pluginRegistry.getExternalPlugins()) {
            PluginInfoDTO info = new PluginInfoDTO();
            info.setId(plugin.getId());
            info.setName(plugin.getName());
            info.setDescription(plugin.getDescription());
            info.setVersion(plugin.getVersion());
            info.setEnabled(true);
            info.setInternal(false);
            plugins.add(info);
        }
        return plugins;
    }

    @Override
    public List<String> scanExternalPlugins(String pluginDir) {
        return pluginRegistry.scanExternalPluginDirectory(pluginDir);
    }

    @Override
    public void loadExternalPlugins(String pluginDir) {
        pluginRegistry.loadExternalPlugins(pluginDir);
    }

    @Override
    public void reloadExternalPlugins() {
        pluginRegistry.reloadExternalPlugins();
    }
}
