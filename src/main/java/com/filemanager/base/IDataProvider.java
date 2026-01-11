package com.filemanager.base;

import com.filemanager.app.baseui.PreviewView;
import com.filemanager.model.ChangeRecord;
import com.filemanager.model.ThemeConfig;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据提供者接口
 * 定义数据获取与状态相关的方法
 */
public interface IDataProvider {
    /**
     * 获取完整的变更列表
     * @return 变更列表
     */
    List<ChangeRecord> getFullChangeList();
    
    /**
     * 获取任务开始时间戳
     * @return 时间戳
     */
    long getTaskStartTimStamp();

    /**
     * 获取源根路径列表
     * @return 根路径列表
     */
    ObservableList<File> getSourceRoots();

    /**
     * 获取管道策略列表
     * @return 策略列表
     */
    ObservableList<IAppStrategy> getPipelineStrategies();

    /**
     * 获取策略原型列表
     * @return 原型列表
     */
    List<IAppStrategy> getStrategyPrototypes();

    /**
     * 获取当前主题配置
     * @return 主题配置
     */
    ThemeConfig getCurrentTheme();
    
    /**
     * 为文件查找根路径
     * @param filePath 文件路径
     * @return 根路径
     */
    String findRootPathForFile(String filePath);
    
    /**
     * 获取预览视图
     * @return 预览视图
     */
    PreviewView getPreviewView();
    
    /**
     * 获取任务运行状态
     * @return 运行状态
     */
    AtomicBoolean getTaskRunningStatus();
    
    /**
     * 设置完整的变更列表
     * @param changeList 变更列表
     */
    void setFullChangeList(List<ChangeRecord> changeList);
}