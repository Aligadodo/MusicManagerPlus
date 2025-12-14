package com.filemanager.app;

import com.filemanager.strategy.AppStrategy;
import com.filemanager.ui.StyleFactory;
import com.filemanager.model.ThemeConfig;
import javafx.collections.ObservableList;
import javafx.scene.control.Spinner;
import org.controlsfx.control.CheckComboBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXCheckBox;

import java.io.File;
import java.util.List;

/**
 * 应用程序控制器接口
 * 定义 View 层与逻辑层交互的契约
 */
public interface IAppController {
    
    // --- 数据获取 ---
    ObservableList<File> getSourceRoots();
    ObservableList<AppStrategy> getPipelineStrategies();
    List<AppStrategy> getStrategyPrototypes();
    ThemeConfig getCurrentTheme();
    StyleFactory getStyleFactory();

    // --- 全局控件引用 (供 View 布局使用) ---
    JFXComboBox<String> getCbRecursionMode();
    Spinner<Integer> getSpRecursionDepth();
    CheckComboBox<String> getCcbFileTypes();
    JFXTextField getTxtSearchFilter();
    JFXComboBox<String> getCbStatusFilter();
    JFXCheckBox getChkHideUnchanged();
    JFXCheckBox getChkSaveLog();
    Spinner<Integer> getSpGlobalThreads();

    // --- 业务操作 ---
    void addDirectoryAction();
    void removeSourceDir(File dir);
    void clearSourceDirs();
    
    void addStrategyStep(AppStrategy template);
    void removeStrategyStep(AppStrategy strategy);
    
    void runPipelineAnalysis();
    void runPipelineExecution();
    void forceStop();
    
    void invalidatePreview(String reason);
    void refreshPreviewTableFilter();
    
    void openFileInSystem(File f);
    void openParentDirectory(File f);
    
    void clearLog();
    
    // --- 界面交互 ---
    void showAppearanceDialog();
    void saveConfigAction();
    void loadConfigAction();

    String getBgImagePath();
    void setBgImagePath(String bgPath);

    public void log(String s);
}