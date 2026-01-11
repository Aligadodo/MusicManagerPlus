/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.tools;

import lombok.var;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author 28667
 */
public class ParallelStreamWalker {

    // 结束标记（毒丸）：使用一个特殊的、不可能存在的Path对象作为标记
    // 这样BlockingQueue中只存储Path类型，避免了泛型混淆。
    private static final Path END_MARKER = Paths.get("___END_OF_SCANNING_MARKER___");

    /**
     * 并发扫描目录并返回 Stream<File>
     *
     * @param root        起始路径
     * @param maxDepth    最大深度
     * @param parallelism 并发线程数
     * @return Stream<File> (流的顺序是随机的)
     */
    public static Stream<Path> walk(Path root, int minDepth, int maxDepth, AtomicInteger globalLimitRemaining, AtomicInteger dirLimitRemaining, int parallelism, AtomicBoolean isTaskRunning) {
        // 1. 创建阻塞队列作为缓冲区，只存储Path类型
        BlockingQueue<Path> queue = new LinkedBlockingQueue<>(1024);

        // 2. 创建线程池
        ForkJoinPool pool = new ForkJoinPool(parallelism);

        // 3. 启动后台扫描任务
        pool.submit(() -> {
            try {
                // 执行递归扫描
                pool.invoke(new FileWalkAction(root, 0, minDepth, maxDepth, globalLimitRemaining, dirLimitRemaining, queue, isTaskRunning));
            } finally {
                // 扫描结束（无论成功失败），放入结束标记
                offerMarker(queue);
            }
        });

        // 4. 将队列转换为 Iterator<File>
        Iterator<Path> fileIterator = new Iterator() {
            private Path nextFile;
            private boolean finished = false;

            @Override
            public boolean hasNext() {
                if (nextFile != null) return true;
                if (finished) return false;

                try {
                    // 阻塞等待，直到有数据或收到结束标记
                    Path item = queue.take();

                    // 检查是否是结束标记
                    if (item == END_MARKER) {
                        finished = true;
                        return false;
                    }

                    // 将 Path 转换为 File
                    nextFile = item;
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    finished = true;
                    return false;
                }
            }

            @Override
            public Path next() {
                if (!hasNext()) throw new java.util.NoSuchElementException();
                Path result = nextFile;
                nextFile = null;
                return result;
            }
        };

        // 5. 将 Iterator 转换为 Stream<File>
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(fileIterator, Spliterator.NONNULL | Spliterator.ORDERED),
                false // 不在 Stream 管道中并行处理，因为数据源已经是并发的
        ).onClose(() -> {
            // 6. 当 Stream 关闭时，强制关闭线程池，停止后台扫描
            pool.shutdownNow();
        });
    }

    // 辅助方法：放入结束标记
    private static void offerMarker(BlockingQueue<Path> queue) {
        try {
            // 使用 put() 确保标记一定能放入队列
            queue.put(END_MARKER);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 递归任务类
    private static class FileWalkAction extends RecursiveAction {
        private final Path dir;
        private final int currentDepth;
        private final int minDepth;
        private final int depthRemaining;
        private final BlockingQueue<Path> queue;
        private final AtomicBoolean isTaskRunning;
        private final AtomicInteger globalLimitRemaining;
        private final AtomicInteger dirLimitRemaining;

        public FileWalkAction(Path dir, int currentDepth, int minDepth, int depthRemaining, AtomicInteger globalLimitRemaining, AtomicInteger dirLimitRemaining, BlockingQueue<Path> queue, AtomicBoolean isTaskRunning) {
            this.dir = dir;
            this.currentDepth = currentDepth;
            this.minDepth = minDepth;
            this.depthRemaining = depthRemaining;
            this.queue = queue;
            this.isTaskRunning = isTaskRunning;
            this.globalLimitRemaining = globalLimitRemaining;
            this.dirLimitRemaining = dirLimitRemaining;
        }

        @Override
        protected void compute() {
            if (this.currentDepth >= minDepth) {
                // 尝试将当前目录放入队列
                putAndCheckLimit(dir);
            }
            if (depthRemaining <= 0 || !isTaskRunning.get() || reachedLimit()) return;

            var subTasks = new ArrayList<FileWalkAction>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        subTasks.add(new FileWalkAction(entry, currentDepth + 1, minDepth, depthRemaining - 1, globalLimitRemaining, dirLimitRemaining, queue, isTaskRunning));
                    } else if (this.currentDepth >= minDepth) {
                        // 是文件，直接放入队列
                        putAndCheckLimit(entry);
                    }
                }
            } catch (IOException e) {
                // 忽略访问权限异常或IO错误
            }

            if (!subTasks.isEmpty()) {
                invokeAll(subTasks);
            }
        }


        private boolean reachedLimit() {
            if (globalLimitRemaining.get() < 0) {
                return true;
            }
            return dirLimitRemaining.get() < 0;
        }

        private boolean putAndCheckLimit(Path entry) {
            if (globalLimitRemaining.decrementAndGet() < 0) {
                return false;
            }
            if (dirLimitRemaining.decrementAndGet() < 0) {
                return false;
            }
            try {
                queue.put(entry);
            } catch (InterruptedException e) {
                return true; // 退出
            }
            return true;
        }
    }
}