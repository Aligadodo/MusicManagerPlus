/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-17 
 */
package com.filemanager.app.tools.display;

import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 统一样式管理器
 * 集中管理所有组件样式，解决样式代码重复、内联CSS难以维护、样式冲突等问题
 * 提供样式优先级机制、样式模板系统和CSS类名支持
 */
public class NewStyleManager {
    private static final NewStyleManager INSTANCE = new NewStyleManager();
    private ThemeConfig theme;
    private final Map<String, StyleTemplate> styleTemplates;
    private final Map<Node, String> customStyles;

    // 样式优先级：自定义样式 > 主题样式 > 默认样式
    public enum StylePriority {
        DEFAULT,    // 默认样式
        THEME,      // 主题样式
        CUSTOM      // 自定义样式
    }

    /**
     * 样式模板类
     * 定义组件样式的模板，包含CSS类名和内联样式模板
     */
    private static class StyleTemplate {
        String cssClass;
        Function<ThemeConfig, String> styleTemplate;

        StyleTemplate(String cssClass, Function<ThemeConfig, String> styleTemplate) {
            this.cssClass = cssClass;
            this.styleTemplate = styleTemplate;
        }
    }

    private NewStyleManager() {
        this.styleTemplates = new HashMap<>();
        this.customStyles = new HashMap<>();
        
        // 初始化样式模板
        initStyleTemplates();
        
        // 注册主题变更监听器
        ThemeManager.getInstance().addThemeChangeListener(newTheme -> {
            this.theme = newTheme;
            // 主题变更时更新所有组件样式
            // 这里可以添加全局更新逻辑，或者让组件自行监听主题变更
        });
        
        // 初始化当前主题
        this.theme = ThemeManager.getInstance().getCurrentTheme();
    }

    /**
     * 获取单例实例
     */
    public static NewStyleManager getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化样式模板
     */
    private void initStyleTemplates() {
        // 初始化各种组件的样式模板
        initTabPaneTemplate();
        initListViewTemplate();
        initTableViewTemplate();
        initTreeTableViewTemplate();
        initTreeViewTemplate();
        initTitledPaneTemplate();
        initTextAreaTemplate();
        initButtonTemplate();
        initTextFieldTemplate();
        initLayoutTemplate();
    }

    /**
     * 初始化TabPane样式模板
     */
    private void initTabPaneTemplate() {
        styleTemplates.put("TabPane", new StyleTemplate(
                "theme-tab-pane",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: %.1f; -fx-tab-max-height: %.1f; -fx-tab-min-width: %.1f; -fx-tab-max-width: %.1f;\n" +
                        ".tab-pane > .tab-header-area {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-padding: 0 10 0 10;\n" +
                        "}\n" +
                        ".tab-pane > .tab-header-area > .tab-header-background {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: 0 0 %.1f 0;\n" +
                        "}\n" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: %.1f %.1f 0 %.1f;\n" +
                        "    -fx-border-radius: %.1f %.1f 0 0;\n" +
                        "    -fx-cursor: hand;\n" +
                        "}\n" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab:hover {\n" +
                        "    -fx-background-color: %s;\n" +
                        "}\n" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab:selected {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-border-color: %s %s %s %s;\n" +
                        "    -fx-border-width: %.1f %.1f 0 %.1f;\n" +
                        "}\n" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab > .tab-container > .tab-label {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-family: %s;\n" +
                        "    -fx-font-size: %.1fpx;\n" +
                        "    -fx-font-weight: normal;\n" +
                        "    -fx-alignment: center;\n" +
                        "    -fx-padding: 5 10;\n" +
                        "}\n" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab:hover > .tab-container > .tab-label {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-weight: 500;\n" +
                        "}\n" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab:selected > .tab-container > .tab-label {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-weight: bold;\n" +
                        "    -fx-padding: 5 10;\n" +
                        "}\n" +
                        ".tab-pane > .tab-content-area {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: %.1f;\n" +
                        "    -fx-border-radius: 0 %.1f %.1f %.1f;\n" +
                        "}",
                        theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                        50.0, 50.0, 80.0, 180.0,
                        theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                        theme.getBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                        theme.getPanelHoverColor(),
                        theme.getPanelBgColor(), theme.getAccentColor(), theme.getAccentColor(), theme.getPanelBgColor(), theme.getAccentColor(), theme.getBorderWidth(), theme.getBorderWidth(), theme.getBorderWidth(),
                        theme.getTextSecondaryColor(), theme.getFontFamily(), 13.0,
                        theme.getTextPrimaryColor(),
                        theme.getAccentColor(),
                        theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(), theme.getCornerRadius()
                )
        ));
    }

    /**
     * 初始化ListView样式模板
     */
    private void initListViewTemplate() {
        styleTemplates.put("ListView", new StyleTemplate(
                "theme-list-view",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;\n" +
                        ".list-view .list-cell {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-family: %s;\n" +
                        "    -fx-padding: 8 10;\n" +
                        "}\n" +
                        ".list-view .list-cell:filled:selected {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-text-fill: %s;\n" +
                        "}\n" +
                        ".list-view .list-cell:filled:hover {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-text-fill: %s;\n" +
                        "}\n" +
                        ".list-view .virtual-flow .scroll-bar:vertical,\n" +
                        ".list-view .virtual-flow .scroll-bar:horizontal {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".list-view .virtual-flow .scroll-bar .thumb {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-background-radius: 4;\n" +
                        "}\n" +
                        ".list-view .virtual-flow .scroll-bar .track {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}",
                        theme.getListBgColor(), theme.getListBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                        theme.getTextPrimaryColor(), theme.getFontFamily(),
                        theme.getListRowSelectedBgColor(), theme.getListRowSelectedTextColor(),
                        theme.getListRowHoverBgColor(), theme.getTextPrimaryColor(),
                        theme.getTextTertiaryColor()
                )
        ));
    }

    /**
     * 初始化TableView样式模板
     */
    private void initTableViewTemplate() {
        styleTemplates.put("TableView", new StyleTemplate(
                "theme-table-view",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;\n" +
                        ".table-view .column-header-background {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: 0 0 %.1f 0;\n" +
                        "    -fx-background-radius: %.1f %.1f 0 0;\n" +
                        "}\n" +
                        ".table-view .column-header-background .filler {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".table-view .column-header {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: 0 %.1f 0 0;\n" +
                        "}\n" +
                        ".table-view .column-header .label {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-family: %s;\n" +
                        "    -fx-font-weight: bold;\n" +
                        "    -fx-padding: 12 10;\n" +
                        "}\n" +
                        ".table-view .table-row-cell {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-border-color: transparent;\n" +
                        "    -fx-border-width: 1 0 0 0;\n" +
                        "}\n" +
                        ".table-view .table-row-cell:filled {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".table-view .table-row-cell:filled:selected {\n" +
                        "    -fx-background-color: %s;\n" +
                        "}\n" +
                        ".table-view .table-row-cell:filled:hover {\n" +
                        "    -fx-background-color: %s;\n" +
                        "}\n" +
                        ".table-view .table-cell {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-family: %s;\n" +
                        "    -fx-padding: 10 10;\n" +
                        "    -fx-border-color: transparent;\n" +
                        "}\n" +
                        ".table-view .virtual-flow .scroll-bar:vertical,\n" +
                        ".table-view .virtual-flow .scroll-bar:horizontal {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".table-view .virtual-flow .scroll-bar .thumb {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-background-radius: 4;\n" +
                        "}\n" +
                        ".table-view .virtual-flow .scroll-bar .track {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}",
                        theme.getListBgColor(), theme.getListBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                        theme.getListHeaderBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(),
                        theme.getBorderColor(), theme.getBorderWidth(),
                        theme.getTextPrimaryColor(), theme.getFontFamily(),
                        theme.getListRowSelectedBgColor(),
                        theme.getListRowHoverBgColor(),
                        theme.getTextPrimaryColor(), theme.getFontFamily(),
                        theme.getTextTertiaryColor()
                )
        ));
    }

    /**
     * 初始化TreeTableView样式模板
     */
    private void initTreeTableViewTemplate() {
        styleTemplates.put("TreeTableView", new StyleTemplate(
                "theme-tree-table-view",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;\n" +
                        ".tree-table-view .column-header-background {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: 0 0 %.1f 0;\n" +
                        "    -fx-background-radius: %.1f %.1f 0 0;\n" +
                        "}\n" +
                        ".tree-table-view .column-header-background .filler {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-table-view .column-header {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-border-width: 0 %.1f 0 0;\n" +
                        "}\n" +
                        ".tree-table-view .column-header .label {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-family: %s;\n" +
                        "    -fx-font-weight: bold;\n" +
                        "    -fx-padding: 12 10;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-row-cell {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-border-color: transparent;\n" +
                        "    -fx-border-width: 1 0 0 0;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-row-cell:filled {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-row-cell:filled:selected {\n" +
                        "    -fx-background-color: %s;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-row-cell:filled:hover {\n" +
                        "    -fx-background-color: %s;\n" +
                        "}\n" +
                        ".tree-table-view .tree-table-cell {\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-family: %s;\n" +
                        "    -fx-padding: 10 10;\n" +
                        "    -fx-border-color: transparent;\n" +
                        "}\n" +
                        ".tree-table-view .virtual-flow .scroll-bar:vertical,\n" +
                        ".tree-table-view .virtual-flow .scroll-bar:horizontal {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-table-view .virtual-flow .scroll-bar .thumb {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-background-radius: 4;\n" +
                        "}\n" +
                        ".tree-table-view .virtual-flow .scroll-bar .track {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}",
                        theme.getListBgColor(), theme.getListBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                        theme.getListHeaderBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(),
                        theme.getBorderColor(), theme.getBorderWidth(),
                        theme.getTextPrimaryColor(), theme.getFontFamily(),
                        theme.getListRowSelectedBgColor(),
                        theme.getListRowHoverBgColor(),
                        theme.getTextPrimaryColor(), theme.getFontFamily(),
                        theme.getTextTertiaryColor()
                )
        ));
    }

    /**
     * 初始化TreeView样式模板
     */
    private void initTreeViewTemplate() {
        styleTemplates.put("TreeView", new StyleTemplate(
                "theme-tree-view",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f; -fx-border-radius: %.1f;\n" +
                        ".tree-view .tree-cell {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-text-fill: %s;\n" +
                        "    -fx-font-family: %s;\n" +
                        "    -fx-padding: 6 10;\n" +
                        "}\n" +
                        ".tree-view .tree-cell:filled:selected {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-text-fill: %s;\n" +
                        "}\n" +
                        ".tree-view .tree-cell:filled:hover {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-text-fill: %s;\n" +
                        "}\n" +
                        ".tree-view .virtual-flow .scroll-bar:vertical,\n" +
                        ".tree-view .virtual-flow .scroll-bar:horizontal {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".tree-view .virtual-flow .scroll-bar .thumb {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-background-radius: 4;\n" +
                        "}\n" +
                        ".tree-view .virtual-flow .scroll-bar .track {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}",
                        theme.getListBgColor(), theme.getListBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                        theme.getTextPrimaryColor(), theme.getFontFamily(),
                        theme.getListRowSelectedBgColor(), theme.getTextPrimaryColor(),
                        theme.getListRowHoverBgColor(), theme.getTextPrimaryColor(),
                        theme.getTextTertiaryColor()
                )
        ));
    }

    /**
     * 初始化TitledPane样式模板
     */
    private void initTitledPaneTemplate() {
        styleTemplates.put("TitledPane", new StyleTemplate(
                "theme-titled-pane",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;\n" +
                        ".titled-pane > .title {\n" +
                        "    -fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: %.1fpx; -fx-font-weight: bold;\n" +
                        "    -fx-padding: 10 15 10 15; -fx-border-width: 0 0 %.1f 0; -fx-border-color: %s;\n" +
                        "}\n" +
                        ".titled-pane > .title > .arrow-button {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".titled-pane > .title > .arrow-button .arrow {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-effect: none;\n" +
                        "}\n" +
                        ".titled-pane > .content {\n" +
                        "    -fx-background-color: %s; -fx-border-width: 0;\n" +
                        "    -fx-background-radius: 0 0 %.1f %.1f;\n" +
                        "}",
                        theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                        theme.getPanelBgColor(), theme.getTextPrimaryColor(), theme.getFontFamily(), theme.getFontSize(), theme.getBorderWidth(), theme.getBorderColor(),
                        theme.getTextPrimaryColor(),
                        theme.getPanelBgColor(), theme.getCornerRadius(), theme.getCornerRadius()
                )
        ));
    }

    /**
     * 初始化TextArea样式模板
     */
    private void initTextAreaTemplate() {
        styleTemplates.put("TextArea", new StyleTemplate(
                "theme-text-area",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;\n" +
                        "-fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: %.1f;\n" +
                        ".text-area .scroll-pane {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".text-area .scroll-pane .viewport {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".text-area .scroll-pane .content {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}\n" +
                        ".text-area .scroll-bar:vertical {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-background-radius: 0;\n" +
                        "}\n" +
                        ".text-area .scroll-bar:horizontal {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "    -fx-background-radius: 0;\n" +
                        "}\n" +
                        ".text-area .scroll-bar .thumb {\n" +
                        "    -fx-background-color: %s;\n" +
                        "    -fx-background-radius: 4;\n" +
                        "}\n" +
                        ".text-area .scroll-bar .track {\n" +
                        "    -fx-background-color: transparent;\n" +
                        "}",
                        theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(), theme.getCornerRadius(), theme.getCornerRadius(),
                        theme.getTextPrimaryColor(), theme.getLogFontFamily(), theme.getLogFontSize(),
                        theme.getTextTertiaryColor()
                )
        ));
    }

    /**
     * 初始化Button样式模板
     */
    private void initButtonTemplate() {
        styleTemplates.put("Button", new StyleTemplate(
                "theme-button",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;\n" +
                        "-fx-font-family: %s; -fx-font-size: %.1fpx; -fx-padding: 8 16;\n" +
                        ".button:hover {\n" +
                        "    -fx-background-color: %s;\n" +
                        "}\n" +
                        ".button:pressed {\n" +
                        "    -fx-background-color: %s;\n" +
                        "}",
                        theme.getButtonBgColor(), theme.getButtonTextColor(), theme.getButtonBorderColor(), theme.getBorderWidth(),
                        theme.getCornerRadius(), theme.getCornerRadius(),
                        theme.getFontFamily(), theme.getFontSize(),
                        theme.getButtonPrimaryHoverColor(),
                        theme.getButtonPrimaryPressedColor()
                )
        ));
    }

    /**
     * 初始化TextField样式模板
     */
    private void initTextFieldTemplate() {
        styleTemplates.put("TextField", new StyleTemplate(
                "theme-text-field",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;\n" +
                        "-fx-font-family: %s; -fx-font-size: %.1fpx; -fx-padding: 8 12;\n" +
                        ".text-field:focused {\n" +
                        "    -fx-border-color: %s;\n" +
                        "    -fx-background-color: %s;\n" +
                        "}",
                        theme.getPanelBgColor(), theme.getTextPrimaryColor(), theme.getBorderColor(), theme.getBorderWidth(),
                        theme.getCornerRadius(), theme.getCornerRadius(),
                        theme.getFontFamily(), theme.getFontSize(),
                        theme.getAccentColor(),
                        theme.getPanelBgColor()
                )
        ));
    }

    /**
     * 初始化布局组件样式模板
     */
    private void initLayoutTemplate() {
        styleTemplates.put("VBox", new StyleTemplate(
                "theme-vbox",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;",
                        theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                        theme.getCornerRadius(), theme.getCornerRadius()
                )
        ));

        styleTemplates.put("HBox", new StyleTemplate(
                "theme-hbox",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;",
                        theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                        theme.getCornerRadius(), theme.getCornerRadius()
                )
        ));

        styleTemplates.put("BorderPane", new StyleTemplate(
                "theme-border-pane",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;",
                        theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                        theme.getCornerRadius(), theme.getCornerRadius()
                )
        ));

        styleTemplates.put("GridPane", new StyleTemplate(
                "theme-grid-pane",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;",
                        theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                        theme.getCornerRadius(), theme.getCornerRadius()
                )
        ));

        styleTemplates.put("StackPane", new StyleTemplate(
                "theme-stack-pane",
                theme -> String.format(
                        "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-border-radius: %.1f; -fx-background-radius: %.1f;",
                        theme.getPanelBgColor(), theme.getBorderColor(), theme.getBorderWidth(),
                        theme.getCornerRadius(), theme.getCornerRadius()
                )
        ));
    }

    /**
     * 应用样式到组件
     * @param node 要应用样式的组件
     * @param priority 样式优先级
     */
    public void applyStyle(Node node, StylePriority priority) {
        if (node == null) return;

        // 获取组件类型
        String componentType = node.getClass().getSimpleName();
        StyleTemplate template = styleTemplates.get(componentType);

        if (template == null) {
            // 如果没有找到对应组件的样式模板，尝试查找父类模板
            Class<?> superClass = node.getClass().getSuperclass();
            while (superClass != null && template == null) {
                template = styleTemplates.get(superClass.getSimpleName());
                superClass = superClass.getSuperclass();
            }
        }

        if (template != null) {
            // 根据优先级应用样式
            switch (priority) {
                case DEFAULT:
                    // 应用默认样式
                    applyDefaultStyle(node, template);
                    break;
                case THEME:
                    // 应用主题样式
                    applyThemeStyle(node, template);
                    break;
                case CUSTOM:
                    // 应用自定义样式（需要先设置自定义样式）
                    applyCustomStyle(node);
                    break;
            }
        }
    }

    /**
     * 应用默认样式
     */
    private void applyDefaultStyle(Node node, StyleTemplate template) {
        // 仅添加CSS类名，不设置内联样式
        node.getStyleClass().add(template.cssClass);
    }

    /**
     * 应用主题样式
     */
    private void applyThemeStyle(Node node, StyleTemplate template) {
        // 添加CSS类名
        node.getStyleClass().add(template.cssClass);
        
        // 检查是否有自定义样式，如果没有则应用主题内联样式
        if (!customStyles.containsKey(node)) {
            String style = template.styleTemplate.apply(theme);
            node.setStyle(style);
        }
    }

    /**
     * 应用自定义样式
     */
    private void applyCustomStyle(Node node) {
        String customStyle = customStyles.get(node);
        if (customStyle != null) {
            node.setStyle(customStyle);
        }
    }

    /**
     * 设置组件的自定义样式
     * @param node 要设置自定义样式的组件
     * @param style 自定义样式字符串
     */
    public void setCustomStyle(Node node, String style) {
        if (node == null) return;
        
        customStyles.put(node, style);
        applyCustomStyle(node);
    }

    /**
     * 移除组件的自定义样式
     * @param node 要移除自定义样式的组件
     */
    public void removeCustomStyle(Node node) {
        if (node == null) return;
        
        customStyles.remove(node);
        
        // 移除自定义样式后，重新应用主题样式
        String componentType = node.getClass().getSimpleName();
        StyleTemplate template = styleTemplates.get(componentType);
        if (template != null) {
            applyThemeStyle(node, template);
        }
    }

    /**
     * 检查组件是否有自定义样式
     * @param node 要检查的组件
     * @return 是否有自定义样式
     */
    public boolean hasCustomStyle(Node node) {
        return node != null && customStyles.containsKey(node);
    }

    /**
     * 更新组件样式
     * 通常在主题变更时调用
     * @param node 要更新样式的组件
     */
    public void updateStyle(Node node) {
        if (node == null) return;
        
        // 移除旧的CSS类名
        for (StyleTemplate template : styleTemplates.values()) {
            node.getStyleClass().remove(template.cssClass);
        }
        
        // 重新应用样式
        if (hasCustomStyle(node)) {
            applyCustomStyle(node);
        } else {
            String componentType = node.getClass().getSimpleName();
            StyleTemplate template = styleTemplates.get(componentType);
            if (template != null) {
                applyThemeStyle(node, template);
            }
        }
    }

    /**
     * 更新所有组件样式
     * 通常在主题变更时调用
     * @param root 根节点
     */
    public void updateAllStyles(Parent root) {
        if (root == null) return;
        
        // 更新根节点样式
        updateStyle(root);
        
        // 递归更新子节点样式
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Parent) {
                updateAllStyles((Parent) child);
            } else {
                updateStyle(child);
            }
        }
    }

    /**
     * 获取组件的当前样式
     * @param node 要获取样式的组件
     * @return 当前样式字符串
     */
    public String getCurrentStyle(Node node) {
        if (node == null) return null;
        
        String customStyle = customStyles.get(node);
        if (customStyle != null) {
            return customStyle;
        }
        
        return node.getStyle();
    }

    /**
     * 清理组件样式
     * 移除所有样式相关的CSS类名和内联样式
     * @param node 要清理样式的组件
     */
    public void clearStyle(Node node) {
        if (node == null) return;
        
        // 移除CSS类名
        for (StyleTemplate template : styleTemplates.values()) {
            node.getStyleClass().remove(template.cssClass);
        }
        
        // 移除内联样式
        node.setStyle("");
        
        // 移除自定义样式记录
        customStyles.remove(node);
    }
}
