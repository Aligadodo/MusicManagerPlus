package com.filemanager.strategy;

import com.filemanager.base.IAppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.model.FileStatisticInfo;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.file.FileRegexReplaceUtil;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * demo
 */
public class CueFileRenameStrategy extends IAppStrategy {
    private final JFXComboBox<String> mode;
    private final TextField fileName;
    private String pMode;
    private String pFileName;

    public CueFileRenameStrategy() {
        mode = new JFXComboBox<>(FXCollections.observableArrayList("全自动修改"));
        mode.getSelectionModel().select(0);
        fileName = new TextField("album");
    }

    @Override
    public String getName() {
        return "专辑文件重命名";
    }

    @Override
    public void captureParams() {
        pMode = mode.getValue();
        pFileName = fileName.getText();
    }

    @Override
    public String getDescription() {
        return "为了解决cue文件在部分软件下，由于中文命名导致的无法加载的问题，支持统一调整cue及对应的音频文件命名。请同时扫描cue文件和音频文件，否则不生效。";
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("mode", mode.getValue());
        props.setProperty("filename", fileName.getText());
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("mode")) mode.getSelectionModel().select(props.getProperty("mode"));
        if (props.containsKey("filename")) fileName.setText(props.getProperty("filename"));
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FOLDERS_ONLY;
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        box.getChildren().addAll(StyleFactory.createParamPairLine("模式:", mode),
                StyleFactory.createParamPairLine("分隔符:", fileName));
        return box;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.CUE_RENAME) {
            return;
        }
        File s = rec.getFileHandle();
        File t = new File(rec.getNewPath());
        if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
        Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
        if (t.getName().endsWith(".cue")) {
            // 修改文件内容
            FileRegexReplaceUtil.replaceWithAutoCharset(t.getAbsolutePath(),
                    "FILE \"" + rec.getExtraParams().get("cue_target_name") + "\" WAVE");
        }
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        if (rec.getFileHandle().isFile()) {
            return Collections.emptyList();
        }
        File[] filesUnderDir = rec.getFileHandle().listFiles();
        if (filesUnderDir == null || filesUnderDir.length == 0) {
            return Collections.emptyList();
        }
        Map<String, File> cueFiles = Arrays.stream(filesUnderDir)
                .filter(file -> StringUtils.endsWithIgnoreCase(file.getName(), ".cue"))
                .filter(file -> FileRegexReplaceUtil.hasMatchingLine(file.getAbsolutePath()))
                .collect(Collectors.toMap(file -> FileStatisticInfo.create(file).oriName, Function.identity()));
        if (cueFiles.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, File> targetFiles = new HashMap<>();
        Arrays.stream(filesUnderDir)
                .forEach(file -> {
                    FileStatisticInfo statisticInfo = FileStatisticInfo.create(file);
                    if (!statisticInfo.isMusic()) {
                        return;
                    }
                    if (cueFiles.containsKey(statisticInfo.oriName)) {
                        targetFiles.put(statisticInfo.oriName, file);
                    }
                });
        int count = 0;
        List<String> cueNames = new ArrayList<>(targetFiles.keySet());
        cueNames.sort(String::compareToIgnoreCase);
        for (String ky : cueNames) {
            ChangeRecord cueFileRecord = getTargetFile(cueFiles.get(ky), inputRecords);
            ChangeRecord musicFileRecord = getTargetFile(targetFiles.get(ky), inputRecords);
            if (cueFileRecord != null && musicFileRecord != null) {
                count++;
                FileStatisticInfo statisticInfo = FileStatisticInfo.create(musicFileRecord.getFileHandle());
                String fileNameRank = pFileName + "disk(" + count + ")";
                if (targetFiles.size() == 1) {
                    // 只有一组音轨，无需设置后缀
                    fileNameRank = pFileName;
                }
                String targetFileName = fileNameRank + "." + statisticInfo.type;
                musicFileRecord.setNewName(targetFileName);
                musicFileRecord.setNewPath(new File(rec.getFileHandle(), targetFileName).getAbsolutePath());
                musicFileRecord.setChanged(true);
                musicFileRecord.setOpType(OperationType.CUE_RENAME);
                cueFileRecord.setNewName(fileNameRank + ".cue");
                cueFileRecord.setNewPath(new File(rec.getFileHandle(), pFileName + ".cue").getAbsolutePath());
                cueFileRecord.setChanged(true);
                cueFileRecord.getExtraParams().put("cue_target_name", targetFileName);
                cueFileRecord.setOpType(OperationType.CUE_RENAME);
            }
        }
        return Collections.emptyList();
    }


}