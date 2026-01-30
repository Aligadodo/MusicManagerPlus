/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-30
 */
package com.filemanager.tool.rate.config;

import com.filemanager.strategy.base.IPersistableConfig;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.IntegerStringConverter;

import java.util.Properties;

/**
 * 限流配置类
 * 实现IPersistableConfig接口，支持配置限流参数
 */
public class RateLimiterConfig implements IPersistableConfig {
    
    private static final String PREFIX = "rate_limiter_";
    private static final String MAX_REQUESTS_KEY = PREFIX + "max_requests";
    private static final String PERIOD_MS_KEY = PREFIX + "period_ms";
    
    private int maxRequests = 5;
    private int periodMs = 3000;
    
    private final VBox configBox;
    private final Spinner<Integer> spMaxRequests;
    private final Spinner<Integer> spPeriodMs;
    
    public RateLimiterConfig() {
        configBox = new VBox(10);
        configBox.setPadding(new javafx.geometry.Insets(10));
        
        // 最大请求数配置
        HBox maxRequestsBox = new HBox(10);
        maxRequestsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblMaxRequests = new Label("最大请求数:");
        lblMaxRequests.setPrefWidth(100);
        
        spMaxRequests = new Spinner<>();
        spMaxRequests.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, maxRequests, 1));
        spMaxRequests.setEditable(true);
        spMaxRequests.setPrefWidth(100);
        
        // 修复Spinner编辑功能
        spMaxRequests.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int value = Integer.parseInt(newVal);
                if (value >= 1 && value <= 100) {
                    spMaxRequests.getValueFactory().setValue(value);
                }
            } catch (NumberFormatException e) {
                // 忽略无效输入
            }
        });
        
        maxRequestsBox.getChildren().addAll(lblMaxRequests, spMaxRequests, new Label("次"));
        
        // 周期时间配置
        HBox periodMsBox = new HBox(10);
        periodMsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblPeriodMs = new Label("限流周期:");
        lblPeriodMs.setPrefWidth(100);
        
        spPeriodMs = new Spinner<>();
        spPeriodMs.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 10000, periodMs, 100));
        spPeriodMs.setEditable(true);
        spPeriodMs.setPrefWidth(100);
        
        // 修复Spinner编辑功能
        spPeriodMs.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int value = Integer.parseInt(newVal);
                if (value >= 100 && value <= 10000) {
                    spPeriodMs.getValueFactory().setValue(value);
                }
            } catch (NumberFormatException e) {
                // 忽略无效输入
            }
        });
        
        periodMsBox.getChildren().addAll(lblPeriodMs, spPeriodMs, new Label("毫秒"));
        
        // 添加到主容器
        configBox.getChildren().addAll(
            new Label("=== 限流配置 ==="),
            maxRequestsBox,
            periodMsBox,
            new Label("说明: 配置在指定时间内允许的最大请求次数")
        );
    }
    
    @Override
    public Node getConfigNode() {
        return configBox;
    }
    
    @Override
    public void captureParams() {
        this.maxRequests = spMaxRequests.getValue();
        this.periodMs = spPeriodMs.getValue();
    }
    
    @Override
    public void saveConfig(Properties props) {
        props.setProperty(MAX_REQUESTS_KEY, String.valueOf(maxRequests));
        props.setProperty(PERIOD_MS_KEY, String.valueOf(periodMs));
    }
    
    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey(MAX_REQUESTS_KEY)) {
            try {
                maxRequests = Integer.parseInt(props.getProperty(MAX_REQUESTS_KEY));
                spMaxRequests.getValueFactory().setValue(maxRequests);
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
        
        if (props.containsKey(PERIOD_MS_KEY)) {
            try {
                periodMs = Integer.parseInt(props.getProperty(PERIOD_MS_KEY));
                spPeriodMs.getValueFactory().setValue(periodMs);
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
    }
    
    public int getMaxRequests() {
        return maxRequests;
    }
    
    public int getPeriodMs() {
        return periodMs;
    }
    
    @Override
    public void reload() {
        // 配置重载逻辑
        captureParams();
    }
}
