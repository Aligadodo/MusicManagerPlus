package com.filemanager.domain.dto;

public class PreconditionDTO {
    private String id;
    private String field;
    private OperatorType operator;
    private Object value;
    private String description;

    public enum OperatorType {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_EQUALS,
        LESS_THAN_EQUALS,
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
        IS_EMPTY,
        HAS_SUBDIRECTORIES,
        DEPTH_GREATER_THAN,
        FILE_COUNT_GREATER_THAN,
        FORMAT_IN
    }

    public PreconditionDTO() {
    }

    public PreconditionDTO(String id, String field, OperatorType operator, Object value, String description) {
        this.id = id;
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.description = description;
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
