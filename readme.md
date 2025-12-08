dist/
├── EchoMusicManager.bat  (双击运行)
├── ffmpeg.exe            (内置工具)
├── bin/
│   └── EchoMusicManager.jar (核心程序)
└── jre/                  (内置Java环境)
└── bin/
└── lib/
...

**使用方法：**
直接将 `dist` 文件夹压缩成 ZIP 发送给用户。用户解压后，双击 `EchoMusicManager.bat` 即可运行，无需安装任何 Java 环境。


要在 IntelliJ IDEA 中解决这个问题，您需要将相同的 JVM 参数添加到项目的“运行配置 (Run Configuration)”中。
请按照以下步骤操作：
在 IDEA 顶部工具栏，点击运行配置下拉菜单（通常显示类名 MusicFileManagerApp），选择 Edit Configurations... (编辑配置)。
在左侧选择您的主程序配置（MusicFileManagerApp）。
在右侧找到 VM options (虚拟机选项) 输入框。
提示：如果没有看到 VM options，请点击 Modify options (修改选项) -> 勾选 Add VM options (添加虚拟机选项)。
将以下参数完整复制并粘贴到 VM options 框中（注意参数之间用空格分隔，不要换行）：
Plaintext
--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED --add-opens javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED --add-opens javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED --add-opens javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED
点击 Apply 和 OK。
重新点击运行（Run）或调试（Debug）按钮，程序即可正常启动且无报错。

