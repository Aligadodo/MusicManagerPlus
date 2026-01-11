/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.rule.rename;

import com.filemanager.model.RuleCondition;
import com.filemanager.tool.file.FileTypeUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RenameRule {
    public List<RuleCondition> conditions;
    public RenameActionType actionType;
    public String findStr;
    public String replaceStr;
    public RenameMode extensionProcessMode;

    public RenameRule(List<RuleCondition> c, RenameActionType a, String f, String r) {
        this(c, a, f, r, RenameMode.ONLY_FILENAME);
    }
    
    public RenameRule(List<RuleCondition> c, RenameActionType a, String f, String r, RenameMode m) {
        conditions = c;
        actionType = a;
        findStr = f;
        replaceStr = r;
        extensionProcessMode = m;
    }

    public boolean matches(String s) {
        if (conditions == null || conditions.isEmpty())
            return true;
        for (RuleCondition c : conditions)
            if (!c.test(new File(s)))
                return false; // Condition test needs File but here we check string logic inside RuleCondition
                              // if possible, or adapt.
        // Note: RuleCondition.test takes File. Here we are matching filename string.
        // Simplified for this context: RenameRule conditions usually match filename
        // string.
        // Adapting RuleCondition to check String s:
        for (RuleCondition c : conditions) {
            // Temporary hack: RuleCondition expects File, but we have String name.
            // We should construct a dummy file or overload test.
            // Assuming RuleCondition.test logic mainly checks getName().
            if (!c.test(new File(s)))
                return false;
        }
        return true;
    }

    public String apply(String s, boolean isDirectory) {
        String v = replaceStr == null ? "" : replaceStr;
        // 统一处理：分割文件名和扩展名
        String fileName = isDirectory ? s : FileTypeUtil.getFileNameNoneTypeStr(s);
        String typeName = isDirectory ? "" : FileTypeUtil.getFullTypeStr(s);
        String tempName = "";

        try {
            // 根据处理模式选择要处理的目标字符串
            switch (extensionProcessMode) {
                case ONLY_FILENAME:
                    tempName = fileName;
                    break;
                case ONLY_EXTENSION:
                    tempName = typeName;
                    break;
                case ALL:
                    tempName = s;
                    break;
            }

            // 对目标字符串应用操作
            switch (actionType) {
                case REPLACE_TEXT:
                    if (findStr != null && !findStr.isEmpty()) {
                        tempName = tempName.replace(findStr, v);
                    }
                    break;
                case REPLACE_REGEX:
                    if (findStr != null && !findStr.isEmpty()) {
                        tempName = tempName.replaceAll(findStr, v);
                    }
                    break;
                case PREPEND:
                    tempName = v + tempName;
                    break;
                case APPEND:
                    tempName = tempName + v;
                    break;
                case TO_LOWER:
                    tempName = tempName.toLowerCase();
                    break;
                case TO_UPPER:
                    tempName = tempName.toUpperCase();
                    break;
                case TRIM:
                    tempName = tempName.trim();
                    break;
                case BATCH_REMOVE:
                    if (findStr != null) {
                        for (String t : findStr.split(" ")) {
                            if (!t.isEmpty()) {
                                tempName = tempName.replace(t, "");
                            }
                        }
                        tempName = tempName.trim();
                    }
                    break;
                case CLEAN_NOISE:
                    tempName = tempName.replaceAll("(?i)\\[(mqms|flac|mp3|wav|cue|log|iso|ape|dsf|dff).*?\\]", "");
                    tempName = tempName.replaceAll("[《》]", "");
                    if (findStr != null) {
                        for (String w : findStr.split("[,，、]")) {
                            if (!w.trim().isEmpty()) {
                                tempName = tempName.replace(w.trim(), "");
                            }
                        }
                    }
                    tempName = tempName.replaceAll("\\s+", " ").trim();
                    break;
                case ADD_LETTER_PREFIX:
                    // 这个操作只影响文件名，不影响扩展名
                    if (extensionProcessMode != RenameMode.ONLY_EXTENSION) {
                        String core = tempName;
                        if (findStr != null && !findStr.isEmpty()
                                && tempName.toLowerCase().startsWith(findStr.toLowerCase())) {
                            core = tempName.substring(findStr.length()).trim();
                        }
                        char first = getFirstValidChar(core);
                        if (first != 0) {
                            char py = getPinyinFirstLetter(first);
                            if (py != 0) {
                                String sep = v.isEmpty() ? " - " : v;
                                String p = Character.toUpperCase(py) + sep;
                                if (!tempName.startsWith(p)) {
                                    tempName = p + tempName;
                                }
                            }
                        }
                    }
                    break;
                case CUT_PREFIX: {
                    int len = 0;
                    if (findStr != null && !findStr.isEmpty() && StringUtils.isNumeric(findStr)) {
                        len = Integer.parseInt(findStr);
                    }
                    if (len > 0 && tempName.length() > len) {
                        tempName = tempName.substring(len);
                    }
                    break;
                }
                case CUT_SUFFIX: {
                    int len = 0;
                    if (findStr != null && !findStr.isEmpty() && StringUtils.isNumeric(findStr)) {
                        len = Integer.parseInt(findStr);
                    }
                    if (len > 0 && tempName.length() > len) {
                        tempName = tempName.substring(0, tempName.length() - len);
                    }
                    break;
                }
                case KEEP_PREFIX: {
                    int len = 0;
                    if (findStr != null && !findStr.isEmpty() && StringUtils.isNumeric(findStr)) {
                        len = Integer.parseInt(findStr);
                    }
                    if (len > 0 && tempName.length() > len) {
                        tempName = tempName.substring(0, len);
                    }
                    break;
                }
                case KEEP_SUFFIX: {
                    int len = 0;
                    if (findStr != null && !findStr.isEmpty() && StringUtils.isNumeric(findStr)) {
                        len = Integer.parseInt(findStr);
                    }
                    if (len > 0 && tempName.length() > len) {
                        tempName = tempName.substring(tempName.length() - len);
                    }
                    break;
                }
                case REMOVE_PREFIX: {
                    if (findStr != null && !findStr.isEmpty() && tempName.startsWith(findStr)) {
                        tempName = tempName.substring(findStr.length());
                    }
                    break;
                }
                case REMOVE_SUFFIX: {
                    if (findStr != null && !findStr.isEmpty() && tempName.endsWith(findStr)) {
                        tempName = tempName.substring(0, tempName.length() - findStr.length());
                    }
                    break;
                }
                case TRADITIONAL_TO_SIMPLIFIED: {
                    try {
                        tempName = com.github.houbb.opencc4j.util.ZhConverterUtil.toSimple(tempName);
                    } catch (Exception e) {
                        // 如果转换失败，保留原始文件名
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // 发生异常时返回原始文件名
            return s;
        }

        // 根据处理模式返回结果
        switch (extensionProcessMode) {
            case ONLY_FILENAME:
                return tempName + typeName;
            case ONLY_EXTENSION:
                return fileName + tempName;
            case ALL:
            default:
                return tempName;
        }
    }

    public String getActionDesc() {
        return actionType + " " + (findStr != null ? findStr : "")
                + (replaceStr != null && !replaceStr.isEmpty() ? " -> " + replaceStr : "");
    }

    private char getFirstValidChar(String s) {
        for (char c : s.toCharArray())
            if (Character.isLetterOrDigit(c) || (c >= 0x4e00 && c <= 0x9fa5))
                return c;
        return 0;
    }

    private char getPinyinFirstLetter(char c) {
        // 处理英文大小写字母
        if (c >= 'a' && c <= 'z')
            return (char) (c - 32);
        if (c >= 'A' && c <= 'Z')
            return c;

        // 处理数字
        if (Character.isDigit(c))
            return c;

        try {
            // 使用ICU4J的Transliterator将各种语言字符转换为拉丁字母
            com.ibm.icu.text.Transliterator transliterator = com.ibm.icu.text.Transliterator
                    .getInstance("Any-Latin; Latin-ASCII; Lower; Upper");
            String result = transliterator.transliterate(String.valueOf(c));

            // 提取第一个有效字符
            if (result != null && !result.isEmpty()) {
                for (char ch : result.toCharArray()) {
                    if (Character.isLetter(ch)) {
                        return ch;
                    }
                }
            }
        } catch (Exception e) {
            // 降级处理：使用原始方法处理中文字符
            try {
                byte[] b = String.valueOf(c).getBytes("GBK");
                if (b.length < 2)
                    return 0;
                int code = (b[0] & 0xFF) * 256 + (b[1] & 0xFF);
                if (code >= 45217 && code <= 45252)
                    return 'A';
                if (code >= 45253 && code <= 45760)
                    return 'B';
                if (code >= 45761 && code <= 46317)
                    return 'C';
                if (code >= 46318 && code <= 46825)
                    return 'D';
                if (code >= 46826 && code <= 47009)
                    return 'E';
                if (code >= 47010 && code <= 47296)
                    return 'F';
                if (code >= 47297 && code <= 47613)
                    return 'G';
                if (code >= 47614 && code <= 48118)
                    return 'H';
                if (code >= 48119 && code <= 49061)
                    return 'J';
                if (code >= 49062 && code <= 49323)
                    return 'K';
                if (code >= 49324 && code <= 49895)
                    return 'L';
                if (code >= 49896 && code <= 50370)
                    return 'M';
                if (code >= 50371 && code <= 50613)
                    return 'N';
                if (code >= 50614 && code <= 50621)
                    return 'O';
                if (code >= 50622 && code <= 50905)
                    return 'P';
                if (code >= 50906 && code <= 51386)
                    return 'Q';
                if (code >= 51387 && code <= 51445)
                    return 'R';
                if (code >= 51446 && code <= 52217)
                    return 'S';
                if (code >= 52218 && code <= 52697)
                    return 'T';
                if (code >= 52698 && code <= 52979)
                    return 'W';
                if (code >= 52980 && code <= 53688)
                    return 'X';
                if (code >= 53689 && code <= 54480)
                    return 'Y';
                if (code >= 54481 && code <= 55289)
                    return 'Z';
            } catch (Exception e1) {
            }
        }
        return 0;
    }
}