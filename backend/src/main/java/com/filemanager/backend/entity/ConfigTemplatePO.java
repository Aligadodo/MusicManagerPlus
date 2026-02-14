package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ConfigTemplatePO {
    private String templateId;
    private String templateName;
    private String templateType;
    private String snapshotId;
    private String category;
    private String tags;
    private String description;
    private Boolean isDefault;
    private Integer usageCount;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
}
