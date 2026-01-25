/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-12
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.tools.display.FXDialogUtils;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.file.FolderMergeUtil;
import com.filemanager.tool.file.PathUtils;
import com.filemanager.tool.unzip.UnarchiveEngine;
import com.filemanager.tool.unzip.UnarchiveFactory;
import com.filemanager.tool.unzip.UnarchiveTask;
import com.filemanager.tool.unzip.engine.EngineType;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import com.filemanager.app.tools.display.FloatingTooltip;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 批量智能解压策略 (v5.1 - SSD 极速优化版)
 * 优化点：
 * 1. [核心] 移除全局 IO 信号量限制，允许 SSD 全速并发解压。
 * 2. [核心] 增大 Java IO 缓冲区至 64KB，提升吞吐量。
 * 3. 保持了密码箱、智能目录、失败保护等所有特性。
 */
public class FileUnzipStrategy extends IAppStrategy {

    // --- UI Components ---
    private final JFXComboBox<String> cbEngine;
    private final TextField txtExePath;
    private final JFXComboBox<String> cbOutputMode;
    private final TextField txtCustomPath;

    // 选项
    private final CheckBox chkSmartFolder;
    private final CheckBox chkMergeSameName;
    private final CheckBox chkDeleteSource; // 解压成功后删除
    private final CheckBox chkOverwrite;
    private final CheckBox chkDeleteOnFail; // 解压失败后删除
    private final CheckBox chkNestedFolderMerge; // 嵌套文件夹合并

    // 密码箱 UI
    private final ListView<String> lvPasswords;
    private final TextField txtNewPass;
    private final JFXButton btnAddPass;
    private final JFXButton btnDelPass;

    // --- Runtime Params ---
    private String pEngine;
    private String pExePath;
    private String pMode;
    private String pCustomPath;
    private boolean pSmart;
    private boolean pMergeSameName;
    private boolean pDeleteSuccess;
    private boolean pDeleteFail;
    private boolean pOverwrite;
    private boolean pNestedFolderMerge;
    private List<String> pPasswords;

    public FileUnzipStrategy() {
        // 引擎选择
        cbEngine = new JFXComboBox<>(FXCollections.observableArrayList(
                "Java 内置引擎",
                "7-Zip 引擎",
                "Bandizip 命令行工具"
        ));
        cbEngine.getSelectionModel().select(0);

        txtExePath = new TextField();
        txtExePath.setPromptText("7z.exe 或 bz.exe 路径");
        txtExePath.visibleProperty().bind(cbEngine.getSelectionModel().selectedItemProperty().isNotEqualTo("Java 内置引擎"));
        
        // 添加悬浮提示信息
        ArrayList<String> exePathTooltipLines = new ArrayList<>();
        exePathTooltipLines.add("参数名称：可执行文件路径");
        exePathTooltipLines.add("参数用途：用于设置外部解压工具的可执行文件路径");
        exePathTooltipLines.add("示例：");
        exePathTooltipLines.add("- 7-Zip：C:\\Program Files\\7-Zip\\7z.exe");
        exePathTooltipLines.add("- Bandizip：C:\\Program Files\\Bandizip\\bz.exe");
        FloatingTooltip.bindToNode(txtExePath, "批量解压设置", exePathTooltipLines);

        // 监听引擎选择变化，自动检测对应工具路径
        cbEngine.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals("Java 内置引擎")) {
                autoDetectExternalTools(newValue);
            }
        });

        // 路径模式
        cbOutputMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "当前目录 (Current Dir)",
                "指定目录 (Custom Path)",
                "同级新建文件夹 (Sibling Folder)"
        ));
        cbOutputMode.getSelectionModel().select(0);
        
        // 添加悬浮提示信息
        ArrayList<String> outputModeTooltipLines = new ArrayList<>();
        outputModeTooltipLines.add("参数名称：输出模式");
        outputModeTooltipLines.add("参数用途：用于设置解压文件的输出目录模式");
        outputModeTooltipLines.add("示例：");
        outputModeTooltipLines.add("- 当前目录：解压到当前文件所在目录");
        outputModeTooltipLines.add("- 指定目录：解压到指定的目录");
        outputModeTooltipLines.add("- 同级新建文件夹：在同级目录创建新文件夹并解压到其中");
        FloatingTooltip.bindToNode(cbOutputMode, "批量解压设置", outputModeTooltipLines);

        txtCustomPath = new TextField("Unzipped");
        txtCustomPath.setPromptText("目标文件夹路径");
        txtCustomPath.visibleProperty().bind(cbOutputMode.getSelectionModel().selectedItemProperty().isEqualTo("指定目录 (Custom Path)"));
        
        // 添加悬浮提示信息
        ArrayList<String> customPathTooltipLines = new ArrayList<>();
        customPathTooltipLines.add("参数名称：目标文件夹路径");
        customPathTooltipLines.add("参数用途：用于设置解压文件的目标文件夹路径");
        customPathTooltipLines.add("示例：");
        customPathTooltipLines.add("- Unzipped：在当前目录创建Unzipped文件夹");
        customPathTooltipLines.add("- D:/Extracted：解压到D盘的Extracted文件夹");
        FloatingTooltip.bindToNode(txtCustomPath, "批量解压设置", customPathTooltipLines);

        // 选项
        chkSmartFolder = new CheckBox("自动解压到独立文件夹");
        chkSmartFolder.setSelected(true);
        chkSmartFolder.setTooltip(new Tooltip("始终先在独立文件夹解压，若解压后发现只有单目录则自动移出。\n防止\"解压炸弹\"弄乱目录。"));
        
        // 添加悬浮提示信息
        ArrayList<String> smartFolderTooltipLines = new ArrayList<>();
        smartFolderTooltipLines.add("参数名称：自动解压到独立文件夹");
        smartFolderTooltipLines.add("参数用途：用于防止解压炸弹弄乱目录");
        smartFolderTooltipLines.add("示例：");
        smartFolderTooltipLines.add("- 启用：先在独立文件夹解压，若只有单目录则自动移出");
        smartFolderTooltipLines.add("- 禁用：直接解压到目标目录");
        FloatingTooltip.bindToNode(chkSmartFolder, "批量解压设置", smartFolderTooltipLines);

        chkMergeSameName = new CheckBox("同名父子文件夹合并");
        chkMergeSameName.setSelected(true);
        chkMergeSameName.setTooltip(new Tooltip("将具有相同名称的父子文件夹进行合并，如 '音乐/音乐/' 合并为 '音乐/'。"));
        
        // 添加悬浮提示信息
        ArrayList<String> mergeSameNameTooltipLines = new ArrayList<>();
        mergeSameNameTooltipLines.add("参数名称：同名父子文件夹合并");
        mergeSameNameTooltipLines.add("参数用途：用于合并具有相同名称的父子文件夹");
        mergeSameNameTooltipLines.add("示例：");
        mergeSameNameTooltipLines.add("- 合并前：音乐/音乐/");
        mergeSameNameTooltipLines.add("- 合并后：音乐/");
        FloatingTooltip.bindToNode(chkMergeSameName, "批量解压设置", mergeSameNameTooltipLines);

        chkNestedFolderMerge = new CheckBox("嵌套文件夹合并");
        chkNestedFolderMerge.setSelected(false);
        chkNestedFolderMerge.setTooltip(new Tooltip("当父目录只有一个子目录文件夹且没有其他文件时，自动合并掉这些空的目录层次。"));
        
        // 添加悬浮提示信息
        ArrayList<String> nestedMergeTooltipLines = new ArrayList<>();
        nestedMergeTooltipLines.add("参数名称：嵌套文件夹合并");
        nestedMergeTooltipLines.add("参数用途：用于合并嵌套的空目录层次");
        nestedMergeTooltipLines.add("示例：");
        nestedMergeTooltipLines.add("- 合并前：A/B/C/文件.txt");
        nestedMergeTooltipLines.add("- 合并后：C/文件.txt");
        FloatingTooltip.bindToNode(chkNestedFolderMerge, "批量解压设置", nestedMergeTooltipLines);

        chkDeleteSource = new CheckBox("解压成功并校验后删除源文件");
        chkDeleteSource.setSelected(false);
        chkDeleteSource.setStyle("-fx-text-fill: #27ae60;"); // 绿色提示
        
        // 添加悬浮提示信息
        ArrayList<String> deleteSourceTooltipLines = new ArrayList<>();
        deleteSourceTooltipLines.add("参数名称：解压成功后删除源文件");
        deleteSourceTooltipLines.add("参数用途：用于在解压成功并校验后删除源文件");
        deleteSourceTooltipLines.add("示例：");
        deleteSourceTooltipLines.add("- 启用：解压成功后删除原始压缩文件");
        deleteSourceTooltipLines.add("- 禁用：保留原始压缩文件");
        FloatingTooltip.bindToNode(chkDeleteSource, "批量解压设置", deleteSourceTooltipLines);

        chkDeleteOnFail = new CheckBox("失败后删除源文件 (慎用)");
        chkDeleteOnFail.setSelected(false);
        chkDeleteOnFail.setStyle("-fx-text-fill: #e74c3c;"); // 红色警示
        
        // 添加悬浮提示信息
        ArrayList<String> deleteOnFailTooltipLines = new ArrayList<>();
        deleteOnFailTooltipLines.add("参数名称：失败后删除源文件");
        deleteOnFailTooltipLines.add("参数用途：用于在解压失败后删除源文件");
        deleteOnFailTooltipLines.add("示例：");
        deleteOnFailTooltipLines.add("- 启用：解压失败后删除原始压缩文件");
        deleteOnFailTooltipLines.add("- 禁用：保留原始压缩文件");
        FloatingTooltip.bindToNode(chkDeleteOnFail, "批量解压设置", deleteOnFailTooltipLines);

        chkOverwrite = new CheckBox("覆盖已存在");
        chkOverwrite.setSelected(false);
        
        // 添加悬浮提示信息
        ArrayList<String> overwriteTooltipLines = new ArrayList<>();
        overwriteTooltipLines.add("参数名称：覆盖已存在");
        overwriteTooltipLines.add("参数用途：用于设置是否覆盖已存在的文件");
        overwriteTooltipLines.add("示例：");
        overwriteTooltipLines.add("- 启用：覆盖已存在的文件");
        overwriteTooltipLines.add("- 禁用：跳过已存在的文件");
        FloatingTooltip.bindToNode(chkOverwrite, "批量解压设置", overwriteTooltipLines);

        // 密码箱初始化
        lvPasswords = StyleFactory.createListView();
        lvPasswords.setPrefHeight(80);
        lvPasswords.setPlaceholder(StyleFactory.createParamLabel("无密码 (默认尝试空密码)"));

        txtNewPass = new TextField();
        txtNewPass.setPromptText("输入常用密码...");
        HBox.setHgrow(txtNewPass, Priority.ALWAYS);

        btnAddPass = new JFXButton("添加");
        btnAddPass.setOnAction(e -> {
            String pwd = txtNewPass.getText();
            if (pwd != null && !pwd.isEmpty() && !lvPasswords.getItems().contains(pwd)) {
                lvPasswords.getItems().add(pwd);
                txtNewPass.clear();
            }
        });

        btnDelPass = new JFXButton("删除");
        btnDelPass.setOnAction(e -> {
            String sel = lvPasswords.getSelectionModel().getSelectedItem();
            if (sel != null) lvPasswords.getItems().remove(sel);
        });

        // 初始检测当前选择的引擎（如果不是内置引擎）
        String initialEngine = cbEngine.getSelectionModel().getSelectedItem();
        if (initialEngine != null && !initialEngine.equals("Java 内置引擎")) {
            autoDetectExternalTools(initialEngine);
        }
    }

    private void autoDetectExternalTools(String engineType) {
        // 根据引擎类型设置默认检测路径
        List<String> paths = new ArrayList<>();
        String currentDir = System.getProperty("user.dir");

        if ("7-Zip 引擎".equals(engineType)) {
            // 7-Zip 引擎检测路径
            paths.add(currentDir + "\\tools\\7z.exe");
            paths.add(currentDir + "\\tools\\7-Zip\\7z.exe");
            paths.add("C:\\Program Files\\7-Zip\\7z.exe");
            paths.add("C:\\Program Files (x86)\\7-Zip\\7z.exe");
        } else if ("Bandizip 命令行工具".equals(engineType)) {
            // Bandizip 引擎检测路径
            paths.add(currentDir + "\\tools\\bz.exe");
            paths.add(currentDir + "\\tools\\bc.exe");
            paths.add(currentDir + "\\tools\\Bandizip\\bz.exe");
            paths.add(currentDir + "\\tools\\Bandizip\\bc.exe");
            paths.add("C:\\Program Files\\Bandizip\\bz.exe");
            paths.add("C:\\Program Files\\Bandizip\\bc.exe");
        }

        // 尝试检测对应引擎的路径
        for (String p : paths) {
            if (new File(p).exists()) {
                txtExePath.setText(p);
                return;
            }
        }

        // 如果没有检测到，清空路径
        txtExePath.clear();
    }

    @Override
    public String getName() {
        return "批量智能解压工具";
    }

    @Override
    public String getDescription() {
        return "支持对压缩文件自动解压，并支持内置算法和7zip程序功能等解压方式，使得功能更稳定和扩展性更强。" +
                "【内置的Java解压引擎兼容性一般，建议自行安装7zip，" +
                "请按照默认路径将7zip安装到C盘下，以支持自动识别路径】";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(80);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        // 1. 引擎配置
        HBox exeBox = new HBox(5, cbEngine, txtExePath);
        HBox.setHgrow(cbEngine, Priority.ALWAYS);
        HBox.setHgrow(txtExePath, Priority.ALWAYS);
        JFXButton btnExePick = new JFXButton("浏览");
        btnExePick.visibleProperty().bind(txtExePath.visibleProperty());
        btnExePick.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("选择解压程序 (7z.exe / bz.exe)");
            File f = fc.showOpenDialog(null);
            if (f != null) txtExePath.setText(f.getAbsolutePath());
        });
        exeBox.getChildren().add(btnExePick);
        grid.add(new Label("解压引擎:"), 0, 0);
        grid.add(exeBox, 1, 0);

        // 2. 路径配置
        HBox pathBox = new HBox(5, cbOutputMode, txtCustomPath);
        HBox.setHgrow(cbOutputMode, Priority.ALWAYS);
        HBox.setHgrow(txtCustomPath, Priority.ALWAYS);
        JFXButton btnPathPick = new JFXButton("...");
        btnPathPick.visibleProperty().bind(txtCustomPath.visibleProperty());
        btnPathPick.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtCustomPath.setText(f.getAbsolutePath());
        });
        pathBox.getChildren().add(btnPathPick);
        grid.add(new Label("解压位置:"), 0, 1);
        grid.add(pathBox, 1, 1);

        // 3. 密码箱配置
        VBox passBox = new VBox(5);
        HBox passInput = new HBox(5, txtNewPass, btnAddPass, btnDelPass);
        passBox.getChildren().addAll(lvPasswords, passInput);

        TitledPane tpPass = new TitledPane("密码管理箱 (自动匹配)", passBox);
        tpPass.setExpanded(false);

        // 4. 选项配置
        VBox opts = new VBox(5);
        opts.getChildren().addAll(chkSmartFolder, chkMergeSameName, chkNestedFolderMerge, chkOverwrite, chkDeleteSource, chkDeleteOnFail);

        // 移除了内部线程配置 UI，由主程序统一控制

        box.getChildren().addAll(grid, tpPass, new Separator(), new Label("操作选项:"), opts);
        return box;
    }

    @Override
    public void captureParams() {
        pEngine = cbEngine.getValue();
        pExePath = txtExePath.getText();
        pMode = cbOutputMode.getValue();
        pCustomPath = txtCustomPath.getText();
        pSmart = chkSmartFolder.isSelected();
        pMergeSameName = chkMergeSameName.isSelected();
        pDeleteSuccess = chkDeleteSource.isSelected();
        pDeleteFail = chkDeleteOnFail.isSelected();
        pOverwrite = chkOverwrite.isSelected();
        pNestedFolderMerge = chkNestedFolderMerge.isSelected();
        pPasswords = new ArrayList<>(lvPasswords.getItems());

        // 检查引擎选择和运行环境
        if (!pEngine.equals("Java 内置引擎")) {
            File exeFile = new File(pExePath);
            if (pExePath.isEmpty() || !exeFile.exists() || !exeFile.isFile()) {
                // 如果引擎不是Java内置引擎，且执行路径无效，显示提示
                if (app != null) {
                    FXDialogUtils.showToast(app.getPrimaryStage(), "请安装对应程序到正确目录下！",
                            FXDialogUtils.ToastType.INFO);
                }
            }
        }
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("zip_engine", pEngine);
        props.setProperty("zip_exe", pExePath);
        props.setProperty("zip_mode", pMode);
        props.setProperty("zip_path", pCustomPath);
        props.setProperty("zip_smart", String.valueOf(pSmart));
        props.setProperty("zip_merge_same", String.valueOf(pMergeSameName));
        props.setProperty("zip_del_ok", String.valueOf(pDeleteSuccess));
        props.setProperty("zip_del_fail", String.valueOf(pDeleteFail));
        props.setProperty("zip_over", String.valueOf(pOverwrite));

        // Save passwords list
        props.setProperty("zip_pwd_count", String.valueOf(lvPasswords.getItems().size()));
        for (int i = 0; i < lvPasswords.getItems().size(); i++) {
            props.setProperty("zip_pwd_" + i, lvPasswords.getItems().get(i));
        }
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("zip_engine")) cbEngine.getSelectionModel().select(props.getProperty("zip_engine"));
        if (props.containsKey("zip_exe")) txtExePath.setText(props.getProperty("zip_exe"));
        if (props.containsKey("zip_mode")) cbOutputMode.getSelectionModel().select(props.getProperty("zip_mode"));
        if (props.containsKey("zip_path")) txtCustomPath.setText(props.getProperty("zip_path"));
        if (props.containsKey("zip_smart"))
            chkSmartFolder.setSelected(Boolean.parseBoolean(props.getProperty("zip_smart")));
        if (props.containsKey("zip_merge_same"))
            chkMergeSameName.setSelected(Boolean.parseBoolean(props.getProperty("zip_merge_same")));
        if (props.containsKey("zip_del_ok"))
            chkDeleteSource.setSelected(Boolean.parseBoolean(props.getProperty("zip_del_ok")));
        if (props.containsKey("zip_del_fail"))
            chkDeleteOnFail.setSelected(Boolean.parseBoolean(props.getProperty("zip_del_fail")));
        if (props.containsKey("zip_over"))
            chkOverwrite.setSelected(Boolean.parseBoolean(props.getProperty("zip_over")));
        if (props.containsKey("zip_nested_merge"))
            chkNestedFolderMerge.setSelected(Boolean.parseBoolean(props.getProperty("zip_nested_merge")));

        // Load passwords
        lvPasswords.getItems().clear();
        int count = Integer.parseInt(props.getProperty("zip_pwd_count", "0"));
        for (int i = 0; i < count; i++) {
            String pwd = props.getProperty("zip_pwd_" + i);
            if (pwd != null) lvPasswords.getItems().add(pwd);
        }
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        Set<String> archiveExts = new HashSet<>(Arrays.asList("zip", "7z", "rar", "tar", "gz", "jar", "xz", "bz2", "iso"));
        File file = rec.getFileHandle();
        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot == -1) return Collections.emptyList();
        String ext = name.substring(dot + 1);

        if (!archiveExts.contains(ext)) return Collections.emptyList();
        String formatPathName = PathUtils.fixFolderName(getBaseName(file.getName()));
        // 1. 计算目标路径
        File baseDestDir;
        if (pMode.startsWith("当前目录")) {
            baseDestDir = file.getParentFile();
        } else if (pMode.startsWith("指定目录")) {
            baseDestDir = new File(pCustomPath);
        } else {
            baseDestDir = new File(file.getParentFile(), "Extracted_" + formatPathName);
        }

        // 预览路径（如果是智能模式，实际路径在执行时才确定，这里显示基础路径）
        File previewDest = pSmart ? new File(baseDestDir, formatPathName) : baseDestDir;

        String displayName = (pEngine.contains("外部") ? "[外部] " : "[内置] ") +
                (pSmart ? "智能解压 -> " : "解压 -> ") + previewDest.getName();

        // 序列化参数
        Map<String, String> params = new HashMap<>();
        params.put("baseDest", baseDestDir.getAbsolutePath());
        params.put("engine", pEngine);
        params.put("exePath", pExePath);
        params.put("smart", String.valueOf(pSmart));
        params.put("mergeSameName", String.valueOf(pMergeSameName));
        params.put("overwrite", String.valueOf(pOverwrite));
        params.put("deleteSuccess", String.valueOf(pDeleteSuccess));
        params.put("deleteFail", String.valueOf(pDeleteFail));
        params.put("nestedFolderMerge", String.valueOf(pNestedFolderMerge));

        rec.setNewName(displayName);
        rec.setChanged(true);
        rec.setStatus(ExecStatus.PENDING);
        rec.setOpType(OperationType.UNZIP);
        rec.setNewPath(previewDest.getAbsolutePath());
        rec.setExtraParams(params);
        return Collections.emptyList();
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (!rec.getExtraParams().containsKey("baseDest")) return;

        // [优化] 直接调用执行，不加锁，允许全速并发
        // 并发度由主程序的 executorService 控制
        executeInternal(rec);
    }

    private void executeInternal(ChangeRecord rec) throws Exception {
        File archiveFile = rec.getFileHandle();
        String baseDestPath = rec.getExtraParams().get("baseDest");
        String engine = rec.getExtraParams().get("engine");
        boolean smart = Boolean.parseBoolean(rec.getExtraParams().get("smart"));
        boolean mergeSameName = Boolean.parseBoolean(rec.getExtraParams().get("mergeSameName"));
        boolean deleteSuccess = Boolean.parseBoolean(rec.getExtraParams().get("deleteSuccess"));
        boolean deleteFail = Boolean.parseBoolean(rec.getExtraParams().get("deleteFail"));
        boolean overwrite = Boolean.parseBoolean(rec.getExtraParams().get("overwrite"));
        boolean nestedFolderMerge = Boolean.parseBoolean(rec.getExtraParams().get("nestedFolderMerge"));
        String exePath = rec.getExtraParams().get("exePath");

        File baseDestDir = new File(baseDestPath);
        if (!baseDestDir.exists()) baseDestDir.mkdirs();

        // 1. 确定解压根目录 (Wrapper)
        File extractRoot;
        if (smart) {
            String wrapperName = PathUtils.fixFolderName(getBaseName(archiveFile.getName()));
            extractRoot = new File(baseDestDir, wrapperName);
            if (!extractRoot.exists()) extractRoot.mkdirs();
        } else {
            extractRoot = baseDestDir;
        }

        // 2. 准备尝试列表
        List<String> passwordsToTry = new ArrayList<>();
        passwordsToTry.add(null);
        if (pPasswords != null) passwordsToTry.addAll(pPasswords);

        boolean success = false;
        Exception lastError = null;

        // 3. 循环尝试解压
        for (String pwd : passwordsToTry) {
            try {
                // 根据选择的引擎创建对应的解压引擎实例
                UnarchiveEngine unarchiveEngine;
                EngineType engineType;

                // 检查引擎和环境
                if (!engine.equals("Java 内置引擎")) {
                    File exeFile = new File(exePath);
                    if (exePath.isEmpty() || !exeFile.exists() || !exeFile.isFile()) {
                        // 如果引擎不是Java内置引擎，且执行路径无效，显示提示并抛出异常
                        if (app != null) {
                            FXDialogUtils.showToast(app.getPrimaryStage(), "请安装对应程序到正确目录下！",
                                    FXDialogUtils.ToastType.INFO);
                        }
                        throw new IOException(engine + " 未安装或路径无效，请检查配置！");
                    }
                }

                if (engine.equals("Java 内置引擎")) {
                    engineType = EngineType.BUILT_IN;
                    unarchiveEngine = UnarchiveFactory.getSpecificEngine(engineType, null);
                } else if (engine.equals("7-Zip 引擎")) {
                    engineType = EngineType.SEVEN_ZIP;
                    unarchiveEngine = UnarchiveFactory.getSpecificEngine(engineType, exePath);
                } else if (engine.equals("Bandizip 命令行工具")) {
                    engineType = EngineType.BANDIZIP;
                    unarchiveEngine = UnarchiveFactory.getSpecificEngine(engineType, exePath);
                } else {
                    // 默认使用内置引擎
                    engineType = EngineType.BUILT_IN;
                    unarchiveEngine = UnarchiveFactory.getSpecificEngine(engineType, null);
                }

                // 创建解压任务
                UnarchiveTask task = new UnarchiveTask(archiveFile.getAbsolutePath(), extractRoot.getAbsolutePath());
                task.overwrite = overwrite;
                task.password = pwd;

                // 执行解压
                if (!unarchiveEngine.extract(task)) {
                    throw new IOException("解压引擎执行失败");
                }

                // 校验阶段：确保有文件产出
                String[] files = extractRoot.list();
                if (files == null || files.length == 0) {
                    throw new IOException("解压程序返回成功，但目标目录为空 (可能是密码错误或程序假死)");
                }

                success = true;
                break;
            } catch (Exception e) {
                lastError = e;
            }
        }

        if (!success) {
            if (deleteFail) {
                try {
                    Files.delete(archiveFile.toPath());
                } catch (Exception ignored) {
                }
                throw new IOException("解压失败(源已删): " + (lastError != null ? lastError.getMessage() : "未知"));
            } else {
                throw new IOException("解压失败: " + (lastError != null ? lastError.getMessage() : "未知"));
            }
        }

        // 4. 后置智能处理
        if (smart) {
            optimizeSmartFolder(extractRoot, baseDestDir);
        }

        // 5. 嵌套文件夹合并
        if (nestedFolderMerge) {
            try {
                // 确定合并的起始目录：如果使用了智能目录，从baseDestDir开始；否则从extractRoot开始
                File mergeRoot = smart ? baseDestDir : extractRoot;
                int mergedCount = FolderMergeUtil.mergeNestedFolders(mergeRoot, overwrite);
                if (mergedCount > 0) {
                    log("嵌套文件夹合并完成，共合并了 " + mergedCount + " 个空目录层次");
                }
            } catch (IOException e) {
                logError("嵌套文件夹合并失败: " + e.getMessage());
            }
        }

        // 6. 重名文件夹合并
        if (mergeSameName) {
            try {
                // 确定合并的起始目录：如果使用了智能目录，从baseDestDir开始；否则从extractRoot开始
                File[] files = extractRoot.listFiles();
                if (files != null) {
                    File subDir = Arrays.stream(files).filter(file -> file.isDirectory() && file.getName().equals(extractRoot.getName())).findFirst().orElse(null);
                    if (subDir != null) {
                        List<File> conflictingFiles = FolderMergeUtil.mergeSameNameParentChild(extractRoot, subDir, true);
                        log("父子文件夹合并完成，共合并了 " + conflictingFiles.size() + " 个冲突文件");
                    }
                }
            } catch (IOException e) {
                logError("嵌套文件夹合并失败: " + e.getMessage());
            }
        }

        // 7. 成功后删除源
        if (deleteSuccess) {
            try {
                Files.delete(archiveFile.toPath());
            } catch (IOException e) { /* log warn */ }
        }
    }

    // --- 解压引擎实现已迁移到 com.filemanager.tool.unzip 包下 ---

    private void optimizeSmartFolder(File wrapperDir, File parentDir) {
        if (wrapperDir == null || !wrapperDir.exists() || !wrapperDir.isDirectory()) return;

        File[] files = wrapperDir.listFiles();
        List<File> validFiles = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.getName().equals(".DS_Store") || f.getName().equalsIgnoreCase("Thumbs.db")) continue;
                validFiles.add(f);
            }
        }

        if (validFiles.size() == 1 && validFiles.get(0).isDirectory()) {
            File singleInnerDir = validFiles.get(0);
            try {
                // 不同名或未开启合并选项，直接移动到父目录
                File targetDir = new File(parentDir, singleInnerDir.getName());
                if (!targetDir.exists()) {
                    Files.move(singleInnerDir.toPath(), targetDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    deleteDirectoryRecursively(wrapperDir);
                }
            } catch (IOException e) {
                logError("Smart folder optimization failed: " + e.getMessage());
            }
        }
    }

    private void deleteDirectoryRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) for (File entry : entries) deleteDirectoryRecursively(entry);
        }
        Files.delete(file.toPath());
    }

    private String getBaseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}