package plus.type;

public enum RenameActionType {
    REPLACE_TEXT("文本替换"),
    REPLACE_REGEX("正则替换"),
    PREPEND("添加到开头"),
    APPEND("添加到结尾"),
    TO_LOWER("全部小写"),
    TO_UPPER("全部大写"),
    TRIM("去除首尾空格");

    private final String desc;

    RenameActionType(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return desc;
    }
}