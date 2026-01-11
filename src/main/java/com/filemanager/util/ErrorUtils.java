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

public class ErrorUtils {

    public static void error(String reason, String detail) {
        System.out.println("Error");
        System.out.println("=> " + reason);

        if (!detail.isEmpty()) {
            System.out.println("=> " + detail);
        }

        //System.exit(1);
    }

    public static void error(String reason) {
        error(reason, "");
    }

}
