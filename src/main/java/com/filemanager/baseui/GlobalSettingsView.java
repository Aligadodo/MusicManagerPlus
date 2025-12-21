package com.filemanager.baseui;

import com.filemanager.app.IAppController;
import com.filemanager.tool.display.StyleFactory;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.controlsfx.control.CheckComboBox;

@Getter
public class GlobalSettingsView {
    private final IAppController app;
    private VBox viewNode;

    // UI Controls
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;
    private CheckComboBox<String> ccbFileTypes;
    private Spinner<Integer> spGlobalThreads;
    private JFXComboBox<Integer> numberDisplay;

    public GlobalSettingsView(IAppController app) {
        this.app = app;
        this.initControls();
        this.buildUI();
        StyleFactory.setBasicStyle(viewNode);
    }

    private void initControls() {
        cbRecursionMode = new JFXComboBox<>(FXCollections.observableArrayList("当前目录", "全部文件", "指定目录层级"));
        cbRecursionMode.getSelectionModel().select(1);

        spRecursionDepth = new Spinner<>(1, 20, 2);
        spRecursionDepth.setEditable(true);
        spRecursionDepth.disableProperty().bind(cbRecursionMode.getSelectionModel().selectedItemProperty().isNotEqualTo("指定目录层级"));

        ccbFileTypes = new CheckComboBox<>(FXCollections.observableArrayList("[directory]", "[compressed]", "[music]", "mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "iso", "jpg", "png", "nfo", "cue", "tak"));
        ccbFileTypes.getCheckModel().checkAll();

        spGlobalThreads = new Spinner<>(1, 32, 4);
        spGlobalThreads.setEditable(true);

        // 设置预览数量 默认200
        numberDisplay = new JFXComboBox<>(FXCollections.observableArrayList(50, 100, 200, 500));
        numberDisplay.getSelectionModel().select(1);
//        UnaryOperator<TextFormatter.Change> filter = change -> {
//            String text = change.getControlNewText();
//            // 正则表达式：允许为空，或者只允许数字
//            if (text.matches("\\d*")) {
//                return change;
//            }
//            return null; // 拒绝修改
//        };
//        numberDisplay.setTextFormatter(new TextFormatter<>(filter));
    }

    private void buildUI() {
        viewNode = StyleFactory.createVBoxPanel();
        viewNode.getChildren().addAll(
                StyleFactory.createParamPairLine("文件扫描模式:", cbRecursionMode),
                StyleFactory.createParamPairLine("文件扫描层级:", spRecursionDepth),
                StyleFactory.createParamPairLine("文件类型筛选:", ccbFileTypes),
                StyleFactory.createParamPairLine("并发线程数量:", spGlobalThreads),
                StyleFactory.createParamPairLine("显示数量限制:", numberDisplay)
        );
    }

    public Node getViewNode() {
        return viewNode;
    }
}