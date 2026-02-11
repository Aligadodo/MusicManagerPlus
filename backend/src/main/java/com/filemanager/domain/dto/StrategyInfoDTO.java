package com.filemanager.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 策略信息DTO
 * 用于传输策略的基本信息和配置字段
 */
public class StrategyInfoDTO {

    private String id;
    private String name;
    private String description;
    private String version;
    private boolean enabled;
    private List<ConfigFieldDTO> configFields;
    private List<PreconditionGroupDTO> preconditionGroups;
    private String pipelineId;

    public StrategyInfoDTO() {
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty("enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty("enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @JsonProperty("configFields")
    public List<ConfigFieldDTO> getConfigFields() {
        return configFields;
    }

    @JsonProperty("configFields")
    public void setConfigFields(List<ConfigFieldDTO> configFields) {
        this.configFields = configFields;
    }

    @JsonProperty("preconditionGroups")
    public List<PreconditionGroupDTO> getPreconditionGroups() {
        return preconditionGroups;
    }

    @JsonProperty("preconditionGroups")
    public void setPreconditionGroups(List<PreconditionGroupDTO> preconditionGroups) {
        this.preconditionGroups = preconditionGroups;
    }

    @JsonProperty("pipelineId")
    public String getPipelineId() {
        return pipelineId;
    }

    @JsonProperty("pipelineId")
    public void setPipelineId(String pipelineId) {
        this.pipelineId = pipelineId;
    }
}
