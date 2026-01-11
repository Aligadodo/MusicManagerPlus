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

import com.filemanager.app.FileManagerPlusApp;
import com.filemanager.app.base.IAppStrategy;
import com.filemanager.model.RuleCondition;
import com.filemanager.model.RuleConditionGroup;
import com.filemanager.type.ConditionType;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * 配置管理器
 * 负责全局配置、策略流水线、外观设置的持久化
 */
public class ConfigFileManager {

    private final FileManagerPlusApp app;
    private final Properties props = new Properties();

    public ConfigFileManager(FileManagerPlusApp app) {
        this.app = app;
    }

    public void saveConfig(File file) {
        try (FileOutputStream os = new FileOutputStream(file)) {
            // 1. 保存全局组件配置
            app.getAutoReloadNodes().forEach(node -> node.saveConfig(props));
            // 2. 流水线配置 (核心)
            savePipeline();
            props.store(os, "FileManager Plus Config");
            app.log("配置已保存至: " + file.getName());
        } catch (Exception e) {
            app.logError("配置保存失败: " + e.getMessage());
        }
    }

    public void loadConfig(File file) {
        if (!file.exists()) return;
        try (FileInputStream is = new FileInputStream(file)) {
            props.clear();
            props.load(is);
            // 1. 恢复全局组件配置
            app.getAutoReloadNodes().forEach(node -> node.loadConfig(props));
            // 2. 恢复流水线
            loadPipeline();
            // 3.立即应用外观
            app.applyAppearance();

            app.log("配置已加载: " + file.getAbsolutePath());
        } catch (Exception e) {
            app.logError("配置加载失败: " + ExceptionUtils.getStackTrace(e));
        }
    }

    // --- Internal: Pipeline Serialization ---

    private void savePipeline() {
        ObservableList<IAppStrategy> strategies = app.getPipelineStrategies();
        props.setProperty("pipeline.size", String.valueOf(strategies.size()));
        for (int i = 0; i < strategies.size(); i++) {
            IAppStrategy s = strategies.get(i);
            String prefix = "pipeline." + i + ".";
            // 保存类名以便反射
            props.setProperty(prefix + "class", s.getClass().getName());
            
            // 让策略保存自己的参数
            // 这里使用一个临时的 Props 来捕获策略的参数，然后加上前缀存入全局 Props
            Properties strategyProps = new Properties();
            s.saveConfig(strategyProps);
            for (String key : strategyProps.stringPropertyNames()) {
                props.setProperty(prefix + "param." + key, strategyProps.getProperty(key));
            }
            
            // 保存通用前置条件 (Condition Groups)
            int gSize = s.getConditionGroups().size();
            props.setProperty(prefix + "cg.size", String.valueOf(gSize));
            
            for (int j = 0; j < gSize; j++) {
                RuleConditionGroup group = s.getConditionGroups().get(j);
                String gPre = prefix + "cg." + j + ".";
                int cSize = group.getConditions().size();
                props.setProperty(gPre + "c.size", String.valueOf(cSize));
                
                for (int k = 0; k < cSize; k++) {
                    RuleCondition c = group.getConditions().get(k);
                    props.setProperty(gPre + "c." + k + ".type", c.getType().name());
                    props.setProperty(gPre + "c." + k + ".val", c.getValue() == null ? "" : c.getValue());
                }
            }
        }
    }

    private void loadPipeline() {
        app.getPipelineStrategies().clear();
        // 清空 UI 容器需要主程序配合，这里不直接操作 UI 容器，而是通过数据驱动
        // 实际上需要在主程序加载完后刷新 UI
        
        int size = Integer.parseInt(props.getProperty("pipeline.size", "0"));
        for (int i = 0; i < size; i++) {
            String prefix = "pipeline." + i + ".";
            String className = props.getProperty(prefix + "class");
            if (className == null) continue;

            try {
                // 反射创建实例
                Class<?> clazz = Class.forName(className);
                IAppStrategy strategy = (IAppStrategy) clazz.getDeclaredConstructor().newInstance();
                strategy.setContext(app);

                // 恢复策略参数
                Properties strategyProps = new Properties();
                String paramPrefix = prefix + "param.";
                for (String key : props.stringPropertyNames()) {
                    if (key.startsWith(paramPrefix)) {
                        strategyProps.setProperty(key.substring(paramPrefix.length()), props.getProperty(key));
                    }
                }
                strategy.loadConfig(strategyProps);

                // 恢复前置条件
                strategy.getConditionGroups().clear();
                int gSize = Integer.parseInt(props.getProperty(prefix + "cg.size", "0"));
                for (int j = 0; j < gSize; j++) {
                    RuleConditionGroup group = new RuleConditionGroup();
                    String gPre = prefix + "cg." + j + ".";
                    int cSize = Integer.parseInt(props.getProperty(gPre + "c.size", "0"));
                    for (int k = 0; k < cSize; k++) {
                        String typeStr = props.getProperty(gPre + "c." + k + ".type");
                        String val = props.getProperty(gPre + "c." + k + ".val");
                        if (typeStr != null) {
                            group.add(new RuleCondition(ConditionType.valueOf(typeStr), val));
                        }
                    }
                    strategy.getConditionGroups().add(group);
                }

                app.getPipelineStrategies().add(strategy);

            } catch (Exception e) {
                app.logError("策略恢复失败 [" + i + "]: " + e.getMessage());
            }
        }
        // 通知主程序刷新列表选中状态
        app.refreshPipelineSelection();
    }
}