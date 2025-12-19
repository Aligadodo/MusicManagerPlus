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
