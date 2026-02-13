package com.filemanager.backend.integration;

import com.filemanager.domain.dto.ConfigFieldDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.backend.service.impl.StrategyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FrontendConfigFlowIntegrationTest {

    @Autowired
    private StrategyServiceImpl strategyService;

    private Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("frontend_test_");
    }

    @Test
    public void testCompleteFrontendConfigFlow() throws Exception {
        // 1. 获取策略列表（前端步骤1）
        List<StrategyInfoDTO> strategies = strategyService.getAvailableStrategies();
        assertNotNull(strategies, "策略列表不应为空");
        assertFalse(strategies.isEmpty(), "应该有可用的策略");
        
        // 验证所有策略都已注册
        List<String> strategyIds = new ArrayList<>();
        for (StrategyInfoDTO strategy : strategies) {
            strategyIds.add(strategy.getId());
            assertNotNull(strategy.getId(), "策略ID不应为空");
            assertNotNull(strategy.getName(), "策略名称不应为空");
            assertNotNull(strategy.getDescription(), "策略描述不应为空");
            assertNotNull(strategy.getConfigFields(), "配置字段不应为空");
        }
        
        // 验证所有14个策略都已注册
        assertTrue(strategyIds.contains("advanced-rename"), "应该包含高级重命名策略");
        assertTrue(strategyIds.contains("audio-converter"), "应该包含音频转换策略");
        assertTrue(strategyIds.contains("metadata-scraper"), "应该包含元数据抓取策略");
        assertTrue(strategyIds.contains("cue-splitter"), "应该包含CUE分轨策略");
        assertTrue(strategyIds.contains("file-migrate"), "应该包含文件迁移策略");
        assertTrue(strategyIds.contains("album-dir-normalize"), "应该包含专辑目录标准化策略");
        assertTrue(strategyIds.contains("file-unzip"), "应该包含文件解压策略");
        assertTrue(strategyIds.contains("file-collection"), "应该包含文件收集策略");
        assertTrue(strategyIds.contains("file-type-fix"), "应该包含文件类型修复策略");
        assertTrue(strategyIds.contains("cue-file-rename"), "应该包含CUE文件重命名策略");
        assertTrue(strategyIds.contains("ncm-integrated"), "应该包含网易云音乐集成策略");
        assertTrue(strategyIds.contains("track-number"), "应该包含音轨编号策略");
        assertTrue(strategyIds.contains("file-cleanup"), "应该包含文件清理策略");
        assertTrue(strategyIds.contains("file-rename"), "应该包含文件重命名策略");
    }

    @Test
    public void testGetStrategyInfo() {
        // 2. 获取策略信息（前端步骤2）
        StrategyInfoDTO strategy = strategyService.getStrategyInfo("file-collection");
        assertNotNull(strategy, "策略信息不应为空");
        assertEquals("file-collection", strategy.getId());
        assertEquals("文件收集插件", strategy.getName());
        assertNotNull(strategy.getConfigFields(), "配置字段不应为空");
        assertFalse(strategy.getConfigFields().isEmpty(), "应该有配置字段");
        
        // 验证配置字段
        for (ConfigFieldDTO field : strategy.getConfigFields()) {
            assertNotNull(field.getName(), "字段名称不应为空");
            assertNotNull(field.getLabel(), "字段标签不应为空");
            assertNotNull(field.getType(), "字段类型不应为空");
            assertNotNull(field.getDefaultValue(), "默认值不应为空");
            assertNotNull(field.getDescription(), "描述不应为空");
        }
    }

    @Test
    public void testGetStrategyConfig() {
        // 3. 获取策略配置（前端步骤3）
        StrategyConfigDTO config = strategyService.getStrategyConfig("file-collection");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        // 验证配置值
        assertTrue(config.getConfigValues().containsKey("targetDirectory"), 
            "应该包含targetDirectory配置");
        assertTrue(config.getConfigValues().containsKey("targetType"), 
            "应该包含targetType配置");
        assertTrue(config.getConfigValues().containsKey("similarityThreshold"), 
            "应该包含similarityThreshold配置");
    }

    @Test
    public void testUpdateStrategyConfig() {
        // 4. 更新策略配置（前端步骤4）
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new java.util.HashMap<>());
        config.getConfigValues().put("targetDirectory", tempDir.toString() + "/collected");
        config.getConfigValues().put("targetType", "FOLDERS_ONLY");
        config.getConfigValues().put("similarityThreshold", "0.95");
        
        boolean success = strategyService.updateStrategyConfig("file-collection", config);
        assertTrue(success, "配置更新应该成功");
        
        // 验证配置已更新
        StrategyConfigDTO updatedConfig = strategyService.getStrategyConfig("file-collection");
        assertEquals("0.95", updatedConfig.getConfigValues().get("similarityThreshold"), 
            "配置值应该已更新");
    }

    @Test
    public void testAnalyzeFiles() throws Exception {
        // 5. 分析文件（前端步骤5）
        File testFile = new File(tempDir.toFile(), "test.txt");
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("test content");
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new java.util.HashMap<>());
        config.getConfigValues().put("targetDirectory", tempDir.toString() + "/collected");
        config.getConfigValues().put("targetType", "FOLDERS_ONLY");
        config.getConfigValues().put("similarityThreshold", "0.9");
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-collection", filePaths, config);
        assertNotNull(records, "变更记录不应为空");
        assertFalse(records.isEmpty(), "应该有变更记录");
        
        // 验证变更记录
        ChangeRecord record = records.get(0);
        assertNotNull(record.getId(), "记录ID不应为空");
        assertNotNull(record.getOriginalName(), "原始名称不应为空");
        assertNotNull(record.getNewName(), "新名称不应为空");
        assertNotNull(record.getFilePath(), "文件路径不应为空");
        assertNotNull(record.getOperationType(), "操作类型不应为空");
        assertNotNull(record.getStatus(), "状态不应为空");
    }

    @Test
    public void testExecuteStrategy() throws Exception {
        // 6. 执行策略（前端步骤6）
        File testFile = new File(tempDir.toFile(), "test.txt");
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("test content");
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new java.util.HashMap<>());
        config.getConfigValues().put("targetDirectory", tempDir.toString() + "/collected");
        config.getConfigValues().put("targetType", "FOLDERS_ONLY");
        config.getConfigValues().put("similarityThreshold", "0.9");
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.executeStrategy("file-collection", filePaths, config);
        assertNotNull(records, "变更记录不应为空");
        assertFalse(records.isEmpty(), "应该有变更记录");
        
        // 验证执行结果
        ChangeRecord record = records.get(0);
        assertTrue(record.isChanged(), "文件应该已变更");
        assertEquals("SUCCESS", record.getStatus(), "状态应该是SUCCESS");
        
        // 验证文件已移动
        File targetFile = new File(tempDir.toString() + "/collected/test.txt");
        assertTrue(targetFile.exists(), "目标文件应该存在");
        assertFalse(testFile.exists(), "源文件应该不存在");
    }

    @Test
    public void testAllStrategiesConfigFields() {
        // 验证所有策略的配置字段都支持前端配置
        List<StrategyInfoDTO> strategies = strategyService.getAvailableStrategies();
        
        for (StrategyInfoDTO strategy : strategies) {
            assertNotNull(strategy.getConfigFields(), 
                "策略 " + strategy.getId() + " 应该有配置字段");
            
            // 验证每个配置字段
            for (ConfigFieldDTO field : strategy.getConfigFields()) {
                assertNotNull(field.getName(), 
                    "策略 " + strategy.getId() + " 的字段名称不应为空");
                assertNotNull(field.getLabel(), 
                    "策略 " + strategy.getId() + " 的字段标签不应为空");
                assertNotNull(field.getType(), 
                    "策略 " + strategy.getId() + " 的字段类型不应为空");
                
                // 验证字段类型是否支持
                assertTrue(
                    field.getType().equals("text") ||
                    field.getType().equals("number") ||
                    field.getType().equals("boolean") ||
                    field.getType().equals("select") ||
                    field.getType().equals("directory") ||
                    field.getType().equals("list"),
                    "策略 " + strategy.getId() + " 的字段类型 " + field.getType() + " 不支持"
                );
            }
        }
    }

    @Test
    public void testConfigFieldTypesSupport() {
        // 验证所有字段类型都有对应的前端支持
        List<StrategyInfoDTO> strategies = strategyService.getAvailableStrategies();
        
        for (StrategyInfoDTO strategy : strategies) {
            for (ConfigFieldDTO field : strategy.getConfigFields()) {
                String type = field.getType();
                
                // 验证每种字段类型都有对应的Builder
                switch (type) {
                    case "text":
                    case "number":
                    case "boolean":
                    case "select":
                    case "directory":
                    case "list":
                        // 这些类型都有对应的Builder
                        break;
                    default:
                        fail("不支持的字段类型: " + type + " 在策略 " + strategy.getId() + " 中");
                }
            }
        }
    }

    @Test
    public void testEnumOptionsSupport() {
        // 验证所有枚举选项都正确配置
        List<StrategyInfoDTO> strategies = strategyService.getAvailableStrategies();
        
        for (StrategyInfoDTO strategy : strategies) {
            for (ConfigFieldDTO field : strategy.getConfigFields()) {
                if ("select".equals(field.getType())) {
                    assertNotNull(field.getEnumOptions(), 
                        "策略 " + strategy.getId() + " 的字段 " + field.getName() + " 应该有枚举选项");
                    assertFalse(field.getEnumOptions().isEmpty(), 
                        "策略 " + strategy.getId() + " 的字段 " + field.getName() + " 的枚举选项不应为空");
                    
                    // 验证每个枚举选项
                    for (EnumOptionDTO option : field.getEnumOptions()) {
                        assertNotNull(option.getValue(), 
                            "策略 " + strategy.getId() + " 的枚举选项值不应为空");
                        assertNotNull(option.getLabel(), 
                            "策略 " + strategy.getId() + " 的枚举选项显示名称不应为空");
                    }
                }
            }
        }
    }
}
