# FileManager Plus 迭代规范

## 项目架构说明

### 框架区分
- **新框架**: Flutter Web 前端 + Spring Boot 后端
  - 前端: Flutter Web (clients/flutter-web-cli)
  - 后端: Spring Boot REST API (backend/)
  - 架构: 前后端分离，通过 REST API 通信
  - UI: Flutter 组件，不使用 JavaFX

- **老框架**: JavaFX 桌面应用 (src/main/java/)
  - UI: JavaFX + JFoenix
  - 架构: 单体桌面应用
  - 状态: 已废弃，不再维护

### 重要原则
1. **新框架不使用 JavaFX 和 JFoenix**: 新框架使用 Flutter 前端，不依赖任何 JavaFX 或 JFoenix 库
2. **老框架代码保留但不再修改**: src/main/java/ 下的老框架代码保留用于参考，但不应在新功能中使用
3. **新功能开发**: 所有新功能应在 Flutter 前端和 Spring Boot 后端中实现
4. **依赖管理**: 新框架的依赖管理在 pom.xml (backend) 和 pubspec.yaml (flutter-web-cli) 中

## 1. 代码质量要求

### 1.1 类型检查
- **强制要求**: 所有代码必须通过类型检查，无类型错误
- **检查工具**: 
  - Flutter 前端: `flutter analyze`
  - Java 后端: `mvn compile` 或 IDE 内置检查
- **常见类型错误处理**:
  - 空值检查: 所有可能为 null 的变量必须进行空值处理
  - 类型转换: 确保类型转换的安全性
  - 泛型使用: 正确使用泛型，避免原始类型

### 1.2 导入管理
- **强制要求**: 所有使用的类和方法必须有正确的导入
- **检查要点**:
  - 不存在未使用的导入
  - 不存在缺少的导入
  - 导入路径正确
  - 依赖版本兼容
- **新框架导入规则**:
  - Flutter: 只导入 Flutter 相关的包和项目自定义包
  - Java 后端: 只导入 Spring Boot、Java 标准库和项目自定义包
  - **禁止**: 不导入 javafx.* 或 com.jfoenix.* 包

### 1.3 依赖管理
- **强制要求**: 所有依赖必须在 pom.xml 或 pubspec.yaml 中正确声明
- **检查要点**:
  - 依赖版本明确
  - 依赖范围正确
  - 不存在冲突的依赖
  - 所有必需的依赖都已添加
- **新框架依赖规则**:
  - **禁止**: 不添加 JavaFX 相关依赖 (javafx-controls, javafx-fxml, javafx-graphics 等)
  - **禁止**: 不添加 JFoenix 依赖 (jfoenix)
  - **允许**: Spring Boot、Apache Commons、JUnit、Mockito 等标准依赖

## 2. 常见问题预防

### 2.1 Flutter 前端常见问题
- **类型错误**:
  - 确保所有变量都有明确的类型
  - 使用 `??` 运算符处理可能为 null 的值
  - 避免使用 `dynamic` 类型，除非必要
- **导入问题**:
  - 检查所有使用的模型类是否正确导入
  - 确保所有 widget 类都有正确的导入
  - 验证第三方库的导入路径

### 2.2 Java 后端常见问题
- **类型错误**:
  - 确保所有泛型类型参数正确
  - 检查接口实现的方法签名
  - 验证所有方法调用的参数类型
- **导入问题**:
  - **禁止**: 不导入 javafx.* 或 com.jfoenix.* 包
  - 检查 Apache Commons 等第三方库的导入
  - 验证自定义类的包路径
- **依赖问题**:
  - **禁止**: 不添加 JavaFX 依赖
  - **禁止**: 不添加 JFoenix 依赖
  - 验证 Apache Commons 依赖

### 2.3 框架混淆问题
- **问题**: 在新框架中错误使用老框架的代码或依赖
- **预防措施**:
  - 明确区分新框架和老框架的目录结构
  - 新框架代码不引用 src/main/java/ 下的类
  - 新框架不使用 JavaFX 相关的类和方法
  - 新框架的依赖只在 pom.xml (backend) 中管理

## 3. 迭代流程

### 3.1 准备阶段
1. **代码分析**:
   - 使用 `flutter analyze` 检查前端代码
   - 使用 `mvn compile` 检查后端代码
   - 修复所有类型错误和导入问题

2. **依赖检查**:
   - 检查 pom.xml 中的依赖 (backend)
   - 检查 pubspec.yaml 中的依赖 (flutter-web-cli)
   - **验证**: 不包含 JavaFX 和 JFoenix 依赖
   - 更新过期的依赖

3. **测试准备**:
   - 确保所有单元测试通过
   - 准备集成测试环境

### 3.2 开发阶段
1. **代码编写**:
   - 遵循项目的代码风格
   - 确保类型安全
   - 正确管理导入
   - **验证**: 不使用 JavaFX 或 JFoenix 相关代码

2. **持续检查**:
   - 定期运行 `flutter analyze` 和 `mvn compile`
   - 及时修复发现的问题
   - 确保代码质量
   - **验证**: 不引入 JavaFX 或 JFoenix 依赖

3. **测试**:
   - 编写单元测试
   - 运行集成测试
   - 验证功能正确性

### 3.3 完成阶段
1. **最终检查**:
   - 运行完整的类型检查
   - 验证所有导入正确
   - 确保依赖完整
   - **验证**: 不包含 JavaFX 和 JFoenix 依赖

2. **构建验证**:
   - 构建前端应用
   - 构建后端服务
   - 验证构建产物

3. **部署测试**:
   - 部署到测试环境
   - 验证所有功能
   - 检查日志中的错误

## 4. 工具和脚本

### 4.1 自动化检查脚本
- **前端检查**:
  ```bash
  # 检查前端代码
  cd clients/flutter-web-cli
  flutter analyze
  flutter test
  ```

- **后端检查**:
  ```bash
  # 检查后端代码
  cd backend
  mvn compile
  mvn test
  ```

### 4.2 部署脚本
- **前端部署**:
  ```bash
  # 构建并部署前端
  cd clients/flutter-web-cli
  flutter build web --release --no-wasm-dry-run
  rm -rf ../../frontend/*
  cp -r build/web/* ../../frontend/
  ```

- **后端部署**:
  ```bash
  # 构建并部署后端
  cd backend
  mvn clean package -DskipTests
  cp target/backend-1.0.0.jar ../../bin/
  ```

## 5. 问题处理指南

### 5.1 类型错误处理
1. **Flutter 类型错误**:
   - **错误信息**: `TypeError: null: type 'X' is not a subtype of type 'Y'`
   - **处理方法**: 添加空值检查，使用 `??` 运算符

2. **Java 类型错误**:
   - **错误信息**: `Cannot resolve type X`
   - **处理方法**: 检查导入，添加缺少的依赖

### 5.2 导入错误处理
1. **Flutter 导入错误**:
   - **错误信息**: `Target of URI doesn't exist`
   - **处理方法**: 检查文件路径，确保文件存在

2. **Java 导入错误**:
   - **错误信息**: `The import X cannot be resolved`
   - **处理方法**: 检查包路径，添加缺少的依赖
   - **特别注意**: 如果是 JavaFX 相关导入，说明代码位置错误

### 5.3 依赖错误处理
1. **Maven 依赖错误**:
   - **错误信息**: `Could not find artifact X`
   - **处理方法**: 检查依赖版本，添加正确的仓库

2. **Flutter 依赖错误**:
   - **错误信息**: `Could not find package X`
   - **处理方法**: 检查 pubspec.yaml，运行 `flutter pub get`

### 5.4 框架混淆错误处理
1. **错误**: 在新框架中引用了老框架的代码
   - **处理方法**: 
     - 将功能迁移到新框架
     - 或在新框架中重新实现该功能
     - 不直接引用 src/main/java/ 下的类

2. **错误**: 在新框架中添加了 JavaFX 依赖
   - **处理方法**: 
     - 移除 JavaFX 依赖
     - 使用 Flutter 组件替代 JavaFX 组件
     - 或通过 REST API 调用后端实现

## 6. 最佳实践

### 6.1 代码组织
- **模块化**:
  - 按功能模块组织代码
  - 避免过大的文件
  - 使用合理的包结构

- **命名规范**:
  - 类名使用 PascalCase
  - 方法名使用 camelCase
  - 变量名使用 camelCase
  - 常量名使用 UPPER_SNAKE_CASE

### 6.2 错误处理
- **异常处理**:
  - 使用 try-catch 处理异常
  - 记录异常信息
  - 提供友好的错误提示

- **日志记录**:
  - 所有错误都应记录到日志
  - 关键操作应记录到日志
  - 日志级别使用合理

### 6.3 性能优化
- **代码优化**:
  - 避免不必要的计算
  - 使用合适的数据结构
  - 优化循环和递归

- **资源管理**:
  - 及时释放资源
  - 使用 try-with-resources 语法
  - 避免内存泄漏

### 6.4 前后端分离
- **API 设计**:
  - 设计清晰的 REST API
  - 使用统一的响应格式
  - 提供完整的 API 文档

- **数据传输**:
  - 使用 JSON 格式传输数据
  - 定义清晰的数据模型
  - 验证数据完整性

## 7. 检查清单

### 7.1 代码提交前检查
- [ ] 所有类型错误已修复
- [ ] 所有导入问题已解决
- [ ] 所有依赖问题已处理
- [ ] 所有测试已通过
- [ ] 代码风格符合规范
- [ ] **不包含 JavaFX 依赖**
- [ ] **不包含 JFoenix 依赖**
- [ ] **不引用老框架代码**

### 7.2 迭代完成检查
- [ ] 前端构建成功
- [ ] 后端构建成功
- [ ] 所有功能正常
- [ ] 所有错误已修复
- [ ] 文档已更新
- [ ] **验证不包含 JavaFX 和 JFoenix**

## 8. 附录

### 8.1 常用依赖

#### Flutter 依赖 (pubspec.yaml)
```yaml
dependencies:
  flutter:
    sdk: flutter
  cupertino_icons: ^1.0.6
  http: ^1.2.1
  dio: ^5.4.3+1
  provider: ^6.1.2
  riverpod: ^2.5.1
  flutter_riverpod: ^2.5.1
  web_socket_channel: ^2.4.0
  path: ^1.9.0
  path_provider: ^2.1.2
  file_picker: ^8.0.6
  url_launcher: ^6.2.5
  intl: ^0.19.0
  json_annotation: ^4.9.0
  flutter_colorpicker: ^1.1.0
```

#### Java 后端依赖 (pom.xml)
```xml
<dependencies>
    <!-- Spring Boot 依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Apache Commons 依赖 -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
        <version>3.14.0</version>
    </dependency>
    
    <!-- 测试依赖 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.8.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 8.2 禁止使用的依赖

#### JavaFX 依赖 (禁止)
```xml
<!-- 禁止使用 -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-graphics</artifactId>
</dependency>
```

#### JFoenix 依赖 (禁止)
```xml
<!-- 禁止使用 -->
<dependency>
    <groupId>com.jfoenix</groupId>
    <artifactId>jfoenix</artifactId>
</dependency>
```

### 8.3 常见错误修复示例

#### Flutter 空值检查
```dart
// 错误
String path = file.name; // 可能为 null

// 正确
String path = file.name ?? '';
```

#### Java 导入修复
```java
// 错误 - 禁止使用 JavaFX
import javafx.scene.control.TextField;
import com.jfoenix.controls.JFXButton;

// 正确 - 使用 Spring Boot
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
```

#### 依赖管理修复
```xml
<!-- 错误 - 禁止添加 JavaFX 依赖 -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21.0.1</version>
</dependency>

<!-- 正确 - 只添加必要的依赖 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.14.0</version>
</dependency>
```

## 9. 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0 | 2026-02-13 | 初始版本 |
| 2.0 | 2026-02-13 | 明确区分新旧框架，禁止使用 JavaFX 和 JFoenix |

---

**重要提示**: 
1. 本规范适用于新框架（Flutter Web + Spring Boot）
2. 老框架（JavaFX）代码保留但不维护
3. 严禁在新框架中使用 JavaFX 或 JFoenix
4. 所有新功能应在 Flutter 前端和 Spring Boot 后端中实现
