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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PropertiesFactory {
    public static Properties createProperties(){
        return new Properties() {
            private final Map<Object, Object> map;
            {
                // 使用 LinkedHashMap 替代 Hashtable
                map = new LinkedHashMap<>();
            }

            @Override
            public Enumeration<Object> keys() {
                return Collections.enumeration(map.keySet());
            }

            @Override
            public synchronized Object get(Object key) {
                return map.get(key);
            }

            @Override
            public Object put(Object key, Object value) {
                return map.put(key, value);
            }

            @Override
            public void load(InputStream inStream) throws IOException {
                map.clear();
                super.load(inStream);
            }
        };
    }

}
