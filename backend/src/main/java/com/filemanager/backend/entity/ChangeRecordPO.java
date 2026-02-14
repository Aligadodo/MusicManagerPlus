package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ChangeRecordPO {
    private Long id;
    private String taskId;
    private String recordId;
    private String originalName;
    private String newName;
    private String filePath;
    private String newPath;
    private String operationType;
    private String status;
    private Boolean changed;
    private Boolean selected;
    private String failReason;
    private String extraParams;
    private Date analyzeTime;
    private Date executeTime;
    private Date createdAt;
}
