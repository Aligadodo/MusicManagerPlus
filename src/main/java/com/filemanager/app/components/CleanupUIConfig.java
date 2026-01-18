package com.filemanager.app.components;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.filemanager.strategy.FileCleanupStrategy;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import javafx.beans.binding.BooleanBinding;

public class CleanupUIConfig {
    // --- UI Components ---
    private final JFXComboBox<FileCleanupStrategy.CleanupMode> cbMode;
    private final JFXComboBox<FileCleanupStrategy.DeleteMethod> cbMethod;
    private final TextField txtTrashPath; // 回收站路径（支持相对或绝对）
    private final CheckBox chkKeepLargest;
    private final CheckBox chkKeepEarliest;
    private final TextField txtKeepExt;
    // 文件名预处理选项
    private final CheckBox chkPreprocessLower;
    private final CheckBox chkPreprocessUpper;
    private final CheckBox chkPreprocessSimplified;
    // 文件大小范围选择
    private final JFXComboBox<FileCleanupStrategy.FileSizeRange> cbSizeRange;
    // 音频特殊处理
    private final CheckBox chkAudioSpecial;

    public CleanupUIConfig() {
        cbMode = new JFXComboBox<>(FXCollections.observableArrayList(FileCleanupStrategy.CleanupMode.values()));
        cbMode.getSelectionModel().select(FileCleanupStrategy.CleanupMode.DEDUP_FILES);
        cbMode.setTooltip(new Tooltip("选择清理的逻辑规则"));

        cbMethod = new JFXComboBox<>(FXCollections.observableArrayList(FileCleanupStrategy.DeleteMethod.values()));
        cbMethod.getSelectionModel().select(FileCleanupStrategy.DeleteMethod.PSEUDO_DELETE);
        cbMethod.setTooltip(new Tooltip("选择删除的方式"));

        txtTrashPath = new TextField(".EchoTrash");
        txtTrashPath.setPromptText("回收站位置");
        txtTrashPath.setTooltip(new Tooltip("输入相对名称（如 .del）将在各盘根目录创建；输入绝对路径（如 D:/Trash）则统一移动到该处。"));

        chkKeepLargest = new CheckBox("保留体积/质量最佳的副本");
        chkKeepLargest.setSelected(true);
        chkKeepLargest.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.CleanupMode.DEDUP_FILES));
        chkKeepLargest.setTooltip(new Tooltip("勾选：保留最大的文件；不勾选：保留名字最短（通常是原件）的文件"));

        chkKeepEarliest = new CheckBox("保留日期最早/最晚的副本");
        chkKeepEarliest.setSelected(true);
        // 直接清理模式不需要显示日期保留选项
        BooleanBinding showKeepEarliest = cbMode.getSelectionModel().selectedItemProperty().isNotEqualTo(FileCleanupStrategy.CleanupMode.REMOVE_EMPTY_DIRS)
                .and(cbMode.getSelectionModel().selectedItemProperty().isNotEqualTo(FileCleanupStrategy.CleanupMode.DIRECT_CLEANUP));
        chkKeepEarliest.visibleProperty().bind(showKeepEarliest);
        chkKeepEarliest.setTooltip(new Tooltip("勾选：保留日期最早的文件(夹)；不勾选：保留最新的文件(夹)"));

        txtKeepExt = new TextField("wav");
        txtKeepExt.setPromptText("优先保留后缀");
        txtKeepExt.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.CleanupMode.DEDUP_FILES));
        
        // 文件名预处理选项初始化
        chkPreprocessLower = new CheckBox("文件名转小写");
        chkPreprocessLower.setSelected(true);
        chkPreprocessLower.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.CleanupMode.DEDUP_FILES));
        chkPreprocessLower.setTooltip(new Tooltip("将文件名转换为小写后进行比较"));
        
        chkPreprocessUpper = new CheckBox("文件名转大写");
        chkPreprocessUpper.setSelected(false);
        chkPreprocessUpper.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.CleanupMode.DEDUP_FILES));
        chkPreprocessUpper.setTooltip(new Tooltip("将文件名转换为大写后进行比较"));
        
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
        chkPreprocessSimplified.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.CleanupMode.DEDUP_FILES));
        chkPreprocessSimplified.setTooltip(new Tooltip("将文件名中的繁体中文转换为简体中文后进行比较"));
        
        // 文件大小范围选择初始化
        cbSizeRange = new JFXComboBox<>(FXCollections.observableArrayList(FileCleanupStrategy.FileSizeRange.values()));
        cbSizeRange.getSelectionModel().select(FileCleanupStrategy.FileSizeRange.ALL);
        // 去重文件和直接清理模式都需要显示文件大小范围选择
        BooleanBinding showSizeRange = cbMode.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.CleanupMode.DEDUP_FILES)
                .or(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.CleanupMode.DIRECT_CLEANUP));
        cbSizeRange.visibleProperty().bind(showSizeRange);
        cbSizeRange.setTooltip(new Tooltip("选择要处理的文件大小范围"));
        
        // 音频特殊处理选项初始化
        chkAudioSpecial = new CheckBox("音频文件特殊处理");
        chkAudioSpecial.setSelected(true);
        chkAudioSpecial.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.CleanupMode.DEDUP_FILES));
        chkAudioSpecial.setTooltip(new Tooltip("对音频文件进行特殊处理，确保时间长度一致时优先保留质量较高的文件"));
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
        BooleanBinding showTrashPath = cbMethod.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.DeleteMethod.PSEUDO_DELETE)
                .or(cbMethod.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.DeleteMethod.ROLLBACKABLE_DELETE));
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
        javafx.scene.control.Separator separator1 = new javafx.scene.control.Separator();
        javafx.scene.control.Separator separator2 = new javafx.scene.control.Separator();
        
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
        dedupBox.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(FileCleanupStrategy.CleanupMode.DEDUP_FILES));
        dedupBox.managedProperty().bind(dedupBox.visibleProperty());

        dynamicArea.getChildren().addAll(trashBox, dedupBox);

        box.getChildren().addAll(grid, dynamicArea);
        return box;
    }

    // Getters for UI components
    public JFXComboBox<FileCleanupStrategy.CleanupMode> getCbMode() {
        return cbMode;
    }

    public JFXComboBox<FileCleanupStrategy.DeleteMethod> getCbMethod() {
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

    public JFXComboBox<FileCleanupStrategy.FileSizeRange> getCbSizeRange() {
        return cbSizeRange;
    }

    public CheckBox getChkAudioSpecial() {
        return chkAudioSpecial;
    }
}