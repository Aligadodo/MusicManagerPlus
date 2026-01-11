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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 线程池管理器，负责创建和管理线程池
 */
public class ThreadPoolManager {
    
    // 全局线程池模式
    public static final String MODE_GLOBAL = "全局统一配置";
    // 根路径独立线程池模式
    public static final String MODE_ROOT_PATH = "根路径独立配置";
    
    // 当前线程池模式
    private String currentMode;
    
    // 全局线程池
    private RetryableThreadPool globalExecutor;
    
    // 根路径线程池映射
    private Map<String, RetryableThreadPool> rootPathExecutors;
    
    // 全局线程数配置
    private int globalPreviewThreads;
    private int globalExecutionThreads;
    
    // 根路径线程数配置
    private Map<String, Integer> rootPathPreviewThreads;
    private Map<String, Integer> rootPathExecutionThreads;
    
    /**
     * 构造函数
     */
    public ThreadPoolManager() {
        this.currentMode = MODE_GLOBAL;
        this.rootPathExecutors = new ConcurrentHashMap<>();
        this.rootPathPreviewThreads = new ConcurrentHashMap<>();
        this.rootPathExecutionThreads = new ConcurrentHashMap<>();
        this.globalPreviewThreads = 5; // 默认预览线程数
        this.globalExecutionThreads = 5; // 默认执行线程数
    }
    
    /**
     * 获取或创建预览线程池
     * @param rootPath 根路径
     * @return 线程池
     */
    public RetryableThreadPool getPreviewThreadPool(String rootPath) {
        if (MODE_GLOBAL.equals(currentMode)) {
            if (globalExecutor == null) {
                globalExecutor = new RetryableThreadPool(1, globalPreviewThreads, 10, TimeUnit.SECONDS);
            }
            return globalExecutor;
        } else {
            return rootPathExecutors.computeIfAbsent(rootPath + "_preview", k -> {
                int threads = rootPathPreviewThreads.getOrDefault(rootPath, globalPreviewThreads);
                return new RetryableThreadPool(1, threads, 10, TimeUnit.SECONDS);
            });
        }
    }
    
    /**
     * 获取或创建执行线程池
     * @param rootPath 根路径
     * @return 线程池
     */
    public RetryableThreadPool getExecutionThreadPool(String rootPath) {
        if (MODE_GLOBAL.equals(currentMode)) {
            if (globalExecutor == null) {
                globalExecutor = new RetryableThreadPool(1, globalExecutionThreads, 10, TimeUnit.SECONDS);
            }
            return globalExecutor;
        } else {
            return rootPathExecutors.computeIfAbsent(rootPath + "_execution", k -> {
                int threads = rootPathExecutionThreads.getOrDefault(rootPath, globalExecutionThreads);
                return new RetryableThreadPool(1, threads, 10, TimeUnit.SECONDS);
            });
        }
    }
    
    /**
     * 设置全局预览线程数
     * @param threads 线程数
     */
    public void setGlobalPreviewThreads(int threads) {
        this.globalPreviewThreads = threads;
        if (MODE_GLOBAL.equals(currentMode) && globalExecutor != null) {
            globalExecutor.setCorePoolSize(1);
            globalExecutor.setMaximumPoolSize(threads);
        }
    }
    
    /**
     * 设置全局执行线程数
     * @param threads 线程数
     */
    public void setGlobalExecutionThreads(int threads) {
        this.globalExecutionThreads = threads;
        if (MODE_GLOBAL.equals(currentMode) && globalExecutor != null) {
            globalExecutor.setCorePoolSize(1);
            globalExecutor.setMaximumPoolSize(threads);
        }
    }
    
    /**
     * 设置根路径预览线程数
     * @param rootPath 根路径
     * @param threads 线程数
     */
    public void setRootPathPreviewThreads(String rootPath, int threads) {
        this.rootPathPreviewThreads.put(rootPath, threads);
        if (MODE_ROOT_PATH.equals(currentMode)) {
            RetryableThreadPool executor = rootPathExecutors.get(rootPath + "_preview");
            if (executor != null) {
                executor.setCorePoolSize(1);
                executor.setMaximumPoolSize(threads);
            }
        }
    }
    
    /**
     * 设置根路径执行线程数
     * @param rootPath 根路径
     * @param threads 线程数
     */
    public void setRootPathExecutionThreads(String rootPath, int threads) {
        this.rootPathExecutionThreads.put(rootPath, threads);
        if (MODE_ROOT_PATH.equals(currentMode)) {
            RetryableThreadPool executor = rootPathExecutors.get(rootPath + "_execution");
            if (executor != null) {
                executor.setCorePoolSize(1);
                executor.setMaximumPoolSize(threads);
            }
        }
    }
    
    /**
     * 设置线程池模式
     * @param mode 模式
     */
    public void setThreadPoolMode(String mode) {
        this.currentMode = mode;
        // 如果切换模式，需要重新创建线程池
        shutdownAll();
    }
    
    /**
     * 获取当前线程池模式
     * @return 模式
     */
    public String getThreadPoolMode() {
        return currentMode;
    }
    
    /**
     * 关闭所有线程池
     */
    public void shutdownAll() {
        if (globalExecutor != null) {
            globalExecutor.shutdown();
            globalExecutor = null;
        }
        for (RetryableThreadPool executor : rootPathExecutors.values()) {
            executor.shutdown();
        }
        rootPathExecutors.clear();
    }
    
    /**
     * 等待所有线程池终止
     */
    public void awaitTermination() {
        if (globalExecutor != null) {
            try {
                while (!globalExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    // 等待线程池终止
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        for (RetryableThreadPool executor : rootPathExecutors.values()) {
            try {
                while (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    // 等待线程池终止
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
