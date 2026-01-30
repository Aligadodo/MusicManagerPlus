/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-30
 */
package com.filemanager.strategy.collection.validator;

import com.filemanager.model.ChangeRecord;
import com.filemanager.type.ScanTarget;

import java.io.File;
import java.util.List;

/**
 * 文件归类验证器
 * 负责验证文件是否符合归类条件
 */
public class FileCollectionValidator {
    private final ScanTarget targetType;
    private final String collectionSuffix;

    public FileCollectionValidator(ScanTarget targetType, String collectionSuffix) {
        this.targetType = targetType;
        this.collectionSuffix = collectionSuffix;
    }

    public boolean isFileTypeMatch(File file) {
        if (file == null) {
            return false;
        }

        switch (targetType) {
            case FILES_ONLY:
                return file.isFile();
            case FOLDERS_ONLY:
                return file.isDirectory();
            case ALL:
                return true;
            default:
                return false;
        }
    }

    public boolean isInCollectionFolder(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        File parentDir = file.getParentFile();
        if (parentDir == null) {
            return false;
        }

        return parentDir.getName().contains(collectionSuffix);
    }

    public boolean isCollectionFolder(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        return file.getName().contains(collectionSuffix);
    }

    public boolean isMostlySingleCollection(List<ChangeRecord> records) {
        if (records == null || records.isEmpty()) {
            return false;
        }

        int collectionCount = 0;
        for (ChangeRecord record : records) {
            File file = record.getFileHandle();
            if (file != null && isInCollectionFolder(file)) {
                collectionCount++;
            }
        }

        double ratio = (double) collectionCount / records.size();
        return ratio > 0.8;
    }
}
