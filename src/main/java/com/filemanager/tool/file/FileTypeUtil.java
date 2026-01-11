/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.tool.file;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author 28667
 */
public class FileTypeUtil {
    public static List<String> MUSIC_TYPES = Arrays.asList("dsf", "dff", "dts", "ape", "wav", "flac",
            "m4a", "dfd", "tak", "tta", "wv", "mp3", "aac", "ogg", "wma");

    public static boolean isMusicFile(File file) {
        if (file.isDirectory()) {
            return false;
        }
        String ext = "";
        int dot = file.getName().lastIndexOf('.');
        if (dot > 0) {
            ext = file.getName().substring(dot);
        }
        return MUSIC_TYPES.contains(ext.toLowerCase());
    }

    /**
     * 获取文件名的最后一节类型名
     *
     * @param filename
     * @return
     */
    public static String getLastTypeStr(String filename) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            ext = filename.substring(dot);
        }
        return ext;
    }

    /**
     * 获取文件名的类型全名
     *
     * @param filename
     * @return
     */
    public static String getFullTypeStr(String filename) {
        String ext = "";
        int dot = filename.indexOf('.');
        if (dot > 0) {
            if (dot < 4  && getLastTypeStr(filename).length() < 6 && getLastTypeStr(filename).length() > 2) {
                return getLastTypeStr(filename);
            }
            ext = filename.substring(dot);
        }
        return ext;
    }

    /**
     * 获取文件名的不带类型全名
     *
     * @param filename
     * @return
     */
    public static String getFileNameNoneTypeStr(String filename) {
        int dot = filename.indexOf('.');
        if (dot > 0) {
          if (dot < 4  && getLastTypeStr(filename).length() < 6 && getLastTypeStr(filename).length() > 2) {
                return filename.substring(0, filename.lastIndexOf('.'));
            }
            return filename.substring(0, dot);
        }
        return filename;
    }

}
