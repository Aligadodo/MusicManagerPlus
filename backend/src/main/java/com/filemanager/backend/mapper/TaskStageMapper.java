package com.filemanager.backend.mapper;

import com.filemanager.backend.entity.TaskStagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskStageMapper {
    
    int insert(TaskStagePO taskStage);
    
    int update(TaskStagePO taskStage);
    
    int deleteById(@Param("id") Long id);
    
    int deleteByTaskId(@Param("taskId") String taskId);
    
    TaskStagePO selectById(@Param("id") Long id);
    
    List<TaskStagePO> selectByTaskId(@Param("taskId") String taskId);
    
    List<TaskStagePO> selectByTaskIdAndStageType(
        @Param("taskId") String taskId,
        @Param("stageType") String stageType
    );
    
    List<TaskStagePO> selectByTaskIdAndStatus(
        @Param("taskId") String taskId,
        @Param("status") String status
    );
    
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    int updateDuration(@Param("id") Long id, @Param("duration") Long duration);
}
