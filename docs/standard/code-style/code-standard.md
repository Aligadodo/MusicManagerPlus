# 代码规范

## 概述

本文档定义了FileManager Plus项目的代码编写规范，确保代码质量、可读性和可维护性。

## 通用规范

### 命名规范

#### 变量命名

**规则**: 驼峰命名法

```java
// 正确
String userName;
int maxCount;
boolean isValid;

// 错误
String user_name;
int max_count;
boolean is_valid;
```

#### 常量命名

**规则**: 全大写，下划线分隔

```java
// 正确
private static final int MAX_RETRY_COUNT = 3;
private static final String DEFAULT_ENCODING = "UTF-8";

// 错误
private static final int maxRetryCount = 3;
private static final String defaultEncoding = "UTF-8";
```

#### 类命名

**规则**: 首字母大写的驼峰命名法

```java
// 正确
public class UserService {}
public class FileScanner {}
public class TaskManager {}

// 错误
public class userService {}
public class file_scanner {}
public class task_manager {}
```

#### 方法命名

**规则**: 动词开头，驼峰命名法

```java
// 正确
public void executeTask() {}
public String getUserName() {}
public boolean isValid() {}

// 错误
public void task_execute() {}
public String username_get() {}
public boolean valid_check() {}
```

### 代码格式

#### 缩进

**规则**: 使用4个空格缩进，不使用Tab

```java
// 正确
public void method() {
    if (condition) {
        doSomething();
    }
}

// 错误
public void method() {
	if (condition) {
		doSomething();
	}
}
```

#### 行长度

**规则**: 每行不超过120个字符

```java
// 正确
String message = String.format("User %s has completed %d tasks", 
                              userName, taskCount);

// 错误
String message = String.format("User %s has completed %d tasks", userName, taskCount);
```

#### 空行

**规则**: 逻辑块之间使用空行分隔

```java
// 正确
public void method() {
    // 初始化
    int count = 0;
    
    // 处理逻辑
    for (int i = 0; i < 10; i++) {
        count += i;
    }
    
    // 返回结果
    return count;
}
```

### 注释规范

#### 类注释

**规则**: 使用Javadoc格式

```java
/**
 * 文件扫描器
 * 
 * 扫描指定目录下的文件，支持递归扫描和文件过滤
 * 
 * @author FileManager Plus Team
 * @version 1.0
 */
public class FileScanner {
}
```

#### 方法注释

**规则**: 使用Javadoc格式，说明参数和返回值

```java
/**
 * 扫描指定目录
 * 
 * @param directory 要扫描的目录
 * @param recursive 是否递归扫描
 * @return 扫描到的文件列表
 * @throws IOException 如果目录不存在或无法访问
 */
public List<File> scan(String directory, boolean recursive) throws IOException {
}
```

#### 行内注释

**规则**: 解释复杂逻辑，不描述显而易见的代码

```java
// 正确
// 使用编辑距离算法计算相似度
double similarity = calculateLevenshteinDistance(s1, s2);

// 错误
// 将s1和s2赋值给变量
String s1 = "hello";
String s2 = "world";
```

## Java代码规范

### 异常处理

**规则**: 捕获具体异常，避免捕获Exception

```java
// 正确
try {
    doSomething();
} catch (IOException e) {
    log.error("IO错误", e);
    throw new BusinessException("文件操作失败", e);
}

// 错误
try {
    doSomething();
} catch (Exception e) {
    log.error("错误", e);
}
```

### 资源管理

**规则**: 使用try-with-resources管理资源

```java
// 正确
try (InputStream is = new FileInputStream(file)) {
    // 使用输入流
}

// 错误
InputStream is = new FileInputStream(file);
try {
    // 使用输入流
} finally {
    if (is != null) {
        is.close();
    }
}
```

### 集合操作

**规则**: 使用Stream API进行集合操作

```java
// 正确
List<String> filtered = list.stream()
    .filter(s -> s.length() > 5)
    .collect(Collectors.toList());

// 错误
List<String> filtered = new ArrayList<>();
for (String s : list) {
    if (s.length() > 5) {
        filtered.add(s);
    }
}
```

## Flutter代码规范

### Widget命名

**规则**: 使用描述性名称，以功能或内容命名

```dart
// 正确
class UserListWidget extends StatelessWidget {}
class TaskDetailWidget extends StatefulWidget {}

// 错误
class Widget1 extends StatelessWidget {}
class MyWidget extends StatefulWidget {}
```

### 状态管理

**规则**: 使用Provider或Riverpod进行状态管理

```dart
// 正确
class TaskProvider extends ChangeNotifier {
  List<Task> _tasks = [];
  
  List<Task> get tasks => _tasks;
  
  void addTask(Task task) {
    _tasks.add(task);
    notifyListeners();
  }
}

// 错误
class TaskWidget extends StatefulWidget {
  List<Task> _tasks = [];
  
  @override
  Widget build(BuildContext context) {
    // 直接操作状态
  }
}
```

### 异步处理

**规则**: 使用async/await处理异步操作

```dart
// 正确
Future<void> loadData() async {
  try {
    final data = await api.fetchData();
    setState(() {
      _data = data;
    });
  } catch (e) {
    log.error('加载数据失败', e);
  }
}

// 错误
void loadData() {
  api.fetchData().then((data) {
    setState(() {
      _data = data;
    });
  }).catchError((e) {
    log.error('加载数据失败', e);
  });
}
```

## AI提示词

当AI助手编写代码时，请遵循以下指导：

```
你正在为FileManager Plus项目编写代码。请遵循以下代码规范：

1. 命名规范：
   - 变量使用驼峰命名法: userName, maxCount
   - 常量使用全大写下划线分隔: MAX_RETRY_COUNT
   - 类使用首字母大写驼峰: UserService, FileScanner
   - 方法使用动词开头驼峰: executeTask, getUserName

2. 代码格式：
   - 使用4个空格缩进，不使用Tab
   - 每行不超过120个字符
   - 逻辑块之间使用空行分隔

3. 注释规范：
   - 类和方法使用Javadoc格式
   - 行内注释解释复杂逻辑
   - 不描述显而易见的代码

4. Java代码规范：
   - 捕获具体异常，避免捕获Exception
   - 使用try-with-resources管理资源
   - 使用Stream API进行集合操作

5. Flutter代码规范：
   - Widget使用描述性名称
   - 使用Provider或Riverpod进行状态管理
   - 使用async/await处理异步操作

6. 代码质量：
   - 保持方法简洁，单一职责
   - 避免重复代码
   - 使用有意义的变量名
   - 添加必要的错误处理

请确保代码符合上述规范，并保持代码的可读性和可维护性。
```

## 相关文档

- [设计规范](../design-style/)
- [测试标准](../test-style/)
- [迭代流程](../process/)

---

**文档版本**: 1.0  
**最后更新**: 2026-02-03  
**维护者**: FileManager Plus Team
