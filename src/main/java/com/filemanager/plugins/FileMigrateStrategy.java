package com.filemanager.plugins;

import com.filemanager.AppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
import com.jfoenix.controls.JFXButton;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Demo 功能待实现
 */
public class FileMigrateStrategy extends AppStrategy {
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
        return "文件归档/整理";
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
    public void saveConfig(Properties props) {
        if (!txtDestDir.getText().isEmpty()) props.setProperty("fms_dest", txtDestDir.getText());
        props.setProperty("fms_pattern", txtPathPattern.getText());
        props.setProperty("fms_clean", String.valueOf(chkCleanEmpty.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("fms_dest")) txtDestDir.setText(props.getProperty("fms_dest"));
        if (props.containsKey("fms_pattern")) txtPathPattern.setText(props.getProperty("fms_pattern"));
        if (props.containsKey("fms_clean"))
            chkCleanEmpty.setSelected(Boolean.parseBoolean(props.getProperty("fms_clean")));
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        JFXButton btn = new JFXButton("浏览");
        btn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtDestDir.setText(f.getAbsolutePath());
        });
        box.getChildren().addAll(new Label("目标根目录:"), new HBox(10, txtDestDir, btn), new Label("结构模板 (/分隔):"), txtPathPattern, chkCleanEmpty);
        return box;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.MOVE) return;
        File s = rec.getFileHandle();
        File t = new File(rec.getNewPath());
        if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
        Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);

        if ("true".equals(rec.getExtraParams().get("cleanSource"))) {
            File p = s.getParentFile();
            if (p != null && p.isDirectory() && Objects.requireNonNull(p.list()).length == 0) p.delete();
        }
    }

    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        if (pDestDir == null || pDestDir.isEmpty()) return inputRecords;

        return inputRecords.parallelStream().map(rec -> {
            File vFile = new File(rec.getNewPath());
            if (!checkConditions(vFile)) return rec;

            MetadataHelper.AudioMeta meta = MetadataHelper.getSmartMetadata(rec.getFileHandle(), false);

            String relPath = MetadataHelper.format(pPattern, meta).replaceAll("[*?\"<>|]", "_");
            String ext = "";
            int dot = vFile.getName().lastIndexOf('.');
            if (dot > 0) ext = vFile.getName().substring(dot);

            if (!relPath.toLowerCase().endsWith(ext.toLowerCase())) relPath += ext;

            File target = new File(pDestDir, relPath);

            return new ChangeRecord(rec.getOriginalName(), target.getName(), rec.getFileHandle(), true, target.getAbsolutePath(), OperationType.MOVE,
                    pClean ? Collections.singletonMap("cleanSource", "true") : new HashMap<>(), ExecStatus.PENDING);
        }).collect(Collectors.toList());
    }
}