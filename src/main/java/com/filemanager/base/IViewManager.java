package com.filemanager.base;

import javafx.scene.Node;
import javafx.stage.Stage;
import java.util.List;

/**
 * 视图管理器接口
 * 定义视图管理相关的方法
 */
public interface IViewManager {
    /**
     * 切换视图
     * @param node 新视图节点
     */
    void switchView(Node node);

    /**
     * 获取主舞台
     * @return 主舞台
     */
    Stage getPrimaryStage();

    /**
     * 获取自动重载节点列表
     * @return 节点列表
     */
    List<IAutoReloadAble> getAutoReloadNodes();

    /**
     * 显示外观设置对话框
     */
    void showAppearanceDialog();

    /**
     * 获取全局设置视图
     * @return 设置视图节点
     */
    Node getGlobalSettingsView();
}