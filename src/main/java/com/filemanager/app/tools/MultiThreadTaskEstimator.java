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

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多线程任务执行进度预估工具类
 */
public class MultiThreadTaskEstimator {
    // 总任务数
    private final long totalTasks;
    // 已完成任务数
    private final AtomicLong startTasks= new AtomicLong(0);
    // 已完成任务数
    private final AtomicLong completedTasks= new AtomicLong(0);
    // 已失败任务数
    private final AtomicLong failedTasks= new AtomicLong(0);


    // 滑动窗口：用于存储最近完成任务的时间戳，计算近期吞吐量
    // 窗口越大越平滑，窗口越小对近期波动越敏感
    private final int windowSize;
    private final ConcurrentLinkedDeque<Long> completionWindow;
    // 任务开始时间 (ms)
    private long startTime;
    // 任务结束时间 (ms)
    private long endTime;
    private volatile boolean isStarted = false;
    private volatile boolean isFinished = false;

    /**
     * @param totalTasks 总任务数
     * @param windowSize 样本窗口大小（建议设为线程数的 5-10 倍）
     */
    public MultiThreadTaskEstimator(long totalTasks, int windowSize) {
        this.totalTasks = totalTasks;
        this.windowSize = windowSize;
        this.completionWindow = new ConcurrentLinkedDeque<>();
    }

    /**
     * 辅助格式化工具
     */
    public static String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * 标记任务正式开始
     */
    public synchronized void start() {
        this.startTime = System.currentTimeMillis();
        this.isStarted = true;
    }

    /**
     * 每当一个子任务完成时调用
     */
    public void oneStarted() {
        if (!isStarted || isFinished) return;
        startTasks.incrementAndGet();
    }

    /**
     * 每当一个子任务完成时调用
     */
    public void oneCompleted() {
        if (!isStarted || isFinished) return;

        long now = System.currentTimeMillis();
        completedTasks.incrementAndGet();

        // 记录完成时间戳到滑动窗口
        completionWindow.offerLast(now);
        if (completionWindow.size() > windowSize) {
            completionWindow.pollFirst();
        }
    }

    public int getRunningTaskCount() {
        return startTasks.intValue() - completedTasks.intValue();
    }

    /**
     * 标记全部任务结束
     */
    public synchronized void finish() {
        this.isFinished = true;
        this.endTime = System.currentTimeMillis();
    }

    /**
     * 获取已运行总时长（毫秒）
     */
    public long getElapsedMillis() {
        if (!isStarted) return 0;
        long end = isFinished ? endTime : System.currentTimeMillis();
        return end - startTime;
    }

    /**
     * 获取预估剩余时长（毫秒）
     */
    public long getEstimatedRemainingMillis() {
        if (!isStarted || isFinished || completedTasks.get() == 0) return -1;

        long now = System.currentTimeMillis();
        long done = completedTasks.get();
        long remaining = totalTasks - done;

        if (remaining <= 0) return 0;

        // 算法核心：计算近期吞吐量 (Tasks per ms)
        double tasksPerMs;

        if (completionWindow.size() < 2) {
            // 如果窗口样本不足，退化为全局平均速度
            tasksPerMs = (double) done / (now - startTime);
        } else {
            // 计算窗口内第一个和最后一个样本的时间差
            long firstInWindow = completionWindow.peekFirst();
            long lastInWindow = completionWindow.peekLast();
            long duration = lastInWindow - firstInWindow;

            if (duration > 0) {
                // 近期速度 = 窗口内任务数 / 窗口时间跨度
                tasksPerMs = (double) completionWindow.size() / duration;
            } else {
                // 极短时间内大量完成，降级处理
                tasksPerMs = (double) done / (now - startTime);
            }
        }

        return tasksPerMs > 0 ? (long) (remaining / tasksPerMs) : -1;
    }

    /**
     * 获取格式化的预估剩余时间 (HH:mm:ss)
     */
    public String getFormattedRemainingTime() {
        long ms = getEstimatedRemainingMillis();
        if (ms < 0) return "计算中...";
        if (ms == 0) return "00:00:00";
        return formatDuration(ms);
    }

    // Getters
    public long getCompletedTasks() {
        return completedTasks.get();
    }

    public String getProgressPercentage() {
        return String.format("%.2f", (double) completedTasks.get() / totalTasks * 100);
    }

    /**
     * 获取进度值 (0.0 到 1.0)
     */
    public double getProgress() {
        if (totalTasks == 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) completedTasks.get() / totalTasks);
    }
    
    /**
     * 获取总任务数
     */
    public long getTotalTasks() {
        return totalTasks;
    }
    
    public String getDisplayInfo() {
        return " 总共：" + totalTasks
                + " 已处理:" + completedTasks.get()
                + " 耗时:" + MultiThreadTaskEstimator.formatDuration(System.currentTimeMillis() - startTime)
                + " 进度:" + getProgressPercentage()
                + "% 预计剩余时间：" + this.getFormattedRemainingTime();
    }


}