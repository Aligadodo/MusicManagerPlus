package com.filemanager.plugin;

import java.util.*;
import java.util.stream.Collectors;

public class PluginRegistry {
    private static final PluginRegistry INSTANCE = new PluginRegistry();
    private final Map<String, IPlugin> plugins = new HashMap<>();

    private PluginRegistry() {
        loadPlugins();
    }

    public static PluginRegistry getInstance() {
        return INSTANCE;
    }

    private void loadPlugins() {
        ServiceLoader<IPlugin> serviceLoader = ServiceLoader.load(IPlugin.class);
        for (IPlugin plugin : serviceLoader) {
            plugins.put(plugin.getId(), plugin);
        }
    }

    public void reloadPlugins() {
        plugins.clear();
        loadPlugins();
    }

    public IPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    public List<IPlugin> getAvailablePlugins() {
        return new ArrayList<>(plugins.values());
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
}
