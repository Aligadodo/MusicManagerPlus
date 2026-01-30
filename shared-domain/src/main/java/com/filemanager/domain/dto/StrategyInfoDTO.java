package com.filemanager.domain.dto;

import java.util.List;

public class StrategyInfoDTO {
    private String id;
    private String name;
    private String description;
    private List<ConfigFieldDTO> configFields;
    private boolean enabled;

    public StrategyInfoDTO() {
    }

    public StrategyInfoDTO(String id, String name, String description, List<ConfigFieldDTO> configFields, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.configFields = configFields;
        this.enabled = enabled;
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

    public List<ConfigFieldDTO> getConfigFields() {
        return configFields;
    }

    public void setConfigFields(List<ConfigFieldDTO> configFields) {
        this.configFields = configFields;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
