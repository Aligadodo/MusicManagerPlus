package com.filemanager.backend.mapper;

import com.filemanager.backend.entity.TaskOperationLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface TaskOperationLogMapper {
    
    int insert(TaskOperationLogPO log);
    
    int update(TaskOperationLogPO log);
    
    int deleteById(@Param("id") Long id);
    
    int deleteByTaskId(@Param("taskId") String taskId);
    
    TaskOperationLogPO selectById(@Param("id") Long id);
    
    List<TaskOperationLogPO> selectByTaskId(@Param("taskId") String taskId);
    
    List<TaskOperationLogPO> selectByTaskIdAndOperationType(
        @Param("taskId") String taskId,
        @Param("operationType") String operationType
    );
    
    List<TaskOperationLogPO> selectByTaskIdOrderByTime(
        @Param("taskId") String taskId,
        @Param("sortOrder") String sortOrder
    );
    
    List<TaskOperationLogPO> selectByPage(
        @Param("taskId") String taskId,
        @Param("operationType") String operationType,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("sortBy") String sortBy,
        @Param("sortOrder") String sortOrder,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
    
    int countByPage(
        @Param("taskId") String taskId,
        @Param("operationType") String operationType,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate
    );
}
