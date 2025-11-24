package plus.model;

import plus.type.RenameActionType;

import java.util.List;

public class RenameRule {
    public List<RuleCondition> conditions;
    public RenameActionType renameActionType;
    public String findStr;
    public String replaceStr;

    public RenameRule(List<RuleCondition> conditions, RenameActionType renameActionType, String findStr, String replaceStr) {
        this.conditions = conditions;
        this.renameActionType = renameActionType;
        this.findStr = findStr;
        this.replaceStr = replaceStr;
    }

    // 判断规则是否适用于文件名
    public boolean matches(String filename) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (RuleCondition c : conditions) {
            if (!c.test(filename)) return false;
        }
        return true;
    }

    // 执行替换逻辑
    public String apply(String filename) {
        String result = filename;
        String rVal = replaceStr == null ? "" : replaceStr;
        try {
            switch (renameActionType) {
                case REPLACE_TEXT:
                    if (findStr != null && !findStr.isEmpty())
                        result = filename.replace(findStr, rVal);
                    break;
                case REPLACE_REGEX:
                    if (findStr != null && !findStr.isEmpty())
                        result = filename.replaceAll(findStr, rVal);
                    break;
                case PREPEND:
                    result = rVal + filename;
                    break;
                case APPEND:
                    // 智能追加：如果有扩展名，加在扩展名之前
                    int dot = filename.lastIndexOf('.');
                    if (dot > 0) {
                        result = filename.substring(0, dot) + rVal + filename.substring(dot);
                    } else {
                        result = filename + rVal;
                    }
                    break;
                case TO_LOWER:
                    result = filename.toLowerCase();
                    break;
                case TO_UPPER:
                    result = filename.toUpperCase();
                    break;
                case TRIM:
                    result = filename.trim();
                    break;
            }
        } catch (Exception e) {
            // 忽略正则错误等，保持原名
            System.err.println("Rule execution failed: " + e.getMessage());
        }
        return result;
    }
}