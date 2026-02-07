package com.filemanager.domain.dto;

import java.util.List;

/**
 * 前置条件组DTO
 * 用于存储和传输前置条件组的信息
 */
public class PreconditionGroupDTO {

    private String id;
    private String name;
    private String description;
    private String logicOperator;
    private String logicType;
    private List<PreconditionDTO> preconditions;

    public PreconditionGroupDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogicOperator() {
        return logicOperator;
    }

    public void setLogicOperator(String logicOperator) {
        this.logicOperator = logicOperator;
    }

    public String getLogicType() {
        return logicType;
    }

    public void setLogicType(String logicType) {
        this.logicType = logicType;
    }

    public List<PreconditionDTO> getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(List<PreconditionDTO> preconditions) {
        this.preconditions = preconditions;
    }
}