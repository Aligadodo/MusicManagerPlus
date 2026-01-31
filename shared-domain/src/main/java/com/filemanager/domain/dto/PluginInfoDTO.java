package com.filemanager.domain.dto;

public class PluginInfoDTO {
    private String id;
    private String name;
    private String description;
    private String version;
    private boolean enabled;
    private boolean internal;

    public PluginInfoDTO() {
    }

    public PluginInfoDTO(String id, String name, String description, String version, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }
}
