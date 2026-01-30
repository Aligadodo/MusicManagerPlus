package com.filemanager.domain.entity;

public class RuleCondition {
    private String id;
    private String property;
    private String operator;
    private String value;

    public RuleCondition() {
    }

    public RuleCondition(String id, String property, String operator, String value) {
        this.id = id;
        this.property = property;
        this.operator = operator;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
