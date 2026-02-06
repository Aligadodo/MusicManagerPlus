package com.filemanager.plugin.impl.fileunzip.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum UnzipEngine implements PluginEnum {
    
    JAVA_BUILTIN("java_builtin", "Java 内置引擎", "Java Built-in", "Java内置解压引擎", "Java built-in unzip engine"),
    SEVEN_ZIP("seven_zip", "7-Zip 引擎", "7-Zip", "7-Zip解压引擎", "7-Zip unzip engine"),
    BANDIZIP("bandizip", "Bandizip 命令行工具", "Bandizip CLI", "Bandizip命令行工具", "Bandizip command line tool");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    UnzipEngine(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
        this.code = code;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
    }
    
    @Override
    public String getCode() {
        return code;
    }
    
    @Override
    public String getNameZh() {
        return nameZh;
    }
    
    @Override
    public String getNameEn() {
        return nameEn;
    }
    
    @Override
    public String getDescriptionZh() {
        return descriptionZh;
    }
    
    @Override
    public String getDescriptionEn() {
        return descriptionEn;
    }
    
    public boolean isJavaBuiltin() {
        return this == JAVA_BUILTIN;
    }
    
    public boolean isSevenZip() {
        return this == SEVEN_ZIP;
    }
    
    public boolean isBandizip() {
        return this == BANDIZIP;
    }
    
    public boolean requiresExternalExecutable() {
        return this == SEVEN_ZIP || this == BANDIZIP;
    }
    
    public static UnzipEngine fromCode(String code) {
        return PluginEnum.fromCode(code, UnzipEngine.class, JAVA_BUILTIN);
    }
}
