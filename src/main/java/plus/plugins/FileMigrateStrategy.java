package plus.plugins;

import com.jfoenix.controls.JFXButton;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import plus.AppStrategy;
import plus.model.ChangeRecord;
import plus.type.ScanTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
public class FileMigrateStrategy extends AppStrategy {
    private final TextField txtTargetDir;
    private final JFXButton btnPickTarget;

    public FileMigrateStrategy() {
        txtTargetDir = new TextField();
        txtTargetDir.setPromptText("默认：原处创建子文件夹");
        txtTargetDir.setEditable(false);
        btnPickTarget = new JFXButton("...");
        btnPickTarget.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtTargetDir.setText(f.getAbsolutePath());
        });
    }

    @Override public String getName() { return "按歌手归档 (文件迁移)"; }
    @Override public ScanTarget getTargetType() { return ScanTarget.FILES_ONLY; }

    @Override
    public Node getConfigNode() {
        return new VBox(10, new Label("归档根目录:"), new HBox(5, txtTargetDir, btnPickTarget));
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
        List<ChangeRecord> records = new ArrayList<>();
        String targetBase = txtTargetDir.getText();
        if (targetBase == null || targetBase.trim().isEmpty()) {
            targetBase = rootDirs.isEmpty() ? "" : rootDirs.get(0).getAbsolutePath();
        }

        for (File f : files) {
            String artist = "其他";
            if (f.getName().contains("陈粒")) artist = "陈粒";
            if (f.getName().contains("周杰伦")) artist = "周杰伦";

            String newDirStr = targetBase + File.separator + artist;
            String newPath = newDirStr + File.separator + f.getName();
            boolean changed = !f.getParentFile().getAbsolutePath().equals(newDirStr);
            records.add(new ChangeRecord(f.getName(), f.getName(), f, changed, newPath, true));
        }
        return records;
    }
}