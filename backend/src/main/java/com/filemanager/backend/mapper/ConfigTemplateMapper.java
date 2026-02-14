package com.filemanager.backend.mapper;

import com.filemanager.backend.entity.ConfigTemplatePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ConfigTemplateMapper {
    
    int insert(ConfigTemplatePO template);
    
    int update(ConfigTemplatePO template);
    
    int deleteByTemplateId(@Param("templateId") String templateId);
    
    ConfigTemplatePO selectByTemplateId(@Param("templateId") String templateId);
    
    List<ConfigTemplatePO> selectAll();
    
    List<ConfigTemplatePO> selectByTemplateType(@Param("templateType") String templateType);
    
    List<ConfigTemplatePO> selectByCategory(@Param("category") String category);
    
    List<ConfigTemplatePO> selectByIsDefault(@Param("isDefault") Boolean isDefault);
    
    List<ConfigTemplatePO> selectByPage(
        @Param("templateType") String templateType,
        @Param("category") String category,
        @Param("isDefault") Boolean isDefault,
        @Param("sortBy") String sortBy,
        @Param("sortOrder") String sortOrder,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
    
    int countByPage(
        @Param("templateType") String templateType,
        @Param("category") String category,
        @Param("isDefault") Boolean isDefault
    );
    
    int incrementUsageCount(@Param("templateId") String templateId);
}
