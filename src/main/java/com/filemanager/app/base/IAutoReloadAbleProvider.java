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

import java.util.List;

/**
 * 提供可自动重新加载的组件列表的接口
 */
public interface IAutoReloadAbleProvider {
    /**
     * 获取所有需要自动重新加载的组件列表
     * @return 可自动重新加载的组件列表
     */
    List<IAutoReloadAble> getAutoReloadNodes();
}