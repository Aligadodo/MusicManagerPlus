package com.filemanager.domain.entity;

import java.util.List;

public class RuleConditionGroup {
    private String id;
    private String logicalOperator;
    private List<RuleCondition> conditions;
    private List<RuleConditionGroup> groups;

    public RuleConditionGroup() {
    }

    public RuleConditionGroup(String id, String logicalOperator, List<RuleCondition> conditions, List<RuleConditionGroup> groups) {
        this.id = id;
        this.logicalOperator = logicalOperator;
        this.conditions = conditions;
        this.groups = groups;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(String logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    public List<RuleCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<RuleCondition> conditions) {
        this.conditions = conditions;
    }

    public List<RuleConditionGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<RuleConditionGroup> groups) {
        this.groups = groups;
    }
}
