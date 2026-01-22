/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.ui;

import java.io.File;
import java.util.Properties;
import java.util.stream.Collectors;

import com.filemanager.app.base.IAppController;
import com.filemanager.app.base.IAutoReloadAble;
import com.filemanager.app.tools.AdvancedFileTypeManager;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.app.tools.display.AutoShrinkLabel;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.geometry.Insets;
import javafx.scene.control.TitledPane;
import javafx.scene.text.Font;
import lombok.Getter;

@Getter
public class GlobalSettingsView implements IAutoReloadAble {
    private final IAppController app;
    private final AdvancedFileTypeManager fileTypeManager = new AdvancedFileTypeManager();
    private VBox viewNode;
    // UI Controls
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;
    private ListView<String> scanFilterListView;
    private JFXTextField scanFilterInput;
    private ObservableList<String> scanFilterList;

    public GlobalSettingsView(IAppController app) {
        this.app = app;
        this.initControls();
        this.buildUI();
        StyleFactory.setBasicStyle(viewNode);
    }

    // UI Controls for directory level range
    private Spinner<Integer> spMinRecursionDepth;
    private Spinner<Integer> spMaxRecursionDepth;
    private HBox recursionDepthRangeBox;
    
    private void initControls() {
        cbRecursionMode = new JFXComboBox<>(FXCollections.observableArrayList("当前目录", "全部文件", "指定目录层级", "目录层级范围"));
        cbRecursionMode.getSelectionModel().select(1);

        spRecursionDepth = new Spinner<>(1, 20, 2);
        spRecursionDepth.setEditable(true);
        
        // Initialize directory level range controls
        spMinRecursionDepth = new Spinner<>(0, 20, 0);
        spMinRecursionDepth.setEditable(true);
        spMaxRecursionDepth = new Spinner<>(1, 20, 2);
        spMaxRecursionDepth.setEditable(true);
        
        recursionDepthRangeBox = new HBox(10);
        recursionDepthRangeBox.getChildren().addAll(
                StyleFactory.createParamPairLine("最小层级:", spMinRecursionDepth),
                StyleFactory.createParamPairLine("最大层级:", spMaxRecursionDepth)
        );
        
        // Set visibility based on selected mode
        cbRecursionMode.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean showSingleDepth = "指定目录层级".equals(newVal);
            boolean showRangeDepth = "目录层级范围".equals(newVal);
            spRecursionDepth.setVisible(showSingleDepth);
            spRecursionDepth.setManaged(showSingleDepth);
            recursionDepthRangeBox.setVisible(showRangeDepth);
            recursionDepthRangeBox.setManaged(showRangeDepth);
        });
        
        // Initialize visibility
        spRecursionDepth.setVisible(false);
        spRecursionDepth.setManaged(false);
        recursionDepthRangeBox.setVisible(false);
        recursionDepthRangeBox.setManaged(false);
        
        // 初始化文件扫描过滤配置
        scanFilterList = FXCollections.observableArrayList(
                // 功能相关过滤
                "*Convert*",
                "*Split*",
                "*System*",
                "*trash*",
                "*Temp*",
                "*Cache*",
                "*Log*",
                
                // Windows系统路径
                "*\\Windows\\*",
                "*\\Program Files\\*",
                "*\\Program Files (x86)\\*",
                "*\\ProgramData\\*",
                "*\\AppData\\*",
                "*\\Local Settings\\*",
                "*\\Application Data\\*",
                
                // 系统特殊目录
                "*\\Recycle Bin\\*",
                "*\\System Volume Information\\*",
                
                // 隐藏文件和临时文件
                "*\\.*",  // 隐藏文件和目录
                "*\\~*",  // 临时文件和备份
                "*\\Thumbs.db",  // 缩略图缓存
                
                // 临时文件目录
                "*\\Temp\\*",
                "*\\TMP\\*"
        );
        scanFilterListView = StyleFactory.createListView();
        scanFilterListView.setPrefHeight(150);
        scanFilterListView.setItems(scanFilterList);
        scanFilterListView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(null); // 使用 Graphic 布局
                    BorderPane pane = new BorderPane();

                    // 简化内容布局，只显示规则文本，缩小字体
                    Label ruleLabel = new Label(item);
                    ruleLabel.setFont(Font.font(12));
                    ruleLabel.setStyle("-fx-text-fill: " + app.getCurrentTheme().getTextPrimaryColor() + ";");

                    // 操作按钮
                    HBox actions = new HBox(5);
                    Button upBtn = new Button("↑");
                    Button downBtn = new Button("↓");
                    Button deleteBtn = new Button("×");
                    
                    // 设置小按钮样式
                    String smallButtonStyle = "-fx-min-width: 20; -fx-min-height: 20; -fx-font-size: 10; -fx-padding: 0;";
                    upBtn.setStyle(smallButtonStyle);
                    downBtn.setStyle(smallButtonStyle);
                    deleteBtn.setStyle(smallButtonStyle + " -fx-text-fill: #e74c3c;");
                    
                    upBtn.setOnAction(e -> moveFilterRule(getIndex(), -1));
                    downBtn.setOnAction(e -> moveFilterRule(getIndex(), 1));
                    deleteBtn.setOnAction(e -> scanFilterList.remove(item));
                    
                    actions.getChildren().addAll(upBtn, downBtn, deleteBtn);

                    pane.setCenter(ruleLabel);
                    pane.setRight(actions);
                    setGraphic(pane);
                    setStyle("-fx-background-color: transparent; -fx-border-color: " + app.getCurrentTheme().getBorderColor() + "; -fx-border-width: 0 0 1 0; -fx-padding: 4 0;");
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (!isEmpty() && getItem() != null) {
                    // 简化选中样式
                    if (selected) {
                        setStyle("-fx-background-color: rgba(52, 152, 219, 0.1); -fx-border-color: " + app.getCurrentTheme().getBorderColor() + "; -fx-border-width: 0 0 1 0;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-border-color: " + app.getCurrentTheme().getBorderColor() + "; -fx-border-width: 0 0 1 0;");
                    }
                }
            }
        });
        
        scanFilterInput = new JFXTextField();
        scanFilterInput.setPromptText("输入过滤规则（如：*Convert*），按回车添加");
        scanFilterInput.setPrefWidth(300);
        scanFilterInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String filter = scanFilterInput.getText().trim();
                if (!filter.isEmpty() && !scanFilterList.contains(filter)) {
                    scanFilterList.add(filter);
                    scanFilterInput.clear();
                }
            }
        });
    }

    private void buildUI() {
        viewNode = StyleFactory.createVBoxPanel();
        viewNode.setSpacing(15);
        
        // 将扫描模式和层级合并到一行显示
        VBox scanSettingsBox = new VBox(10);
        scanSettingsBox.setPadding(new Insets(10));
        
        // 调整扫描层级Spinner的宽度
        spRecursionDepth.setPrefWidth(60);
        spMinRecursionDepth.setPrefWidth(60);
        spMaxRecursionDepth.setPrefWidth(60);
        
        HBox scanModeBox = new HBox(10);
        scanModeBox.getChildren().addAll(
                StyleFactory.createParamPairLine("扫描模式:", cbRecursionMode)
        );
        
        // 扫描层级参数行
        HBox singleDepthBox = new HBox(10);
        AutoShrinkLabel recursionDepthLabel = StyleFactory.createParamLabel("扫描层级:");
        singleDepthBox.getChildren().addAll(
                recursionDepthLabel,
                spRecursionDepth
        );
        
        // 绑定扫描层级行的可见性
        cbRecursionMode.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean showSingleDepth = "指定目录层级".equals(newVal);
            boolean showRangeDepth = "目录层级范围".equals(newVal);
            spRecursionDepth.setVisible(showSingleDepth);
            spRecursionDepth.setManaged(showSingleDepth);
            recursionDepthLabel.setVisible(showSingleDepth);
            recursionDepthLabel.setManaged(showSingleDepth);
            recursionDepthRangeBox.setVisible(showRangeDepth);
            recursionDepthRangeBox.setManaged(showRangeDepth);
        });
        
        // 初始化可见性
        spRecursionDepth.setVisible(false);
        spRecursionDepth.setManaged(false);
        recursionDepthLabel.setVisible(false);
        recursionDepthLabel.setManaged(false);
        recursionDepthRangeBox.setVisible(false);
        recursionDepthRangeBox.setManaged(false);
        
        scanSettingsBox.getChildren().addAll(
                scanModeBox,
                singleDepthBox,
                recursionDepthRangeBox
        );
        
        // 文件扫描过滤配置
        VBox scanFilterBox = new VBox(8);
        scanFilterBox.setPadding(new Insets(10));
        
        HBox scanFilterInputBox = new HBox(10);
        scanFilterInputBox.getChildren().addAll(
                scanFilterInput
        );
        
        scanFilterBox.getChildren().addAll(
                scanFilterInputBox,
                scanFilterListView
        );
        
        // 创建折叠框
        TitledPane globalFilterPane = new TitledPane("全局过滤", scanFilterBox);
        TitledPane globalScanPane = new TitledPane("扫描模式", scanSettingsBox);
        TitledPane fileTypePane = new TitledPane("文件类型", fileTypeManager.getView());
        
        // 设置折叠框样式
        applyTitledPaneStyle(globalScanPane);
        applyTitledPaneStyle(fileTypePane);
        applyTitledPaneStyle(globalFilterPane);
        
        // 实现只能同时打开一个折叠框的功能
        setupTitledPaneInteraction(globalScanPane, fileTypePane, globalFilterPane);
        
        // 默认打开全局筛选折叠框
        globalScanPane.setExpanded(true);
        
        viewNode.getChildren().addAll(
                globalScanPane,
                fileTypePane,
                globalFilterPane
        );
    }
    
    /**
     * 应用折叠框样式
     */
    private void applyTitledPaneStyle(TitledPane pane) {
        pane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;",
                app.getCurrentTheme().getPanelBgColor(),
                app.getCurrentTheme().getBorderColor()
        ));
        pane.setAnimated(true);
        pane.setCollapsible(true);
    }
    
    /**
     * 设置折叠框交互，确保只能同时打开一个
     */
    private void setupTitledPaneInteraction(TitledPane... panes) {
        for (TitledPane pane : panes) {
            pane.expandedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    // 关闭其他所有折叠框
                    for (TitledPane otherPane : panes) {
                        if (otherPane != pane && otherPane.isExpanded()) {
                            otherPane.setExpanded(false);
                        }
                    }
                }
            });
        }
    }
    
    private Button createRemoveButton() {
        Button removeBtn = new Button("删除选中");
        removeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 5 10; -fx-border-width: 1; -fx-border-color: #c0392b; -fx-min-height: 28; -fx-min-width: 80;");
        removeBtn.setOnAction(event -> {
            String selected = scanFilterListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                scanFilterList.remove(selected);
            }
        });
        return removeBtn;
    }
    
    /**
     * 移动过滤规则的位置
     * @param currentIndex 当前索引
     * @param direction 方向：-1 上移，1 下移
     */
    private void moveFilterRule(int currentIndex, int direction) {
        int newIndex = currentIndex + direction;
        if (newIndex >= 0 && newIndex < scanFilterList.size()) {
            String rule = scanFilterList.remove(currentIndex);
            scanFilterList.add(newIndex, rule);
            scanFilterListView.getSelectionModel().select(newIndex);
        }
    }
    
    /**
     * 如果setting目录不存在则创建
     */
    private void createSettingDirIfNotExists() {
        File settingDir = new File("setting");
        if (!settingDir.exists()) {
            settingDir.mkdir();
        }
    }

    /**
     * 判断是否需要的文件类型
     *
     * @param file
     * @return
     */
    public boolean isFileIncluded(File file) {
        return fileTypeManager.accept(file) && !isFileFiltered(file);
    }
    
    /**
     * 判断文件是否被过滤规则匹配
     * @param file 文件
     * @return true表示被过滤，false表示不过滤
     */
    public boolean isFileFiltered(File file) {
        String fullPath = file.getAbsolutePath();
        for (String filter : scanFilterList) {
            if (matchesFilter(fullPath, filter)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查文件完整路径是否匹配过滤规则
     * @param fullPath 文件完整路径
     * @param filter 过滤规则（支持通配符）
     * @return true表示匹配
     */
    private boolean matchesFilter(String fullPath, String filter) {
        // 将通配符转换为正则表达式
        // 首先转义所有特殊字符，然后再处理通配符
        String regex = filter
                // 转义正则表达式特殊字符
                .replaceAll("([\\\\/:\\[\\]{}()+.^$|])", "\\\\$1")
                // 处理通配符
                .replace("*", ".*")
                .replace("?", ".");
        return fullPath.matches(regex);
    }

    public Node getViewNode() {
        return viewNode;
    }

    public JFXComboBox<String> getCbRecursionMode() {
        return cbRecursionMode;
    }

    public Spinner<Integer> getSpRecursionDepth() {
        return spRecursionDepth;
    }
    
    public Spinner<Integer> getSpMinRecursionDepth() {
        return spMinRecursionDepth;
    }
    
    public Spinner<Integer> getSpMaxRecursionDepth() {
        return spMaxRecursionDepth;
    }
    
    public ObservableList<String> getScanFilterList() {
        return scanFilterList;
    }

    @Override
    public void saveConfig(Properties props) {
        if (props == null) return;
        this.fileTypeManager.saveConfig(props);
        props.setProperty("filter.recursion.mode",
                String.valueOf(cbRecursionMode.getSelectionModel().getSelectedIndex()));
        props.setProperty("filter.recursion.depth",
                spRecursionDepth.getValue() != null ? String.valueOf(spRecursionDepth.getValue()) : "1");
        // Save directory level range
        props.setProperty("filter.recursion.minDepth",
                spMinRecursionDepth.getValue() != null ? String.valueOf(spMinRecursionDepth.getValue()) : "0");
        props.setProperty("filter.recursion.maxDepth",
                spMaxRecursionDepth.getValue() != null ? String.valueOf(spMaxRecursionDepth.getValue()) : "2");
        ObservableList<File> roots = app.getSourceRoots();
        if (!roots.isEmpty()) {
            String paths = roots.stream().map(File::getAbsolutePath).collect(Collectors.joining("||"));
            props.setProperty("filter.global.sources", paths);
        } else {
            props.remove("filter.global.sources");
        }
        
        // 保存文件扫描过滤规则
        String filters = String.join("||", scanFilterList);
        props.setProperty("filter.scan.rules", filters);
    }

    @Override
    public void loadConfig(Properties props) {
        this.fileTypeManager.loadConfig(props);
        int recursionMode = Integer.parseInt(props.getProperty("filter.recursion.mode", "1"));
        cbRecursionMode.getSelectionModel().select(recursionMode);
        int recursionDepth = Integer.parseInt(props.getProperty("filter.recursion.depth", "1"));
        spRecursionDepth.getValueFactory().setValue(recursionDepth);
        // Load directory level range
        int minRecursionDepth = Integer.parseInt(props.getProperty("filter.recursion.minDepth", "0"));
        spMinRecursionDepth.getValueFactory().setValue(minRecursionDepth);
        int maxRecursionDepth = Integer.parseInt(props.getProperty("filter.recursion.maxDepth", "2"));
        spMaxRecursionDepth.getValueFactory().setValue(maxRecursionDepth);
        String paths = props.getProperty("filter.global.sources");
        if (paths != null && !paths.isEmpty()) {
            app.getSourceRoots().clear();
            for (String p : paths.split("\\|\\|")) {
                File f = new File(p);
                if (f.exists()) app.getSourceRoots().add(f);
            }
        }
        
        // 加载文件扫描过滤规则
        String filters = props.getProperty("filter.scan.rules");
        if (filters != null && !filters.isEmpty()) {
            scanFilterList.clear();
            for (String filter : filters.split("\\|\\|")) {
                if (!filter.isEmpty()) {
                    scanFilterList.add(filter);
                }
            }
        }
    }
    
    @Override
    public void reload() {
        // 更新视图节点的基本样式
        StyleFactory.setBasicStyle(viewNode);
        
        // 递归更新所有子组件样式
        StyleFactory.refreshAllComponents(viewNode);
    }
    
    @Override
    public void resetConfig() {
        // 重置过滤规则为默认值
        scanFilterList.clear();
        scanFilterList.addAll(
                "*Convert*",
                "*Split*",
                "*System*",
                "*trash*",
                "*Temp*",
                "*Cache*",
                "*Log*",
                "*\\Windows\\*",
                "*\\Program Files\\*",
                "*\\Program Files (x86)\\*",
                "*\\ProgramData\\*",
                "*\\AppData\\*",
                "*\\Local Settings\\*",
                "*\\Application Data\\*",
                "*\\Recycle Bin\\*",
                "*\\System Volume Information\\*",
                "*\\.*",
                "*\\~*",
                "*\\Thumbs.db",
                "*\\Temp\\*",
                "*\\TMP\\*"
        );
        
        // 重置扫描模式为默认值
        cbRecursionMode.getSelectionModel().selectFirst();
        
        // 重置扫描层级为默认值
        spRecursionDepth.getValueFactory().setValue(1);
        spMinRecursionDepth.getValueFactory().setValue(0);
        spMaxRecursionDepth.getValueFactory().setValue(2);
        
        // 重置输入框
        scanFilterInput.setText("");
    }
    
    public void resetSettings() {
        resetConfig();
    }
}