package com.filemanager.backend.util;

import com.filemanager.backend.service.FileFilterService;
import com.filemanager.backend.logging.UnifiedLogger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileScanner {
    
    private final FileFilterService fileFilterService;
    private final AtomicBoolean isTaskRunning;
    private final int threads;

    public FileScanner(FileFilterService fileFilterService, AtomicBoolean isTaskRunning, int threads) {
        this.fileFilterService = fileFilterService;
        this.isTaskRunning = isTaskRunning;
        this.threads = threads;
    }

    public List<File> scanFilesRobust(File root, int minDepth, int maxDepth, AtomicInteger globalLimit, AtomicInteger dirLimit, Consumer<String> msg) {
        AtomicInteger countScan = new AtomicInteger(0);
        AtomicInteger countIgnore = new AtomicInteger(0);
        List<File> list = new ArrayList<>();
        
        if (!root.exists()) {
            return list;
        }
        
        try (Stream<Path> s = ParallelStreamWalker.walk(root.toPath(), minDepth, maxDepth, globalLimit, dirLimit, threads, isTaskRunning)) {
            list = s.filter(p -> {
                try {
                    if (fileFilterService.isFileIncluded(p.toFile())) {
                        return true;
                    }
                    countIgnore.incrementAndGet();
                    return false;
                } finally {
                    countScan.incrementAndGet();
                    if (countScan.get() % 1000 == 0) {
                        String msgStr = "目录下：" + root.getAbsolutePath()
                                + "，已扫描" + countScan.get() + "个文件"
                                + "，已忽略" + countIgnore.get() + "个文件"
                                + "，已收纳" + (countScan.get() - countIgnore.get()) + "个文件";
                        msg.accept(msgStr);
                        UnifiedLogger.backendOperation("FileScanner", msgStr);
                    }
                }
            }).filter(path -> {
                try {
                    path.toFile();
                } catch (Exception e) {
                    UnifiedLogger.backendError("FileScanner", path + " 文件扫描异常: " + e.getMessage(), e);
                    return false;
                }
                return true;
            }).map(Path::toFile).collect(Collectors.toList());
        } catch (Exception e) {
            UnifiedLogger.backendError("FileScanner", "扫描文件失败：" + e.getMessage(), e);
        }
        
        String msgStr = "目录下(总共)：" + root.getAbsolutePath()
                + "，已扫描" + countScan.get() + "个文件"
                + "，已忽略" + countIgnore.get() + "个文件"
                + "，已收纳" + (countScan.get() - countIgnore.get()) + "个文件";
        msg.accept(msgStr);
        UnifiedLogger.backendOperation("FileScanner", msgStr);
        
        Collections.reverse(list);
        return list;
    }
}
