# 策略配置管理架构设计文档

## 1. 概述

本文档描述了 MusicManagerPlus 项目中策略配置管理的架构设计，包括配置的初始化、保存、加载、读取等功能的实现方式。

## 2. 设计目标

- **内聚性**：配置管理功能内聚到各个策略内部，每个策略负责自己的配置
- **类型安全**：使用合适的参数DTO来维护参数的不同属性和参数之间的关系
- **可扩展性**：支持新策略的快速添加和配置管理
- **一致性**：统一的配置管理接口和标准
- **可测试性**：便于单元测试和集成测试

## 3. 架构设计

### 3.1 核心组件

1. **StrategyConfigurable 接口**：定义策略配置管理的标准方法
2. **AbstractConfigurableStrategy 抽象类**：提供配置管理的默认实现
3. **具体策略实现类**：继承抽象类，实现具体的配置管理逻辑
4. **StrategyRegistry**：管理所有策略实例
5. **StrategyServiceImpl**：使用策略注册器和策略类的配置管理方法

### 3.2 类图

```
+------------------------+
| StrategyConfigurable   |
+------------------------+
| + getId(): String      |
| + getName(): String    |
| + getDescription(): String |
| + getVersion(): String |
| + getConfigFields(): List<ConfigFieldDTO> |
| + initializeDefaultConfig(): StrategyConfigDTO |
| + validateConfig(config: StrategyConfigDTO): boolean |
| + getConfigValue(config: StrategyConfigDTO, key: String, defaultValue: T): T |
| + setConfigValue(config: StrategyConfigDTO, key: String, value: Object): void |
+------------------------+
            ^
            |
+------------------------+
| AbstractConfigurableStrategy |
+------------------------+
| - configFields: List<ConfigFieldDTO> |
| + AbstractConfigurableStrategy() |
| + getId(): String      |
| + getName(): String    |
| + getDescription(): String |
| + getVersion(): String |
| + getConfigFields(): List<ConfigFieldDTO> |
| + initializeDefaultConfig(): StrategyConfigDTO |
| + validateConfig(config: StrategyConfigDTO): boolean |
| + getConfigValue(config: StrategyConfigDTO, key: String, defaultValue: T): T |
| + setConfigValue(config: StrategyConfigDTO, key: String, value: Object): void |
| + addConfigField(name: String, label: String, type: String, defaultValue: Object, description: String, required: boolean, options: List<String>): void |
| # initConfigFields(): void |
| # initDefaultConfigValues(config: StrategyConfigDTO): void |
+------------------------+
            ^
            |
+------------------------+
| AdvancedRenameStrategy |
+------------------------+
| + AdvancedRenameStrategy() |
| + getId(): String      |
| + getName(): String    |
| + getDescription(): String |
| + getVersion(): String |
| # initConfigFields(): void |
| # initDefaultConfigValues(config: StrategyConfigDTO): void |
+------------------------+

+------------------------+
| StrategyRegistry       |
+------------------------+
| - instance: StrategyRegistry |
| - strategyMap: Map<String, StrategyConfigurable> |
| + getInstance(): StrategyRegistry |
| + registerStrategy(strategy: StrategyConfigurable): void |
| + getStrategy(strategyId: String): StrategyConfigurable |
| + getStrategies(): List<StrategyConfigurable> |
| + isStrategyRegistered(strategyId: String): boolean |
| + unregisterStrategy(strategyId: String): void |
| + getStrategyCount(): int |
+------------------------+
```

## 4. 实现细节

### 4.1 StrategyConfigurable 接口

定义了策略配置管理的标准方法，包括：
- 获取策略基本信息（ID、名称、描述、版本）
- 获取配置字段列表
- 初始化默认配置
- 验证配置
- 获取和设置配置值

### 4.2 AbstractConfigurableStrategy 抽象类

提供了配置管理的默认实现，包括：
- 配置字段的管理
- 默认配置的初始化
- 配置的验证
- 配置值的获取和设置
- 提供了添加配置字段的便捷方法
- 定义了两个抽象方法，由具体策略实现：
  - `initConfigFields()`: 初始化配置字段
  - `initDefaultConfigValues()`: 初始化默认配置值

### 4.3 具体策略实现类

每个策略类继承 AbstractConfigurableStrategy，实现自己的配置管理逻辑，包括：
- 定义配置字段
- 设置默认配置值
- 实现策略的基本信息方法

### 4.4 StrategyRegistry

单例类，用于管理所有策略实例，包括：
- 注册策略
- 获取策略
- 获取所有策略
- 检查策略是否已注册
- 注销策略
- 获取策略数量

### 4.5 StrategyServiceImpl

使用策略注册器和策略类的配置管理方法，包括：
- 初始化策略（内置策略和插件策略）
- 加载和保存策略配置
- 提供策略配置的获取和更新方法
- 执行策略分析和执行

## 5. 配置管理流程

### 5.1 初始化流程

1. 应用启动时，StrategyServiceImpl 初始化 StrategyRegistry
2. StrategyServiceImpl 初始化内置策略并注册到 StrategyRegistry
3. StrategyServiceImpl 加载插件策略并注册到 StrategyRegistry
4. StrategyServiceImpl 加载保存的策略配置

### 5.2 配置获取流程

1. 客户端请求获取策略配置
2. StrategyServiceImpl 从 strategyConfigs 缓存中查找配置
3. 如果缓存中不存在，从 StrategyRegistry 获取策略实例
4. 调用策略实例的 initializeDefaultConfig() 方法获取默认配置
5. 将默认配置缓存到 strategyConfigs
6. 返回配置给客户端

### 5.3 配置更新流程

1. 客户端请求更新策略配置
2. StrategyServiceImpl 将配置更新到 strategyConfigs 缓存
3. StrategyServiceImpl 保存配置到文件
4. 返回更新结果给客户端

## 6. 代码示例

### 6.1 定义策略类

```java
public class AdvancedRenameStrategy extends AbstractConfigurableStrategy {

    public AdvancedRenameStrategy() {
        super();
    }

    @Override
    protected void initConfigFields() {
        addConfigField("crossDriveMode", "跨盘动作", "select", "移动 (Move)", 
            "跨盘操作时的动作", false, 
            Arrays.asList("移动 (Move)", "复制 (Copy)"));
        addConfigField("processScope", "处理范围", "select", "全部处理", 
            "处理的文件类型范围", false, 
            Arrays.asList("仅处理文件", "仅处理文件夹", "全部处理"));
        addConfigField("rules", "重命名规则", "list", new ArrayList<>(), 
            "重命名规则列表", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "crossDriveMode", "移动 (Move)");
        setConfigValue(config, "processScope", "全部处理");
        setConfigValue(config, "rules", new ArrayList<>());
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
}
```

### 6.2 注册策略

```java
private void initBuiltInStrategies() {
    // 1. AdvancedRenameStrategy - 高级重命名策略
    com.filemanager.plugin.impl.AdvancedRenameStrategy advancedRenameStrategy = new com.filemanager.plugin.impl.AdvancedRenameStrategy();
    strategyRegistry.registerStrategy(advancedRenameStrategy);

    // 2. AudioConverterStrategy - 音频格式转换策略
    com.filemanager.plugin.impl.AudioConverterStrategy audioConverterStrategy = new com.filemanager.plugin.impl.AudioConverterStrategy();
    strategyRegistry.registerStrategy(audioConverterStrategy);

    // 其他策略...
}
```

### 6.3 获取策略配置

```java
@Override
public StrategyConfigDTO getStrategyConfig(String strategyId) {
    logger.info("[Service] 获取策略配置 - strategyId: {}", strategyId);
    StrategyConfigDTO config = strategyConfigs.get(strategyId);
    if (config == null) {
        logger.info("[Service] 策略配置不存在，创建默认配置 - strategyId: {}", strategyId);
        // 尝试从策略注册器获取策略并初始化默认配置
        StrategyConfigurable strategy = strategyRegistry.getStrategy(strategyId);
        if (strategy != null) {
            config = strategy.initializeDefaultConfig();
        } else {
            // 如果策略不存在，创建空配置
            config = new StrategyConfigDTO();
        }
        strategyConfigs.put(strategyId, config);
    }
    logger.info("[Service] 返回策略配置 - strategyId: {}, 配置项数量: {}", strategyId, config.getConfigValues() != null ? config.getConfigValues().size() : 0);
    return config;
}
```

## 7. 优势

- **内聚性**：配置管理功能内聚到各个策略内部，每个策略负责自己的配置
- **类型安全**：使用 ConfigFieldDTO 和 StrategyConfigDTO 等类型安全的DTO类
- **可扩展性**：新策略只需继承 AbstractConfigurableStrategy 并实现必要的方法
- **一致性**：统一的配置管理接口和标准
- **可测试性**：便于单元测试和集成测试
- **灵活性**：支持不同类型的配置字段和选项

## 8. 未来改进

- **配置验证增强**：添加更复杂的配置验证逻辑
- **配置版本管理**：支持配置版本控制和迁移
- **配置导出和导入**：支持配置的导出和导入
- **配置模板**：支持配置模板的创建和使用
- **配置历史**：支持配置修改历史的记录和回滚

## 9. 前端配置交互增强

### 9.1 自动提示功能

前端实现了基于"开启使用说明"勾选开关的自动提示功能，包括：

- **配置管理**：在ConfigProvider中添加showTooltips配置项
- **UI控制**：MainLayout组件中的"开启使用说明"复选框控制全局提示显示
- **组件适配**：TooltipUtils类支持条件显示工具提示
- **字段适配**：所有ConfigFieldBuilder实现添加showTooltip参数
- **策略配置面板适配**：StrategyConfigPanel类使用ConfigProvider中的配置

### 9.2 规则管理增强

前端的RenameRuleEditor组件添加了规则的上移和下移功能，与老架构保持一致：

- **功能实现**：添加_moveRuleUp和_moveRuleDown方法
- **UI交互**：在规则卡片中添加上移和下移按钮
- **优先级管理**：支持通过拖拽或按钮调整规则执行顺序

## 9. 代码组织规范

### 9.1 目录划分原则

#### 9.1.1 策略目录结构

每个策略都应该有自己的独立目录，包含该策略的所有相关代码：

```
backend/src/main/java/com/filemanager/plugin/impl/
├── advancedrename/          # 高级重命名策略
│   ├── AdvancedRenameStrategy.java
│   ├── enums/               # 策略特定的枚举
│   └── utils/               # 策略特定的工具类
├── audioconverter/          # 音频转换策略
│   ├── AudioConverterStrategy.java
│   ├── enums/
│   └── utils/
├── filecollection/          # 文件归类策略
│   ├── FileCollectionStrategy.java
│   ├── collection/          # 核心算法组件
│   │   ├── SimilarityCalculator.java
│   │   ├── FileCluster.java
│   │   ├── CollectionNameGenerator.java
│   │   ├── FileMetadataExtractor.java
│   │   ├── KeywordFilter.java
│   │   ├── FileClusteringAlgorithm.java
│   │   ├── TextSimilarityCalculator.java
│   │   └── FilenameNormalizer.java
│   └── enums/
└── ...
```

#### 9.1.2 工具类放置规则

1. **策略特定工具类**：只属于某个策略的工具类应该放在该策略的目录下
   - 例如：`filecollection/collection/` 目录下的所有类都是文件归类策略专用的

2. **通用工具类**：被多个策略使用的工具类应该放在公共的util包中
   - 例如：`com.filemanager.plugin.utils.EnumOptionProvider`、`EnumConverter`

3. **判断标准**：
   - 如果一个工具类只被一个策略使用，应该放在该策略的目录下
   - 如果一个工具类被多个策略使用，应该放在公共的util包中
   - 如果不确定，优先放在策略目录下，需要时再提取到公共包

### 9.2 代码文件大小规范

#### 9.2.1 文件行数限制

- **策略实现类**：不超过400行
- **工具类**：不超过300行
- **DTO类**：不超过200行
- **枚举类**：不超过150行

#### 9.2.2 拆分原则

当文件超过行数限制时，应该按照以下原则进行拆分：

1. **按功能拆分**：将不同的功能模块拆分到不同的文件
2. **按职责拆分**：将不同的职责拆分到不同的类
3. **按层次拆分**：将不同层次的逻辑拆分到不同的类

### 9.3 命名规范

#### 9.3.1 包命名

- 策略包：`com.filemanager.plugin.impl.{strategyname}`
- 策略枚举包：`com.filemanager.plugin.impl.{strategyname}.enums`
- 策略工具包：`com.filemanager.plugin.impl.{strategyname}.utils` 或 `com.filemanager.plugin.impl.{strategyname}.{feature}`

#### 9.3.2 类命名

- 策略类：`{StrategyName}Strategy`
- 枚举类：`{EnumName}`
- 工具类：`{Functionality}Utils` 或 `{Functionality}Calculator`、`{Functionality}Processor` 等
- DTO类：`{Entity}DTO`

### 9.4 依赖管理

#### 9.4.1 依赖方向

- 策略类可以依赖自己的工具类
- 策略类可以依赖公共工具类
- 策略类不应该依赖其他策略的工具类
- 公共工具类不应该依赖任何策略类

#### 9.4.2 循环依赖

- 禁止任何形式的循环依赖
- 如果出现循环依赖，应该重新设计代码结构

### 9.5 测试代码组织

#### 9.5.1 测试目录结构

测试代码应该与源代码保持相同的包结构：

```
backend/src/test/java/com/filemanager/plugin/impl/
├── advancedrename/
│   └── AdvancedRenameStrategyTest.java
├── audioconverter/
│   └── AudioConverterStrategyTest.java
├── filecollection/
│   └── FileCollectionStrategyTest.java
└── ...
```

#### 9.5.2 测试类命名

测试类应该与被测试类保持相同的名称，并添加`Test`后缀。

## 10. 结论

本设计通过将配置管理功能下沉到各个策略类中，使用合适的参数DTO来维护参数的不同属性和参数之间的关系，实现了配置管理的内聚性和类型安全性。同时，通过统一的接口设计和抽象类实现，确保了配置管理的一致性和可扩展性。

前端的交互增强，包括自动提示功能和规则管理功能，提升了用户体验，使配置过程更加直观和高效。

代码组织规范确保了项目的可维护性和可扩展性，通过合理的目录划分、文件大小控制和依赖管理，使代码更加清晰和易于理解。