package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class TaskInfoPO {
    private String taskId;
    private String taskName;
    private String status;
    private String currentStage;
    private Double overallProgress;
    private String message;
    private Date createdAt;
    private Date updatedAt;
    private Date completedAt;
    private String configSnapshotId;
}
