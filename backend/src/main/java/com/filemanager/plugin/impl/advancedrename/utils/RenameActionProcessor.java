package com.filemanager.plugin.impl.advancedrename.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameActionProcessor {
    
    public static String applyAction(String fileName, String type, Object value) {
        switch (type) {
            case "替换文本":
                @SuppressWarnings("unchecked")
                Map<String, Object> replaceValue = (Map<String, Object>) value;
                String searchText = (String) replaceValue.get("searchText");
                String replaceText = (String) replaceValue.get("replaceText");
                return fileName.replace(searchText, replaceText);
            case "添加前缀":
                return (String) value + fileName;
            case "添加后缀":
                return fileName + (String) value;
            case "删除文本":
                return fileName.replace((String) value, "");
            case "大小写转换":
                String caseType = (String) value;
                return applyCaseConversion(fileName, caseType);
            case "正则替换":
                @SuppressWarnings("unchecked")
                Map<String, Object> regexValue = (Map<String, Object>) value;
                String pattern = (String) regexValue.get("pattern");
                String replacement = (String) regexValue.get("replacement");
                Pattern regex = Pattern.compile(pattern);
                return regex.matcher(fileName).replaceAll(replacement);
            default:
                return fileName;
        }
    }
    
    private static String applyCaseConversion(String fileName, String caseType) {
        switch (caseType) {
            case "全部大写":
                return fileName.toUpperCase();
            case "全部小写":
                return fileName.toLowerCase();
            case "首字母大写":
                if (fileName.isEmpty()) return fileName;
                return fileName.substring(0, 1).toUpperCase() + fileName.substring(1);
            case "标题大小写":
                return toTitleCase(fileName);
            default:
                return fileName;
        }
    }
    
    private static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        
        return result.toString();
    }
}