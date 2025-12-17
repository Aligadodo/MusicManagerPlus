package com.filemanager.ui;

import com.filemanager.app.IAppController;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.controlsfx.control.CheckComboBox;

import java.util.function.UnaryOperator;

@Getter
public class GlobalSettingsView {
    private final IAppController controller;
    private final StyleFactory styles;
    private VBox viewNode;

    // UI Controls
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;
    private CheckComboBox<String> ccbFileTypes;
    private Spinner<Integer> spGlobalThreads;
    private TextField numberDisplay;// [新增] 预览数量限制

    public GlobalSettingsView(IAppController controller) {
        this.controller = controller;
        this.styles = controller.getStyleFactory();
        initControls();
        buildUI();
    }

    private void initControls() {
        cbRecursionMode = new JFXComboBox<>(FXCollections.observableArrayList("仅当前目录", "递归所有子目录", "指定目录深度"));
        cbRecursionMode.getSelectionModel().select(1);

        spRecursionDepth = new Spinner<>(1, 20, 2);
        spRecursionDepth.setEditable(true);
        spRecursionDepth.disableProperty().bind(cbRecursionMode.getSelectionModel().selectedItemProperty().isNotEqualTo("指定目录深度"));

        ccbFileTypes = new CheckComboBox<>(FXCollections.observableArrayList("[directory]", "[compressed]", "[music]", "mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "iso", "jpg", "png", "nfo", "cue", "tak"));
        ccbFileTypes.getCheckModel().checkAll();

        spGlobalThreads = new Spinner<>(1, 128, 4);
        spGlobalThreads.setEditable(true);

        // 设置预览数量 默认200
        numberDisplay = new TextField("200");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            // 正则表达式：允许为空，或者只允许数字
            if (text.matches("\\d*")) {
                return change;
            }
            return null; // 拒绝修改
        };
        numberDisplay.setTextFormatter(new TextFormatter<>(filter));
    }

    private void buildUI() {
        viewNode = new VBox(10);
        viewNode.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 4;");

        viewNode.getChildren().addAll(
                styles.createNormalLabel("递归模式:"), new HBox(5, cbRecursionMode, spRecursionDepth),
                styles.createNormalLabel("文件扩展名:"), ccbFileTypes,
                new Separator(),
                styles.createNormalLabel("并发线程:"), spGlobalThreads,
                new Separator(),
                styles.createNormalLabel("显示数量限制:"), numberDisplay
        );
    }

    public Node getViewNode() {
        return viewNode;
    }
}