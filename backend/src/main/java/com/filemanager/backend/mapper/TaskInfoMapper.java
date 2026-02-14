package com.filemanager.backend.mapper;

import com.filemanager.backend.entity.TaskInfoPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface TaskInfoMapper {
    
    int insert(TaskInfoPO taskInfo);
    
    int update(TaskInfoPO taskInfo);
    
    int deleteByTaskId(@Param("taskId") String taskId);
    
    TaskInfoPO selectByTaskId(@Param("taskId") String taskId);
    
    List<TaskInfoPO> selectAll();
    
    List<TaskInfoPO> selectByStatus(@Param("status") String status);
    
    List<TaskInfoPO> selectByDateRange(
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate
    );
    
    List<TaskInfoPO> selectByKeyword(@Param("keyword") String keyword);
    
    List<TaskInfoPO> selectByPage(
        @Param("status") String status,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("keyword") String keyword,
        @Param("sortBy") String sortBy,
        @Param("sortOrder") String sortOrder,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
    
    int countByPage(
        @Param("status") String status,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("keyword") String keyword
    );
    
    int updateStatus(@Param("taskId") String taskId, @Param("status") String status);
    
    int updateProgress(@Param("taskId") String taskId, @Param("progress") Double progress);
    
    int updateMessage(@Param("taskId") String taskId, @Param("message") String message);
}
