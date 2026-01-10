package com.filemanager.app;

import com.filemanager.base.IAppController;
import com.filemanager.baseui.GlobalSettingsView;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.Collections;
import java.util.stream.Stream;

public class FileScanner {
    private final GlobalSettingsView globalSettingsView;
    private final IAppController appController;
    private final AtomicBoolean isTaskRunning;
    
    public FileScanner(IAppController appController, GlobalSettingsView globalSettingsView, AtomicBoolean isTaskRunning) {
        this.appController = appController;
        this.globalSettingsView = globalSettingsView;
        this.isTaskRunning = isTaskRunning;
    }
    
    public List<File> scanFilesRobust(File root, int maxDepth, Consumer<String> msg) {
        AtomicInteger countScan = new AtomicInteger(0);
        AtomicInteger countIgnore = new AtomicInteger(0);
        List<File> list = new ArrayList<>();
        if (!root.exists()) return list;
        int threads = appController.getSpPreviewThreads().getValue();
        try (Stream<Path> s = StreamFileWalker.walk(root.toPath(), maxDepth)) {
            list = s.filter(p -> {
                try {
                    if (!isTaskRunning.get()) {
                        throw new RuntimeException("已中断");
                    }
                    if (globalSettingsView.isFileIncluded(p.toFile())) {
                        return true;
                    }
                    countIgnore.incrementAndGet();
                    return false;
                } finally {
                    countScan.incrementAndGet();
                    if (countScan.incrementAndGet() % 1000 == 0) {
                        String msgStr = "目录下：" + root.getAbsolutePath()
                                + "，已扫描" + countScan.get() + "个文件"
                                + "，已忽略" + countIgnore.get() + "个文件"
                                + "，已收纳" + (countScan.get() - countIgnore.get()) + "个文件";
                        msg.accept(msgStr);
                        appController.log(msgStr);
                    }
                }
            }).filter(path -> {
                try {
                    path.toFile();
                } catch (Exception e) {
                    appController.logError(path + " 文件扫描异常: " + e.getMessage());
                    return false;
                }
                return true;
            }).map(Path::toFile).collect(Collectors.toList());
        } catch (Exception e) {
            appController.logError("扫描文件失败：" + ExceptionUtils.getStackTrace(e));
        }
        String msgStr = "目录下(总共)：" + root.getAbsolutePath()
                + "，已扫描" + countScan.get() + "个文件"
                + "，已忽略" + countIgnore.get() + "个文件"
                + "，已收纳" + (countScan.get() - countIgnore.get()) + "个文件";
        msg.accept(msgStr);
        appController.log(msgStr);
        // 反转列表，便于由下而上处理文件，保证处理成功
        Collections.reverse(list);
        return list;
    }
}