package com.filemanager.tool.file;

import com.filemanager.app.FileManagerPlusApp;
import com.filemanager.model.RuleCondition;
import com.filemanager.model.RuleConditionGroup;
import com.filemanager.model.ThemeConfig;
import com.filemanager.strategy.AppStrategy;
import com.filemanager.type.ConditionType;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.stream.Collectors;

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
            // 1. 全局筛选配置
            props.setProperty("g_recMode", String.valueOf(app.getCbRecursionMode().getSelectionModel().getSelectedIndex()));
            props.setProperty("g_recDepth", String.valueOf(app.getSpRecursionDepth().getValue()));
            props.setProperty("g_threads", String.valueOf(app.getSpGlobalThreads().getValue()));

            // 2. 源目录
            ObservableList<File> roots = app.getSourceRoots();
            if (!roots.isEmpty()) {
                String paths = roots.stream().map(File::getAbsolutePath).collect(Collectors.joining("||"));
                props.setProperty("g_sources", paths);
            } else {
                props.remove("g_sources");
            }

            // 3. 外观配置
            ThemeConfig theme = app.getCurrentTheme();
            props.setProperty("ui_accent_color", theme.getAccentColor());
            props.setProperty("ui_text_color", theme.getTextColor());
            props.setProperty("ui_glass_opacity", String.valueOf(theme.getGlassOpacity()));
            props.setProperty("ui_dark_bg", String.valueOf(theme.isDarkBackground()));
            props.setProperty("ui_bg_image", app.getBgImagePath());

            // 4. 流水线配置 (核心)
            savePipeline();

            props.store(os, "FileManager Plus Config");
            app.log("配置已保存至: " + file.getName());
        } catch (Exception e) {
            app.log("配置保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadConfig(File file) {
        if (!file.exists()) return;
        try (FileInputStream is = new FileInputStream(file)) {
            props.clear();
            props.load(is);

            // 1. 恢复全局筛选
            if (props.containsKey("g_recMode")) {
                app.getCbRecursionMode().getSelectionModel().select(Integer.parseInt(props.getProperty("g_recMode")));
            }
            if (props.containsKey("g_recDepth")) {
                app.getSpRecursionDepth().getValueFactory().setValue(Integer.parseInt(props.getProperty("g_recDepth")));
            }
            if (props.containsKey("g_threads")) {
                app.getSpGlobalThreads().getValueFactory().setValue(Integer.parseInt(props.getProperty("g_threads")));
            }

            // 2. 恢复源目录
            String paths = props.getProperty("g_sources");
            if (paths != null && !paths.isEmpty()) {
                app.getSourceRoots().clear();
                for (String p : paths.split("\\|\\|")) {
                    File f = new File(p);
                    if (f.exists()) app.getSourceRoots().add(f);
                }
            }

            // 3. 恢复外观
            ThemeConfig theme = app.getCurrentTheme();
            if (props.containsKey("ui_accent_color")) theme.setAccentColor(props.getProperty("ui_accent_color"));
            if (props.containsKey("ui_text_color")) theme.setTextColor(props.getProperty("ui_text_color"));
            if (props.containsKey("ui_glass_opacity")) theme.setGlassOpacity(Double.parseDouble(props.getProperty("ui_glass_opacity")));
            if (props.containsKey("ui_dark_bg")) theme.setDarkBackground(Boolean.parseBoolean(props.getProperty("ui_dark_bg")));
            
            String bgPath = props.getProperty("ui_bg_image");
            if (bgPath != null) app.setBgImagePath(bgPath);
            
            // 立即应用外观
            app.applyAppearance();

            // 4. 恢复流水线
            loadPipeline();
            
            app.log("配置已加载: " + file.getName());
            
        } catch (Exception e) {
            app.logError("配置加载失败: " + e.getMessage());
        }
    }

    // --- Internal: Pipeline Serialization ---

    private void savePipeline() {
        ObservableList<AppStrategy> strategies = app.getPipelineStrategies();
        props.setProperty("pipeline.size", String.valueOf(strategies.size()));
        
        for (int i = 0; i < strategies.size(); i++) {
            AppStrategy s = strategies.get(i);
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
                AppStrategy strategy = (AppStrategy) clazz.getDeclaredConstructor().newInstance();
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
                app.log("策略恢复失败 [" + i + "]: " + e.getMessage());
            }
        }
        
        // 通知主程序刷新列表选中状态
        app.refreshPipelineSelection();
    }
}