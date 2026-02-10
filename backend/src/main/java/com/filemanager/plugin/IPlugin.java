package com.filemanager.plugin;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.io.File;
import java.util.List;

public interface IPlugin {
    String getId();
    String getName();
    String getDescription();
    String getVersion();
    PluginConfigDTO getDefaultConfig();
    List<PluginParameterDTO> getParameters();
    List<PreconditionGroupDTO> getDefaultPreconditionGroups();
    
    List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
    List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
    
    List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs, 
        PluginConfigDTO config, 
        ExecutionContext context);
    
    void execute(ChangeRecord record, 
        PluginConfigDTO config, 
        ExecutionContext context) throws Exception;
}
