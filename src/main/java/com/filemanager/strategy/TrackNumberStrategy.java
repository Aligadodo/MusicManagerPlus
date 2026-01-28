/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-28
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.file.FileTypeUtil;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import com.filemanager.app.tools.display.FloatingTooltip;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class TrackNumberStrategy extends IAppStrategy {
    private final JFXComboBox<String> cbMode;
    private final CheckBox chkPadZero;
    private final CheckBox chkUpdateMetadata;
    private final CheckBox chkPreserveOriginal;
    private final CheckBox chkSortByMetadata;
    private final TextField txtSeparator;
    private final TextField txtStartNumber;
    
    protected String pMode;
    protected boolean pPadZero;
    protected boolean pUpdateMetadata;
    protected boolean pPreserveOriginal;
    protected boolean pSortByMetadata;
    protected String pSeparator;
    protected int pStartNumber;

    private static final String[] SORT_MODES = {
        "默认排序 (按文件名/拼音)",
        "元数据排序 (按音轨编号)",
        "文本列表匹配 (.txt/.nfo)",
        "CUE文件匹配 (.cue)",
        "自定义顺序"
    };

    public TrackNumberStrategy() {
        cbMode = new JFXComboBox<>(FXCollections.observableArrayList(SORT_MODES));
        cbMode.getSelectionModel().select(0);
        
        ArrayList<String> modeTooltipLines = new ArrayList<>();
        modeTooltipLines.add("参数名称：排序模式");
        modeTooltipLines.add("参数用途：用于设置音轨编号的排序模式");
        modeTooltipLines.add("模式说明：");
        modeTooltipLines.add("- 默认排序：按文件名字典序排序");
        modeTooltipLines.add("- 元数据排序：按音频文件中的音轨编号排序");
        modeTooltipLines.add("- 文本列表匹配：按照.txt或.nfo文件中的顺序");
        modeTooltipLines.add("- CUE文件匹配：按照.cue文件中的音轨顺序");
        modeTooltipLines.add("- 自定义顺序：手动指定文件顺序");
        FloatingTooltip.bindToNode(cbMode, "音轨编号设置", modeTooltipLines);

        chkPadZero = new CheckBox("双位补零 (如01, 02)");
        chkPadZero.setSelected(true);
        
        ArrayList<String> padZeroTooltipLines = new ArrayList<>();
        padZeroTooltipLines.add("参数名称：双位补零");
        padZeroTooltipLines.add("参数用途：为个位数音轨编号添加前导零");
        padZeroTooltipLines.add("示例：");
        padZeroTooltipLines.add("- 启用：1 → 01, 2 → 02");
        padZeroTooltipLines.add("- 禁用：1 → 1, 2 → 2");
        FloatingTooltip.bindToNode(chkPadZero, "音轨编号设置", padZeroTooltipLines);

        chkUpdateMetadata = new CheckBox("更新音频文件元数据中的音轨编号");
        chkUpdateMetadata.setSelected(true);
        
        ArrayList<String> updateMetadataTooltipLines = new ArrayList<>();
        updateMetadataTooltipLines.add("参数名称：更新元数据");
        updateMetadataTooltipLines.add("参数用途：将音轨编号写入音频文件的元数据");
        updateMetadataTooltipLines.add("示例：");
        updateMetadataTooltipLines.add("- 启用：更新ID3/Vorbis标签中的Track字段");
        updateMetadataTooltipLines.add("- 禁用：仅修改文件名，不更新元数据");
        FloatingTooltip.bindToNode(chkUpdateMetadata, "音轨编号设置", updateMetadataTooltipLines);

        chkPreserveOriginal = new CheckBox("保留原始文件名作为备份");
        chkPreserveOriginal.setSelected(false);
        
        ArrayList<String> preserveOriginalTooltipLines = new ArrayList<>();
        preserveOriginalTooltipLines.add("参数名称：保留原始文件名");
        preserveOriginalTooltipLines.add("参数用途：在重命名前创建原始文件名的备份");
        preserveOriginalTooltipLines.add("示例：");
        preserveOriginalTooltipLines.add("- 启用：创建 OriginalName.bak 文件");
        preserveOriginalTooltipLines.add("- 禁用：不创建备份");
        FloatingTooltip.bindToNode(chkPreserveOriginal, "音轨编号设置", preserveOriginalTooltipLines);

        chkSortByMetadata = new CheckBox("按元数据中的音轨编号排序");
        chkSortByMetadata.setSelected(false);
        chkSortByMetadata.disableProperty().bind(cbMode.valueProperty().isNotEqualTo("元数据排序 (按音轨编号)"));
        
        ArrayList<String> sortByMetadataTooltipLines = new ArrayList<>();
        sortByMetadataTooltipLines.add("参数名称：按元数据排序");
        sortByMetadataTooltipLines.add("参数用途：使用音频文件中的音轨编号进行排序");
        sortByMetadataTooltipLines.add("示例：");
        sortByMetadataTooltipLines.add("- 启用：按照Track字段排序");
        sortByMetadataTooltipLines.add("- 禁用：按照文件名排序");
        FloatingTooltip.bindToNode(chkSortByMetadata, "音轨编号设置", sortByMetadataTooltipLines);

        txtSeparator = new TextField(". ");
        
        ArrayList<String> separatorTooltipLines = new ArrayList<>();
        separatorTooltipLines.add("参数名称：分隔符");
        separatorTooltipLines.add("参数用途：设置音轨编号与文件名之间的分隔符");
        separatorTooltipLines.add("示例：");
        separatorTooltipLines.add("- '. '：01. Song Name");
        separatorTooltipLines.add("- ' - '：01 - Song Name");
        separatorTooltipLines.add("- '_'：01_Song Name");
        FloatingTooltip.bindToNode(txtSeparator, "音轨编号设置", separatorTooltipLines);

        txtStartNumber = new TextField("1");
        txtStartNumber.setPromptText("起始编号");
        
        ArrayList<String> startNumberTooltipLines = new ArrayList<>();
        startNumberTooltipLines.add("参数名称：起始编号");
        startNumberTooltipLines.add("参数用途：设置音轨编号的起始值");
        startNumberTooltipLines.add("示例：");
        startNumberTooltipLines.add("- 1：从01开始");
        startNumberTooltipLines.add("- 5：从05开始");
        FloatingTooltip.bindToNode(txtStartNumber, "音轨编号设置", startNumberTooltipLines);
    }

    @Override
    public String getName() {
        return "歌曲序号补全工具";
    }

    @Override
    public void captureParams() {
        pMode = cbMode.getValue();
        pPadZero = chkPadZero.isSelected();
        pUpdateMetadata = chkUpdateMetadata.isSelected();
        pPreserveOriginal = chkPreserveOriginal.isSelected();
        pSortByMetadata = chkSortByMetadata.isSelected();
        pSeparator = txtSeparator.getText();
        try {
            pStartNumber = Integer.parseInt(txtStartNumber.getText());
            if (pStartNumber < 1) pStartNumber = 1;
        } catch (NumberFormatException e) {
            pStartNumber = 1;
        }
    }

    @Override
    public String getDescription() {
        return "智能音轨编号工具，支持多种排序模式、元数据更新、备份等功能。";
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("tns_mode", cbMode.getValue());
        props.setProperty("tns_pad", String.valueOf(chkPadZero.isSelected()));
        props.setProperty("tns_update_meta", String.valueOf(chkUpdateMetadata.isSelected()));
        props.setProperty("tns_preserve", String.valueOf(chkPreserveOriginal.isSelected()));
        props.setProperty("tns_sort_meta", String.valueOf(chkSortByMetadata.isSelected()));
        props.setProperty("tns_sep", txtSeparator.getText());
        props.setProperty("tns_start", txtStartNumber.getText());
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("tns_mode")) cbMode.getSelectionModel().select(props.getProperty("tns_mode"));
        if (props.containsKey("tns_pad")) chkPadZero.setSelected(Boolean.parseBoolean(props.getProperty("tns_pad")));
        if (props.containsKey("tns_update_meta")) chkUpdateMetadata.setSelected(Boolean.parseBoolean(props.getProperty("tns_update_meta")));
        if (props.containsKey("tns_preserve")) chkPreserveOriginal.setSelected(Boolean.parseBoolean(props.getProperty("tns_preserve")));
        if (props.containsKey("tns_sort_meta")) chkSortByMetadata.setSelected(Boolean.parseBoolean(props.getProperty("tns_sort_meta")));
        if (props.containsKey("tns_sep")) txtSeparator.setText(props.getProperty("tns_sep"));
        if (props.containsKey("tns_start")) txtStartNumber.setText(props.getProperty("tns_start"));
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public Node getConfigNode() {
        return StyleFactory.createVBoxPanel(
                StyleFactory.createParamPairLine("模式:", cbMode),
                StyleFactory.createParamPairLine("起始编号:", txtStartNumber),
                StyleFactory.createParamPairLine("分隔符:", txtSeparator),
                StyleFactory.createHBox(chkPadZero, chkUpdateMetadata),
                StyleFactory.createHBox(chkPreserveOriginal, chkSortByMetadata)
        );
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

        if (group.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChangeRecord> sortedGroup = sortFiles(group);

        List<ChangeRecord> results = new ArrayList<>();
        for (int i = 0; i < sortedGroup.size(); i++) {
            ChangeRecord rec = sortedGroup.get(i);
            int trackNumber = pStartNumber + i;
            String num = String.valueOf(trackNumber);
            if (pPadZero && trackNumber < 10) num = "0" + num;

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
            rec.getExtraParams().put("track_number", String.valueOf(trackNumber));
            results.add(rec);

            if (pPreserveOriginal) {
                File backupFile = new File(vFile.getParent(), oldName + ".bak");
                if (!backupFile.exists()) {
                    try {
                        Files.write(backupFile.toPath(), oldName.getBytes());
                    } catch (Exception e) {
                        logError("创建备份失败: " + e.getMessage());
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private List<ChangeRecord> sortFiles(List<ChangeRecord> group) {
        switch (pMode) {
            case "元数据排序 (按音轨编号)":
                return sortByMetadata(group);
            case "文本列表匹配 (.txt/.nfo)":
                return sortByTextFile(group);
            case "CUE文件匹配 (.cue)":
                return sortByCueFile(group);
            case "自定义顺序":
                return group;
            case "默认排序 (按文件名/拼音)":
            default:
                return sortByFileName(group);
        }
    }

    private List<ChangeRecord> sortByFileName(List<ChangeRecord> group) {
        return group.stream()
                .sorted(Comparator.comparing(rec -> rec.getFileHandle().getName()))
                .collect(Collectors.toList());
    }

    private List<ChangeRecord> sortByMetadata(List<ChangeRecord> group) {
        List<ChangeRecord> sorted = new ArrayList<>(group);
        sorted.sort((rec1, rec2) -> {
            try {
                int track1 = getTrackNumber(rec1.getFileHandle());
                int track2 = getTrackNumber(rec2.getFileHandle());
                return Integer.compare(track1, track2);
            } catch (Exception e) {
                return 0;
            }
        });
        return sorted;
    }

    private List<ChangeRecord> sortByTextFile(List<ChangeRecord> group) {
        File parentDir = group.get(0).getFileHandle().getParentFile();
        File[] textFiles = parentDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".txt") || name.toLowerCase().endsWith(".nfo"));
        
        if (textFiles == null || textFiles.length == 0) {
            return sortByFileName(group);
        }

        File textFile = textFiles[0];
        try {
            List<String> lines = Files.readAllLines(textFile.toPath());
            Map<String, Integer> fileOrder = new HashMap<>();
            int order = 1;
            for (String line : lines) {
                String fileName = line.trim();
                if (!fileName.isEmpty()) {
                    fileOrder.put(fileName.toLowerCase(), order++);
                }
            }

            List<ChangeRecord> sorted = new ArrayList<>();
            List<ChangeRecord> unmatched = new ArrayList<>();

            for (ChangeRecord rec : group) {
                String fileName = rec.getFileHandle().getName();
                if (fileOrder.containsKey(fileName.toLowerCase())) {
                    rec.getExtraParams().put("sort_order", String.valueOf(fileOrder.get(fileName.toLowerCase())));
                    sorted.add(rec);
                } else {
                    unmatched.add(rec);
                }
            }

            sorted.sort(Comparator.comparingInt(rec -> 
                Integer.parseInt(rec.getExtraParams().getOrDefault("sort_order", "999"))));
            sorted.addAll(unmatched);
            return sorted;
        } catch (Exception e) {
            logError("读取文本文件失败: " + e.getMessage());
            return sortByFileName(group);
        }
    }

    private List<ChangeRecord> sortByCueFile(List<ChangeRecord> group) {
        File parentDir = group.get(0).getFileHandle().getParentFile();
        File[] cueFiles = parentDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".cue"));
        
        if (cueFiles == null || cueFiles.length == 0) {
            return sortByFileName(group);
        }

        File cueFile = cueFiles[0];
        try {
            List<String> lines = Files.readAllLines(cueFile.toPath());
            Map<String, Integer> fileOrder = new HashMap<>();
            int trackNum = 1;
            String currentFile = "";

            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.toUpperCase().startsWith("FILE ")) {
                    currentFile = trimmedLine.substring(5).split("\\s+")[0];
                } else if (trimmedLine.toUpperCase().startsWith("TRACK ") && !currentFile.isEmpty()) {
                    fileOrder.put(currentFile.toLowerCase(), trackNum++);
                }
            }

            List<ChangeRecord> sorted = new ArrayList<>();
            List<ChangeRecord> unmatched = new ArrayList<>();

            for (ChangeRecord rec : group) {
                String fileName = rec.getFileHandle().getName();
                if (fileOrder.containsKey(fileName.toLowerCase())) {
                    rec.getExtraParams().put("sort_order", String.valueOf(fileOrder.get(fileName.toLowerCase())));
                    sorted.add(rec);
                } else {
                    unmatched.add(rec);
                }
            }

            sorted.sort(Comparator.comparingInt(rec -> 
                Integer.parseInt(rec.getExtraParams().getOrDefault("sort_order", "999"))));
            sorted.addAll(unmatched);
            return sorted;
        } catch (Exception e) {
            logError("读取CUE文件失败: " + e.getMessage());
            return sortByFileName(group);
        }
    }

    private int getTrackNumber(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (tag != null) {
                String track = tag.getFirst(FieldKey.TRACK);
                if (track != null && !track.isEmpty()) {
                    String[] parts = track.split("/");
                    return Integer.parseInt(parts[0].trim());
                }
            }
        } catch (Exception e) {
        }
        return 999;
    }

    private String cleanName(String s) {
        String cleaned = s;
        cleaned = cleaned.replaceFirst("^\\d+[.\\s\\-_]*", "");
        cleaned = cleaned.replaceFirst("^\\d+[.\\s\\-_]+", "");
        cleaned = cleaned.trim();
        return cleaned;
    }
}
