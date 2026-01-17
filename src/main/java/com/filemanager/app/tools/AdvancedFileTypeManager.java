/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.tools;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.*;

import com.filemanager.app.tools.display.StyleFactory;

/**
 * 高级文件类型管理器
 * 特性：
 * 1. 树形级联选择 (CheckBoxTreeItem)
 * 2. 内置丰富的默认规则
 * 3. 支持用户自定义输入后缀
 * 4. 高性能匹配缓存
 */
public class AdvancedFileTypeManager {

    // --- 数据定义 ---
    private static final Map<String, List<String>> PRESET_RULES = new LinkedHashMap<>();

    // 静态代码块初始化内置规则
    static {
        PRESET_RULES.put("除文件夹", Collections.singletonList("[FILE]"));
        PRESET_RULES.put("文件夹", Collections.singletonList("[DIR]"));
        PRESET_RULES.put("音频", Arrays.asList("dsf", "dff", "dts", "ape", "wav", "flac",
                "m4a", "dfd", "tak", "tta", "wv", "mp3", "aac", "ogg", "wma"));
        PRESET_RULES.put("音频其他", Arrays.asList("cue", "lrc"));
        PRESET_RULES.put("图片", Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tif", "tiff"));
        PRESET_RULES.put("视频", Arrays.asList("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "ts"));
        PRESET_RULES.put("文档", Arrays.asList("txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "md", "csv"));
        PRESET_RULES.put("压缩包", Arrays.asList("zip", "rar", "7z", "tar", "gz", "iso", "jar"));
        PRESET_RULES.put("代码", Arrays.asList("java", "c", "cpp", "py", "js", "html", "css", "json", "xml", "sql", "sh", "bat"));
        PRESET_RULES.put("程序", Arrays.asList("exe", "msi", "bat", "cmd", "sh", "app"));
    }

    // --- UI 组件 ---
    private final CheckBoxTreeItem<String> rootItem;
    private final TreeView<String> treeView;
    private final TextField customInputCallback;

    // --- 缓存状态 (用于加速文件判断) ---
    // 存储当前被勾选的所有后缀名 (小写)
    private final Set<String> activeExtensions = Collections.synchronizedSet(new HashSet<>());
    // 标记是否勾选了“文件夹”
    private final BooleanProperty isFolderSelected = new SimpleBooleanProperty(true);
    // 标记是否勾选了“除文件夹”
    private final BooleanProperty isFileSelected = new SimpleBooleanProperty(true);
    // 存储用户自定义的后缀
    private final Set<String> customExtensions = Collections.synchronizedSet(new HashSet<>());

    public AdvancedFileTypeManager() {
        // 1. 构建树根
        rootItem = new CheckBoxTreeItem<>("所有文件类型");
        rootItem.setExpanded(true);
        rootItem.setSelected(true); // 默认全选

        // 2. 构建树结构
        buildTreeNodes();

        // 3. 创建 TreeView 并配置
        treeView = new TreeView<>(rootItem);
        treeView.setCellFactory(CheckBoxTreeCell.forTreeView());
        treeView.setShowRoot(true);

        // 4. 创建自定义输入框
        customInputCallback = new TextField();
        customInputCallback.setPromptText("自定义类型，用分号隔开 (如: log;bak;tmp)");

        // 5. 绑定事件监听 (核心：当UI变化时，更新缓存Set，而不是每次判断文件都去遍历树)
        setupListeners();

        // 初始化缓存
        refreshCache();
    }

    /**
     * 构建预设的树形节点
     */
    private void buildTreeNodes() {
        for (Map.Entry<String, List<String>> entry : PRESET_RULES.entrySet()) {
            String category = entry.getKey();
            List<String> exts = entry.getValue();

            CheckBoxTreeItem<String> categoryItem = new CheckBoxTreeItem<>(category);
            categoryItem.setExpanded(false); // 默认折叠，保持界面整洁

            // 将具体后缀添加为叶子节点
            for (String ext : exts) {
                if ("[FILE]".equals(ext)) {
                    // 文件夹特殊处理，不做子节点，直接由 categoryItem 控制逻辑
                    categoryItem.setValue("除文件夹");
                }else if ("[DIR]".equals(ext)) {
                    // 文件夹特殊处理，不做子节点，直接由 categoryItem 控制逻辑
                    categoryItem.setValue("文件夹");
                } else {
                    categoryItem.getChildren().add(new CheckBoxTreeItem<>(ext));
                }
            }
            rootItem.getChildren().add(categoryItem);
        }
    }

    /**
     * 设置监听器，实现 UI -> 逻辑 的同步
     */
    private void setupListeners() {
        // 监听根节点及其下属所有节点的修改是比较复杂的
        // 这里使用一种简化的策略：监听 TreeView 的选中状态变化有点困难，
        // 我们利用 CheckBoxTreeItem 的 selectedProperty 和 indeterminateProperty
        // 但为了性能，更推荐在需要刷新过滤时（比如用户点击了"应用"或者鼠标离开区域）触发 refreshCache
        // 或者，我们可以递归地给所有 Item 添加监听。

        addRecursiveListener(rootItem);

        // 监听自定义输入框
        customInputCallback.textProperty().addListener((obs, oldVal, newVal) -> parseCustomInput(newVal));
    }

    private void addRecursiveListener(CheckBoxTreeItem<String> item) {
        item.selectedProperty().addListener((obs, oldVal, newVal) -> refreshCache());
        item.indeterminateProperty().addListener((obs, oldVal, newVal) -> refreshCache());
        for (TreeItem<String> child : item.getChildren()) {
            addRecursiveListener((CheckBoxTreeItem<String>) child);
        }
    }

    /**
     * 解析用户输入的自定义字符串
     */
    private void parseCustomInput(String text) {
        customExtensions.clear();
        if (text != null && !text.isEmpty()) {
            String[] parts = text.split("[;\\s,]+"); // 支持分号、空格、逗号分隔
            for (String part : parts) {
                // 去掉 *. 前缀
                String ext = part.replace("*", "").replace(".", "").trim().toLowerCase();
                if (!ext.isEmpty()) {
                    customExtensions.add(ext);
                }
            }
        }
    }

    /**
     * 核心逻辑：遍历树的状态，更新 activeExtensions 缓存集合
     * 这样 accept 方法就可以达到 O(1) 的速度
     */
    public void refreshCache() {
        Set<String> newSet = new HashSet<>();
        boolean folderCheck = false;
        boolean fileCheck = false;

        // 遍历一级节点 (类别)
        for (TreeItem<String> catNode : rootItem.getChildren()) {
            CheckBoxTreeItem<String> catCheckNode = (CheckBoxTreeItem<String>) catNode;

            if (catCheckNode.isSelected() || catCheckNode.isIndeterminate()) {
                String catName = catCheckNode.getValue();

                // 处理文件夹特殊逻辑
                if (catName.equals("文件夹")) {
                    if (catCheckNode.isSelected()) folderCheck = true;
                    continue;
                }

                // 处理文件夹特殊逻辑
                if (catName.equals("除文件夹")) {
                    if (catCheckNode.isSelected()) fileCheck = true;
                    continue;
                }

                // 处理普通类别
                for (TreeItem<String> leafNode : catCheckNode.getChildren()) {
                    CheckBoxTreeItem<String> leafCheckNode = (CheckBoxTreeItem<String>) leafNode;
                    if (leafCheckNode.isSelected()) {
                        newSet.add(leafCheckNode.getValue().toLowerCase());
                    }
                }
            }
        }

        synchronized (activeExtensions) {
            activeExtensions.clear();
            activeExtensions.addAll(newSet);
        }
        isFolderSelected.set(folderCheck);
        isFileSelected.set(fileCheck);

        //System.out.println("规则已更新: 文件夹=" + folderCheck + ", 选中后缀数=" + activeExtensions.size());
    }

    /**
     * 对外提供 UI 面板
     */
    public VBox getView() {
        Node lblCustom = StyleFactory.createParamLabel("自定义扩展名:");
        
        // 创建自定义输入框并应用主题样式
        StyleFactory.applyTextInputControlStyle(customInputCallback);
        
        VBox topBox = new VBox(5, lblCustom, customInputCallback);
        StyleFactory.setBasicStyle(topBox);
        
        // 创建TreeView并应用主题样式
        StyleFactory.applyTreeViewStyle(treeView);
        
        VBox layout = StyleFactory.createVBoxPanel();
        layout.getChildren().addAll(topBox, treeView);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        
        // 应用主题样式到整个面板
        StyleFactory.setBasicStyle(layout);
        
        return layout;
    }

    /**
     * 判断文件是否符合当前规则
     */
    public boolean accept(File file) {
        if (file == null) return false;

        // 1. 判断是否全选 (优化路径)
        if (rootItem.isSelected()) return true;

        // 2. 文件夹判断
        if (isFolderSelected.get()&&file.isDirectory()) {
            return true;
        }
        if (isFileSelected.get()&&!file.isDirectory()) {
            return true;
        }

        // 3. 文件后缀判断
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            String ext = name.substring(lastDot + 1).toLowerCase();

            // 检查 TreeView 中的选择
            if (activeExtensions.contains(ext)) return true;

            // 检查 用户自定义输入
            if (customExtensions.contains(ext)) return true;
        }

        return false;
    }

    /**
     * 保存当前配置到 Properties 对象
     * 策略：
     * 1. 保存自定义输入框的内容
     * 2. 保存“文件夹”是否被勾选
     * 3. 保存所有被勾选的具体后缀名（逗号分隔）
     */
    public void saveConfig(Properties props) {
        if (props == null) return;

        // 1. 保存自定义输入
        props.setProperty("filter.custom.input", customInputCallback.getText());

        // 2. 保存文件夹选中状态
        props.setProperty("filter.option.folders", String.valueOf(isFolderSelected.get()));

        // 3. 保存被选中的后缀列表
        // 注意：activeExtensions 是当前生效的所有后缀（包含树中勾选的和自定义输入的）
        // 但为了准确恢复 UI 状态，我们需要区分哪些是树里选的。
        // 这里我们遍历树来获取“树中被选中的项”，以确保加载时能准确还原 UI 勾选状态。
        Set<String> treeSelectedExts = new HashSet<>();

        for (TreeItem<String> catItem : rootItem.getChildren()) {
            // 跳过文件夹节点（上面已经单独保存了）
            if (catItem.getValue().contains("文件夹")) continue;

            for (TreeItem<String> leafItem : catItem.getChildren()) {
                CheckBoxTreeItem<String> chkItem = (CheckBoxTreeItem<String>) leafItem;
                if (chkItem.isSelected()) {
                    treeSelectedExts.add(chkItem.getValue());
                }
            }
        }

        String joinedExts = String.join(",", treeSelectedExts);
        props.setProperty("filter.tree.selected", joinedExts);
    }

    /**
     * 从 Properties 对象加载配置并应用到 UI
     */
    public void loadConfig(Properties props) {
        if (props == null) return;

        // 1. 恢复自定义输入
        String customInput = props.getProperty("filter.custom.input", "");
        customInputCallback.setText(customInput);

        // 2. 解析保存的后缀列表
        String selectedStr = props.getProperty("filter.tree.selected", "");
        Set<String> savedExts = new HashSet<>();
        if (!selectedStr.isEmpty()) {
            Collections.addAll(savedExts, selectedStr.split(","));
        }

        // 3. 恢复文件夹状态
        boolean includeFolders = Boolean.parseBoolean(props.getProperty("filter.option.folders", "true"));

        // 4. 应用到树形结构 (核心逻辑)
        // 先暂时移除监听以避免大规模重绘时的性能损耗（可选优化），这里直接操作即可

        // 重要：先取消根节点的全选，重置为干净状态
        rootItem.setSelected(false);
        rootItem.setIndeterminate(false);

        // 遍历树节点进行状态恢复
        for (TreeItem<String> catItem : rootItem.getChildren()) {
            CheckBoxTreeItem<String> catChk = (CheckBoxTreeItem<String>) catItem;

            // A. 处理文件夹节点
            if (catItem.getValue().contains("文件夹")) {
                catChk.setSelected(includeFolders);
                continue;
            }

            // B. 处理普通分类节点
            for (TreeItem<String> leafItem : catItem.getChildren()) {
                CheckBoxTreeItem<String> leafChk = (CheckBoxTreeItem<String>) leafItem;
                String ext = leafChk.getValue();

                // 如果该后缀在保存的列表中，则勾选
                if (savedExts.contains(ext)) {
                    leafChk.setSelected(true);
                }
            }
        }

        // 5. 强制刷新缓存，确保内部逻辑与 UI 同步
        refreshCache();
    }
}
