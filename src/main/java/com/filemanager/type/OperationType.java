package com.filemanager.type;

public enum OperationType {
    NONE("无", "未指定任何操作"),
    RENAME("重命名", "对文件进行简单的重命名操作"),
    ALBUM_RENAME("专辑重命名", "根据元数据对整个专辑文件夹进行重命名"),
    CUE_RENAME("CUE重命名", "修改CUE索引文件中的文件名"),
    MOVE("移动", "将文件移动到新的目录结构中"),
    CONVERT("转换", "转换文件编码或多媒体格式（如FLAC转MP3）"),
    SCRAPER("刮削", "从互联网获取并更新文件的元数据信息"),
    SPLIT("分割", "将整轨文件（如APE/FLAC）按CUE索引切分为单曲"),
    DELETE("删除", "将文件从磁盘中永久删除"),
    UNZIP("解压", "对压缩包文件进行解解压操作"),
    FIX_TYPE("修复类型", "修复文件的真正类型");

    public final String name;
    public final String desc;

    // 构造函数
    OperationType(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    // 获取操作的简写名称
    public String getName() {
        return name;
    }

    // 获取操作的详细描述
    public String getDesc() {
        return desc;
    }

    /**
     * 可选：根据枚举名称查找（防止异常，提供默认值）
     */
    public static OperationType fromString(String text) {
        for (OperationType b : OperationType.values()) {
            if (b.name().equalsIgnoreCase(text)) {
                return b;
            }
        }
        return NONE;
    }
}