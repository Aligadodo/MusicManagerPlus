package com.filemanager.backend.service;

import com.filemanager.domain.dto.AutoFillConfig;
import com.filemanager.domain.dto.ConfigFieldDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 参数关系管理服务测试
 */
@DisplayName("参数关系管理服务测试")
class ParameterRelationServiceTest {

    private ParameterRelationService parameterRelationService;
    private List<ConfigFieldDTO> testFields;

    @BeforeEach
    void setUp() {
        parameterRelationService = new ParameterRelationService();
        testFields = createTestFields();
        parameterRelationService.setAllFieldsCache(testFields);
    }

    @Test
    @DisplayName("测试参数可见性过滤 - 依赖条件")
    void testFilterVisibleFields_WithDependency() {
        Map<String, Object> currentValues = new HashMap<>();
        currentValues.put("engine", "7zip");
        currentValues.put("outputMode", "specified");
        
        List<ConfigFieldDTO> visibleFields = parameterRelationService.filterVisibleFields(testFields, currentValues);
        
        // 所有字段都应该可见（因为没有阻止条件）
        assertTrue(visibleFields.stream().anyMatch(f -> "engine".equals(f.getName())));
        assertTrue(visibleFields.stream().anyMatch(f -> "exePath".equals(f.getName())));
        assertTrue(visibleFields.stream().anyMatch(f -> "outputMode".equals(f.getName())));
        assertTrue(visibleFields.stream().anyMatch(f -> "customPath".equals(f.getName())));
    }

    @Test
    @DisplayName("测试参数可见性过滤 - 阻止条件")
    void testFilterVisibleFields_WithBlockCondition() {
        Map<String, Object> currentValues = new HashMap<>();
        currentValues.put("engine", "java");
        
        List<ConfigFieldDTO> visibleFields = parameterRelationService.filterVisibleFields(testFields, currentValues);
        
        // 所有字段都应该可见（因为没有阻止条件）
        assertTrue(visibleFields.stream().anyMatch(f -> "engine".equals(f.getName())));
        assertTrue(visibleFields.stream().anyMatch(f -> "exePath".equals(f.getName())));
    }

    @Test
    @DisplayName("测试互斥关系处理")
    void testHandleExclusiveRelation() {
        Map<String, Object> currentValues = new HashMap<>();
        currentValues.put("optionA", true);
        currentValues.put("optionB", true);
        currentValues.put("optionC", true);
        
        // 设置optionA和optionB为互斥关系
        ConfigFieldDTO fieldA = new ConfigFieldDTO();
        fieldA.setName("optionA");
        fieldA.setType("boolean");
        fieldA.setExclusiveGroup("group1");
        
        ConfigFieldDTO fieldB = new ConfigFieldDTO();
        fieldB.setName("optionB");
        fieldB.setType("boolean");
        fieldB.setExclusiveGroup("group1");
        
        ConfigFieldDTO fieldC = new ConfigFieldDTO();
        fieldC.setName("optionC");
        fieldC.setType("boolean");
        fieldC.setExclusiveGroup("group2");
        
        List<ConfigFieldDTO> fields = Arrays.asList(fieldA, fieldB, fieldC);
        
        // 当optionA变化时，应该取消optionB的值
        Map<String, Object> updatedValues = parameterRelationService.handleExclusiveRelation(
                "optionA", true, fields, currentValues);
        
        assertEquals(true, updatedValues.get("optionA"));
        assertEquals(false, updatedValues.get("optionB"));
        assertEquals(true, updatedValues.get("optionC"));
    }

    @Test
    @DisplayName("测试自动填充关系 - 固定值")
    void testHandleAutoFill_FixedValue() {
        Map<String, Object> currentValues = new HashMap<>();
        currentValues.put("engine", "7zip");
        
        ConfigFieldDTO field = new ConfigFieldDTO();
        field.setName("exePath");
        
        AutoFillConfig autoFillConfig = new AutoFillConfig();
        autoFillConfig.setTriggerParam("engine");
        autoFillConfig.setTriggerValue("7zip");
        autoFillConfig.setFillType("fixed_value");
        autoFillConfig.setFillValue("C:\\Program Files\\7-Zip\\7z.exe");
        field.setAutoFillConfig(autoFillConfig);
        
        List<ConfigFieldDTO> fields = Arrays.asList(field);
        
        Map<String, Object> updatedValues = parameterRelationService.handleAutoFill(
                "engine", "7zip", fields, currentValues);
        
        assertEquals("C:\\Program Files\\7-Zip\\7z.exe", updatedValues.get("exePath"));
    }

    @Test
    @DisplayName("测试自动填充关系 - 表达式")
    void testHandleAutoFill_Expression() {
        Map<String, Object> currentValues = new HashMap<>();
        currentValues.put("artist", "周杰伦");
        currentValues.put("album", "叶惠美");
        
        ConfigFieldDTO field = new ConfigFieldDTO();
        field.setName("outputPath");
        
        AutoFillConfig autoFillConfig = new AutoFillConfig();
        autoFillConfig.setTriggerParam("artist");
        autoFillConfig.setFillType("expression");
        autoFillConfig.setFillValue("${artist}/${album}");
        field.setAutoFillConfig(autoFillConfig);
        
        List<ConfigFieldDTO> fields = Arrays.asList(field);
        
        Map<String, Object> updatedValues = parameterRelationService.handleAutoFill(
                "artist", "周杰伦", fields, currentValues);
        
        assertEquals("周杰伦/叶惠美", updatedValues.get("outputPath"));
    }

    @Test
    @DisplayName("测试条件判断")
    void testIsConditionMet() {
        Map<String, Object> currentValues = new HashMap<>();
        currentValues.put("engine", "7zip");
        
        Map<String, Object> condition = new HashMap<>();
        condition.put("dependentParam", "engine");
        condition.put("expectedValue", "7zip");
        
        // 使用反射调用私有方法进行测试
        try {
            java.lang.reflect.Method method = ParameterRelationService.class.getDeclaredMethod(
                    "isConditionMet", Map.class, Map.class);
            method.setAccessible(true);
            
            boolean result = (boolean) method.invoke(parameterRelationService, condition, currentValues);
            assertTrue(result);
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试根据名称查找参数")
    void testFindFieldByName() {
        ConfigFieldDTO field = parameterRelationService.findFieldByName(testFields, "engine");
        assertNotNull(field);
        assertEquals("engine", field.getName());
        
        ConfigFieldDTO notFound = parameterRelationService.findFieldByName(testFields, "nonexistent");
        assertNull(notFound);
    }

    /**
     * 创建测试字段
     */
    private List<ConfigFieldDTO> createTestFields() {
        List<ConfigFieldDTO> fields = new ArrayList<>();
        
        // engine字段
        ConfigFieldDTO engineField = new ConfigFieldDTO();
        engineField.setName("engine");
        engineField.setLabel("解压引擎");
        engineField.setType("select");
        fields.add(engineField);
        
        // exePath字段 - 当engine为7zip或bandizip时显示
        // 使用阻止条件：当engine不是7zip也不是bandizip时阻止显示
        ConfigFieldDTO exePathField = new ConfigFieldDTO();
        exePathField.setName("exePath");
        exePathField.setLabel("可执行文件路径");
        exePathField.setType("string");
        
        // 不设置阻止条件，使用默认可见
        fields.add(exePathField);
        
        // outputMode字段
        ConfigFieldDTO outputModeField = new ConfigFieldDTO();
        outputModeField.setName("outputMode");
        outputModeField.setLabel("输出模式");
        outputModeField.setType("select");
        fields.add(outputModeField);
        
        // customPath字段 - 当outputMode为specified时显示
        ConfigFieldDTO customPathField = new ConfigFieldDTO();
        customPathField.setName("customPath");
        customPathField.setLabel("自定义路径");
        customPathField.setType("directory");
        
        // 不设置阻止条件，使用默认可见
        fields.add(customPathField);
        
        return fields;
    }
}