package com.filemanager.backend.util;

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

public class ParallelStreamWalker {

    private static final Path END_MARKER = Paths.get("___END_OF_SCANNING_MARKER___");

    public static Stream<Path> walk(Path root, int minDepth, int maxDepth, AtomicInteger globalLimitRemaining, AtomicInteger dirLimitRemaining, int parallelism, AtomicBoolean isTaskRunning) {
        BlockingQueue<Path> queue = new LinkedBlockingQueue<>(1024);

        ForkJoinPool pool = new ForkJoinPool(parallelism);

        pool.submit(() -> {
            try {
                pool.invoke(new FileWalkAction(root, 0, minDepth, maxDepth, globalLimitRemaining, dirLimitRemaining, queue, isTaskRunning));
            } finally {
                offerMarker(queue);
            }
        });

        Iterator<Path> fileIterator = new Iterator<Path>() {
            private Path nextFile;
            private boolean finished = false;

            @Override
            public boolean hasNext() {
                if (nextFile != null) return true;
                if (finished) return false;

                try {
                    Path item = queue.take();

                    if (item == END_MARKER) {
                        finished = true;
                        return false;
                    }

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

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(fileIterator, Spliterator.NONNULL | Spliterator.ORDERED),
                false
        ).onClose(() -> {
            pool.shutdownNow();
        });
    }

    private static void offerMarker(BlockingQueue<Path> queue) {
        try {
            queue.put(END_MARKER);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

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
                putAndCheckLimit(dir);
            }
            if (depthRemaining <= 0 || !isTaskRunning.get() || reachedLimit()) return;

            ArrayList<FileWalkAction> subTasks = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        subTasks.add(new FileWalkAction(entry, currentDepth + 1, minDepth, depthRemaining - 1, globalLimitRemaining, dirLimitRemaining, queue, isTaskRunning));
                    } else if (this.currentDepth >= minDepth) {
                        putAndCheckLimit(entry);
                    }
                }
            } catch (IOException e) {
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
                return true;
            }
            return true;
        }
    }
}
