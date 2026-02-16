package com.filemanager.backend.mapper;

import com.filemanager.backend.entity.ConfigSnapshotPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ConfigSnapshotMapper {
    
    int insert(ConfigSnapshotPO snapshot);
    
    int update(ConfigSnapshotPO snapshot);
    
    int deleteBySnapshotId(@Param("snapshotId") String snapshotId);
    
    ConfigSnapshotPO selectBySnapshotId(@Param("snapshotId") String snapshotId);
    
    ConfigSnapshotPO selectById(@Param("snapshotId") String snapshotId);
    
    List<ConfigSnapshotPO> selectAll();
    
    List<ConfigSnapshotPO> selectBySnapshotType(@Param("snapshotType") String snapshotType);
    
    List<ConfigSnapshotPO> selectByType(@Param("snapshotType") String snapshotType);
    
    ConfigSnapshotPO getLatestSnapshotByType(@Param("snapshotType") String snapshotType);
    
    List<ConfigSnapshotPO> selectByIsTemplate(@Param("isTemplate") Boolean isTemplate);
    
    List<ConfigSnapshotPO> selectByPage(
        @Param("snapshotType") String snapshotType,
        @Param("isTemplate") Boolean isTemplate,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("sortBy") String sortBy,
        @Param("sortOrder") String sortOrder,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
    
    int countByPage(
        @Param("snapshotType") String snapshotType,
        @Param("isTemplate") Boolean isTemplate,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate
    );
    
    int updateConfigData(@Param("snapshotId") String snapshotId, @Param("configData") String configData);
    
    int deleteById(@Param("snapshotId") String snapshotId);
    
    int deleteOldSnapshots(@Param("cutoffDate") Date cutoffDate);
}
