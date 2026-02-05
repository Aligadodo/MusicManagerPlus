package com.filemanager.domain.dto;

import java.util.List;

public class PreconditionGroupDTO {
    private String id;
    private String name;
    private String description;
    private String logicType;
    private List<PreconditionDTO> preconditions;

    public PreconditionGroupDTO() {
    }

    public PreconditionGroupDTO(String id, String name, String description, String logicType, List<PreconditionDTO> preconditions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.logicType = logicType;
        this.preconditions = preconditions;
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
