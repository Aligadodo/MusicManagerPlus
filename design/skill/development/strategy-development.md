# 策略开发技能

## 概述

本文档提供了FileManager Plus老架构中策略开发的技能指导，帮助开发者快速掌握策略扩展开发。

## 快速开始

### 创建新策略的三步法

#### 第一步：创建策略类

```java
public class MyNewStrategy extends IAppStrategy {
    
    private final JFXComboBox<String> cbMode;
    private final TextField txtParam;
    private String pMode;
    private String pParam;
    
    public MyNewStrategy() {
        cbMode = new JFXComboBox<>();
        cbMode.getItems().addAll("模式1", "模式2");
        cbMode.getSelectionModel().select(0);
        
        txtParam = new TextField();
    }
    
    @Override
    public String getName() {
        return "我的新策略";
    }
    
    @Override
    public String getDescription() {
        return "策略的功能描述";
    }
    
    @Override
    public Node getConfigNode() {
        VBox configPane = new VBox(10);
        configPane.getChildren().addAll(
            StyleFactory.createLabel("模式选择"),
            cbMode,
            StyleFactory.createLabel("参数"),
            txtParam
        );
        return configPane;
    }
    
    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }
    
    @Override
    public void captureParams() {
        pMode = cbMode.getValue();
        pParam = txtParam.getText();
    }
    
    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                       List<ChangeRecord> inputRecords, 
                                       List<File> rootDirs) {
        return Collections.emptyList();
    }
    
    @Override
    public void execute(ChangeRecord rec) throws Exception {
    }
}
```

#### 第二步：注册策略

```java
public static List<IAppStrategy> getAppStrategies() {
    List<IAppStrategy> strategyPrototypes = new ArrayList<>();
    strategyPrototypes.add(new MyNewStrategy());
    return strategyPrototypes;
}
```

#### 第三步：添加操作类型（如果需要）

```java
public enum OperationType {
    MY_OPERATION("我的操作", "操作的描述");
    
    public final String name;
    public final String desc;
}
```

## ChangeRecord使用指南

### 修改现有文件

```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                   List<ChangeRecord> inputRecords, 
                                   List<File> rootDirs) {
    File file = currentRecord.getFileHandle();
    String newName = "new_" + file.getName();
    File newFile = new File(file.getParent(), newName);
    
    currentRecord.setNewName(newName);
    currentRecord.setNewPath(newFile.getAbsolutePath());
    currentRecord.setChanged(true);
    currentRecord.setOpType(OperationType.RENAME);
    currentRecord.setStatus(ExecStatus.PENDING);
    
    return Collections.emptyList();
}
```

### 生成新文件

```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                   List<ChangeRecord> inputRecords, 
                                   List<File> rootDirs) {
    List<ChangeRecord> result = new ArrayList<>();
    
    File sourceFile = currentRecord.getFileHandle();
    String targetPath = sourceFile.getParent() + File.separator + 
                       sourceFile.getName().replace(".flac", ".mp3");
    
    ChangeRecord record = new ChangeRecord(
        sourceFile.getName(),
        sourceFile.getName().replace(".flac", ".mp3"),
        sourceFile,
        true,
        targetPath,
        OperationType.CONVERT
    );
    
    record.getExtraParams().put("audioFormat", "mp3");
    record.getExtraParams().put("bitrate", "320k");
    record.getExtraParams().put("overwrite", "true");
    
    record.setStatus(ExecStatus.PENDING);
    
    result.add(record);
    return result;
}
```

## 命名规范

### 运行时参数命名

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 字符串 | `p` + 驼峰 | `pMode`, `pSeparator` |
| 布尔值 | `p` + `is/has` + 驼峰 | `pIsForce`, `pHasMetadata` |
| 数值 | `p` + 驼峰 | `pThreshold`, `pMaxDepth` |

### UI组件命名

| 组件类型 | 前缀 | 示例 |
|---------|------|------|
| ComboBox | `cb` | `cbMode`, `cbFunction` |
| TextField | `txt` | `txtSeparator`, `txtDestDir` |
| CheckBox | `chk` | `chkPadZero`, `chkCleanEmpty` |
| Button | `btn` | `btnAddRule`, `btnRemoveRule` |

## 常用工具方法

### 获取目标文件

```java
ChangeRecord target = getTargetFile(file, inputRecords);
```

### 获取目录下的文件

```java
List<ChangeRecord> files = getFilesUnderDir(dir, inputRecords);
```

### 日志记录

```java
log("开始处理文件: " + file.getName());
logError("处理失败: " + e.getMessage());
invalidatePreview("配置已更改");
```

## AI提示词

当AI助手开发策略时，请遵循以下指导：

```
你正在为FileManager Plus老架构开发新策略。请按照以下步骤进行：

1. 创建策略类：
   - 继承IAppStrategy接口
   - 声明UI组件（使用cb, txt, chk等前缀）
   - 声明运行时参数（使用p前缀）
   - 在构造函数中初始化UI组件

2. 实现必需方法：
   - getName(): 返回策略名称
   - getDescription(): 返回策略描述
   - getConfigNode(): 返回配置界面
   - getTargetType(): 返回扫描目标类型
   - captureParams(): 捕获UI参数到运行时参数
   - analyze(): 分析文件，返回ChangeRecord列表
   - execute(): 执行文件操作

3. 注册策略：
   - 在AppStrategyFactory中注册策略
   - 如果需要新的操作类型，在OperationType中添加

4. 使用ChangeRecord：
   - 修改现有文件：设置newName, newPath, opType, changed, status
   - 生成新文件：创建新的ChangeRecord对象
   - 使用extraParams传递额外参数

5. 遵循命名规范：
   - 运行时参数使用p前缀：pMode, pSeparator
   - UI组件使用特定前缀：cb(ComboBox), txt(TextField), chk(CheckBox)
   - extraParams键使用小写下划线：audio_format, bitrate

6. 使用工具方法：
   - getTargetFile(): 获取特定文件的ChangeRecord
   - getFilesUnderDir(): 获取目录下的ChangeRecord
   - log(): 记录正常信息
   - logError(): 记录错误信息
   - invalidatePreview(): 使预览失效

注意事项：
- analyze()方法中不要执行文件操作
- execute()方法中不要修改UI
- 始终处理异常并更新执行状态
- 使用synchronized保护共享资源
- 及时关闭文件流，释放系统资源
```

## 相关文档

- [IAppStrategy接口设计](../../old-framework/module-detail/iappstrategy-interface-design.md)
- [策略总览](../../old-framework/module-detail/strategy-overview.md)
- [代码规范](../../standard/code-style/)

---

**文档版本**: 1.0  
**最后更新**: 2026-02-03  
**维护者**: FileManager Plus Team
