package com.filemanager.backend.entity;

import lombok.Data;
import java.util.Date;

@Data
public class TaskOperationLogPO {
    private Long id;
    private String taskId;
    private String operationType;
    private String operationStage;
    private String operator;
    private Date operationTime;
    private String operationDetail;
    private String result;
    private String errorMessage;
}
