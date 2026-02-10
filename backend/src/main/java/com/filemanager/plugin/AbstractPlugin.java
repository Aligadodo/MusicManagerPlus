package com.filemanager.plugin;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractPlugin implements IPlugin {

    protected String id;
    protected String name;
    protected String description;
    protected String version;
    protected List<PluginParameterDTO> parameters;
    protected PluginConfigDTO defaultConfig;

    public AbstractPlugin(String id, String name, String description, String version) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.parameters = new ArrayList<>();
        this.defaultConfig = new PluginConfigDTO();
        initParameters();
        initDefaultConfig();
    }

    protected abstract void initParameters();
    protected abstract void initDefaultConfig();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        return parameters;
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    protected void addParameter(String name, String label, String type, Object defaultValue, String description, boolean required) {
        PluginParameterDTO parameter = new PluginParameterDTO(name, label, description, type, defaultValue, required);
        parameters.add(parameter);
    }

    protected void addParameter(String name, String label, String type, Object defaultValue, String description, boolean required, String[] options) {
        PluginParameterDTO parameter = new PluginParameterDTO(name, label, description, type, defaultValue, required);
        parameter.setOptions(options);
        parameters.add(parameter);
    }

    protected void addParameter(String name, String label, String type, Object defaultValue, String description, boolean required, List<String> options) {
        PluginParameterDTO parameter = new PluginParameterDTO(name, label, description, type, defaultValue, required);
        parameter.setOptions(options.toArray(new String[0]));
        parameters.add(parameter);
    }

    protected void setDefaultConfigValue(String key, Object value) {
        defaultConfig.setValue(key, value);
    }

    protected <T> T getConfigValue(PluginConfigDTO config, String key, T defaultValue) {
        if (config != null) {
            Object value = config.getValue(key);
            if (value != null) {
                return (T) value;
            }
        }
        return defaultValue;
    }

    protected ChangeRecord createChangeRecord(String originalPath, String newPath, String status) {
        ChangeRecord record = new ChangeRecord();
        record.setId("change-" + System.currentTimeMillis() + "-" + originalPath.hashCode());
        record.setOriginalName(originalPath);
        record.setNewName(newPath);
        record.setFilePath(originalPath);
        record.setChanged(!originalPath.equals(newPath));
        record.setStatus(status);
        return record;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        context.logInfo("Previewing plugin: " + name);
        List<ChangeRecord> changes = new ArrayList<>();
        
        for (int i = 0; i < filePaths.size(); i++) {
            String filePath = filePaths.get(i);
            context.updateProgress(i + 1, filePaths.size());
            
            if (context.isCancelled()) {
                context.logInfo("Preview cancelled");
                break;
            }
            
            ChangeRecord record = createPreviewRecord(filePath, config, context);
            if (record != null) {
                changes.add(record);
            }
        }
        
        context.logInfo("Preview completed: " + changes.size() + " changes");
        return changes;
    }

    protected abstract ChangeRecord createPreviewRecord(String filePath, PluginConfigDTO config, ExecutionContext context);

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        context.logInfo("Executing plugin: " + name);
        List<ChangeRecord> changes = new ArrayList<>();
        
        for (int i = 0; i < filePaths.size(); i++) {
            String filePath = filePaths.get(i);
            context.updateProgress(i + 1, filePaths.size());
            
            if (context.isCancelled()) {
                context.logInfo("Execution cancelled");
                break;
            }
            
            try {
                ChangeRecord record = executeForFile(filePath, config, context);
                if (record != null) {
                    changes.add(record);
                }
            } catch (Exception e) {
                context.logError("Error processing file " + filePath + ": " + e.getMessage());
                ChangeRecord errorRecord = createChangeRecord(filePath, filePath, "ERROR");
                changes.add(errorRecord);
            }
        }
        
        context.logInfo("Execution completed: " + changes.size() + " changes");
        return changes;
    }

    protected abstract ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context);

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs, 
        PluginConfigDTO config, 
        ExecutionContext context) {
        
        String filePath = currentRecord.getFilePath();
        ChangeRecord previewRecord = createPreviewRecord(filePath, config, context);
        
        if (previewRecord != null) {
            return Collections.singletonList(previewRecord);
        }
        
        return Collections.emptyList();
    }

    @Override
    public void execute(ChangeRecord record, 
        PluginConfigDTO config, 
        ExecutionContext context) throws Exception {
        
        String filePath = record.getFilePath();
        ChangeRecord executedRecord = executeForFile(filePath, config, context);
        
        if (executedRecord != null) {
            record.setStatus(executedRecord.getStatus());
            record.setNewPath(executedRecord.getNewPath());
            record.setNewName(executedRecord.getNewName());
            record.setChanged(executedRecord.isChanged());
            record.setFailReason(executedRecord.getFailReason());
        }
    }
}
