/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-28
 */
package com.filemanager.strategy.duplicate;

import java.io.File;
import java.util.List;

/**
 * 文件去重策略接口
 * 定义了处理重复文件的基本方法
 */
public interface DuplicateStrategy {
    /**
     * 处理重复文件
     * @param duplicates 重复文件列表
     * @return 处理后的文件列表，第一个元素通常是要保留的文件
     */
    List<File> processDuplicates(List<File> duplicates);
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getName();
    
    /**
     * 获取策略描述
     * @return 策略描述
     */
    String getDescription();
}
