package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class SystemConfigPO {
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private String category;
    private Boolean isEncrypted;
    private Date createdAt;
    private Date updatedAt;
}
