/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.type.OperationType;

import java.util.ArrayList;
import java.util.List;

public class AppStrategyFactory {

    public static List<IAppStrategy> getAppStrategies() {
        List<IAppStrategy> strategyPrototypes = new ArrayList<IAppStrategy>();
        strategyPrototypes.add(new AdvancedRenameStrategy());
        strategyPrototypes.add(new AudioConverterStrategy());
        strategyPrototypes.add(new FileMigrateStrategy());
        strategyPrototypes.add(new AlbumDirNormalizeStrategy());
        strategyPrototypes.add(new TrackNumberStrategy());
        strategyPrototypes.add(new CueSplitterStrategy());
        strategyPrototypes.add(new MetadataScraperStrategy());
        strategyPrototypes.add(new FileCleanupStrategy());
        strategyPrototypes.add(new FileUnzipStrategy());
        strategyPrototypes.add(new CueFileRenameStrategy());
        strategyPrototypes.add(new FileTypeFixStrategy());
        return strategyPrototypes;
    }

    public static IAppStrategy findStrategyForOp(OperationType op, List<IAppStrategy> pipelineStrategies) {
        for (int i = pipelineStrategies.size() - 1; i >= 0; i--) {
            IAppStrategy s = pipelineStrategies.get(i);
            // 简单匹配，实际应更严谨
            if (op == OperationType.RENAME && s instanceof AdvancedRenameStrategy) return s;
            if (op == OperationType.ALBUM_RENAME && s instanceof AlbumDirNormalizeStrategy) return s;
            if (op == OperationType.CONVERT && s instanceof AudioConverterStrategy) return s;
            if (op == OperationType.SCRAPER && s instanceof MetadataScraperStrategy) return s;
            if (op == OperationType.UNZIP && s instanceof FileUnzipStrategy) return s;
            if (op == OperationType.MOVE && s instanceof FileMigrateStrategy) return s;
            if (op == OperationType.SPLIT && s instanceof CueSplitterStrategy) return s;
            if (op == OperationType.DELETE && s instanceof FileCleanupStrategy) return s;
            if (op == OperationType.CUE_RENAME && s instanceof CueFileRenameStrategy) return s;
            if (op == OperationType.FIX_TYPE && s instanceof FileTypeFixStrategy) return s;
        }
        return null;
    }
}
