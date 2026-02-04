package com.filemanager.plugin.impl.advancedrename.utils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameConditionChecker {
    
    public static boolean checkCondition(File file, String type, String operator, Object value) {
        String fileName = file.getName();
        String filePath = file.getPath();
        
        switch (type) {
            case "文件名匹配":
                return checkStringCondition(fileName, operator, (String) value);
            case "文件路径匹配":
                return checkStringCondition(filePath, operator, (String) value);
            case "文件大小":
                long fileSize = file.length();
                return checkNumberCondition(fileSize, operator, (Number) value);
            case "文件修改日期":
                long lastModified = file.lastModified();
                return checkNumberCondition(lastModified, operator, (Number) value);
            case "文件扩展名":
                String extension = getFileExtension(file);
                return checkStringCondition(extension, operator, (String) value);
            case "正则表达式":
                Pattern pattern = Pattern.compile((String) value);
                Matcher matcher = pattern.matcher(fileName);
                return matcher.matches();
            default:
                return false;
        }
    }
    
    private static boolean checkStringCondition(String text, String operator, String value) {
        switch (operator) {
            case "等于":
                return text.equals(value);
            case "包含":
                return text.contains(value);
            case "开始于":
                return text.startsWith(value);
            case "结束于":
                return text.endsWith(value);
            case "不等于":
                return !text.equals(value);
            case "不包含":
                return !text.contains(value);
            default:
                return false;
        }
    }
    
    private static boolean checkNumberCondition(long number, String operator, Number value) {
        long compareValue = value.longValue();
        switch (operator) {
            case "等于":
                return number == compareValue;
            case "大于":
                return number > compareValue;
            case "小于":
                return number < compareValue;
            case "大于等于":
                return number >= compareValue;
            case "小于等于":
                return number <= compareValue;
            default:
                return false;
        }
    }
    
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
}