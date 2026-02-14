package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class TaskStagePO {
    private Long id;
    private String taskId;
    private String stageType;
    private String status;
    private Date startTime;
    private Date endTime;
    private Long duration;
    private Integer totalFiles;
    private Integer processedFiles;
    private Integer successCount;
    private Integer failedCount;
    private Integer changedFiles;
    private String statsJson;
}
