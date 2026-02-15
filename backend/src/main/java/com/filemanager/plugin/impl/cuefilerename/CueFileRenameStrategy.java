package com.filemanager.plugin.impl.cuefilerename;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.plugin.util.FileRegexReplaceUtil;
import com.filemanager.plugin.util.FileStatisticInfo;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CueFileRenameStrategy extends AbstractConfigurableStrategy {

    public CueFileRenameStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "cue-file-rename";
    }

    @Override
    public String getName() {
        return "专辑文件重命名";
    }

    @Override
    public String getDescription() {
        return "为了解决cue文件在部分软件下，由于中文命名导致的无法加载的问题，支持统一调整cue及对应的音频文件命名。请同时扫描cue文件和音频文件，否则不生效。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FOLDERS_ONLY;
    }

    @Override
    protected void initConfigFields() {
        List<EnumOptionDTO> modeOptions = new ArrayList<>();
        modeOptions.add(createEnumOption("全自动修改", "全自动修改"));
        modeOptions.add(createEnumOption("仅修改音频文件", "仅修改音频文件"));
        modeOptions.add(createEnumOption("仅修改CUE文件", "仅修改CUE文件"));
        addEnumConfigField("mode", "修改模式", "select", "全自动修改", 
            "修改模式", false, modeOptions);
        addConfigField("fileName", "文件名前缀", "text", "album", 
            "文件名前缀", false);
    }

    private EnumOptionDTO createEnumOption(String value, String label) {
        EnumOptionDTO option = new EnumOptionDTO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "mode", "全自动修改");
        setConfigValue(config, "fileName", "album");
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        StrategyConfigDTO config,
        ExecutionContext context) {
        
        if (currentRecord.getFileHandle().isFile()) {
            return Collections.emptyList();
        }
        
        File[] filesUnderDir = currentRecord.getFileHandle().listFiles();
        if (filesUnderDir == null || filesUnderDir.length == 0) {
            return Collections.emptyList();
        }
        
        String pMode = getConfigValue(config, "mode", "全自动修改");
        String pFileName = getConfigValue(config, "fileName", "album");
        
        Map<String, File> cueFiles = Arrays.stream(filesUnderDir)
                .filter(file -> StringUtils.endsWithIgnoreCase(file.getName(), ".cue"))
                .filter(file -> FileRegexReplaceUtil.hasMatchingLine(file.getAbsolutePath()))
                .collect(Collectors.toMap(file -> FileStatisticInfo.create(file).oriName, Function.identity()));
        
        if (cueFiles.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, File> targetFiles = new HashMap<>();
        Arrays.stream(filesUnderDir)
                .forEach(file -> {
                    FileStatisticInfo statisticInfo = FileStatisticInfo.create(file);
                    if (!statisticInfo.isMusic()) {
                        return;
                    }
                    if (cueFiles.containsKey(statisticInfo.oriName)) {
                        targetFiles.put(statisticInfo.oriName, file);
                    }
                });
        
        int count = 0;
        List<String> cueNames = new ArrayList<>(targetFiles.keySet());
        cueNames.sort(String::compareToIgnoreCase);
        
        List<ChangeRecord> inputRecords = context.getInputRecords();
        
        for (String ky : cueNames) {
            ChangeRecord cueFileRecord = getTargetFile(cueFiles.get(ky), inputRecords);
            ChangeRecord musicFileRecord = getTargetFile(targetFiles.get(ky), inputRecords);
            
            if (cueFileRecord != null && musicFileRecord != null) {
                count++;
                FileStatisticInfo statisticInfo = FileStatisticInfo.create(musicFileRecord.getFileHandle());
                String fileNameRank = pFileName + "disk(" + count + ")";
                
                if (targetFiles.size() == 1) {
                    fileNameRank = pFileName;
                }
                
                String targetFileName = fileNameRank + "." + statisticInfo.type;
                musicFileRecord.setNewName(targetFileName);
                musicFileRecord.setNewPath(new File(currentRecord.getFileHandle(), targetFileName).getAbsolutePath());
                musicFileRecord.setChanged(true);
                musicFileRecord.setOperationType("CUE_RENAME");
                
                cueFileRecord.setNewName(fileNameRank + ".cue");
                cueFileRecord.setNewPath(new File(currentRecord.getFileHandle(), pFileName + ".cue").getAbsolutePath());
                cueFileRecord.setChanged(true);
                cueFileRecord.getExtraParams().put("cue_target_name", targetFileName);
                cueFileRecord.setOperationType("CUE_RENAME");
            }
        }
        
        return Collections.emptyList();
    }

    @Override
    public void execute(ChangeRecord record, 
        StrategyConfigDTO config, 
        ExecutionContext context) throws Exception {
        
        if (!"CUE_RENAME".equals(record.getOperationType())) {
            return;
        }
        
        File sourceFile = record.getFileHandle();
        File targetFile = new File(record.getNewPath());
        
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        
        Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        if (targetFile.getName().endsWith(".cue")) {
            String cueTargetName = record.getExtraParams().get("cue_target_name");
            if (cueTargetName != null) {
                FileRegexReplaceUtil.replaceWithAutoCharset(targetFile.getAbsolutePath(),
                        "FILE \"" + cueTargetName + "\" WAVE");
            }
        }
    }

    private ChangeRecord getTargetFile(File file, List<ChangeRecord> inputRecords) {
        for (ChangeRecord rec : inputRecords) {
            if (rec.getFileHandle().equals(file)) {
                return rec;
            }
        }
        return null;
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setOperationType("CUE_RENAME");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setFileHandle(sourceFile);
        
        try {
            if ("CUE_RENAME".equals(record.getOperationType())) {
                execute(record, config, context);
                record.setStatus("SUCCESS");
            } else {
                record.setStatus("SKIPPED");
            }
        } catch (Exception e) {
            context.logError("Error processing file " + filePath + ": " + e.getMessage());
            record.setStatus("ERROR");
            record.setFailReason(e.getMessage());
        }
        
        return record;
    }
}