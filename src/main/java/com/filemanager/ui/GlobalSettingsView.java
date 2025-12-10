package com.filemanager.ui;

import com.filemanager.app.IAppController;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.controlsfx.control.CheckComboBox;

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

        ccbFileTypes = new CheckComboBox<>(FXCollections.observableArrayList("mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "iso", "jpg", "png", "nfo", "cue", "tak"));
        ccbFileTypes.getCheckModel().checkAll();

        spGlobalThreads = new Spinner<>(1, 128, 4);
        spGlobalThreads.setEditable(true);
    }

    private void buildUI() {
        viewNode = new VBox(10);
        viewNode.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 4;");

        viewNode.getChildren().addAll(
                styles.createNormalLabel("递归模式:"), new HBox(5, cbRecursionMode, spRecursionDepth),
                styles.createNormalLabel("文件扩展名:"), ccbFileTypes,
                new Separator(),
                styles.createNormalLabel("并发线程:"), spGlobalThreads
        );
    }

    public Node getViewNode() {
        return viewNode;
    }
}