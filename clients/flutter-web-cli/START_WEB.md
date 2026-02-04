# Flutter Web 启动说明

## 启动方式

### 方式一：使用启动脚本（推荐）

```bash
# 使用默认端口（8081）
./start_web.sh

# 指定端口
./start_web.sh 3000

# 使用环境变量指定端口
FLUTTER_WEB_PORT=3000 ./start_web.sh
```

### 方式二：直接使用 Flutter 命令

```bash
# 使用默认端口
flutter run -d chrome

# 指定端口
flutter run -d chrome --web-port=3000
```

## 配置文件

在项目根目录创建 `.flutter_config` 文件来配置默认端口：

```
web_port=8081
```

## 端口配置优先级

1. 命令行参数（最高优先级）
2. 环境变量 `FLUTTER_WEB_PORT`
3. 配置文件 `.flutter_config`
4. 默认端口 8081（最低优先级）

## 注意事项

- 确保端口没有被其他程序占用
- 默认端口 8081 与后端端口 8080 不冲突
- 建议使用启动脚本，便于管理和配置
