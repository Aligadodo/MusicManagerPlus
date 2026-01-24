package com.filemanager.tool.unzip.engine;

/**
 * 压缩/解压引擎类型枚举
 * 包含了引擎的中文名称、推荐安装路径以及支持的功能描述
 */
public enum EngineType {

    SEVEN_ZIP(
            "7-Zip 引擎",
            "tools/7-Zip/7z.exe",
            "最推荐。支持 .7z, .rar, .zip, .tar, .gz 等几乎所有格式。支持极速多线程解压和加密压缩包。",
            "https://www.7-zip.org/"
    ),

    BANDIZIP(
            "Bandizip 命令行工具",
            "tools/Bandizip/bc.exe",
            "性能卓越。对国产软件生成的压缩包（如乱码处理）兼容性好。支持主流压缩格式。",
            "https://www.bandisoft.com/bandizip/"
    ),

    BUILT_IN(
            "Java 内置引擎",
            "无需外部依赖",
            "基于 Apache Commons Compress。支持 .zip, .tar, .gz 等基础格式。不支持加密文件和 RAR5。",
            "N/A"
    );

    private final String name;           // 中文友好名称
    private final String recommendPath;  // 推荐的存放/安装路径
    private final String description;    // 功能描述与支持类型
    private final String website;        // 官方下载地址

    EngineType(String name, String recommendPath, String description, String website) {
        this.name = name;
        this.recommendPath = recommendPath;
        this.description = description;
        this.website = website;
    }

    // --- Getter 方法 ---

    public String getName() {
        return name;
    }

    public String getRecommendPath() {
        return recommendPath;
    }

    public String getDescription() {
        return description;
    }

    public String getWebsite() {
        return website;
    }

    /**
     * 重写 toString 提供更详细的调试信息
     */
    @Override
    public String toString() {
        return String.format("[%s] 建议路径: %s | 描述: %s", name, recommendPath, description);
    }
}