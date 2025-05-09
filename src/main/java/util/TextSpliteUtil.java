package util;

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
