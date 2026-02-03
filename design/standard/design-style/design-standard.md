# 设计规范

## 概述

本文档定义了FileManager Plus项目的设计规范，确保系统设计的一致性和可维护性。

## 系统设计原则

### 1. 单一职责原则

每个模块或类应该只有一个改变的理由。

**示例**:
```java
// 正确：每个类只负责一个功能
class FileScanner {
    public List<File> scan(String path) {}
}

class FileValidator {
    public boolean isValid(File file) {}
}

// 错误：一个类负责多个功能
class FileHandler {
    public List<File> scan(String path) {}
    public boolean isValid(File file) {}
    public void move(File source, File target) {}
}
```

### 2. 开闭原则

对扩展开放，对修改关闭。

**示例**:
```java
// 正确：使用接口，便于扩展
public interface IStrategy {
    void execute(File file);
}

public class RenameStrategy implements IStrategy {
    public void execute(File file) {}
}

public class MoveStrategy implements IStrategy {
    public void execute(File file) {}
}

// 错误：使用if-else，难以扩展
public class FileHandler {
    public void handle(File file, String operation) {
        if (operation.equals("rename")) {
            // 重命名逻辑
        } else if (operation.equals("move")) {
            // 移动逻辑
        }
    }
}
```

### 3. 依赖倒置原则

依赖抽象，不依赖具体实现。

**示例**:
```java
// 正确：依赖接口
public class TaskExecutor {
    private final IStrategy strategy;
    
    public TaskExecutor(IStrategy strategy) {
        this.strategy = strategy;
    }
}

// 错误：依赖具体类
public class TaskExecutor {
    private final RenameStrategy strategy;
    
    public TaskExecutor() {
        this.strategy = new RenameStrategy();
    }
}
```

## 接口设计规范

### RESTful API设计

#### URL设计

**规则**: 使用名词复数形式，小写字母

```
正确:
GET    /api/files
POST   /api/files
GET    /api/files/{id}
DELETE  /api/files/{id}

错误:
GET    /api/file
POST   /api/createFile
GET    /api/getFile/{id}
DELETE  /api/deleteFile/{id}
```

#### HTTP方法使用

| 方法 | 用途 | 幂等性 |
|------|------|--------|
| GET | 获取资源 | 是 |
| POST | 创建资源 | 否 |
| PUT | 更新资源 | 是 |
| DELETE | 删除资源 | 是 |

#### 状态码使用

| 状态码 | 含义 | 使用场景 |
|--------|------|---------|
| 200 | 成功 | 请求成功处理 |
| 201 | 已创建 | 资源创建成功 |
| 400 | 错误请求 | 请求参数错误 |
| 401 | 未授权 | 需要认证 |
| 404 | 未找到 | 资源不存在 |
| 500 | 服务器错误 | 服务器内部错误 |

### 数据模型设计

#### JSON格式

**规则**: 使用驼峰命名法

```json
// 正确
{
  "userName": "张三",
  "taskCount": 10,
  "isValid": true
}

// 错误
{
  "user_name": "张三",
  "task_count": 10,
  "is_valid": true
}
```

#### 日期时间格式

**规则**: 使用ISO 8601格式

```json
// 正确
{
  "createdAt": "2026-02-03T12:00:00Z",
  "updatedAt": "2026-02-03T12:00:00Z"
}

// 错误
{
  "createdAt": "2026-02-03 12:00:00",
  "updatedAt": "2026/02/03 12:00:00"
}
```

## UI设计规范

### 布局规范

#### 间距

**规则**: 使用8的倍数作为间距单位

```
标准间距: 8, 16, 24, 32, 48, 64
```

#### 字体大小

**规则**: 使用标准字体大小

```
标题1: 32px
标题2: 24px
标题3: 20px
正文: 16px
辅助文字: 14px
```

### 颜色规范

#### 主题色

```
主色: #1976D2 (蓝色)
成功: #4CAF50 (绿色)
警告: #FF9800 (橙色)
错误: #F44336 (红色)
```

#### 中性色

```
文字主色: #212121
文字次色: #757575
文字辅助色: #9E9E9E
边框色: #E0E0E0
背景色: #F5F5F5
```

### 交互规范

#### 按钮状态

```
正常: 主色背景，白色文字
悬停: 主色加深10%
点击: 主色加深20%
禁用: 灰色背景，灰色文字
```

#### 表单验证

```
实时验证: 失焦时验证
错误提示: 输入框下方红色文字
成功提示: 输入框右侧绿色图标
```

## 数据库设计规范

### 表命名

**规则**: 使用小写字母和下划线

```
正确: user_tasks, file_records, strategy_configs
错误: UserTasks, file-records, strategyConfigs
```

### 字段命名

**规则**: 使用小写字母和下划线

```
正确: user_name, task_count, created_at
错误: userName, task-count, createdAt
```

### 索引命名

**规则**: 使用idx_前缀

```
正确: idx_user_name, idx_task_status
错误: user_name_index, task_status_idx
```

## AI提示词

当AI助手进行系统设计时，请遵循以下指导：

```
你正在为FileManager Plus项目进行系统设计。请遵循以下设计规范：

1. 系统设计原则：
   - 单一职责原则：每个模块只负责一个功能
   - 开闭原则：对扩展开放，对修改关闭
   - 依赖倒置原则：依赖抽象，不依赖具体实现

2. 接口设计规范：
   - RESTful API使用名词复数形式，小写字母
   - 正确使用HTTP方法：GET(获取), POST(创建), PUT(更新), DELETE(删除)
   - 使用标准HTTP状态码：200(成功), 201(已创建), 400(错误请求), 401(未授权), 404(未找到), 500(服务器错误)
   - JSON使用驼峰命名法
   - 日期时间使用ISO 8601格式

3. 数据模型设计：
   - JSON格式使用驼峰命名法
   - 日期时间使用ISO 8601格式
   - 枚举类型使用字符串表示

4. UI设计规范：
   - 间距使用8的倍数：8, 16, 24, 32, 48, 64
   - 字体大小：标题1(32px), 标题2(24px), 标题3(20px), 正文(16px), 辅助文字(14px)
   - 主题色：主色(#1976D2), 成功(#4CAF50), 警告(#FF9800), 错误(#F44336)
   - 中性色：文字主色(#212121), 文字次色(#757575), 文字辅助色(#9E9E9E), 边框色(#E0E0E0), 背景色(#F5F5F5)
   - 按钮状态：正常(主色背景), 悬停(主色加深10%), 点击(主色加深20%), 禁用(灰色背景)
   - 表单验证：实时验证(失焦时), 错误提示(下方红色文字), 成功提示(右侧绿色图标)

5. 数据库设计规范：
   - 表命名使用小写字母和下划线
   - 字段命名使用小写字母和下划线
   - 索引命名使用idx_前缀

6. 设计文档要求：
   - 包含架构图
   - 包含数据流图
   - 包含接口定义
   - 包含数据模型定义
   - 包含UI设计稿

请确保设计符合上述规范，并保持设计的一致性和可维护性。
```

## 相关文档

- [代码规范](../code-style/)
- [文档标准](../doc-style/)
- [迭代流程](../process/)

---

**文档版本**: 1.0  
**最后更新**: 2026-02-03  
**维护者**: FileManager Plus Team
