package com.filemanager.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一配置管理器
 * 负责管理所有配置参数，实现配置的持久化和缓存
 */
@Component
public class ConfigManager {

    private static final String CONFIG_FILE_PATH = "config.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 配置缓存
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();

    // 线程池配置键
    public static final String KEY_PREVIEW_THREADS = "previewThreads";
    public static final String KEY_EXECUTION_THREADS = "executionThreads";
    public static final String KEY_THREAD_POOL_MODE = "threadPoolMode";

    // 运行配置键
    public static final String KEY_AUTO_REFRESH = "autoRefresh";
    public static final String KEY_PREVIEW_LIMIT = "previewLimit";
    public static final String KEY_EXECUTION_LIMIT = "executionLimit";

    // 扫描配置键
    public static final String KEY_RECURSION_MODE = "recursionMode";
    public static final String KEY_RECURSION_DEPTH = "recursionDepth";
    public static final String KEY_MIN_RECURSION_DEPTH = "minRecursionDepth";
    public static final String KEY_MAX_RECURSION_DEPTH = "maxRecursionDepth";

    // 过滤规则键
    public static final String KEY_SCAN_FILTER_LIST = "scanFilterList";

    // 文件类型筛选键
    public static final String KEY_FILE_TYPE_TREE = "fileTypeTree";
    public static final String KEY_CUSTOM_FILE_TYPES = "customFileTypes";

    // 默认值
    private static final Map<String, Object> DEFAULT_CONFIG = new HashMap<>();

    static {
        // 线程池配置默认值
        DEFAULT_CONFIG.put(KEY_PREVIEW_THREADS, 10);
        DEFAULT_CONFIG.put(KEY_EXECUTION_THREADS, 4);
        DEFAULT_CONFIG.put(KEY_THREAD_POOL_MODE, "GLOBAL");

        // 运行配置默认值
        DEFAULT_CONFIG.put(KEY_AUTO_REFRESH, true);
        DEFAULT_CONFIG.put(KEY_PREVIEW_LIMIT, 200);
        DEFAULT_CONFIG.put(KEY_EXECUTION_LIMIT, 1000);

        // 扫描配置默认值
        DEFAULT_CONFIG.put(KEY_RECURSION_MODE, "ALL");
        DEFAULT_CONFIG.put(KEY_RECURSION_DEPTH, 3);
        DEFAULT_CONFIG.put(KEY_MIN_RECURSION_DEPTH, 1);
        DEFAULT_CONFIG.put(KEY_MAX_RECURSION_DEPTH, 3);

        // 过滤规则默认值
        DEFAULT_CONFIG.put(KEY_SCAN_FILTER_LIST, new String[]{
            "*Convert*", "*Split*", "*System*", "*trash*", "*Temp*", "*tmp*", "*cache*", "*backup*"
        });

        // 文件类型筛选默认值
        DEFAULT_CONFIG.put(KEY_FILE_TYPE_TREE, new HashMap<>());
        DEFAULT_CONFIG.put(KEY_CUSTOM_FILE_TYPES, new String[]{});
    }

    /**
     * 初始化配置
     */
    public void init() {
        loadConfig();
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                FileReader reader = new FileReader(configFile);
                Map<String, Object> config = objectMapper.readValue(reader, Map.class);
                configCache.putAll(config);
                reader.close();
            } else {
                // 使用默认配置
                configCache.putAll(DEFAULT_CONFIG);
                saveConfig();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 使用默认配置
            configCache.putAll(DEFAULT_CONFIG);
        }
    }

    /**
     * 保存配置
     */
    public void saveConfig() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            FileWriter writer = new FileWriter(configFile);
            objectMapper.writeValue(writer, configCache);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取配置值
     */
    public <T> T getConfig(String key, Class<T> clazz) {
        if (configCache.containsKey(key)) {
            Object value = configCache.get(key);
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
        }
        // 返回默认值
        if (DEFAULT_CONFIG.containsKey(key)) {
            Object value = DEFAULT_CONFIG.get(key);
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
        }
        return null;
    }

    /**
     * 获取配置值，带默认值
     */
    public <T> T getConfig(String key, Class<T> clazz, T defaultValue) {
        T value = getConfig(key, clazz);
        return value != null ? value : defaultValue;
    }

    /**
     * 设置配置值
     */
    public void setConfig(String key, Object value) {
        configCache.put(key, value);
        saveConfig();
    }

    /**
     * 获取所有配置
     */
    public Map<String, Object> getAllConfig() {
        return new HashMap<>(configCache);
    }

    /**
     * 更新多个配置
     */
    public void updateConfig(Map<String, Object> config) {
        configCache.putAll(config);
        saveConfig();
    }

    /**
     * 重置配置到默认值
     */
    public void resetConfig() {
        configCache.clear();
        configCache.putAll(DEFAULT_CONFIG);
        saveConfig();
    }

    /**
     * 验证配置值
     */
    public boolean validateConfig(String key, Object value) {
        switch (key) {
            case KEY_PREVIEW_THREADS:
            case KEY_EXECUTION_THREADS:
                if (value instanceof Integer) {
                    int threads = (Integer) value;
                    return threads >= 1 && threads <= 16;
                }
                return false;
            case KEY_RECURSION_DEPTH:
            case KEY_MIN_RECURSION_DEPTH:
            case KEY_MAX_RECURSION_DEPTH:
                if (value instanceof Integer) {
                    int depth = (Integer) value;
                    return depth >= 1 && depth <= 10;
                }
                return false;
            case KEY_PREVIEW_LIMIT:
            case KEY_EXECUTION_LIMIT:
                if (value instanceof Integer) {
                    int limit = (Integer) value;
                    return limit >= 1 && limit <= 10000;
                }
                return false;
            case KEY_THREAD_POOL_MODE:
                if (value instanceof String) {
                    String mode = (String) value;
                    return "GLOBAL".equals(mode) || "ROOT_PATH".equals(mode);
                }
                return false;
            case KEY_RECURSION_MODE:
                if (value instanceof String) {
                    String mode = (String) value;
                    return "ALL".equals(mode) || "CURRENT".equals(mode) || "SPECIFIC".equals(mode) || "RANGE".equals(mode);
                }
                return false;
            default:
                return true;
        }
    }
}