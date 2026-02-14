package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ConfigSnapshotPO {
    private String snapshotId;
    private String snapshotName;
    private String snapshotType;
    private String configData;
    private String description;
    private Boolean isTemplate;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
}
