package plusv2.model;

import plusv2.type.ActionType;

import java.util.List;

public  class RenameRule {
    public List<RuleCondition> conditions;
    public ActionType actionType;
    public String findStr;
    public String replaceStr;

    public RenameRule(List<RuleCondition> c, ActionType a, String f, String r){
        conditions=c; actionType=a; findStr=f; replaceStr=r;
    }

    public boolean matches(String s){
        if(conditions==null||conditions.isEmpty()) return true;
        for(RuleCondition c:conditions) if(!c.test(s)) return false;
        return true;
    }

    public String apply(String s) {
        String r = s;
        String v = replaceStr == null ? "" : replaceStr;
        try {
            switch (actionType) {
                case REPLACE_TEXT:
                    if (findStr!=null && !findStr.isEmpty()) r = s.replace(findStr, v);
                    break;
                case REPLACE_REGEX:
                    if (findStr!=null && !findStr.isEmpty()) r = s.replaceAll(findStr, v);
                    break;
                case PREPEND:
                    r = v + s;
                    break;
                case APPEND:
                    int d = s.lastIndexOf('.');
                    if (d>0) r = s.substring(0, d) + v + s.substring(d); else r = s + v;
                    break;
                case TO_LOWER: r = s.toLowerCase(); break;
                case TO_UPPER: r = s.toUpperCase(); break;
                case TRIM: r = s.trim(); break;

                // --- 优化：首字母前缀 (排除数字/符号) ---
                case ADD_LETTER_PREFIX:
                    String core = s;
                    // 忽略特定开头的词（如 The）
                    if (findStr != null && !findStr.isEmpty() && s.toLowerCase().startsWith(findStr.toLowerCase())) {
                        core = s.substring(findStr.length()).trim();
                    }

                    char first = getFirstValidChar(core);
                    // 仅处理字母或汉字，跳过数字和符号
                    if (isValidStartChar(first)) {
                        char pinyin = getPinyinFirstLetter(first);
                        if (pinyin != 0) {
                            String sep = v.isEmpty() ? " - " : v;
                            String prefix = Character.toUpperCase(pinyin) + sep;
                            // 避免重复添加
                            if (!s.startsWith(prefix)) {
                                r = prefix + s;
                            }
                        }
                    }
                    break;

                // --- 优化：智能清理 (默认词库) ---
                case CLEAN_NOISE:
                    // 1. 清理常见括号及内容 (智能模式)
                    r = r.replaceAll("(?i)\\[(mqms|flac|mp3|wav|cue|log|iso|ape|dsf|dff).*?\\]", ""); // 格式信息
                    r = r.replaceAll("[《》]", ""); // 书名号直接去

                    // 2. 清理用户定义的干扰词 (逗号分隔)
                    if (findStr != null && !findStr.isEmpty()) {
                        String[] words = findStr.split("[,，、]");
                        for (String w : words) {
                            if (!w.trim().isEmpty()) r = r.replace(w.trim(), "");
                        }
                    }

                    // 3. 规范化空格
                    r = r.replaceAll("\\s+", " ").trim();
                    r = r.replaceAll("^\\s*[-_.]+\\s*", "").replaceAll("\\s*[-_.]+\\s*$", "");
                    break;

                // --- 新增：批量移除 (简单暴力) ---
                case BATCH_REMOVE:
                    if (findStr != null && !findStr.isEmpty()) {
                        // 支持空格分隔多个词
                        String[] targets = findStr.split(" ");
                        for (String t : targets) {
                            if (!t.isEmpty()) r = r.replace(t, "");
                        }
                        r = r.trim();
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Rule error: " + e.getMessage());
        }
        return r;
    }

    public String getActionDesc() {
        if (actionType == ActionType.ADD_LETTER_PREFIX) return "首字母前缀 (分隔: '" + (replaceStr.isEmpty()?" - ":replaceStr) + "')";
        if (actionType == ActionType.CLEAN_NOISE) return "智能清理 (附加词: " + findStr + ")";
        if (actionType == ActionType.BATCH_REMOVE) return "批量移除 [" + findStr + "]";
        return actionType + " " + (findStr!=null?findStr:"") + " -> " + (replaceStr!=null?replaceStr:"");
    }

    private boolean isValidStartChar(char c) {
        // 排除数字 (48-57) 和常见符号
        return Character.isLetter(c) || (c >= 0x4e00 && c <= 0x9fa5);
    }

    private char getFirstValidChar(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c) || (c >= 0x4e00 && c <= 0x9fa5)) return c;
        }
        return 0;
    }

    // 简易拼音首字母映射 (优化版)
    private char getPinyinFirstLetter(char c) {
        if (c >= 'a' && c <= 'z') return (char)(c - 32);
        if (c >= 'A' && c <= 'Z') return c;
        // 如果是数字，直接返回 (但外部已经过滤了数字，这里保留逻辑以防万一)
        if (c >= '0' && c <= '9') return c;

        // 汉字范围处理 (GBK 简单映射，覆盖常用字)
        try {
            byte[] b = String.valueOf(c).getBytes("GBK");
            if (b.length < 2) return 0;
            int code = (b[0] & 0xFF) * 256 + (b[1] & 0xFF);
            if (code >= 45217 && code <= 45252) return 'A';
            if (code >= 45253 && code <= 45760) return 'B';
            if (code >= 45761 && code <= 46317) return 'C';
            if (code >= 46318 && code <= 46825) return 'D';
            if (code >= 46826 && code <= 47009) return 'E';
            if (code >= 47010 && code <= 47296) return 'F';
            if (code >= 47297 && code <= 47613) return 'G';
            if (code >= 47614 && code <= 48118) return 'H';
            if (code >= 48119 && code <= 49061) return 'J';
            if (code >= 49062 && code <= 49323) return 'K';
            if (code >= 49324 && code <= 49895) return 'L';
            if (code >= 49896 && code <= 50370) return 'M';
            if (code >= 50371 && code <= 50613) return 'N';
            if (code >= 50614 && code <= 50621) return 'O';
            if (code >= 50622 && code <= 50905) return 'P';
            if (code >= 50906 && code <= 51386) return 'Q';
            if (code >= 51387 && code <= 51445) return 'R';
            if (code >= 51446 && code <= 52217) return 'S';
            if (code >= 52218 && code <= 52697) return 'T';
            if (code >= 52698 && code <= 52979) return 'W';
            if (code >= 52980 && code <= 53688) return 'X';
            if (code >= 53689 && code <= 54480) return 'Y';
            if (code >= 54481 && code <= 55289) return 'Z';
        } catch (Exception e) {}
        return 0;
    }
}
