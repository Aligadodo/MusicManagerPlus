package com.filemanager.strategy;

import com.filemanager.base.IAppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.file.FileTypeUtil;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class TrackNumberStrategy extends IAppStrategy {
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
                StyleFactory.createParamPairLine("模式:", cbMode),
                StyleFactory.createParamPairLine("分隔符:", txtSeparator),
                StyleFactory.createHBox(chkPadZero));
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
    public List<ChangeRecord> analyze(ChangeRecord change, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        File f = change.getFileHandle();
        File[] files = f.listFiles();
        if (f.isFile() || files == null || files.length < 2) {
            return Collections.emptyList();
        }

        List<ChangeRecord> group = getFilesUnderDir(f, inputRecords).stream()
                .filter(rec -> FileTypeUtil.isMusicFile(rec.getFileHandle())).collect(Collectors.toList());

        List<ChangeRecord> results = new ArrayList<>();
        for (int i = 0; i < group.size(); i++) {
            ChangeRecord rec = group.get(i);
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
        return Collections.emptyList();
    }

    private String cleanName(String s) {
        return s.replaceFirst("^\\d+[.\\s\\-_]*", "").trim();
    }
}