/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.base;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 业务操作接口
 * 定义业务操作相关的方法
 */
public interface IBusinessOperations {
    /**
     * 添加目录操作
     */
    void addDirectoryAction();

    /**
     * 移除源目录
     * @param dir 要移除的目录
     */
    void removeSourceDir(File dir);

    /**
     * 清空源目录
     */
    void clearSourceDirs();

    /**
     * 添加策略步骤
     * @param template 策略模板
     */
    void addStrategyStep(IAppStrategy template);

    /**
     * 移除策略步骤
     * @param strategy 要移除的策略
     */
    void removeStrategyStep(IAppStrategy strategy);

    /**
     * 运行管道分析
     */
    void runPipelineAnalysis();

    /**
     * 运行管道执行
     */
    void runPipelineExecution();

    /**
     * 强制停止任务
     */
    void forceStop();

    /**
     * 使预览失效
     * @param reason 失效原因
     */
    void invalidatePreview(String reason);

    /**
     * 刷新预览表格过滤器
     */
    void refreshPreviewTableFilter();

    /**
     * 在系统中打开文件
     * @param f 要打开的文件
     */
    void openFileInSystem(File f);

    /**
     * 打开父目录
     * @param f 文件
     */
    void openParentDirectory(File f);

    /**
     * 健壮地扫描文件
     * @param root 根目录
     * @param minDepth 最小深度
     * @param maxDepth 最大深度
     * @param msg 消息消费者
     * @return 文件列表
     */
    java.util.List<File> scanFilesRobust(File root, int minDepth, int maxDepth, AtomicInteger globalLimit, AtomicInteger dirLimit, Consumer<String> msg);
}