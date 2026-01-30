/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-30
 */
package com.filemanager.strategy.base;

import java.util.Properties;

/**
 * 可持久化配置组件接口
 * 扩展了IConfigComponent，添加了配置持久化能力
 *
 * @author hrcao
 */
public interface IPersistableConfig extends IConfigComponent {
    /**
     * 保存配置
     *
     * @param props 属性对象
     */
    void saveConfig(Properties props);

    /**
     * 加载配置
     *
     * @param props 属性对象
     */
    void loadConfig(Properties props);
}
