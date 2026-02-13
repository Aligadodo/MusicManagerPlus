package com.filemanager.backend.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 路径解析工具类
 * 提供跨系统兼容的路径解析功能
 */
public class PathResolver {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String APP_NAME = "MusicManagerPlus";

    /**
     * 获取用户配置目录（跨系统兼容）
     */
    public static String getUserConfigDir() {
        if (OS_NAME.contains("win")) {
            return Paths.get(USER_HOME, "AppData", "Roaming", APP_NAME).toString();
        } else if (OS_NAME.contains("mac")) {
            return Paths.get(USER_HOME, "Library", "Application Support", APP_NAME).toString();
        } else {
            return Paths.get(USER_HOME, "." + APP_NAME.toLowerCase()).toString();
        }
    }

    /**
     * 获取应用程序根目录
     */
    public static String getAppRootDir() {
        String jarPath = PathResolver.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File jarFile = new File(jarPath);
        
        if (jarFile.isFile()) {
            return jarFile.getParent();
        } else {
            return System.getProperty("user.dir");
        }
    }

    /**
     * 获取当前工作目录
     */
    public static String getWorkingDir() {
        return System.getProperty("user.dir");
    }

    /**
     * 获取主题目录的候选路径列表（按优先级排序）
     * 优先级从高到低：用户配置目录 > 应用程序目录 > 当前工作目录
     */
    public static List<String> getThemeDirCandidates() {
        List<String> candidates = new ArrayList<>();
        
        candidates.add(Paths.get(getUserConfigDir(), "themes").toString());
        candidates.add(Paths.get(getAppRootDir(), "themes").toString());
        candidates.add(Paths.get(getWorkingDir(), "themes").toString());
        
        return candidates;
    }

    /**
     * 获取配置文件的候选路径列表（按优先级排序）
     */
    public static List<String> getConfigFileCandidates() {
        List<String> candidates = new ArrayList<>();
        
        candidates.add(Paths.get(getUserConfigDir(), "config.json").toString());
        candidates.add(Paths.get(getAppRootDir(), "config.json").toString());
        candidates.add(Paths.get(getWorkingDir(), "config.json").toString());
        
        return candidates;
    }

    /**
     * 查找第一个存在的路径
     */
    public static String findFirstExistingPath(List<String> candidates) {
        for (String path : candidates) {
            File file = new File(path);
            if (file.exists()) {
                return path;
            }
        }
        return null;
    }

    /**
     * 查找第一个存在的目录
     */
    public static String findFirstExistingDirectory(List<String> candidates) {
        for (String path : candidates) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                return path;
            }
        }
        return null;
    }

    /**
     * 确保目录存在，如果不存在则创建
     */
    public static File ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 获取默认主题存储目录（用户配置目录）
     */
    public static String getDefaultThemesStorageDir() {
        return Paths.get(getUserConfigDir(), "themes", "default").toString();
    }

    /**
     * 获取自定义主题存储目录（用户配置目录）
     */
    public static String getCustomThemesStorageDir() {
        return Paths.get(getUserConfigDir(), "themes", "custom").toString();
    }

    /**
     * 获取主题预设查找路径列表（按优先级排序）
     * 用于查找系统预设主题
     */
    public static List<String> getThemePresetSearchPaths() {
        List<String> paths = new ArrayList<>();
        
        paths.add(Paths.get(getAppRootDir(), "themes", "default").toString());
        paths.add(Paths.get(getWorkingDir(), "themes", "default").toString());
        paths.add(Paths.get(getUserConfigDir(), "themes", "default").toString());
        
        return paths;
    }

    /**
     * 在多个路径中查找主题文件
     */
    public static File findThemeFile(String themeId, List<String> searchPaths) {
        for (String path : searchPaths) {
            File themeFile = new File(path, themeId + ".json");
            if (themeFile.exists() && themeFile.isFile()) {
                return themeFile;
            }
        }
        return null;
    }

    /**
     * 获取所有主题文件（从多个路径合并）
     */
    public static List<File> findAllThemeFiles(List<String> searchPaths) {
        List<File> themeFiles = new ArrayList<>();
        List<String> foundIds = new ArrayList<>();
        
        for (String path : searchPaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
                if (files != null) {
                    for (File file : files) {
                        String themeId = file.getName().replace(".json", "");
                        if (!foundIds.contains(themeId)) {
                            themeFiles.add(file);
                            foundIds.add(themeId);
                        }
                    }
                }
            }
        }
        
        return themeFiles;
    }
}
