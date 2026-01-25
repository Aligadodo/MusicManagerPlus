/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-25 
 */
package com.filemanager.strategy.ncm;

import javafx.scene.Node;

import java.util.Properties;

/**
 * 配置组件接口
 * 定义了组件的配置UI、参数捕获、配置保存和加载等能力
 */
public interface IConfigComponent {
    /**
     * 获取配置节点
     * @return 配置节点
     */
    Node getConfigNode();
    
    /**
     * 捕获参数
     */
    void captureParams();
    
    /**
     * 保存配置
     * @param props 属性对象
     */
    void saveConfig(Properties props);
    
    /**
     * 加载配置
     * @param props 属性对象
     */
    void loadConfig(Properties props);
}
