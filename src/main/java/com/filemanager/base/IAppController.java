package com.filemanager.base;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.ThemeConfig;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * 应用程序控制器接口
 * 定义 View 层与逻辑层交互的契约
 *
 * @author 28667
 */
public interface IAppController {
    List<ChangeRecord> getFullChangeList();
    long getTaskStartTimStamp();
    // --- 数据获取 ---
    ObservableList<File> getSourceRoots();

    ObservableList<IAppStrategy> getPipelineStrategies();

    List<IAppStrategy> getStrategyPrototypes();

    ThemeConfig getCurrentTheme();

    // --- 全局控件引用 (供 View 布局使用) ---
    Stage getPrimaryStage();

    JFXComboBox<String> getCbRecursionMode();

    Spinner<Integer> getSpRecursionDepth();

    Spinner<Integer> getSpGlobalThreads();

    // --- 业务操作 ---
    void addDirectoryAction();

    void removeSourceDir(File dir);

    void clearSourceDirs();

    void addStrategyStep(IAppStrategy template);

    void removeStrategyStep(IAppStrategy strategy);

    void runPipelineAnalysis();

    void runPipelineExecution();

    void forceStop();

    void invalidatePreview(String reason);

    void refreshPreviewTableFilter();

    void openFileInSystem(File f);

    void openParentDirectory(File f);

    // --- 界面交互 ---
    List<IAutoReloadAble> getAutoReloadNodes();

    void showAppearanceDialog();

    void saveConfigAction();

    void loadConfigAction();

    void log(String s);

    void logError(String s);

    Node getGlobalSettingsView();
}