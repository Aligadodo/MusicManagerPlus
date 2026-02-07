package com.filemanager.domain.dto;

/**
 * 前置条件DTO
 * 用于存储和传输前置条件的信息
 */
public class PreconditionDTO {

    private String id;
    private String field;
    private String operator;
    private Object value;
    private String subField;
    private String description;

    public PreconditionDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getSubField() {
        return subField;
    }

    public void setSubField(String subField) {
        this.subField = subField;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 操作符类型常量
     */
    public static class OperatorType {
        public static final String EQUALS = "equals";
        public static final String NOT_EQUALS = "not_equals";
        public static final String CONTAINS = "contains";
        public static final String NOT_CONTAINS = "not_contains";
        public static final String STARTS_WITH = "startsWith";
        public static final String ENDS_WITH = "endsWith";
        public static final String GREATER_THAN = "greaterThan";
        public static final String LESS_THAN = "lessThan";
        public static final String GREATER_THAN_OR_EQUAL = "greaterThanOrEqual";
        public static final String LESS_THAN_OR_EQUAL = "lessThanOrEqual";
        public static final String BETWEEN = "between";
        public static final String IN = "in";
        public static final String NOT_IN = "notIn";
        public static final String IS_EMPTY = "isEmpty";
        public static final String IS_NOT_EMPTY = "isNotEmpty";
        public static final String MATCHES_REGEX = "regex";
        public static final String NOT_MATCHES_REGEX = "notRegex";
        public static final String IS_TRUE = "isTrue";
        public static final String IS_FALSE = "isFalse";
        public static final String IS_NULL = "isNull";
        public static final String IS_NOT_NULL = "isNotNull";
        public static final String IS = "is";
        public static final String IS_NOT = "isNot";
        public static final String FORMAT_IN = "formatIn";
        public static final String FORMAT_NOT_IN = "formatNotIn";
        public static final String LAST_DAYS = "lastDays";
        public static final String HAS_SUBDIRECTORIES = "hasSubdirectories";
        public static final String HAS_NO_SUBDIRECTORIES = "hasNoSubdirectories";
        public static final String DEPTH_GREATER_THAN = "depthGreaterThan";
        public static final String DEPTH_LESS_THAN = "depthLessThan";
        public static final String FILE_COUNT_GREATER_THAN = "fileCountGreaterThan";
        public static final String FILE_COUNT_LESS_THAN = "fileCountLessThan";
        // 兼容旧版本的常量
        public static final String GREATER_THAN_EQUALS = "greaterThanOrEqual";
        public static final String LESS_THAN_EQUALS = "lessThanOrEqual";
        public static final String REGEX_MATCH = "regex";
    }
}