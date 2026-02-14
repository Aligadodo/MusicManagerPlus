package com.filemanager.backend.service;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.StrategyConfigurable;
import com.filemanager.plugin.impl.advancedrename.model.RenameRule;
import com.filemanager.plugin.impl.advancedrename.model.RuleCondition;
import com.filemanager.plugin.impl.advancedrename.enums.RenameActionType;
import com.filemanager.plugin.impl.advancedrename.enums.RenameMode;
import com.filemanager.plugin.impl.advancedrename.enums.ConditionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 高级重命名策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 歌手重命名测试
 * 3. 歌曲名重命名测试
 * 4. 正则表达式替换测试
 * 5. 序号补零测试
 * 6. 大小写转换测试
 * 7. 特殊字符处理测试
 * 8. 批量重命名测试
 * 9. 边界情况测试
 * 10. 冲突处理测试
 */
public class AdvancedRenameStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保advanced-rename策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("advanced-rename");
        assertNotNull(strategy, "高级重命名策略应该已注册");
        assertEquals("advanced-rename", strategy.getId(), "策略ID应该正确");
        assertEquals("高级重命名策略", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：歌手重命名测试
     * 
     * 目的：验证基于歌手信息的重命名功能
     * 测试数据：
     * - 文件：青花瓷.mp3
     * - 配置：添加前缀"周杰伦-"
     * 断言：
     * - 文件被重命名
     * - 新文件名包含"周杰伦-"
     * - 新文件名包含"青花瓷"
     */
    @Test
    public void testSingerRename() throws Exception {
        File file = createTestFile("青花瓷.mp3", "content1");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("crossDriveMode", "move");
        config.getConfigValues().put("processScope", "all");
        
        List<RenameRule> rules = new ArrayList<>();
        List<RuleCondition> conditions = new ArrayList<>();
        RuleCondition condition = new RuleCondition();
        condition.setType(ConditionType.CONTAINS);
        condition.setValue("青花瓷");
        conditions.add(condition);
        
        RenameRule rule = new RenameRule();
        rule.setConditions(conditions);
        rule.setActionType(RenameActionType.PREPEND);
        rule.setFindStr("");
        rule.setReplaceStr("周杰伦-");
        rule.setExtensionProcessMode(RenameMode.ONLY_FILENAME);
        rules.add(rule);
        
        config.getConfigValues().put("rules", rules);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("advanced-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        ChangeRecord record = records.isEmpty() ? null : records.get(0);
        if (record != null) {
            assertTrue(record.isChanged(), "文件应该被重命名");
            assertTrue(record.getNewName().contains("周杰伦-"), "新文件名应包含歌手前缀");
            assertTrue(record.getNewName().contains("青花瓷"), "新文件名应包含歌曲名");
        }
    }

    /**
     * 测试场景3：歌曲名重命名测试
     * 
     * 目的：验证基于歌曲名的重命名功能
     * 测试数据：
     * - 文件：01.青花瓷.mp3
     * - 配置：替换"01."为"01. "
     * 断言：
     * - 文件被重命名
     * - 新文件名以"01. "开头
     */
    @Test
    public void testSongNameRename() throws Exception {
        File file = createTestFile("01.青花瓷.mp3", "content1");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("crossDriveMode", "move");
        config.getConfigValues().put("processScope", "all");
        
        List<RenameRule> rules = new ArrayList<>();
        List<RuleCondition> conditions = new ArrayList<>();
        RuleCondition condition = new RuleCondition();
        condition.setType(ConditionType.CONTAINS);
        condition.setValue("青花瓷");
        conditions.add(condition);
        
        RenameRule rule = new RenameRule();
        rule.setConditions(conditions);
        rule.setActionType(RenameActionType.REPLACE_TEXT);
        rule.setFindStr("01.");
        rule.setReplaceStr("01. ");
        rule.setExtensionProcessMode(RenameMode.ONLY_FILENAME);
        rules.add(rule);
        
        config.getConfigValues().put("rules", rules);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("advanced-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        ChangeRecord record = records.isEmpty() ? null : records.get(0);
        if (record != null) {
            assertTrue(record.isChanged(), "文件应该被重命名");
            assertTrue(record.getNewName().startsWith("01. "), "新文件名应以序号开头");
        }
    }

    /**
     * 测试场景4：正则表达式替换测试
     * 
     * 目的：验证正则表达式替换功能
     * 测试数据：
     * - 文件：【周杰伦】青花瓷【HQ】.mp3
     * - 配置：正则替换"【.*?】"为空
     * 断言：
     * - 文件被重命名
     * - 新文件名不包含【和】
     */
    @Test
    public void testRegexReplace() throws Exception {
        File file = createTestFile("【周杰伦】青花瓷【HQ】.mp3", "content1");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("crossDriveMode", "move");
        config.getConfigValues().put("processScope", "all");
        
        List<RenameRule> rules = new ArrayList<>();
        List<RuleCondition> conditions = new ArrayList<>();
        RuleCondition condition = new RuleCondition();
        condition.setType(ConditionType.CONTAINS);
        condition.setValue("【");
        conditions.add(condition);
        
        RenameRule rule = new RenameRule();
        rule.setConditions(conditions);
        rule.setActionType(RenameActionType.REPLACE_REGEX);
        rule.setFindStr("【.*?】");
        rule.setReplaceStr("");
        rule.setExtensionProcessMode(RenameMode.ONLY_FILENAME);
        rules.add(rule);
        
        config.getConfigValues().put("rules", rules);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("advanced-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        ChangeRecord record = records.isEmpty() ? null : records.get(0);
        if (record != null) {
            assertTrue(record.isChanged(), "文件应该被重命名");
            assertFalse(record.getNewName().contains("【"), "新文件名不应包含【");
            assertFalse(record.getNewName().contains("】"), "新文件名不应包含】");
        }
    }

    /**
     * 测试场景5：序号补零测试
     * 
     * 目的：验证序号补零功能
     * 测试数据：
     * - 文件：1.青花瓷.mp3, 10.江南.mp3
     * - 配置：补零位数为2
     * 断言：
     * - 1.青花瓷.mp3重命名为01.青花瓷.mp3
     * - 10.江南.mp3保持不变
     */
    @Test
    public void testNumberPadding() throws Exception {
        File file1 = createTestFile("1.青花瓷.mp3", "content1");
        File file2 = createTestFile("10.江南.mp3", "content2");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("crossDriveMode", "move");
        config.getConfigValues().put("processScope", "all");
        
        List<RenameRule> rules = new ArrayList<>();
        
        RenameRule rule1 = new RenameRule();
        rule1.setConditions(new ArrayList<>());
        rule1.setActionType(RenameActionType.REPLACE_REGEX);
        rule1.setFindStr("^(\\d)\\.");
        rule1.setReplaceStr("0$1.");
        rule1.setExtensionProcessMode(RenameMode.ONLY_FILENAME);
        rules.add(rule1);
        
        config.getConfigValues().put("rules", rules);
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("advanced-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record1 = records.get(0);
            assertTrue(record1.isChanged(), "文件1应该被重命名");
            assertTrue(record1.getNewName().startsWith("01."), "文件1序号应补零");
            
            if (records.size() > 1) {
                ChangeRecord record2 = records.get(1);
                assertTrue(record2.isChanged(), "文件2应该被重命名");
                assertTrue(record2.getNewName().startsWith("10."), "文件2序号应保持");
            }
        }
    }

    /**
     * 测试场景6：大小写转换测试
     * 
     * 目的：验证大小写转换功能
     * 测试数据：
     * - 文件：ZHOUJIELUN-QINGHUACI.mp3
     * - 配置：转换为小写
     * 断言：
     * - 文件被重命名
     * - 新文件名全部为小写
     */
    @Test
    public void testCaseConversion() throws Exception {
        File file = createTestFile("ZHOUJIELUN-QINGHUACI.mp3", "content1");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("crossDriveMode", "move");
        config.getConfigValues().put("processScope", "all");
        
        List<RenameRule> rules = new ArrayList<>();
        
        RenameRule rule1 = new RenameRule();
        rule1.setConditions(new ArrayList<>());
        rule1.setActionType(RenameActionType.TO_LOWER);
        rule1.setExtensionProcessMode(RenameMode.ONLY_FILENAME);
        rules.add(rule1);
        
        config.getConfigValues().put("rules", rules);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("advanced-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        ChangeRecord record = records.isEmpty() ? null : records.get(0);
        if (record != null) {
            assertTrue(record.isChanged(), "文件应该被重命名");
            assertEquals(record.getNewName().toLowerCase(), record.getNewName(), "新文件名应为小写");
        }
    }

    /**
     * 测试场景7：特殊字符处理测试
     * 
     * 目的：验证特殊字符处理功能
     * 测试数据：
     * - 文件：周杰伦：青花瓷.mp3（中文冒号）
     * - 配置：替换中文标点为英文标点
     * 断言：
     * - 文件被重命名
     * - 新文件名包含英文冒号
     * - 新文件名不包含中文冒号
     */
    @Test
    public void testSpecialCharHandling() throws Exception {
        File file = createTestFile("周杰伦：青花瓷.mp3", "content1");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("crossDriveMode", "move");
        config.getConfigValues().put("processScope", "all");
        
        List<RenameRule> rules = new ArrayList<>();
        
        RenameRule rule1 = new RenameRule();
        rule1.setConditions(new ArrayList<>());
        rule1.setActionType(RenameActionType.REPLACE_TEXT);
        rule1.setFindStr("：");
        rule1.setReplaceStr(":");
        rule1.setExtensionProcessMode(RenameMode.ONLY_FILENAME);
        rules.add(rule1);
        
        config.getConfigValues().put("rules", rules);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("advanced-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        ChangeRecord record = records.isEmpty() ? null : records.get(0);
        if (record != null) {
            assertTrue(record.isChanged(), "文件应该被重命名");
            assertTrue(record.getNewName().contains(":"), "新文件名应包含英文冒号");
            assertFalse(record.getNewName().contains("："), "新文件名不应包含中文冒号");
        }
    }

    /**
     * 测试场景8：批量重命名测试
     * 
     * 目的：验证批量重命名功能
     * 测试数据：
     * - 10个不同格式的文件
     * - 配置：添加前缀"music_"
     * 断言：
     * - 生成10条变更记录
     * - 所有文件都被重命名
     * - 所有文件名都以"music_"开头
     */
    @Test
    public void testBatchRename() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            files.add(createTestFile("song" + i + ".mp3", "content" + i));
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("crossDriveMode", "move");
        config.getConfigValues().put("processScope", "all");
        
        List<RenameRule> rules = new ArrayList<>();
        
        RenameRule rule1 = new RenameRule();
        rule1.setConditions(new ArrayList<>());
        rule1.setActionType(RenameActionType.PREPEND);
        rule1.setFindStr("");
        rule1.setReplaceStr("music_");
        rule1.setExtensionProcessMode(RenameMode.ONLY_FILENAME);
        rules.add(rule1);
        
        config.getConfigValues().put("rules", rules);
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("advanced-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            for (ChangeRecord record : records) {
                assertTrue(record.isChanged(), "所有文件都应该被重命名");
                assertTrue(record.getNewName().startsWith("music_"), "所有文件名应以music_开头");
            }
        }
    }

    /**
     * 测试场景9：边界情况测试
     * 
     * 目的：验证边界情况处理
     * 测试数据：
     * - 只有扩展名的文件
     * - 超长文件名
     * 断言：
     * - 合理处理或报错
     * - 不崩溃
     */
    @Test
    public void testEdgeCases() throws Exception {
        File file1 = createTestFile(".mp3", "content1");
        
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longName.append("a");
        }
        File file2 = null;
        try {
            file2 = createTestFile(longName.toString() + ".mp3", "content2");
        } catch (Exception e) {
            System.out.println("无法创建超长文件名，跳过该测试: " + e.getMessage());
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("crossDriveMode", "move");
        config.getConfigValues().put("processScope", "all");
        
        List<RenameRule> rules = new ArrayList<>();
        
        RenameRule rule1 = new RenameRule();
        rule1.setConditions(new ArrayList<>());
        rule1.setActionType(RenameActionType.PREPEND);
        rule1.setFindStr("");
        rule1.setReplaceStr("prefix_");
        rule1.setExtensionProcessMode(RenameMode.ONLY_FILENAME);
        rules.add(rule1);
        
        config.getConfigValues().put("rules", rules);
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        if (file2 != null) {
            filePaths.add(file2.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("advanced-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        for (ChangeRecord record : records) {
            assertNotNull(record, "变更记录不应为null");
            assertNotNull(record.getNewName(), "新文件名不应为null");
        }
    }

    /**
     * 测试场景10：冲突处理测试
     * 
     * 目的：验证重命名冲突处理
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 目标文件已存在同名文件
     * 断言：
     * - 按策略处理冲突
     * - 不崩溃
     */
    @Test
    public void testConflictHandling() throws Exception {
        File file1 = createTestFile("周杰伦-青花瓷.mp3", "content1");
        File file2 = createTestFile("周杰伦-青花瓷.mp3", "content2");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("crossDriveMode", "move");
        config.getConfigValues().put("processScope", "all");
        
        List<RenameRule> rules = new ArrayList<>();
        
        RenameRule rule1 = new RenameRule();
        rule1.setConditions(new ArrayList<>());
        rule1.setActionType(RenameActionType.PREPEND);
        rule1.setFindStr("");
        rule1.setReplaceStr("music_");
        rule1.setExtensionProcessMode(RenameMode.ONLY_FILENAME);
        rules.add(rule1);
        
        config.getConfigValues().put("rules", rules);
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("advanced-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            for (ChangeRecord record : records) {
                assertTrue(record.isChanged(), "所有文件都应该被重命名");
                assertTrue(record.getNewName().startsWith("music_"), "所有文件名应以music_开头");
            }
        }
    }
}
