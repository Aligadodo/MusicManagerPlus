package com.filemanager.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件注册表
 * 负责插件的发现、加载和管理
 */
public class PluginRegistry {

    private static final Logger logger = LoggerFactory.getLogger(PluginRegistry.class);
    private static final PluginRegistry instance = new PluginRegistry();
    
    private final Map<String, IPlugin> plugins = new ConcurrentHashMap<>();
    private final List<IPlugin> internalPlugins = new ArrayList<>();
    private final List<IPlugin> externalPlugins = new ArrayList<>();
    private final List<String> loadedPluginPaths = new ArrayList<>();
    
    private PluginRegistry() {
        loadInternalPlugins();
    }
    
    public static PluginRegistry getInstance() {
        return instance;
    }
    
    /**
     * 加载内部插件
     */
    private void loadInternalPlugins() {
        try {
            ServiceLoader<IPlugin> serviceLoader = ServiceLoader.load(IPlugin.class);
            for (IPlugin plugin : serviceLoader) {
                plugins.put(plugin.getId(), plugin);
                internalPlugins.add(plugin);
                logger.info("Loaded internal plugin: {} v{}", plugin.getName(), plugin.getVersion());
            }
        } catch (Exception e) {
            logger.error("Failed to load internal plugins: {}", e.getMessage());
        }
    }
    
    /**
     * 扫描外部插件目录
     * @param pluginDir 插件目录
     * @return 发现的插件路径列表
     */
    public List<String> scanExternalPluginDirectory(String pluginDir) {
        List<String> pluginPaths = new ArrayList<>();
        File dir = new File(pluginDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    pluginPaths.add(jar.getAbsolutePath());
                }
            }
        }
        return pluginPaths;
    }
    
    /**
     * 加载外部插件
     * @param pluginDir 插件目录
     */
    public void loadExternalPlugins(String pluginDir) {
        List<String> pluginPaths = scanExternalPluginDirectory(pluginDir);
        for (String path : pluginPaths) {
            loadExternalPlugin(path);
        }
    }
    
    /**
     * 加载单个外部插件
     * @param pluginPath 插件路径
     */
    private void loadExternalPlugin(String pluginPath) {
        try {
            if (loadedPluginPaths.contains(pluginPath)) {
                logger.info("Plugin already loaded: {}", pluginPath);
                return;
            }
            
            File pluginFile = new File(pluginPath);
            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{pluginFile.toURI().toURL()},
                Thread.currentThread().getContextClassLoader()
            );
            
            ServiceLoader<IPlugin> serviceLoader = ServiceLoader.load(IPlugin.class, classLoader);
            for (IPlugin plugin : serviceLoader) {
                plugins.put(plugin.getId(), plugin);
                externalPlugins.add(plugin);
                loadedPluginPaths.add(pluginPath);
                logger.info("Loaded external plugin: {} v{} from {}", 
                    plugin.getName(), plugin.getVersion(), pluginPath);
            }
        } catch (Exception e) {
            logger.error("Failed to load external plugin {}: {}", pluginPath, e.getMessage());
        }
    }
    
    /**
     * 重新加载插件
     */
    public void reloadPlugins() {
        plugins.clear();
        internalPlugins.clear();
        externalPlugins.clear();
        loadedPluginPaths.clear();
        loadInternalPlugins();
        logger.info("Reloaded all plugins");
    }
    
    /**
     * 重新加载外部插件
     */
    public void reloadExternalPlugins() {
        // 保存外部插件路径
        List<String> externalPaths = new ArrayList<>(loadedPluginPaths);
        
        // 清理外部插件
        for (IPlugin plugin : externalPlugins) {
            plugins.remove(plugin.getId());
        }
        externalPlugins.clear();
        loadedPluginPaths.clear();
        
        // 重新加载内部插件
        loadInternalPlugins();
        
        // 重新加载外部插件
        for (String path : externalPaths) {
            loadExternalPlugin(path);
        }
        
        logger.info("Reloaded external plugins");
    }
    
    /**
     * 获取所有可用插件
     * @return 插件列表
     */
    public List<IPlugin> getAvailablePlugins() {
        return new ArrayList<>(plugins.values());
    }
    
    /**
     * 获取内部插件
     * @return 内部插件列表
     */
    public List<IPlugin> getInternalPlugins() {
        return internalPlugins;
    }
    
    /**
     * 获取外部插件
     * @return 外部插件列表
     */
    public List<IPlugin> getExternalPlugins() {
        return externalPlugins;
    }
    
    /**
     * 根据ID获取插件
     * @param pluginId 插件ID
     * @return 插件实例
     */
    public IPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }
    
    /**
     * 注册插件
     * @param plugin 插件实例
     */
    public void registerPlugin(IPlugin plugin) {
        plugins.put(plugin.getId(), plugin);
        logger.info("Registered plugin: {} v{}", plugin.getName(), plugin.getVersion());
    }
    
    /**
     * 注销插件
     * @param pluginId 插件ID
     */
    public void unregisterPlugin(String pluginId) {
        IPlugin plugin = plugins.remove(pluginId);
        if (plugin != null) {
            internalPlugins.remove(plugin);
            externalPlugins.remove(plugin);
            logger.info("Unregistered plugin: {}", pluginId);
        }
    }
    
    /**
     * 获取已加载的插件路径
     * @return 插件路径列表
     */
    public List<String> getLoadedPluginPaths() {
        return loadedPluginPaths;
    }
    
    /**
     * 获取插件数量
     * @return 插件数量
     */
    public int getPluginCount() {
        return plugins.size();
    }
    
    /**
     * 检查插件是否存在
     * @param pluginId 插件ID
     * @return 是否存在
     */
    public boolean hasPlugin(String pluginId) {
        return plugins.containsKey(pluginId);
    }
}
