/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-25
 */
package com.filemanager.app.tools.display.styles;

import com.filemanager.app.tools.display.ThemeConfig;
import javafx.scene.Node;
import javafx.scene.control.*;

/**
 * 样式管理器工厂类
 * 负责根据组件类型创建和应用相应的样式管理器
 *
 * @author hrcao
 */
public class StyleManagerFactory {

    /**
     * 根据组件类型应用相应的样式
     *
     * @param node  组件节点
     * @param theme 主题配置
     */
    public static void applyStyle(Node node, ThemeConfig theme) {
        if (node == null || theme == null) {
            return;
        }

        // 根据组件类型应用不同的样式
        if (node instanceof TreeTableView) {
            TreeTableViewStyle.applyStyle((TreeTableView<?>) node, theme);
        } else if (node instanceof TableView) {
            TableViewStyle.applyStyle((TableView<?>) node, theme);
        } else if (node instanceof TreeView) {
            TreeViewStyle.applyStyle((TreeView<?>) node, theme);
        } else if (node instanceof ListView) {
            ListViewStyle.applyStyle((ListView<?>) node, theme);
        } else if (node instanceof TextArea) {
            TextAreaStyle.applyStyle((TextArea) node, theme);
        }
        // 可以在这里添加其他组件类型的样式处理
    }

    /**
     * 获取 TreeTableView 样式管理器
     *
     * @return TreeTableViewStyle 实例
     */
    public static TreeTableViewStyle getTreeTableViewStyle() {
        return new TreeTableViewStyle();
    }

    /**
     * 获取 TableView 样式管理器
     *
     * @return TableViewStyle 实例
     */
    public static TableViewStyle getTableViewStyle() {
        return new TableViewStyle();
    }

    /**
     * 获取 TreeView 样式管理器
     *
     * @return TreeViewStyle 实例
     */
    public static TreeViewStyle getTreeViewStyle() {
        return new TreeViewStyle();
    }

    /**
     * 获取 ListView 样式管理器
     *
     * @return ListViewStyle 实例
     */
    public static ListViewStyle getListViewStyle() {
        return new ListViewStyle();
    }

    /**
     * 获取 TextArea 样式管理器
     *
     * @return TextAreaStyle 实例
     */
    public static TextAreaStyle getTextAreaStyle() {
        return new TextAreaStyle();
    }
}
