package com.filemanager.strategy;

import com.filemanager.base.IAppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
import com.google.common.collect.Lists;
import com.jfoenix.controls.JFXButton;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Demo 功能待实现
 *
 * @author 28667
 */
public class FileMigrateStrategy extends IAppStrategy {
    private final TextField txtDestDir;
    private final TextField txtPathPattern;
    private final CheckBox chkCleanEmpty;
    private String pDestDir;
    private String pPattern;
    private boolean pClean;

    public FileMigrateStrategy() {
        txtDestDir = new TextField();
        txtDestDir.setPromptText("选择目标根目录...");
        txtPathPattern = new TextField("%artist%/%year% %album%/%track% - %title%");
        chkCleanEmpty = new CheckBox("移动后清理源空文件夹");
        chkCleanEmpty.setSelected(true);
    }

    @Override
    public String getName() {
        return "文件批量归档和移动";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public void captureParams() {
        pDestDir = txtDestDir.getText();
        pPattern = txtPathPattern.getText();
        pClean = chkCleanEmpty.isSelected();
    }

    @Override
    public String getDescription() {
        return "按照一定规则对文件和文件夹进行移动或者复制。";
    }

    @Override
    public void saveConfig(Properties props) {
        if (!txtDestDir.getText().isEmpty()) {
            props.setProperty("fms_dest", txtDestDir.getText());
        }
        props.setProperty("fms_pattern", txtPathPattern.getText());
        props.setProperty("fms_clean", String.valueOf(chkCleanEmpty.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("fms_dest")) {
            txtDestDir.setText(props.getProperty("fms_dest"));
        }
        if (props.containsKey("fms_pattern")) {
            txtPathPattern.setText(props.getProperty("fms_pattern"));
        }
        if (props.containsKey("fms_clean")) {
            chkCleanEmpty.setSelected(Boolean.parseBoolean(props.getProperty("fms_clean")));
        }
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        JFXButton btn = StyleFactory.createActionButton("浏览目录", "#3498db", () -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtDestDir.setText(f.getAbsolutePath());
        });
        box.getChildren().addAll(
                StyleFactory.createParamPairLine("目标根目录:", txtDestDir, btn),
                StyleFactory.createParamPairLine("结构模板 (/分隔):", txtPathPattern),
                StyleFactory.createHBox(chkCleanEmpty));
        return box;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.MOVE) {
            return;
        }
        File s = rec.getFileHandle();
        File t = new File(rec.getNewPath());
        if (!t.getParentFile().exists()) {
            t.getParentFile().mkdirs();
        }
        Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);

        if ("true".equals(rec.getExtraParams().get("cleanSource"))) {
            File p = s.getParentFile();
            if (p != null && p.isDirectory() && Objects.requireNonNull(p.list()).length == 0) {
                p.delete();
            }
        }
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        if (pDestDir == null || pDestDir.isEmpty()) {
            return inputRecords;
        }

        File vFile = new File(rec.getNewPath());
        MetadataHelper.AudioMeta meta = MetadataHelper.getSmartMetadata(rec.getFileHandle(), false);

        String relPath = MetadataHelper.format(pPattern, meta).replaceAll("[*?\"<>|]", "_");
        String ext = "";
        int dot = vFile.getName().lastIndexOf('.');
        if (dot > 0) {
            ext = vFile.getName().substring(dot);
        }

        if (!relPath.toLowerCase().endsWith(ext.toLowerCase())) {
            relPath += ext;
        }

        File target = new File(pDestDir, relPath);

        return Lists.newArrayList(new ChangeRecord(rec.getOriginalName(), target.getName(), rec.getFileHandle(), true,
                target.getAbsolutePath(), OperationType.MOVE,
                pClean ? Collections.singletonMap("cleanSource", "true") : new HashMap<>(), ExecStatus.PENDING));
    }
}