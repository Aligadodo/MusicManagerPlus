package plus.plugins;


import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import plus.AppStrategy;
import plus.model.ChangeRecord;
import plus.type.ScanTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AdvancedRenameStrategy extends AppStrategy {
    private final JFXComboBox<String> cbMode;
    private final JFXComboBox<String> cbTarget;
    private final TextField txtParam1;
    private final TextField txtParam2;

    public AdvancedRenameStrategy() {
        cbMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "添加前缀", "添加后缀", "字符替换", "扩展名转小写", "去除空格"
        ));
        cbMode.getSelectionModel().select(0);

        cbTarget = new JFXComboBox<>(FXCollections.observableArrayList(
                "仅处理文件", "仅处理文件夹", "全部处理"
        ));
        cbTarget.getSelectionModel().select(0);

        txtParam1 = new TextField();
        txtParam1.setPromptText("输入内容...");
        txtParam2 = new TextField();
        txtParam2.setPromptText("替换为...");

        cbMode.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            txtParam1.setDisable(val.contains("小写") || val.contains("去除空格"));
            txtParam2.setVisible(val.contains("替换"));
        });
        txtParam2.setVisible(false);
    }

    @Override
    public String getName() {
        return "高级批量重命名";
    }

    @Override
    public ScanTarget getTargetType() {
        String t = cbTarget.getValue();
        if ("仅处理文件".equals(t)) return ScanTarget.FILES_ONLY;
        if ("仅处理文件夹".equals(t)) return ScanTarget.FOLDERS_ONLY;
        return ScanTarget.ALL;
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        box.getChildren().addAll(
                new Label("操作对象:"), cbTarget,
                new Label("方式:"), cbMode,
                new Label("参数:"), txtParam1, txtParam2
        );
        return box;
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
        List<ChangeRecord> records = new ArrayList<>();
        String mode = cbMode.getValue();
        String p1 = txtParam1.getText();
        String p2 = txtParam2.getText();
        ScanTarget target = getTargetType();

        for (File f : files) {
            if (rootDirs.contains(f)) continue;

            boolean isDir = f.isDirectory();
            if (target == ScanTarget.FILES_ONLY && isDir) continue;
            if (target == ScanTarget.FOLDERS_ONLY && !isDir) continue;

            String oldName = f.getName();
            String newName = oldName;

            if ("添加前缀".equals(mode) && !p1.isEmpty()) newName = p1 + oldName;
            else if ("添加后缀".equals(mode) && !p1.isEmpty()) {
                if (isDir) {
                    newName = oldName + p1;
                } else {
                    int dot = oldName.lastIndexOf(".");
                    if (dot > 0) newName = oldName.substring(0, dot) + p1 + oldName.substring(dot);
                    else newName = oldName + p1;
                }
            } else if ("字符替换".equals(mode) && !p1.isEmpty()) newName = oldName.replace(p1, p2 == null ? "" : p2);
            else if ("扩展名转小写".equals(mode) && !isDir) {
                int dot = oldName.lastIndexOf(".");
                if (dot > 0) newName = oldName.substring(0, dot) + oldName.substring(dot).toLowerCase();
            } else if ("去除空格".equals(mode)) newName = oldName.replace(" ", "");

            String newPath = f.getParent() + File.separator + newName;
            records.add(new ChangeRecord(oldName, newName, f, !oldName.equals(newName), newPath, false));
        }
        return records;
    }
}