package com.filemanager.app;

import com.filemanager.model.ThemeConfig;
import com.filemanager.strategy.AppStrategy;
import com.filemanager.tool.StyleFactory;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import org.controlsfx.control.CheckComboBox;

import java.io.File;
import java.util.List;

/**
 * 应用程序控制器接口
 * 定义 View 层与逻辑层交互的契约
 *
 * @author 28667
 */
public interface IAppController {

    // --- 数据获取 ---
    ObservableList<File> getSourceRoots();

    ObservableList<AppStrategy> getPipelineStrategies();

    List<AppStrategy> getStrategyPrototypes();

    ThemeConfig getCurrentTheme();

    // --- 全局控件引用 (供 View 布局使用) ---
    JFXComboBox<String> getCbRecursionMode();

    Spinner<Integer> getSpRecursionDepth();

    CheckComboBox<String> getCcbFileTypes();

    JFXTextField getTxtSearchFilter();

    JFXComboBox<String> getCbStatusFilter();

    JFXCheckBox getChkHideUnchanged();

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

    // --- 界面交互 ---
    void showAppearanceDialog();

    void saveConfigAction();

    void loadConfigAction();

    String getBgImagePath();

    void setBgImagePath(String bgPath);

    void log(String s);

    void logError(String s);

    // --- Config IO (包含线程数保存) ---
    void displayRunning(String s);

    Node getGlobalSettingsView();
}