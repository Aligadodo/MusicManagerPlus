package com.filemanager.base;

import java.util.Properties;

public interface IAutoReloadAble {
    // 配置存取
    public abstract void saveConfig(Properties props);

    public abstract void loadConfig(Properties props);
}
