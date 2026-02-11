package com.filemanager.domain.dto;

/**
 * 前置条件DTO
 * 用于存储和传输前置条件的信息
 */
public class PreconditionDTO {

    private String id;
    private String field;
    private String subField;
    private OperatorType operator;
    private Object value;
    private String description;

    public enum OperatorType {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN_OR_EQUAL,
        CONTAINS,
        NOT_CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        REGEX_MATCH,
        IN,
        NOT_IN,
        BETWEEN,
        LAST_DAYS,
        IS,
        IS_NOT,
        IS_EMPTY,
        IS_NOT_EMPTY,
        HAS_SUBDIRECTORIES,
        HAS_NO_SUBDIRECTORIES,
        DEPTH_GREATER_THAN,
        DEPTH_LESS_THAN,
        FILE_COUNT_GREATER_THAN,
        FILE_COUNT_LESS_THAN,
        FORMAT_IN,
        FORMAT_NOT_IN;

        public static OperatorType fromValue(String value) {
            if (value == null) {
                return null;
            }
            try {
                return OperatorType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

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

    public String getSubField() {
        return subField;
    }

    public void setSubField(String subField) {
        this.subField = subField;
    }

    public OperatorType getOperator() {
        return operator;
    }

    public void setOperator(OperatorType operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
