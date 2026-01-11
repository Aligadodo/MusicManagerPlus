/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.tool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自动重试且无队列的动态线程池
 */
public class RetryableThreadPool {

    private final ThreadPoolExecutor executor;

    public RetryableThreadPool(int coreSize, int maxSize, long keepAliveTime, TimeUnit unit) {
        // 使用 SynchronousQueue 实现无任务等待队列
        // 这种队列本身不存储任务，每一个 put 操作必须等待一个 take 操作
        BlockingQueue<Runnable> queue = new SynchronousQueue<>();

        this.executor = new ThreadPoolExecutor(
                coreSize,
                maxSize,
                keepAliveTime,
                unit,
                queue,
                new CustomThreadFactory(),
                new BlockingRetryPolicy() // 自定义重试策略
        );
    }

    /**
     * 提交任务
     */
    public void execute(Runnable task) {
        executor.execute(task);
    }

    /**
     * 动态调整核心线程数
     */
    public void setCorePoolSize(int size) {
        executor.setCorePoolSize(size);
    }

    /**
     * 动态调整最大线程数
     */
    public void setMaximumPoolSize(int size) {
        executor.setMaximumPoolSize(size);
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void shutdownNow() {
        executor.shutdownNow();
    }

    public boolean awaitTermination(long time, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(time, unit);
    }

    /**
     * 自定义拒绝策略：当队列满（实际上是无空闲线程）时，阻塞提交线程直到成功
     */
    private static class BlockingRetryPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                // 如果线程池未关闭
                if (!executor.isShutdown()) {
                    // 利用 SynchronousQueue 的 put 方法，它会一直阻塞直到有线程来取任务
                    // 这实现了“自动重试”的效果，且不会占用额外的内存队列
                    executor.getQueue().put(r);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Task interrupted during retry", e);
            }
        }
    }

    /**
     * 线程工厂，方便追踪线程
     */
    private static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger count = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "RetryPool-Worker-" + count.getAndIncrement());
        }
    }

}