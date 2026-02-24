# 配置管理系统设计文档

## 概述

配置管理系统负责管理MusicManagerPlus的所有配置项，包括全局配置、主题配置、策略配置等，提供统一的配置读取、修改和持久化功能。

## 系统架构

### 核心组件

```
┌─────────────────────────────────────────────────────────────┐
│                  ConfigController                         │
│                   (REST API 层)                          │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                  ConfigManager                           │
│                 (配置管理器)                              │
├─────────────────────────────────────────────────────────────┤
│ - 配置加载                                               │
│ - 配置保存                                               │
│ - 配置验证                                               │
│ - 配置监听                                               │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ├───────────────────────────────────────┐
                   │                                   │
                   ▼                                   ▼
          ┌──────────────┐                    ┌──────────────┐
          │ 配置文件      │                    │   数据库       │
          │ config.json  │                    │ config_snapshot│
          └──────────────┘                    └──────────────┘
```

## 配置分类

### 1. 全局配置 (Global Settings)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| previewThreads | int | 4 | 预览线程数 |
| executionThreads | int | 4 | 执行线程数 |
| threadPoolMode | string | "auto" | 线程池模式: auto, fixed, cached |
| autoRefresh | boolean | true | 自动刷新 |
| previewLimit | int | 1000 | 预览限制 |
| maxThreads | int | 10 | 最大线程数 |
| timeout | int | 300 | 超时时间(秒) |
| dryRun | boolean | false | 试运行模式 |
| overwrite | boolean | false | 覆盖模式 |
| backup | boolean | true | 备份模式 |
| backupPath | string | "./backup" | 备份路径 |
| retryCount | int | 3 | 重试次数 |
| retryInterval | int | 1000 | 重试间隔(毫秒) |

### 2. 外观配置 (Appearance)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| theme | string | "light" | 主题: light, dark, auto |
| primaryColor | string | "#1976D2" | 主色调 |
| fontSize | int | 14 | 字体大小 |
| language | string | "zh-CN" | 语言 |

### 3. 策略配置 (Strategy Config)

每个策略都有自己的配置项，例如：

**文件重命名策略**:
- pattern: 重命名模式
- preview: 是否预览
- dryRun: 是否试运行

**音频转换策略**:
- format: 目标格式
- quality: 音频质量
- bitrate: 比特率

### 4. 源目录配置 (Source Directory)

| 配置项 | 类型 | 说明 |
|--------|------|------|
| path | string | 目录路径 |
| recursive | boolean | 是否递归 |
| depth | int | 递归深度 |
| filters | array | 文件过滤器 |

## 配置管理接口

### 配置读取

```java
// 获取所有配置
Map<String, Object> getAllConfig();

// 获取指定配置
Object getConfigValue(String key);

// 获取嵌套配置
Map<String, Object> getNestedConfig(String category);
```

### 配置修改

```java
// 设置配置值
void setConfigValue(String key, Object value);

// 批量设置配置
void setConfigValues(Map<String, Object> configs);

// 删除配置
void removeConfigValue(String key);
```

### 配置持久化

```java
// 保存配置到文件
void saveConfigToFile();

// 保存配置到数据库
void saveConfigToDatabase();

// 加载配置
void loadConfig();
```

### 配置监听

```java
// 添加配置监听器
void addConfigListener(ConfigListener listener);

// 移除配置监听器
void removeConfigListener(ConfigListener listener);
```

## 配置存储

### 文件存储

配置文件位置: `~/.MusicManagerPlus/config.json`

```json
{
  "globalSettings": {
    "previewThreads": 4,
    "executionThreads": 4,
    "threadPoolMode": "auto",
    "autoRefresh": true,
    "previewLimit": 1000
  },
  "appearance": {
    "theme": "light",
    "primaryColor": "#1976D2",
    "fontSize": 14,
    "language": "zh-CN"
  },
  "strategies": {
    "fileRename": {
      "pattern": "{artist}/{album}/{track} - {title}",
      "preview": true
    },
    "audioConverter": {
      "format": "mp3",
      "quality": "high",
      "bitrate": 320
    }
  },
  "sourceDirectories": [
    {
      "path": "/music",
      "recursive": true,
      "depth": 4,
      "filters": []
    }
  ]
}
```

### 数据库存储

配置快照表: `config_snapshot`

```sql
CREATE TABLE config_snapshot (
    snapshot_id TEXT PRIMARY KEY,
    snapshot_name TEXT NOT NULL,
    snapshot_type TEXT NOT NULL,
    config_data TEXT NOT NULL,
    description TEXT,
    is_template BOOLEAN DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by TEXT
);
```

## 配置验证

### 验证规则

1. **类型验证**: 检查配置值类型是否正确
2. **范围验证**: 检查数值配置是否在有效范围内
3. **依赖验证**: 检查配置项之间的依赖关系
4. **格式验证**: 检查字符串格式是否正确

### 验证示例

```java
// 验证线程数配置
if (threads < 1 || threads > 100) {
    throw new ConfigValidationException("线程数必须在1-100之间");
}

// 验证主题配置
if (!Arrays.asList("light", "dark", "auto").contains(theme)) {
    throw new ConfigValidationException("无效的主题值");
}
```

## 配置热更新

### 更新机制

1. 配置文件监听
2. 配置变更检测
3. 配置验证
4. 配置应用
5. 通知监听器

### 更新流程

```
配置文件变更 → 检测变更 → 验证配置 → 应用配置 → 通知监听器
```

### WebSocket通知

配置变更后，通过WebSocket通知前端：

```json
{
  "type": "config_update",
  "data": {
    "key": "theme",
    "value": "dark"
  }
}
```

## 配置备份和恢复

### 配置备份

- 自动备份: 每次配置修改后自动备份
- 手动备份: 用户可以手动创建备份
- 备份保留: 保留最近10个备份

### 配置恢复

- 从备份恢复: 选择备份版本进行恢复
- 从快照恢复: 选择配置快照进行恢复
- 默认配置: 恢复到默认配置

## 配置模板

### 模板类型

1. **系统模板**: 系统预置的配置模板
2. **用户模板**: 用户创建的配置模板
3. **任务模板**: 从任务创建的配置模板

### 模板管理

- 创建模板
- 编辑模板
- 删除模板
- 应用模板

## API接口

### 配置管理接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/config | GET | 获取所有配置 |
| /api/config | PUT | 更新配置 |
| /api/config/{key} | GET | 获取指定配置 |
| /api/config/{key} | PUT | 更新指定配置 |
| /api/config/{key} | DELETE | 删除指定配置 |
| /api/config/appearance | GET | 获取外观配置 |
| /api/config/globalSettings | GET | 获取全局配置 |
| /api/config/backup | POST | 备份配置 |
| /api/config/restore | POST | 恢复配置 |
| /api/config/templates | GET | 获取配置模板 |
| /api/config/templates | POST | 创建配置模板 |

## 配置安全

### 敏感信息保护

- 密码加密存储
- API Key加密存储
- 路径信息脱敏

### 访问控制

- 配置读取权限
- 配置修改权限
- 配置备份权限

### 审计日志

- 配置修改记录
- 配置访问记录
- 配置恢复记录

## 性能优化

### 配置缓存

- 内存缓存配置
- 延迟加载配置
- 配置变更时更新缓存

### 批量操作

- 批量读取配置
- 批量更新配置
- 批量验证配置

## 扩展性

### 插件配置

- 插件配置注册
- 插件配置管理
- 插件配置验证

### 自定义配置

- 自定义配置项
- 自定义验证规则
- 自定义配置类型

## 总结

配置管理系统提供了统一的配置管理接口，支持配置的读取、修改、持久化、验证和热更新等功能。系统设计注重配置的安全性、性能和扩展性，为整个应用提供了灵活可靠的配置管理能力。
