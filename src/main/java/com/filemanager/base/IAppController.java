package com.filemanager.base;

import com.filemanager.app.baseui.PreviewView;
import com.filemanager.app.components.tools.MultiThreadTaskEstimator;
import com.filemanager.model.ChangeRecord;
import com.filemanager.model.ThemeConfig;
import com.filemanager.type.TaskStatus;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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

    Spinner<Integer> getSpPreviewThreads();

    Spinner<Integer> getSpExecutionThreads();

    JFXCheckBox getAutoRun();

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
    AtomicBoolean getTaskRunningStatus();

    List<IAutoReloadAble> getAutoReloadNodes();

    void showAppearanceDialog();

    void saveConfigAction();

    void loadConfigAction();

    void log(String s);

    void logError(String s);

    Node getGlobalSettingsView();
    
    // --- 新增方法 ---
    void setRunningUI(String msg);
    
    String findRootPathForFile(String filePath);
    
    PreviewView getPreviewView();
    
    void setFullChangeList(List<ChangeRecord> changeList);
    
    void changeExecuteButton(boolean enabled);
    
    void changePreviewButton(boolean enabled);
    
    void changeStopButton(boolean enabled);
    
    void updateProgressStatus(TaskStatus status);
    
    void bindProgress(Task<?> task);
    
    void updateRunningProgress(String msg);
    
    void updateStats();
    
    void refreshComposeView();
    
    List<File> scanFilesRobust(File root, int minDepth, int maxDepth, Consumer<String> msg);

    boolean setThreadPoolMode(String newVal);

    Map<String, Integer> getRootPathThreadConfig();

    MultiThreadTaskEstimator getRootPathEstimator(String rootPath);
}