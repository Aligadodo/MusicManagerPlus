package com.filemanager.backend.mapper;

import com.filemanager.backend.entity.SystemConfigPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemConfigMapper {
    
    int insert(SystemConfigPO config);
    
    int update(SystemConfigPO config);
    
    int deleteByConfigKey(@Param("configKey") String configKey);
    
    SystemConfigPO selectByConfigKey(@Param("configKey") String configKey);
    
    List<SystemConfigPO> selectAll();
    
    List<SystemConfigPO> selectByCategory(@Param("category") String category);
    
    List<SystemConfigPO> selectByPage(
        @Param("category") String category,
        @Param("sortBy") String sortBy,
        @Param("sortOrder") String sortOrder,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
    
    int countByPage(
        @Param("category") String category
    );
}
