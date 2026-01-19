package com.filemanager.tool.unzip;

import com.filemanager.tool.unzip.engine.BandizipEngine;
import com.filemanager.tool.unzip.engine.EngineType;
import com.filemanager.tool.unzip.engine.JavaBuiltInEngine;
import com.filemanager.tool.unzip.engine.SevenZipEngine;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UnarchiveFactory {

    /**
     * 方式 1：全自动探测
     */
    public static UnarchiveEngine getAvailableEngine() {
        String userDir = System.getProperty("user.dir");

        // 1. 定义 7-Zip 可能存在的路径（由近及远）
        List<String> s7zPaths = new ArrayList<>();
        s7zPaths.add(Paths.get(userDir, "tools", "7-Zip", "7z.exe").toString()); // 相对路径
        s7zPaths.add(Paths.get(userDir, "tools", "7z.exe").toString());
        s7zPaths.add("7z"); // 环境变量
        s7zPaths.add("C:\\Program Files\\7-Zip\\7z.exe"); // 标准 64 位
        s7zPaths.add("C:\\Program Files (x86)\\7-Zip\\7z.exe"); // 标准 32 位
        s7zPaths.add("/usr/bin/7z"); // Linux
        s7zPaths.add("/usr/local/bin/7z");

        for (String path : s7zPaths) {
            SevenZipEngine engine = new SevenZipEngine(path);
            if (engine.isAvailable()) return engine;
        }

        // 2. 定义 Bandizip 可能存在的路径
        List<String> bzPaths = new ArrayList<>();
        bzPaths.add(Paths.get(userDir, "tools", "Bandizip", "bc.exe").toString());
        bzPaths.add("bc.exe");
        bzPaths.add("C:\\Program Files\\Bandizip\\bc.exe");
        bzPaths.add("C:\\Program Files (x86)\\Bandizip\\bc.exe");

        for (String path : bzPaths) {
            BandizipEngine engine = new BandizipEngine(path);
            if (engine.isAvailable()) return engine;
        }

        // 3. 兜底内置引擎（始终可用）
        return new JavaBuiltInEngine();
    }

    /**
     * 方式 2：手动指定（如果指定了路径但不可用，则会报错）
     */
    public static UnarchiveEngine getSpecificEngine(EngineType type, String customPath) {
        UnarchiveEngine engine;
        switch (type) {
            case SEVEN_ZIP:
                engine = new SevenZipEngine(customPath != null ? customPath : "7z");
                break;
            case BANDIZIP:
                engine = new BandizipEngine(customPath != null ? customPath : "bc.exe");
                break;
            default:
                return new JavaBuiltInEngine();
        }

        if (!engine.isAvailable() && type != EngineType.BUILT_IN) {
            throw new RuntimeException("指定的引擎路径不存在或不可执行: " + customPath);
        }
        return engine;
    }
}