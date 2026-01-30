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

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.base.IPersistableConfig;
import com.filemanager.strategy.base.PathSelectionComponent;
import javafx.scene.Node;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * NCM基础策略类
 * 提供通用的功能和方法，供具体的NCM功能策略继承
 */
public abstract class NcmBaseStrategy extends IAppStrategy implements IPersistableConfig {
    protected PathSelectionComponent pathSelection;
    
    public NcmBaseStrategy(String propPrefix) {
        // 创建路径选择组件，设置默认参数为"Convert - Cache"子目录
        java.util.Map<String, Object> defaults = new java.util.HashMap<>();
        defaults.put("outputDirMode", "子目录");
        defaults.put("path", "Convert - Cache");
        this.pathSelection = new PathSelectionComponent(propPrefix, defaults);
    }
    
    /**
     * 获取输出路径
     * @param file 源文件
     * @return 输出路径
     */
    protected String getOutputPath(File file) {
        return pathSelection.getOutputPath(file);
    }
    
    @Override
    public abstract Node getConfigNode();
    
    @Override
    public abstract void captureParams();
    
    @Override
    public abstract void saveConfig(Properties props);
    
    @Override
    public abstract void loadConfig(Properties props);
    
    @Override
    public abstract List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs);
    
    @Override
    public abstract void execute(ChangeRecord rec) throws Exception;
}
