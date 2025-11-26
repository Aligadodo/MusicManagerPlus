package plus.type;

public enum ActionType {
    REPLACE_TEXT("文本替换"), REPLACE_REGEX("正则替换"), PREPEND("前缀添加"), APPEND("后缀添加"), TO_LOWER("小写"), TO_UPPER("大写"), TRIM("去空格");
    private final String d;

    ActionType(String d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return d;
    }
}
