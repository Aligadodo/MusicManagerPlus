package com.filemanager.backend.mapper;

import com.filemanager.backend.entity.ChangeRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ChangeRecordMapper {
    
    int insert(ChangeRecordPO changeRecord);
    
    int update(ChangeRecordPO changeRecord);
    
    int deleteById(@Param("id") Long id);
    
    int deleteByTaskId(@Param("taskId") String taskId);
    
    ChangeRecordPO selectById(@Param("id") Long id);
    
    List<ChangeRecordPO> selectByTaskId(@Param("taskId") String taskId);
    
    List<ChangeRecordPO> selectByTaskIdAndStatus(
        @Param("taskId") String taskId,
        @Param("status") String status
    );
    
    List<ChangeRecordPO> selectByTaskIdAndOperationType(
        @Param("taskId") String taskId,
        @Param("operationType") String operationType
    );
    
    List<ChangeRecordPO> selectByTaskIdAndChanged(
        @Param("taskId") String taskId,
        @Param("changed") Boolean changed
    );
    
    List<ChangeRecordPO> selectByPage(
        @Param("taskId") String taskId,
        @Param("status") String status,
        @Param("operationType") String operationType,
        @Param("changed") Boolean changed,
        @Param("searchFields") String searchFields,
        @Param("keyword") String keyword,
        @Param("sortBy") String sortBy,
        @Param("sortOrder") String sortOrder,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
    
    int countByPage(
        @Param("taskId") String taskId,
        @Param("status") String status,
        @Param("operationType") String operationType,
        @Param("changed") Boolean changed,
        @Param("searchFields") String searchFields,
        @Param("keyword") String keyword
    );
    
    int batchInsert(@Param("records") List<ChangeRecordPO> records);
    
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    int updateSelected(@Param("id") Long id, @Param("selected") Boolean selected);
}
