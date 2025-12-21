package com.filemanager.strategy;

import com.filemanager.tool.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class TrackNumberStrategy extends AppStrategy {
    private final JFXComboBox<String> cbMode;
    private final CheckBox chkPadZero;
    private final TextField txtSeparator;
    private String pMode;
    private boolean pPadZero;
    private String pSeparator;

    public TrackNumberStrategy() {
        cbMode = new JFXComboBox<>(FXCollections.observableArrayList("默认排序 (按文件名/拼音)", "文本列表匹配 (.txt/.nfo)"));
        cbMode.getSelectionModel().select(0);
        chkPadZero = new CheckBox("双位补零 (如01, 02)");
        chkPadZero.setSelected(true);
        txtSeparator = new TextField(". ");
    }

    @Override
    public String getName() {
        return "歌曲序号补全工具";
    }

    @Override
    public void captureParams() {
        pMode = cbMode.getValue();
        pPadZero = chkPadZero.isSelected();
        pSeparator = txtSeparator.getText();
    }

    @Override
    public String getDescription() {
        return "对目录下的多个音频文件进行排序，默认按照文件的字典序进行排列。";
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("tns_mode", cbMode.getValue());
        props.setProperty("tns_pad", String.valueOf(chkPadZero.isSelected()));
        props.setProperty("tns_sep", txtSeparator.getText());
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("tns_mode")) cbMode.getSelectionModel().select(props.getProperty("tns_mode"));
        if (props.containsKey("tns_pad")) chkPadZero.setSelected(Boolean.parseBoolean(props.getProperty("tns_pad")));
        if (props.containsKey("tns_sep")) txtSeparator.setText(props.getProperty("tns_sep"));
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public Node getConfigNode() {
        return StyleFactory.createVBoxPanel(
                StyleFactory.createHBox(StyleFactory.createParamLabel("模式:"), cbMode),
                StyleFactory.createHBox(chkPadZero),
                StyleFactory.createHBox(StyleFactory.createParamLabel("分隔符:"), txtSeparator));
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.RENAME) return;
        File s = rec.getFileHandle();
        File t = new File(rec.getNewPath());
        if (s.equals(t)) return;
        if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
        Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        Map<String, List<ChangeRecord>> dirGroups = inputRecords.stream()
                .collect(Collectors.groupingBy(r -> new File(r.getNewPath()).getParent()));

        List<ChangeRecord> results = new ArrayList<>();
        int total = dirGroups.size();
        AtomicInteger processed = new AtomicInteger(0);
        Collator collator = Collator.getInstance(Locale.CHINA);

        for (List<ChangeRecord> group : dirGroups.values()) {
            group.sort((r1, r2) -> {
                String n1 = cleanName(new File(r1.getNewPath()).getName());
                String n2 = cleanName(new File(r2.getNewPath()).getName());
                return collator.compare(n1, n2);
            });

            for (int i = 0; i < group.size(); i++) {
                ChangeRecord rec = group.get(i);
                if (!checkConditions(new File(rec.getNewPath()))) {
                    results.add(rec);
                    continue;
                }

                String num = String.valueOf(i + 1);
                if (pPadZero && i + 1 < 10) num = "0" + num;

                File vFile = new File(rec.getNewPath());
                String oldName = vFile.getName();
                String ext = "";
                int dot = oldName.lastIndexOf('.');
                if (dot > 0) ext = oldName.substring(dot);

                String baseName = cleanName(oldName.substring(0, dot > 0 ? dot : oldName.length()));
                String newName = num + pSeparator + baseName + ext;

                File target = new File(vFile.getParent(), newName);
                rec.setNewName(newName);
                rec.setNewPath(target.getAbsolutePath());
                rec.setChanged(true);
                rec.setOpType(OperationType.RENAME);
                results.add(rec);
            }

            int c = processed.incrementAndGet();
            if (progressReporter != null)
                Platform.runLater(() -> progressReporter.accept((double) c / total, "排序目录..."));
        }
        return results;
    }

    private String cleanName(String s) {
        return s.replaceFirst("^\\d+[.\\s\\-_]*", "").trim();
    }
}