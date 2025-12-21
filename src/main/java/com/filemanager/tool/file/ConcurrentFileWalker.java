package com.filemanager.tool.file;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ConcurrentFileWalker {

    /**
     * 并发扫描目录
     * @param root 起始路径
     * @param maxDepth 最大深度
     * @param parallelism 并发线程数 (建议: SSD可设置较高，如 CPU核心数*2; HDD建议设低，避免磁头争抢)
     * @return 扫描到的所有路径列表
     */
    public static Collection<Path> walk(Path root, int maxDepth, int parallelism) {
        // 优化点1: 使用线程安全的并发队列，避免递归合并 List 带来的内存拷贝开销
        ConcurrentLinkedQueue<Path> resultQueue = new ConcurrentLinkedQueue<>();
        
        // 优化点2: 显式创建指定并行度的 ForkJoinPool
        ForkJoinPool customPool = new ForkJoinPool(parallelism);

        try {
            // 提交根任务
            customPool.invoke(new FileWalkAction(root, maxDepth, resultQueue));
        } finally {
            // 必须关闭线程池，否则可能会阻止 JVM 退出（如果不是守护线程模式）
            // 或者复用该 Pool 以避免频繁创建销毁的开销
            customPool.shutdown(); 
        }

        return resultQueue;
    }

    // 使用 RecursiveAction (无返回值)，因为我们直接往 sharedQueue 里塞数据
    private static class FileWalkAction extends RecursiveAction {
        private final Path dir;
        private final int depthRemaining;
        private final ConcurrentLinkedQueue<Path> sharedQueue;

        public FileWalkAction(Path dir, int depthRemaining, ConcurrentLinkedQueue<Path> sharedQueue) {
            this.dir = dir;
            this.depthRemaining = depthRemaining;
            this.sharedQueue = sharedQueue;
        }

        @Override
        protected void compute() {
            // 添加当前目录
            sharedQueue.add(dir);

            if (depthRemaining <= 0) {
                return;
            }

            List<FileWalkAction> subTasks = new ArrayList<>();

            // 优化点3: try-with-resources 自动关闭流
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        // 遇到子目录，创建子任务
                        subTasks.add(new FileWalkAction(entry, depthRemaining - 1, sharedQueue));
                    } else {
                        // 遇到文件，直接加入队列 (无需 Fork)
                        sharedQueue.add(entry);
                    }
                }
            } catch (IOException e) {
                // 权限不足或IO异常处理，根据需要记录日志
                // System.err.println("Error accessing: " + dir + " - " + e.getMessage());
            }

            // 优化点4: 使用 invokeAll 批量提交子任务，比循环 fork 更高效
            if (!subTasks.isEmpty()) {
                invokeAll(subTasks);
            }
        }
    }

    // --- 测试代码 ---
    public static void main(String[] args) {
        Path root = Paths.get("C:\\Windows\\System32"); // 替换为你的测试目录
        int maxDepth = 5;
        // 设定并发数：对于 IO 密集型任务，通常设置为 CPU 核心数的 2倍 或更高
        int threadCount = Runtime.getRuntime().availableProcessors() * 2; 

        System.out.printf("开始扫描，使用线程数: %d ...%n", threadCount);
        
        long start = System.currentTimeMillis();
        Collection<Path> results = ConcurrentFileWalker.walk(root, maxDepth, threadCount);
        long end = System.currentTimeMillis();

        System.out.printf("扫描完成. 耗时: %d ms, 发现文件数: %d%n", (end - start), results.size());
    }
}