package plusv2.model;

import plusv2.type.ConditionType;

public class RuleCondition {
    ConditionType type;
    String value;

    public RuleCondition(ConditionType type, String value) {
        this.type = type;
        this.value = value;
    }

    public boolean test(String filename) {
        try {
            switch (type) {
                case CONTAINS:
                    return filename.contains(value);
                case NOT_CONTAINS:
                    return !filename.contains(value);
                case STARTS_WITH:
                    return filename.startsWith(value);
                case ENDS_WITH:
                    return filename.endsWith(value);
                case LENGTH_GT:
                    return filename.length() > Integer.parseInt(value);
                case LENGTH_LT:
                    return filename.length() < Integer.parseInt(value);
                case REGEX_MATCH:
                    return filename.matches(value);
                default:
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return type + " [" + value + "]";
    }
}
