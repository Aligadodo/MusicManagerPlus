/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-30
 */
package com.filemanager.tool.rate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通用请求限流组件
 * 基于令牌桶算法实现，支持配置限流周期和请求次数
 */
public class RateLimiter {
    
    private static final ConcurrentHashMap<String, RateLimiter> INSTANCES = new ConcurrentHashMap<>();
    
    private final String key;
    private final int maxRequests;
    private final long periodMs;
    
    private final AtomicLong lastRefillTime;
    private final AtomicInteger availableTokens;
    
    private RateLimiter(String key, int maxRequests, long periodMs) {
        this.key = key;
        this.maxRequests = maxRequests;
        this.periodMs = periodMs;
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        this.availableTokens = new AtomicInteger(maxRequests);
    }
    
    public static RateLimiter getInstance(String key, int maxRequests, long periodMs) {
        return INSTANCES.computeIfAbsent(key, k -> new RateLimiter(k, maxRequests, periodMs));
    }
    
    public static RateLimiter getDefaultInstance() {
        return getInstance("default", 5, 3000); // 默认3秒5次
    }
    
    public synchronized boolean tryAcquire() {
        refillTokens();
        
        if (availableTokens.get() > 0) {
            availableTokens.decrementAndGet();
            return true;
        }
        
        return false;
    }
    
    public boolean tryAcquire(long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (tryAcquire()) {
                return true;
            }
            Thread.sleep(10);
        }
        
        return false;
    }
    
    public void acquire() throws InterruptedException {
        while (!tryAcquire()) {
            Thread.sleep(10);
        }
    }
    
    private void refillTokens() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime.get();
        
        if (elapsed >= periodMs) {
            int newTokens = (int) (elapsed / periodMs) * maxRequests;
            availableTokens.updateAndGet(current -> Math.min(current + newTokens, maxRequests));
            lastRefillTime.set(now);
        }
    }
    
    public int getAvailableTokens() {
        refillTokens();
        return availableTokens.get();
    }
    
    public int getMaxRequests() {
        return maxRequests;
    }
    
    public long getPeriodMs() {
        return periodMs;
    }
    
    public String getKey() {
        return key;
    }
    
    @Override
    public String toString() {
        return "RateLimiter{" +
                "key='" + key + '\'' +
                ", maxRequests=" + maxRequests +
                ", periodMs=" + periodMs +
                ", availableTokens=" + getAvailableTokens() +
                '}';
    }
}
