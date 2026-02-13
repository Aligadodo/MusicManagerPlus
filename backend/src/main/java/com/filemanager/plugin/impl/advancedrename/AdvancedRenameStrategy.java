package com.filemanager.plugin.impl.advancedrename;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.enums.common.CrossDriveMode;
import com.filemanager.plugin.impl.advancedrename.enums.ProcessScope;
import com.filemanager.plugin.impl.advancedrename.model.RenameRule;
import com.filemanager.plugin.impl.advancedrename.model.RuleCondition;
import com.filemanager.plugin.impl.advancedrename.enums.RenameActionType;
import com.filemanager.plugin.impl.advancedrename.enums.RenameMode;
import com.filemanager.plugin.impl.advancedrename.enums.ConditionType;
import com.filemanager.domain.enums.ScanTarget;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

public class AdvancedRenameStrategy extends AbstractConfigurableStrategy {

    public AdvancedRenameStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "advanced-rename";
    }

    @Override
    public String getName() {
        return "高级重命名策略";
    }

    @Override
    public String getDescription() {
        return "基于规则的高级文件重命名功能，支持多种条件和操作";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.ALL;
    }

    @Override
    protected void initConfigFields() {
        addEnumConfigField("crossDriveMode", "跨盘动作", "select", (Object) CrossDriveMode.MOVE.getCode(), 
            "跨盘操作时的动作", false, 
            getCrossDriveModeOptions());
        addEnumConfigField("processScope", "处理范围", "select", (Object) ProcessScope.ALL.getCode(), 
            "处理的文件类型范围", false, 
            getProcessScopeOptions());
        addConfigField("rules", "重命名规则", "list", (Object) new ArrayList<>(), 
            "重命名规则列表", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "crossDriveMode", (Object) CrossDriveMode.MOVE.getCode());
        setConfigValue(config, "processScope", (Object) ProcessScope.ALL.getCode());
        setConfigValue(config, "rules", (Object) new ArrayList<>());
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs,
        StrategyConfigDTO config,
        ExecutionContext context) {
        
        List<RenameRule> rules = getRules(config);
        String crossDriveMode = getConfigValue(config, "crossDriveMode", "move");
        String processScope = getConfigValue(config, "processScope", "all");
        
        boolean isCopy = "copy".equals(crossDriveMode);
        boolean pFile = "files".equals(processScope) || "all".equals(processScope);
        boolean pFolder = "folders".equals(processScope) || "all".equals(processScope);

        File currentVirtualFile = new File(currentRecord.getNewPath());
        boolean isDirectory = currentRecord.getFileHandle().isDirectory();

        if (isDirectory && !pFolder) {
            return Collections.emptyList();
        }
        if (!isDirectory && !pFile) {
            return Collections.emptyList();
        }

        boolean hasNumberPrefixRule = rules.stream().anyMatch(rule -> rule.getActionType() == RenameActionType.ADD_NUMBER_PREFIX);
        
        if (hasNumberPrefixRule && isDirectory) {
            processNumberPrefixRule(currentRecord, inputRecords, rootDirs, rules, isCopy, pFile, pFolder, context);
            return Collections.emptyList();
        }

        String currentName = currentVirtualFile.getName();
        boolean appliedAny = false;
        
        for (RenameRule rule : rules) {
            if (rule.matches(currentName)) {
                String temp = rule.apply(currentName, isDirectory);
                if (!temp.equals(currentName)) {
                    currentName = temp;
                    appliedAny = true;
                }
            }
        }

        if (!appliedAny) {
            return Collections.emptyList();
        }

        File parentDir = currentVirtualFile.getParentFile();
        File targetFile;
        String operationType;

        if (currentName.contains(File.separator)
                || (System.getProperty("os.name").toLowerCase().contains("win") && currentName.contains(":"))) {
            File potentialPath = new File(currentName);
            targetFile = potentialPath.isAbsolute() ? potentialPath : new File(parentDir, currentName);
            operationType = isCopy ? "COPY" : "MOVE";
        } else {
            targetFile = new File(parentDir, currentName);
            operationType = "RENAME";
        }

        currentRecord.setNewName(targetFile.getName());
        currentRecord.setNewPath(targetFile.getAbsolutePath());
        currentRecord.setChanged(true);
        currentRecord.setOperationType(operationType);

        if ("COPY".equals(operationType)) {
            currentRecord.getExtraParams().put("action", "copy");
        }

        return Collections.emptyList();
    }

    @Override
    public void execute(ChangeRecord record, 
        StrategyConfigDTO config, 
        ExecutionContext context) throws Exception {
        
        File sourceFile = record.getFileHandle();
        File targetFile = new File(record.getNewPath());
        
        if (sourceFile.equals(targetFile)) {
            return;
        }

        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        String operationType = record.getOperationType();
        if ("COPY".equals(operationType) && "copy".equals(record.getExtraParams().get("action"))) {
            if (sourceFile.isDirectory()) {
                copyDirectory(sourceFile, targetFile, context);
            } else {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyDirectory(File source, File target, ExecutionContext context) throws IOException {
        Files.walk(source.toPath()).forEach(sourcePath -> {
            File targetFile = target.toPath().resolve(source.toPath().relativize(sourcePath)).toFile();
            try {
                if (sourcePath.toFile().isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    if (!targetFile.getParentFile().exists()) {
                        targetFile.getParentFile().mkdirs();
                    }
                    Files.copy(sourcePath, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                context.logError("Error copying " + sourcePath + " to " + targetFile + ": " + e.getMessage());
            }
        });
    }

    private void processNumberPrefixRule(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> roots, 
                                         List<RenameRule> rules, boolean isCopy, boolean pFile, boolean pFolder,
                                         ExecutionContext context) {
        File dir = rec.getFileHandle();
        if (!dir.isDirectory()) {
            return;
        }

        processDirectoryRecursively(dir, inputRecords, rules, isCopy, pFile, pFolder, context);
    }

    private void processDirectoryRecursively(File dir, List<ChangeRecord> inputRecords, List<RenameRule> rules, 
                                              boolean isCopy, boolean pFile, boolean pFolder,
                                              ExecutionContext context) {
        List<ChangeRecord> fileRecords = new ArrayList<>();
        collectCurrentDirectoryFiles(dir, fileRecords, inputRecords);

        if (!fileRecords.isEmpty()) {
            fileRecords.sort((rec1, rec2) -> {
                File file1 = rec1.getFileHandle();
                File file2 = rec2.getFileHandle();
                
                String ext1 = getFileExtension(file1.getName());
                String ext2 = getFileExtension(file2.getName());
                int extCompare = ext1.compareTo(ext2);
                if (extCompare != 0) {
                    return extCompare;
                }
                
                return file1.getName().compareTo(file2.getName());
            });

            int counter = 1;
            for (ChangeRecord fileRec : fileRecords) {
                File file = fileRec.getFileHandle();
                String fileName = file.getName();
                String newName = generateNumberedName(fileName, counter);
                counter++;

                for (RenameRule rule : rules) {
                    if (rule.getActionType() != RenameActionType.ADD_NUMBER_PREFIX && rule.matches(newName)) {
                        newName = rule.apply(newName, file.isDirectory());
                    }
                }

                File parentDir = file.getParentFile();
                File targetFile = new File(parentDir, newName);
                String operationType = isCopy ? "COPY" : "RENAME";

                fileRec.setNewName(newName);
                fileRec.setNewPath(targetFile.getAbsolutePath());
                fileRec.setChanged(true);
                fileRec.setOperationType(operationType);

                if ("COPY".equals(operationType)) {
                    fileRec.getExtraParams().put("action", "copy");
                }
            }
        }

        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                processDirectoryRecursively(subDir, inputRecords, rules, isCopy, pFile, pFolder, context);
            }
        }
    }

    private void collectCurrentDirectoryFiles(File dir, List<ChangeRecord> fileRecords, List<ChangeRecord> inputRecords) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                for (ChangeRecord rec : inputRecords) {
                    if (rec.getFileHandle().equals(file)) {
                        fileRecords.add(rec);
                        break;
                    }
                }
            }
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    private String generateNumberedName(String fileName, int number) {
        String prefix = String.format("%02d. ", number);
        return prefix + fileName;
    }

    @SuppressWarnings("unchecked")
    private List<RenameRule> getRules(StrategyConfigDTO config) {
        Object rulesObj = getConfigValue(config, "rules", new ArrayList<>());
        if (rulesObj instanceof List) {
            return (List<RenameRule>) rulesObj;
        }
        return new ArrayList<>();
    }

    private List<EnumOptionDTO> getCrossDriveModeOptions() {
        List<EnumOptionDTO> options = new ArrayList<>();
        for (CrossDriveMode mode : CrossDriveMode.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(mode.getCode());
            option.setLabel(mode.getNameZh());
            option.setNameEn(mode.getNameEn());
            option.setDescriptionZh(mode.getDescriptionZh());
            option.setDescriptionEn(mode.getDescriptionEn());
            options.add(option);
        }
        return options;
    }

    private List<EnumOptionDTO> getProcessScopeOptions() {
        List<EnumOptionDTO> options = new ArrayList<>();
        for (ProcessScope scope : ProcessScope.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(scope.getCode());
            option.setLabel(scope.getNameZh());
            option.setNameEn(scope.getNameEn());
            option.setDescriptionZh(scope.getDescriptionZh());
            option.setDescriptionEn(scope.getDescriptionEn());
            options.add(option);
        }
        return options;
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setOperationType("RENAME");
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
            List<ChangeRecord> result = analyze(record, Collections.emptyList(), Collections.emptyList(), config, context);
            if (!result.isEmpty() || record.isChanged()) {
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