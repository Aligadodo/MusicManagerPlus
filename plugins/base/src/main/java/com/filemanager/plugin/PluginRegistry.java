package com.filemanager.plugin;

import java.util.*;
import java.util.stream.Collectors;

public class PluginRegistry {
    private static final PluginRegistry INSTANCE = new PluginRegistry();
    private final Map<String, IPlugin> plugins = new HashMap<>();
    private final PluginLoader pluginLoader = new PluginLoader();
    private String externalPluginDir;

    private PluginRegistry() {
        loadInternalPlugins();
    }

    public static PluginRegistry getInstance() {
        return INSTANCE;
    }

    private void loadInternalPlugins() {
        ServiceLoader<IPlugin> serviceLoader = ServiceLoader.load(IPlugin.class);
        for (IPlugin plugin : serviceLoader) {
            plugins.put(plugin.getId(), plugin);
        }
    }

    public void loadExternalPlugins(String pluginDirPath) {
        this.externalPluginDir = pluginDirPath;
        List<IPlugin> externalPlugins = pluginLoader.loadPluginsFromDirectory(pluginDirPath);
        for (IPlugin plugin : externalPlugins) {
            plugins.put(plugin.getId(), plugin);
        }
    }

    public void reloadPlugins() {
        plugins.clear();
        loadInternalPlugins();
        if (externalPluginDir != null) {
            loadExternalPlugins(externalPluginDir);
        }
    }

    public void reloadExternalPlugins() {
        if (externalPluginDir != null) {
            pluginLoader.reloadExternalPlugins(externalPluginDir);
            plugins.clear();
            loadInternalPlugins();
            loadExternalPlugins(externalPluginDir);
        }
    }

    public List<String> scanExternalPluginDirectory(String pluginDirPath) {
        return pluginLoader.scanPluginDirectory(pluginDirPath);
    }

    public IPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    public List<IPlugin> getAvailablePlugins() {
        return new ArrayList<>(plugins.values());
    }

    public List<IPlugin> getInternalPlugins() {
        List<IPlugin> internalPlugins = new ArrayList<>();
        ServiceLoader<IPlugin> serviceLoader = ServiceLoader.load(IPlugin.class);
        for (IPlugin plugin : serviceLoader) {
            internalPlugins.add(plugin);
        }
        return internalPlugins;
    }

    public List<IPlugin> getExternalPlugins() {
        return pluginLoader.getExternalPlugins();
    }

    public List<IPlugin> getEnabledPlugins() {
        return plugins.values().stream()
                .filter(plugin -> true) // 这里可以根据实际的启用状态判断
                .collect(Collectors.toList());
    }

    public boolean registerPlugin(IPlugin plugin) {
        if (plugin != null && plugin.getId() != null) {
            plugins.put(plugin.getId(), plugin);
            return true;
        }
        return false;
    }

    public boolean unregisterPlugin(String pluginId) {
        return plugins.remove(pluginId) != null;
    }

    public String getExternalPluginDir() {
        return externalPluginDir;
    }

    public void setExternalPluginDir(String externalPluginDir) {
        this.externalPluginDir = externalPluginDir;
    }
}
