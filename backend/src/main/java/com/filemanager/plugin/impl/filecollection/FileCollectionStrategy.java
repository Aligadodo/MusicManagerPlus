package com.filemanager.plugin.impl.filecollection;

import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import com.filemanager.plugin.impl.filecollection.collection.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileCollectionStrategy extends AbstractConfigurableStrategy {

    // 核心组件
    private SimilarityCalculator similarityCalculator;
    private FileCluster fileCluster;
    private CollectionNameGenerator nameGenerator;
    private KeywordFilter keywordFilter;

    public FileCollectionStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "file-collection";
    }

    @Override
    public String getName() {
        return "文件智能归类";
    }

    @Override
    public String getDescription() {
        return "基于文件名相似度和特征将文件/文件夹归类到系列合集文件夹中";
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
        // 相似度阈值
        addConfigField("similarityThreshold", "相似度阈值", "number", 0.9, "文件相似度阈值（0.0-1.0）", false);
        
        // 合集文件夹格式
        addConfigField("collectionSuffix", "合集文件夹格式", "text", "【合集】", "合集文件夹的后缀格式", false);
        
        // 目标类型
        List<EnumOptionDTO> targetTypeOptions = new ArrayList<>();
        targetTypeOptions.add(createEnumOption("FOLDERS_ONLY", "仅文件夹"));
        targetTypeOptions.add(createEnumOption("FILES_ONLY", "仅文件"));
        targetTypeOptions.add(createEnumOption("BOTH", "两者都"));
        addEnumConfigField("targetType", "目标类型", "select", "FOLDERS_ONLY", "要处理的目标类型", false, targetTypeOptions);
        
        // 命名策略
        List<EnumOptionDTO> namingStrategyOptions = new ArrayList<>();
        namingStrategyOptions.add(createEnumOption("PRECISE", "精确模式"));
        namingStrategyOptions.add(createEnumOption("COMMON_PREFIX", "公共前缀"));
        namingStrategyOptions.add(createEnumOption("MOST_FREQUENT", "最频繁词"));
        namingStrategyOptions.add(createEnumOption("COMBINED", "组合模式"));
        addEnumConfigField("namingStrategy", "命名策略", "select", "PRECISE", "合集命名策略", false, namingStrategyOptions);
        
        // 必须包含关键词
        addConfigField("mustContainKeywords", "必须包含关键词", "text", "CD,系列,合集", "必须包含的关键词，用逗号分隔", false);
        
        // 不能包含关键词
        addConfigField("mustNotContainKeywords", "不能包含关键词", "text", "下载,Album,群星", "不能包含的关键词，用逗号分隔", false);
    }

    private EnumOptionDTO createEnumOption(String value, String label) {
        EnumOptionDTO option = new EnumOptionDTO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "similarityThreshold", 0.9);
        setConfigValue(config, "collectionSuffix", "【合集】");
        setConfigValue(config, "targetType", "FOLDERS_ONLY");
        setConfigValue(config, "namingStrategy", "PRECISE");
        setConfigValue(config, "mustContainKeywords", "CD,系列,合集");
        setConfigValue(config, "mustNotContainKeywords", "下载,Album,群星");
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs, 
        StrategyConfigDTO config, 
        ExecutionContext context) {
        
        File file = currentRecord.getFileHandle();
        String targetType = getConfigValue(config, "targetType", "FOLDERS_ONLY");
        double similarityThreshold = getConfigValue(config, "similarityThreshold", 0.9);
        
        context.logInfo("分析文件归类: " + file.getName() + ", 目标类型: " + targetType);
        
        Map<String, String> params = new HashMap<>();
        params.put("targetType", targetType);
        params.put("similarityThreshold", String.valueOf(similarityThreshold));
        
        ChangeRecord record = new ChangeRecord(
            currentRecord.getOriginalName(),
            currentRecord.getOriginalName(),
            currentRecord.getFileHandle(),
            true,
            file.getPath(),
            OperationType.MOVE,
            params,
            ExecStatus.PENDING
        );
        
        return Collections.singletonList(record);
    }

    @Override
    public void execute(ChangeRecord record, StrategyConfigDTO config, ExecutionContext context) throws Exception {
        File file = record.getFileHandle();
        String targetType = getConfigValue(config, "targetType", "FOLDERS_ONLY");
        
        if (!file.exists()) {
            context.logWarn("文件/目录不存在: " + file.getPath());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        try {
            context.logInfo("文件归类处理: " + file.getPath());
            record.setStatus(ExecStatus.SUCCESS.name());
        } catch (Exception e) {
            context.logError("文件归类失败: " + file.getPath() + ", 错误: " + e.getMessage());
            record.setStatus(ExecStatus.FAILED.name());
        }
    }

    private void initializeComponents(StrategyConfigDTO config) {
        double similarityThreshold = getConfigValue(config, "similarityThreshold", 0.9);

        similarityCalculator = new SimilarityCalculator(similarityThreshold);
        fileCluster = new FileCluster(similarityCalculator);
        nameGenerator = new CollectionNameGenerator();
        keywordFilter = KeywordFilter.builder().build();
    }
}
