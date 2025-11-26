package plus.model;

import plus.type.ActionType;

import java.util.List;

public class RenameRule {
    public List<RuleCondition> conditions;
    public ActionType actionType;
    public String findStr;
    public String replaceStr;

    public RenameRule(List<RuleCondition> c, ActionType a, String f, String r) {
        this.conditions = c;
        this.actionType = a;
        this.findStr = f;
        this.replaceStr = r;
    }

    public boolean matches(String s) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (RuleCondition c : conditions) if (!c.test(s)) return false;
        return true;
    }

    public String apply(String s) {
        String r = s;
        String v = replaceStr == null ? "" : replaceStr;
        try {
            switch (actionType) {
                case REPLACE_TEXT:
                    if (findStr != null && !findStr.isEmpty()) r = s.replace(findStr, v);
                    break;
                case REPLACE_REGEX:
                    if (findStr != null && !findStr.isEmpty()) r = s.replaceAll(findStr, v);
                    break;
                case PREPEND:
                    r = v + s;
                    break;
                case APPEND:
                    int d = s.lastIndexOf('.');
                    if (d > 0) r = s.substring(0, d) + v + s.substring(d);
                    else r = s + v;
                    break;
                case TO_LOWER:
                    r = s.toLowerCase();
                    break;
                case TO_UPPER:
                    r = s.toUpperCase();
                    break;
                case TRIM:
                    r = s.trim();
                    break;
            }
        } catch (Exception e) {
        }
        return r;
    }

    public String getActionDesc() {
        return actionType + (findStr != null && !findStr.isEmpty() ? " [" + findStr + "]" : "") + (replaceStr != null ? " -> " + replaceStr : "");
    }
}