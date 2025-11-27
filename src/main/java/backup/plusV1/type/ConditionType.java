package backup.plusV1.type;

public enum ConditionType {
    CONTAINS("包含文本"),
    NOT_CONTAINS("不包含文本"),
    STARTS_WITH("以...开头"),
    ENDS_WITH("以...结尾"),
    LENGTH_GT("长度大于"),
    LENGTH_LT("长度小于"),
    REGEX_MATCH("正则匹配");

    private final String desc;

    ConditionType(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return desc;
    }
}
