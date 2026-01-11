/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.components;

import com.filemanager.app.ui.GlobalSettingsView;
import com.filemanager.app.base.IAppController;
import com.filemanager.app.tools.ParallelStreamWalker;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileScanner {
    private final GlobalSettingsView globalSettingsView;
    private final IAppController app;
    private final AtomicBoolean isTaskRunning;
    
    public FileScanner(IAppController app, GlobalSettingsView globalSettingsView) {
        this.app = app;
        this.globalSettingsView = globalSettingsView;
        this.isTaskRunning = app.getTaskRunningStatus();
    }
    
    public List<File> scanFilesRobust(File root, int minDepth, int maxDepth, AtomicInteger globalLimit, AtomicInteger dirLimit, Consumer<String> msg) {
        AtomicInteger countScan = new AtomicInteger(0);
        AtomicInteger countIgnore = new AtomicInteger(0);
        List<File> list = new ArrayList<>();
        if (!root.exists()) return list;
        int threads = app.getSpPreviewThreads().getValue();
        try (Stream<Path> s = ParallelStreamWalker.walk(root.toPath(), minDepth, maxDepth, globalLimit, dirLimit, threads, isTaskRunning)) {
            list = s.filter(p -> {
                try {
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
                        app.log(msgStr);
                    }
                }
            }).filter(path -> {
                try {
                    path.toFile();
                } catch (Exception e) {
                    app.logError(path + " 文件扫描异常: " + e.getMessage());
                    return false;
                }
                return true;
            }).map(Path::toFile).collect(Collectors.toList());
        } catch (Exception e) {
            app.logError("扫描文件失败：" + ExceptionUtils.getStackTrace(e));
        }
        String msgStr = "目录下(总共)：" + root.getAbsolutePath()
                + "，已扫描" + countScan.get() + "个文件"
                + "，已忽略" + countIgnore.get() + "个文件"
                + "，已收纳" + (countScan.get() - countIgnore.get()) + "个文件";
        msg.accept(msgStr);
        app.log(msgStr);
        // 反转列表，便于由下而上处理文件，保证处理成功
        Collections.reverse(list);
        return list;
    }
}