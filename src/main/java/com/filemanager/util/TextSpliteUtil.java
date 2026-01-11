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

import cn.hutool.extra.tokenizer.TokenizerEngine;
import cn.hutool.extra.tokenizer.TokenizerUtil;

import java.util.LinkedHashSet;
import java.util.Set;

public class TextSpliteUtil {


    private static final TokenizerEngine engine = TokenizerUtil.createEngine();

    public static Set<String> split(String text){
        Set<String> set = new LinkedHashSet<>();
        engine.parse(text).forEach(word -> set.add(word.getText()));
        return set;
    }
}
