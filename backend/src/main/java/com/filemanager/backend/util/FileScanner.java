package com.filemanager.backend.util;

import com.filemanager.backend.service.FileFilterService;
import com.filemanager.backend.service.FileTypeFilterService;
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
    private final FileTypeFilterService fileTypeFilterService;
    private final AtomicBoolean isTaskRunning;
    private final int threads;

    public FileScanner(FileFilterService fileFilterService, FileTypeFilterService fileTypeFilterService, AtomicBoolean isTaskRunning, int threads) {
        this.fileFilterService = fileFilterService;
        this.fileTypeFilterService = fileTypeFilterService;
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
                    File file = p.toFile();
                    if (file.isFile() && fileFilterService.isFileIncluded(file) && (fileTypeFilterService == null || fileTypeFilterService.isFileIncludedByType(file.getName()))) {
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
    
    /**
     * 根据扫描模式执行文件扫描
     * @param root 根目录
     * @param scanMode 扫描模式：ALL（全部文件）、CURRENT（当前目录）、SPECIFIC（指定目录层级）、RANGE（目录层级范围）
     * @param specificDepth 指定的目录层级（仅当scanMode为SPECIFIC时使用）
     * @param minDepth 最小目录层级（仅当scanMode为RANGE时使用）
     * @param maxDepth 最大目录层级（仅当scanMode为ALL或RANGE时使用）
     * @param globalLimit 全局限制
     * @param dirLimit 目录限制
     * @param msg 消息回调
     * @return 扫描到的文件列表
     */
    public List<File> scanFilesByMode(File root, String scanMode, Integer specificDepth, int minDepth, int maxDepth, AtomicInteger globalLimit, AtomicInteger dirLimit, Consumer<String> msg) {
        int actualMinDepth = minDepth;
        int actualMaxDepth = maxDepth;
        
        switch (scanMode) {
            case "CURRENT":
                // 当前目录
                actualMinDepth = 0;
                actualMaxDepth = 0;
                break;
            case "SPECIFIC":
                // 指定目录层级
                if (specificDepth != null) {
                    actualMinDepth = specificDepth;
                    actualMaxDepth = specificDepth;
                }
                break;
            case "RANGE":
                // 目录层级范围
                // 使用传入的minDepth和maxDepth
                break;
            case "ALL":
            default:
                // 全部文件
                // 使用传入的minDepth和maxDepth
                break;
        }
        
        return scanFilesRobust(root, actualMinDepth, actualMaxDepth, globalLimit, dirLimit, msg);
    }
}
