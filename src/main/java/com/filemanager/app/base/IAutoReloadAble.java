/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-12
 */
package com.filemanager.app.base;

import java.util.Properties;

/**
 * @author 28667
 */
public interface IAutoReloadAble {
    // 配置存取
    void saveConfig(Properties props);

    void loadConfig(Properties props);

    // 重置配置
    default void resetConfig() {
        // 默认实现为空，子类可选择性覆盖
    }

    // 样式重新加载
    void reload();
}
