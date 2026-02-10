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
 * 插件/策略注册表
 * 负责插件和策略的发现、加载和管理
 * 
 * 系统采用统一的插件-策略架构，所有策略类都实现了IPlugin接口
 * PluginRegistry通过ServiceLoader机制自动发现和加载所有实现了IPlugin接口的类
 * 
 * 支持两种类型的策略：
 * - 内部策略：位于backend/src/main/java/com/filemanager/plugin/impl/目录下，随应用一起打包
 * - 外部策略：独立打包为JAR文件，可动态加载
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
     * 加载内部策略
     * 通过ServiceLoader机制自动发现所有实现了IPlugin接口的内部策略类
     */
    private void loadInternalPlugins() {
        try {
            ServiceLoader<IPlugin> serviceLoader = ServiceLoader.load(IPlugin.class);
            for (IPlugin plugin : serviceLoader) {
                plugins.put(plugin.getId(), plugin);
                internalPlugins.add(plugin);
                logger.info("Loaded internal strategy: {} v{}", plugin.getName(), plugin.getVersion());
            }
        } catch (Exception e) {
            logger.error("Failed to load internal strategies: {}", e.getMessage());
        }
    }
    
    /**
     * 扫描外部策略目录
     * @param pluginDir 策略目录
     * @return 发现的策略路径列表
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
     * 加载外部策略
     * @param pluginDir 策略目录
     */
    public void loadExternalPlugins(String pluginDir) {
        List<String> pluginPaths = scanExternalPluginDirectory(pluginDir);
        for (String path : pluginPaths) {
            loadExternalPlugin(path);
        }
    }
    
    /**
     * 加载单个外部策略
     * @param pluginPath 策略路径
     */
    private void loadExternalPlugin(String pluginPath) {
        try {
            if (loadedPluginPaths.contains(pluginPath)) {
                logger.info("Strategy already loaded: {}", pluginPath);
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
                logger.info("Loaded external strategy: {} v{} from {}", 
                    plugin.getName(), plugin.getVersion(), pluginPath);
            }
        } catch (Exception e) {
            logger.error("Failed to load external strategy {}: {}", pluginPath, e.getMessage());
        }
    }
    
    /**
     * 重新加载所有策略
     */
    public void reloadPlugins() {
        plugins.clear();
        internalPlugins.clear();
        externalPlugins.clear();
        loadedPluginPaths.clear();
        loadInternalPlugins();
        logger.info("Reloaded all strategies");
    }
    
    /**
     * 重新加载外部策略
     */
    public void reloadExternalPlugins() {
        // 保存外部策略路径
        List<String> externalPaths = new ArrayList<>(loadedPluginPaths);
        
        // 清理外部策略
        for (IPlugin plugin : externalPlugins) {
            plugins.remove(plugin.getId());
        }
        externalPlugins.clear();
        loadedPluginPaths.clear();
        
        // 重新加载内部策略
        loadInternalPlugins();
        
        // 重新加载外部策略
        for (String path : externalPaths) {
            loadExternalPlugin(path);
        }
        
        logger.info("Reloaded external strategies");
    }
    
    /**
     * 获取所有可用策略
     * @return 策略列表
     */
    public List<IPlugin> getAvailablePlugins() {
        return new ArrayList<>(plugins.values());
    }
    
    /**
     * 获取内部策略
     * @return 内部策略列表
     */
    public List<IPlugin> getInternalPlugins() {
        return internalPlugins;
    }
    
    /**
     * 获取外部策略
     * @return 外部策略列表
     */
    public List<IPlugin> getExternalPlugins() {
        return externalPlugins;
    }
    
    /**
     * 根据ID获取策略
     * @param pluginId 策略ID
     * @return 策略实例
     */
    public IPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }
    
    /**
     * 注册策略
     * @param plugin 策略实例
     */
    public void registerPlugin(IPlugin plugin) {
        plugins.put(plugin.getId(), plugin);
        logger.info("Registered strategy: {} v{}", plugin.getName(), plugin.getVersion());
    }
    
    /**
     * 注销策略
     * @param pluginId 策略ID
     */
    public void unregisterPlugin(String pluginId) {
        IPlugin plugin = plugins.remove(pluginId);
        if (plugin != null) {
            internalPlugins.remove(plugin);
            externalPlugins.remove(plugin);
            logger.info("Unregistered strategy: {}", pluginId);
        }
    }
    
    /**
     * 获取已加载的策略路径
     * @return 策略路径列表
     */
    public List<String> getLoadedPluginPaths() {
        return loadedPluginPaths;
    }
    
    /**
     * 获取策略数量
     * @return 策略数量
     */
    public int getPluginCount() {
        return plugins.size();
    }
    
    /**
     * 检查策略是否存在
     * @param pluginId 策略ID
     * @return 是否存在
     */
    public boolean hasPlugin(String pluginId) {
        return plugins.containsKey(pluginId);
    }
}
