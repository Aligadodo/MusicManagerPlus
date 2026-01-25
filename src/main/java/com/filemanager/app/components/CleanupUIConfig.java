package com.filemanager.app.components;

import com.filemanager.app.tools.display.FloatingTooltip;
import com.filemanager.strategy.cleanup.CleanupMode;
import com.filemanager.strategy.cleanup.DeleteMethod;
import com.filemanager.strategy.cleanup.FileSizeRange;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.ArrayList;

public class CleanupUIConfig {
    // --- UI Components ---
    private final JFXComboBox<CleanupMode> cbMode;
    private final JFXComboBox<DeleteMethod> cbMethod;
    private final TextField txtTrashPath; // 回收站路径（支持相对或绝对）
    private final CheckBox chkKeepLargest;
    private final CheckBox chkKeepEarliest;
    private final TextField txtKeepExt;
    // 文件名预处理选项
    private final CheckBox chkPreprocessLower;
    private final CheckBox chkPreprocessUpper;
    private final CheckBox chkPreprocessSimplified;
    // 文件大小范围选择
    private final JFXComboBox<FileSizeRange> cbSizeRange;
    // 音频特殊处理
    private final CheckBox chkAudioSpecial;

    public CleanupUIConfig() {
        cbMode = new JFXComboBox<>(FXCollections.observableArrayList(CleanupMode.values()));
        cbMode.getSelectionModel().select(CleanupMode.DEDUP_FILES);
        cbMode.setTooltip(new Tooltip("选择清理的逻辑规则"));
        
        // 添加悬浮提示信息
        ArrayList<String> modeTooltipLines = new ArrayList<>();
        modeTooltipLines.add("参数名称：清理模式");
        modeTooltipLines.add("参数用途：用于选择清理的逻辑规则");
        modeTooltipLines.add("示例：");
        modeTooltipLines.add("- 文件去重：识别并清理重复文件");
        modeTooltipLines.add("- 文件夹去重：识别并清理重复文件夹");
        modeTooltipLines.add("- 清理空目录：清理所有空目录");
        modeTooltipLines.add("- 直接清理：直接删除符合条件的文件");
        FloatingTooltip.bindToNode(cbMode, "文件清理设置", modeTooltipLines);

        cbMethod = new JFXComboBox<>(FXCollections.observableArrayList(DeleteMethod.values()));
        cbMethod.getSelectionModel().select(DeleteMethod.PSEUDO_DELETE);
        cbMethod.setTooltip(new Tooltip("选择删除的方式"));
        
        // 添加悬浮提示信息
        ArrayList<String> methodTooltipLines = new ArrayList<>();
        methodTooltipLines.add("参数名称：删除方式");
        methodTooltipLines.add("参数用途：用于选择删除的方式");
        methodTooltipLines.add("示例：");
        methodTooltipLines.add("- 伪删除：将文件移动到回收站");
        methodTooltipLines.add("- 直接删除：直接删除文件，不可恢复");
        methodTooltipLines.add("- 可回滚删除：删除后可恢复");
        FloatingTooltip.bindToNode(cbMethod, "文件清理设置", methodTooltipLines);

        txtTrashPath = new TextField(".EchoTrash");
        txtTrashPath.setPromptText("回收站位置");
        txtTrashPath.setTooltip(new Tooltip("输入相对名称（如 .del）将在各盘根目录创建；输入绝对路径（如 D:/Trash）则统一移动到该处。"));
        
        // 添加悬浮提示信息
        ArrayList<String> trashPathTooltipLines = new ArrayList<>();
        trashPathTooltipLines.add("参数名称：回收站路径");
        trashPathTooltipLines.add("参数用途：用于设置回收站的位置");
        trashPathTooltipLines.add("示例：");
        trashPathTooltipLines.add("- 相对路径：.EchoTrash（在各盘根目录创建）");
        trashPathTooltipLines.add("- 绝对路径：D:/Trash（统一移动到该处）");
        FloatingTooltip.bindToNode(txtTrashPath, "文件清理设置", trashPathTooltipLines);

        chkKeepLargest = new CheckBox("保留体积/质量最佳的副本");
        chkKeepLargest.setSelected(true);
        chkKeepLargest.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkKeepLargest.setTooltip(new Tooltip("勾选：保留最大的文件；不勾选：保留名字最短（通常是原件）的文件"));
        
        // 添加悬浮提示信息
        ArrayList<String> keepLargestTooltipLines = new ArrayList<>();
        keepLargestTooltipLines.add("参数名称：保留体积/质量最佳的副本");
        keepLargestTooltipLines.add("参数用途：用于控制去重时保留的文件版本");
        keepLargestTooltipLines.add("示例：");
        keepLargestTooltipLines.add("- 选中：保留最大的文件");
        keepLargestTooltipLines.add("- 不选中：保留名字最短（通常是原件）的文件");
        FloatingTooltip.bindToNode(chkKeepLargest, "文件清理设置", keepLargestTooltipLines);

        chkKeepEarliest = new CheckBox("保留日期最早/最晚的副本");
        chkKeepEarliest.setSelected(true);
        // 直接清理模式不需要显示日期保留选项
        BooleanBinding showKeepEarliest = cbMode.getSelectionModel().selectedItemProperty().isNotEqualTo(CleanupMode.REMOVE_EMPTY_DIRS)
                .and(cbMode.getSelectionModel().selectedItemProperty().isNotEqualTo(CleanupMode.DIRECT_CLEANUP));
        chkKeepEarliest.visibleProperty().bind(showKeepEarliest);
        chkKeepEarliest.setTooltip(new Tooltip("勾选：保留日期最早的文件(夹)；不勾选：保留最新的文件(夹)"));
        
        // 添加悬浮提示信息
        ArrayList<String> keepEarliestTooltipLines = new ArrayList<>();
        keepEarliestTooltipLines.add("参数名称：保留日期最早/最晚的副本");
        keepEarliestTooltipLines.add("参数用途：用于控制去重时保留的文件日期版本");
        keepEarliestTooltipLines.add("示例：");
        keepEarliestTooltipLines.add("- 选中：保留日期最早的文件(夹)");
        keepEarliestTooltipLines.add("- 不选中：保留最新的文件(夹)");
        FloatingTooltip.bindToNode(chkKeepEarliest, "文件清理设置", keepEarliestTooltipLines);

        txtKeepExt = new TextField("wav");
        txtKeepExt.setPromptText("优先保留后缀");
        txtKeepExt.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        
        // 添加悬浮提示信息
        ArrayList<String> keepExtTooltipLines = new ArrayList<>();
        keepExtTooltipLines.add("参数名称：优先后缀");
        keepExtTooltipLines.add("参数用途：用于设置去重时优先保留的文件后缀");
        keepExtTooltipLines.add("示例：");
        keepExtTooltipLines.add("- wav：优先保留wav格式的文件");
        keepExtTooltipLines.add("- mp3：优先保留mp3格式的文件");
        FloatingTooltip.bindToNode(txtKeepExt, "文件清理设置", keepExtTooltipLines);

        // 文件名预处理选项初始化
        chkPreprocessLower = new CheckBox("文件名转小写");
        chkPreprocessLower.setSelected(true);
        chkPreprocessLower.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkPreprocessLower.setTooltip(new Tooltip("将文件名转换为小写后进行比较"));
        
        // 添加悬浮提示信息
        ArrayList<String> preprocessLowerTooltipLines = new ArrayList<>();
        preprocessLowerTooltipLines.add("参数名称：文件名转小写");
        preprocessLowerTooltipLines.add("参数用途：用于在去重时将文件名转换为小写后进行比较");
        preprocessLowerTooltipLines.add("示例：");
        preprocessLowerTooltipLines.add("- 转换前：TestFile.txt");
        preprocessLowerTooltipLines.add("- 转换后：testfile.txt");
        FloatingTooltip.bindToNode(chkPreprocessLower, "文件清理设置", preprocessLowerTooltipLines);

        chkPreprocessUpper = new CheckBox("文件名转大写");
        chkPreprocessUpper.setSelected(false);
        chkPreprocessUpper.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkPreprocessUpper.setTooltip(new Tooltip("将文件名转换为大写后进行比较"));
        
        // 添加悬浮提示信息
        ArrayList<String> preprocessUpperTooltipLines = new ArrayList<>();
        preprocessUpperTooltipLines.add("参数名称：文件名转大写");
        preprocessUpperTooltipLines.add("参数用途：用于在去重时将文件名转换为大写后进行比较");
        preprocessUpperTooltipLines.add("示例：");
        preprocessUpperTooltipLines.add("- 转换前：TestFile.txt");
        preprocessUpperTooltipLines.add("- 转换后：TESTFILE.TXT");
        FloatingTooltip.bindToNode(chkPreprocessUpper, "文件清理设置", preprocessUpperTooltipLines);

        // 实现大小写转换的互斥逻辑
        chkPreprocessLower.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                chkPreprocessUpper.setSelected(false);
            }
        });

        chkPreprocessUpper.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                chkPreprocessLower.setSelected(false);
            }
        });

        chkPreprocessSimplified = new CheckBox("文件名转简体中文");
        chkPreprocessSimplified.setSelected(false);
        chkPreprocessSimplified.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkPreprocessSimplified.setTooltip(new Tooltip("将文件名中的繁体中文转换为简体中文后进行比较"));
        
        // 添加悬浮提示信息
        ArrayList<String> preprocessSimplifiedTooltipLines = new ArrayList<>();
        preprocessSimplifiedTooltipLines.add("参数名称：文件名转简体中文");
        preprocessSimplifiedTooltipLines.add("参数用途：用于在去重时将文件名中的繁体中文转换为简体中文后进行比较");
        preprocessSimplifiedTooltipLines.add("示例：");
        preprocessSimplifiedTooltipLines.add("- 转换前：測試文件.txt");
        preprocessSimplifiedTooltipLines.add("- 转换后：测试文件.txt");
        FloatingTooltip.bindToNode(chkPreprocessSimplified, "文件清理设置", preprocessSimplifiedTooltipLines);

        // 文件大小范围选择初始化
        cbSizeRange = new JFXComboBox<>(FXCollections.observableArrayList(FileSizeRange.values()));
        cbSizeRange.getSelectionModel().select(FileSizeRange.ALL);
        // 去重文件和直接清理模式都需要显示文件大小范围选择
        BooleanBinding showSizeRange = cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES)
                .or(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DIRECT_CLEANUP));
        cbSizeRange.visibleProperty().bind(showSizeRange);
        cbSizeRange.setTooltip(new Tooltip("选择要处理的文件大小范围"));
        
        // 添加悬浮提示信息
        ArrayList<String> sizeRangeTooltipLines = new ArrayList<>();
        sizeRangeTooltipLines.add("参数名称：文件大小范围");
        sizeRangeTooltipLines.add("参数用途：用于选择要处理的文件大小范围");
        sizeRangeTooltipLines.add("示例：");
        sizeRangeTooltipLines.add("- 全部：处理所有大小的文件");
        sizeRangeTooltipLines.add("- 小于1MB：仅处理小于1MB的文件");
        sizeRangeTooltipLines.add("- 大于1GB：仅处理大于1GB的文件");
        FloatingTooltip.bindToNode(cbSizeRange, "文件清理设置", sizeRangeTooltipLines);

        // 音频特殊处理选项初始化
        chkAudioSpecial = new CheckBox("音频文件特殊处理");
        chkAudioSpecial.setSelected(true);
        chkAudioSpecial.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkAudioSpecial.setTooltip(new Tooltip("对音频文件进行特殊处理，确保时间长度一致时优先保留质量较高的文件"));
        
        // 添加悬浮提示信息
        ArrayList<String> audioSpecialTooltipLines = new ArrayList<>();
        audioSpecialTooltipLines.add("参数名称：音频文件特殊处理");
        audioSpecialTooltipLines.add("参数用途：用于对音频文件进行特殊处理，确保时间长度一致时优先保留质量较高的文件");
        audioSpecialTooltipLines.add("示例：");
        audioSpecialTooltipLines.add("- 处理前：多个相同时长的音频文件");
        audioSpecialTooltipLines.add("- 处理后：保留音质最好的音频文件");
        FloatingTooltip.bindToNode(chkAudioSpecial, "文件清理设置", audioSpecialTooltipLines);
    }

    public Node getConfigNode() {
        VBox box = new VBox(10);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("清理模式:"), 0, 0);
        grid.add(cbMode, 1, 0);
        grid.add(new Label("删除方式:"), 0, 1);
        grid.add(cbMethod, 1, 1);

        // 动态配置区
        VBox dynamicArea = new VBox(8);
        dynamicArea.setStyle("-fx-background-color: rgba(0,0,0,0.03); -fx-padding: 10; -fx-background-radius: 5;");

        // 回收站配置
        HBox trashBox = new HBox(10);
        trashBox.setAlignment(Pos.CENTER_LEFT);
        JFXButton btnPickTrash = new JFXButton("浏览...");
        btnPickTrash.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtTrashPath.setText(f.getAbsolutePath());
        });
        trashBox.getChildren().addAll(new Label("回收站路径:"), txtTrashPath, btnPickTrash);

        // 伪删除和可回滚删除都需要显示回收站路径配置
        BooleanBinding showTrashPath = cbMethod.getSelectionModel().selectedItemProperty().isEqualTo(DeleteMethod.PSEUDO_DELETE)
                .or(cbMethod.getSelectionModel().selectedItemProperty().isEqualTo(DeleteMethod.ROLLBACKABLE_DELETE));
        txtTrashPath.visibleProperty().bind(showTrashPath);
        trashBox.visibleProperty().bind(showTrashPath);
        trashBox.managedProperty().bind(trashBox.visibleProperty());

        // 去重配置
        VBox dedupBox = new VBox(8);

        // 分组标题：基本去重选项
        Label lblBasicOptions = new Label("基本去重选项:");
        lblBasicOptions.setStyle("-fx-font-weight: bold;");
        VBox basicOptionsBox = new VBox(5);
        basicOptionsBox.setPadding(new javafx.geometry.Insets(0, 0, 0, 5));

        HBox keepRow1 = new HBox(10, new Label("优先后缀:"), txtKeepExt);
        keepRow1.setAlignment(Pos.CENTER_LEFT);
        HBox keepRow2 = new HBox(10, chkKeepLargest);
        keepRow2.setAlignment(Pos.CENTER_LEFT);
        HBox keepRow3 = new HBox(10, chkKeepEarliest);
        keepRow3.setAlignment(Pos.CENTER_LEFT);

        basicOptionsBox.getChildren().addAll(keepRow1, keepRow2, keepRow3);

        // 分组标题：文件名预处理
        Label lblPreprocess = new Label("文件名预处理:");
        lblPreprocess.setStyle("-fx-font-weight: bold;");
        VBox preprocessBox = new VBox(3);
        preprocessBox.setPadding(new javafx.geometry.Insets(5, 0, 5, 20));
        preprocessBox.getChildren().addAll(chkPreprocessLower, chkPreprocessUpper, chkPreprocessSimplified);

        // 分组标题：文件范围与特殊处理
        Label lblAdvancedOptions = new Label("高级选项:");
        lblAdvancedOptions.setStyle("-fx-font-weight: bold;");
        VBox advancedOptionsBox = new VBox(5);
        advancedOptionsBox.setPadding(new javafx.geometry.Insets(0, 0, 0, 5));

        // 文件大小范围选择
        HBox sizeRangeRow = new HBox(10, new Label("文件大小范围:"), cbSizeRange);
        sizeRangeRow.setAlignment(Pos.CENTER_LEFT);

        // 音频特殊处理选项
        HBox audioSpecialRow = new HBox(10, chkAudioSpecial);
        audioSpecialRow.setAlignment(Pos.CENTER_LEFT);

        advancedOptionsBox.getChildren().addAll(sizeRangeRow, audioSpecialRow);

        // 添加分隔线
        Separator separator1 = new Separator();
        Separator separator2 = new Separator();

        // 提示信息
        Label lblHint = new Label("提示：去重仅在同类型文件（如音频vs音频）间进行，会自动忽略 '(1)', 'Copy' 等后缀。");
        lblHint.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        dedupBox.getChildren().addAll(
                lblBasicOptions, basicOptionsBox,
                separator1,
                lblPreprocess, preprocessBox,
                separator2,
                lblAdvancedOptions, advancedOptionsBox,
                lblHint
        );
        dedupBox.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        dedupBox.managedProperty().bind(dedupBox.visibleProperty());

        dynamicArea.getChildren().addAll(trashBox, dedupBox);

        box.getChildren().addAll(grid, dynamicArea);
        return box;
    }

    // Getters for UI components
    public JFXComboBox<CleanupMode> getCbMode() {
        return cbMode;
    }

    public JFXComboBox<DeleteMethod> getCbMethod() {
        return cbMethod;
    }

    public TextField getTxtTrashPath() {
        return txtTrashPath;
    }

    public CheckBox getChkKeepLargest() {
        return chkKeepLargest;
    }

    public CheckBox getChkKeepEarliest() {
        return chkKeepEarliest;
    }

    public TextField getTxtKeepExt() {
        return txtKeepExt;
    }

    public CheckBox getChkPreprocessLower() {
        return chkPreprocessLower;
    }

    public CheckBox getChkPreprocessUpper() {
        return chkPreprocessUpper;
    }

    public CheckBox getChkPreprocessSimplified() {
        return chkPreprocessSimplified;
    }

    public JFXComboBox<FileSizeRange> getCbSizeRange() {
        return cbSizeRange;
    }

    public CheckBox getChkAudioSpecial() {
        return chkAudioSpecial;
    }
}