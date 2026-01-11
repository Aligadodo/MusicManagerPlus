/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.util;

import java.io.*;

public class StreamUtils {

    public static int readBytes(FileInputStream fis, byte[] b) {
        try {
            return fis.read(b);
        } catch (IOException e) {
            ErrorUtils.error("No more bytes could read");
        }
        return -1;
    }

    public static void skipN(FileInputStream fis, long n) {
        try {
            fis.skip(n);
        } catch (IOException e) {
            ErrorUtils.error("No more bytes could skip");
        }
    }

    // Write bytes in File
    public static void writeBytes(File dest, byte[] data) {

        // Overwrite
        try {
            if (dest.exists()) {
                dest.delete();
            }
        } catch (SecurityException e) {
            ErrorUtils.error("No permission to overwrite dumped files", "Path: " + dest.getAbsolutePath());
        }

        try {
            FileOutputStream fos = new FileOutputStream(dest);
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            ErrorUtils.error("Write bytes failed", "Path: " + dest.getAbsolutePath());
        }
    }

}
