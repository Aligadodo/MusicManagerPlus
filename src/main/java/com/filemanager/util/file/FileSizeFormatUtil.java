/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.util.file;

import java.io.File;
import java.text.DecimalFormat;

/**
 * 主要用于各个地方展示文件大小
 * @author 28667
 */
public class FileSizeFormatUtil {
    public static String formatFileSize(File file) {
        if (file == null) {
            return "NaN";
        }
        long s = file.length();
        if (s <= 0) return "0";
        final String[] u = {"B", "KB", "MB", "GB"};
        int d = (int) (Math.log10(s) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(s / Math.pow(1024, d)) + " " + u[d];
    }
}
