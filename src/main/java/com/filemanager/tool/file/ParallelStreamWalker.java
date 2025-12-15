package com.filemanager.tool.file;

import lombok.var;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
    public static Stream<Path> walk(Path root, int maxDepth, int parallelism) {
        // 1. 创建阻塞队列作为缓冲区，只存储Path类型
        BlockingQueue<Path> queue = new LinkedBlockingQueue<>(1024);

        // 2. 创建线程池
        ForkJoinPool pool = new ForkJoinPool(parallelism);

        // 3. 启动后台扫描任务
        pool.submit(() -> {
            try {
                // 执行递归扫描
                pool.invoke(new FileWalkAction(root, maxDepth, queue));
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

    // --- 测试代码 ---
    public static void main(String[] args) {
        Path root = Paths.get("C:\\Windows\\System32"); // 替换为你的测试目录

        System.out.println("开始流式扫描...");
        long start = System.currentTimeMillis();

        // try-with-resources 确保 Stream 被正确关闭，从而触发线程池关闭
        try (Stream<Path> stream = ParallelStreamWalker.walk(root, 5, 8)) {
            stream
                    .map(Path::toFile)
                    // 过滤出文件，并筛选出以 .dll 结尾的文件名
                    .filter(File::isFile)
                    .filter(f -> f.getName().endsWith(".dll"))
                    .limit(100) // 限制数量，这会提前终止后台扫描！
                    .forEach(f -> System.out.println(f.getName()));
        }

        long end = System.currentTimeMillis();
        System.out.println("扫描和处理耗时: " + (end - start) + "ms");
    }

    // 递归任务类
    private static class FileWalkAction extends RecursiveAction {
        private final Path dir;
        private final int depthRemaining;
        private final BlockingQueue<Path> queue;

        public FileWalkAction(Path dir, int depthRemaining, BlockingQueue<Path> queue) {
            this.dir = dir;
            this.depthRemaining = depthRemaining;
            this.queue = queue;
        }

        @Override
        protected void compute() {
            try {
                // 尝试将当前目录放入队列
                queue.put(dir);
            } catch (InterruptedException e) {
                return; // 如果被打断（stream关闭），直接退出
            }

            if (depthRemaining <= 0) return;

            var subTasks = new java.util.ArrayList<FileWalkAction>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        subTasks.add(new FileWalkAction(entry, depthRemaining - 1, queue));
                    } else {
                        // 是文件，直接放入队列
                        try {
                            queue.put(entry);
                        } catch (InterruptedException e) {
                            return; // 退出
                        }
                    }
                }
            } catch (IOException e) {
                // 忽略访问权限异常或IO错误
            }

            if (!subTasks.isEmpty()) {
                invokeAll(subTasks);
            }
        }
    }
}