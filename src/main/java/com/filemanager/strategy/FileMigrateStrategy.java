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
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
import com.google.common.collect.Lists;
import com.jfoenix.controls.JFXButton;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import com.filemanager.app.tools.display.FloatingTooltip;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class FileMigrateStrategy extends IAppStrategy {
    private final TextField txtDestDir;
    private final ComboBox<String> cbPathPattern;
    private final TextField txtCustomPattern;
    private final CheckBox chkCleanEmpty;
    private final CheckBox chkPreserveStructure;
    private final CheckBox chkCreatePlaylists;
    private final CheckBox chkSkipExisting;
    private final CheckBox chkValidateMetadata;
    private final TextField txtPlaylistName;
    
    protected String pDestDir;
    protected String pPattern;
    protected boolean pClean;
    protected boolean pPreserveStructure;
    protected boolean pCreatePlaylists;
    protected boolean pSkipExisting;
    protected boolean pValidateMetadata;
    protected String pPlaylistName;

    private static final String[] PRESET_PATTERNS = {
        "%artist%/%year% %album%/%track% - %title%",
        "%artist%/%album%/%track% - %title%",
        "%year%/%album%/%track% - %title%",
        "%genre%/%artist% - %album%/%track% - %title%",
        "%artist% - %album%/%track% - %title%",
        "%album%/%track% - %title%",
        "自定义模板"
    };

    public FileMigrateStrategy() {
        txtDestDir = new TextField();
        txtDestDir.setPromptText("选择目标根目录...");
        
        ArrayList<String> destDirTooltipLines = new ArrayList<>();
        destDirTooltipLines.add("参数名称：目标根目录");
        destDirTooltipLines.add("参数用途：用于设置文件移动的目标根目录");
        destDirTooltipLines.add("示例：");
        destDirTooltipLines.add("- D:/Music：将文件移动到D盘的Music文件夹");
        destDirTooltipLines.add("- E:/Media：将文件移动到E盘的Media文件夹");
        FloatingTooltip.bindToNode(txtDestDir, "文件批量归档设置", destDirTooltipLines);
        
        txtCustomPattern = new TextField();
        txtCustomPattern.setPromptText("输入自定义模板，如: %artist%/%album%/%title%");
        txtCustomPattern.setDisable(true);
        
        ArrayList<String> customPatternTooltipLines = new ArrayList<>();
        customPatternTooltipLines.add("参数名称：自定义模板");
        customPatternTooltipLines.add("参数用途：当选择自定义模板时，在此输入自定义目录结构");
        customPatternTooltipLines.add("示例：");
        customPatternTooltipLines.add("- %artist%/%album%/%title%");
        customPatternTooltipLines.add("- %year%/%album%/%track% - %title%");
        FloatingTooltip.bindToNode(txtCustomPattern, "文件批量归档设置", customPatternTooltipLines);
        
        cbPathPattern = new ComboBox<>();
        cbPathPattern.getItems().addAll(PRESET_PATTERNS);
        cbPathPattern.getSelectionModel().select(0);
        cbPathPattern.setOnAction(e -> {
            if ("自定义模板".equals(cbPathPattern.getValue())) {
                txtCustomPattern.setDisable(false);
            } else {
                txtCustomPattern.setDisable(true);
                txtCustomPattern.setText(cbPathPattern.getValue());
            }
        });
        
        ArrayList<String> patternTooltipLines = new ArrayList<>();
        patternTooltipLines.add("参数名称：结构模板");
        patternTooltipLines.add("参数用途：用于设置文件移动的目录结构模板");
        patternTooltipLines.add("支持变量：");
        patternTooltipLines.add("- %artist%：艺术家名称");
        patternTooltipLines.add("- %album%：专辑名称");
        patternTooltipLines.add("- %year%：发行年份");
        patternTooltipLines.add("- %genre%：音乐流派");
        patternTooltipLines.add("- %track%：音轨编号");
        patternTooltipLines.add("- %title%：歌曲标题");
        patternTooltipLines.add("示例：");
        patternTooltipLines.add("- %artist%/%year% %album%/%track% - %title%");
        patternTooltipLines.add("- %year%/%album%/%title%");
        FloatingTooltip.bindToNode(cbPathPattern, "文件批量归档设置", patternTooltipLines);
        
        chkCleanEmpty = new CheckBox("移动后清理源空文件夹");
        chkCleanEmpty.setSelected(true);
        
        ArrayList<String> cleanEmptyTooltipLines = new ArrayList<>();
        cleanEmptyTooltipLines.add("参数名称：清理空文件夹");
        cleanEmptyTooltipLines.add("参数用途：用于设置是否在移动后清理源空文件夹");
        cleanEmptyTooltipLines.add("示例：");
        cleanEmptyTooltipLines.add("- 启用：移动后清理源空文件夹");
        cleanEmptyTooltipLines.add("- 禁用：移动后不清理源空文件夹");
        FloatingTooltip.bindToNode(chkCleanEmpty, "文件批量归档设置", cleanEmptyTooltipLines);

        chkPreserveStructure = new CheckBox("保留原始目录结构");
        chkPreserveStructure.setSelected(false);
        
        ArrayList<String> preserveStructureTooltipLines = new ArrayList<>();
        preserveStructureTooltipLines.add("参数名称：保留原始目录结构");
        preserveStructureTooltipLines.add("参数用途：在目标目录中保留原始的目录层级");
        preserveStructureTooltipLines.add("示例：");
        preserveStructureTooltipLines.add("- 启用：保持原始目录层级");
        preserveStructureTooltipLines.add("- 禁用：按照模板重新组织目录结构");
        FloatingTooltip.bindToNode(chkPreserveStructure, "文件批量归档设置", preserveStructureTooltipLines);

        chkCreatePlaylists = new CheckBox("生成播放列表文件");
        chkCreatePlaylists.setSelected(false);
        
        ArrayList<String> createPlaylistsTooltipLines = new ArrayList<>();
        createPlaylistsTooltipLines.add("参数名称：生成播放列表");
        createPlaylistsTooltipLines.add("参数用途：为每个专辑或艺术家生成播放列表文件");
        createPlaylistsTooltipLines.add("示例：");
        createPlaylistsTooltipLines.add("- 启用：生成 .m3u 或 .pls 播放列表");
        createPlaylistsTooltipLines.add("- 禁用：不生成播放列表");
        FloatingTooltip.bindToNode(chkCreatePlaylists, "文件批量归档设置", createPlaylistsTooltipLines);

        chkSkipExisting = new CheckBox("跳过已存在的文件");
        chkSkipExisting.setSelected(true);
        
        ArrayList<String> skipExistingTooltipLines = new ArrayList<>();
        skipExistingTooltipLines.add("参数名称：跳过已存在文件");
        skipExistingTooltipLines.add("参数用途：跳过目标目录中已存在的文件");
        skipExistingTooltipLines.add("示例：");
        skipExistingTooltipLines.add("- 启用：跳过已存在的文件");
        skipExistingTooltipLines.add("- 禁用：覆盖已存在的文件");
        FloatingTooltip.bindToNode(chkSkipExisting, "文件批量归档设置", skipExistingTooltipLines);

        chkValidateMetadata = new CheckBox("验证元数据完整性");
        chkValidateMetadata.setSelected(true);
        
        ArrayList<String> validateMetadataTooltipLines = new ArrayList<>();
        validateMetadataTooltipLines.add("参数名称：验证元数据");
        validateMetadataTooltipLines.add("参数用途：检查文件的元数据是否完整，跳过不完整的文件");
        validateMetadataTooltipLines.add("示例：");
        validateMetadataTooltipLines.add("- 启用：跳过元数据不完整的文件");
        validateMetadataTooltipLines.add("- 禁用：处理所有文件");
        FloatingTooltip.bindToNode(chkValidateMetadata, "文件批量归档设置", validateMetadataTooltipLines);

        txtPlaylistName = new TextField();
        txtPlaylistName.setPromptText("播放列表名称（可选）");
        txtPlaylistName.setDisable(true);
        chkCreatePlaylists.selectedProperty().addListener((obs, oldVal, newVal) -> {
            txtPlaylistName.setDisable(!newVal);
        });
        
        ArrayList<String> playlistNameTooltipLines = new ArrayList<>();
        playlistNameTooltipLines.add("参数名称：播放列表名称");
        playlistNameTooltipLines.add("参数用途：设置生成的播放列表文件名称");
        playlistNameTooltipLines.add("示例：");
        playlistNameTooltipLines.add("- MyPlaylist：生成 MyPlaylist.m3u");
        playlistNameTooltipLines.add("- 留空：使用默认名称");
        FloatingTooltip.bindToNode(txtPlaylistName, "文件批量归档设置", playlistNameTooltipLines);
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
        pPattern = "自定义模板".equals(cbPathPattern.getValue()) ? txtCustomPattern.getText() : cbPathPattern.getValue();
        pClean = chkCleanEmpty.isSelected();
        pPreserveStructure = chkPreserveStructure.isSelected();
        pCreatePlaylists = chkCreatePlaylists.isSelected();
        pSkipExisting = chkSkipExisting.isSelected();
        pValidateMetadata = chkValidateMetadata.isSelected();
        pPlaylistName = txtPlaylistName.getText();
    }

    @Override
    public String getDescription() {
        return "智能归档和移动文件，支持多种目录结构模板、元数据验证、播放列表生成等功能。";
    }

    @Override
    public void saveConfig(Properties props) {
        if (!txtDestDir.getText().isEmpty()) {
            props.setProperty("fms_dest", txtDestDir.getText());
        }
        props.setProperty("fms_pattern", cbPathPattern.getValue());
        props.setProperty("fms_custom_pattern", txtCustomPattern.getText());
        props.setProperty("fms_clean", String.valueOf(chkCleanEmpty.isSelected()));
        props.setProperty("fms_preserve", String.valueOf(chkPreserveStructure.isSelected()));
        props.setProperty("fms_playlist", String.valueOf(chkCreatePlaylists.isSelected()));
        props.setProperty("fms_skip", String.valueOf(chkSkipExisting.isSelected()));
        props.setProperty("fms_validate", String.valueOf(chkValidateMetadata.isSelected()));
        props.setProperty("fms_playlist_name", txtPlaylistName.getText());
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("fms_dest")) {
            txtDestDir.setText(props.getProperty("fms_dest"));
        }
        if (props.containsKey("fms_pattern")) {
            cbPathPattern.getSelectionModel().select(props.getProperty("fms_pattern"));
        }
        if (props.containsKey("fms_custom_pattern")) {
            txtCustomPattern.setText(props.getProperty("fms_custom_pattern"));
        }
        if (props.containsKey("fms_clean")) {
            chkCleanEmpty.setSelected(Boolean.parseBoolean(props.getProperty("fms_clean")));
        }
        if (props.containsKey("fms_preserve")) {
            chkPreserveStructure.setSelected(Boolean.parseBoolean(props.getProperty("fms_preserve")));
        }
        if (props.containsKey("fms_playlist")) {
            chkCreatePlaylists.setSelected(Boolean.parseBoolean(props.getProperty("fms_playlist")));
        }
        if (props.containsKey("fms_skip")) {
            chkSkipExisting.setSelected(Boolean.parseBoolean(props.getProperty("fms_skip")));
        }
        if (props.containsKey("fms_validate")) {
            chkValidateMetadata.setSelected(Boolean.parseBoolean(props.getProperty("fms_validate")));
        }
        if (props.containsKey("fms_playlist_name")) {
            txtPlaylistName.setText(props.getProperty("fms_playlist_name"));
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
                StyleFactory.createParamPairLine("结构模板 (/分隔):", cbPathPattern),
                StyleFactory.createParamPairLine("自定义模板:", txtCustomPattern),
                StyleFactory.createHBox(chkCleanEmpty, chkPreserveStructure),
                StyleFactory.createHBox(chkSkipExisting, chkValidateMetadata),
                StyleFactory.createHBox(chkCreatePlaylists),
                StyleFactory.createParamPairLine("播放列表名称:", txtPlaylistName)
        );
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

        if (pClean && "true".equals(rec.getExtraParams().get("cleanSource"))) {
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

        if (pValidateMetadata) {
            if (meta.getArtist().isEmpty() || meta.getArtist().equals("Unknown Artist") ||
                meta.getTitle().isEmpty() || meta.getTitle().equals("Unknown Title")) {
                log("跳过文件（元数据不完整）: " + vFile.getName());
                return Collections.emptyList();
            }
        }

        String relPath;
        if (pPreserveStructure) {
            relPath = preserveOriginalStructure(rec.getFileHandle());
        } else {
            if (pPattern == null || pPattern.trim().isEmpty()) {
                pPattern = "%artist%/%year% %album%/%track% - %title%";
            }
            relPath = MetadataHelper.format(pPattern, meta).replaceAll("[*?\"<>|]", "_");
        }

        String ext = "";
        int dot = vFile.getName().lastIndexOf('.');
        if (dot > 0) {
            ext = vFile.getName().substring(dot);
        }

        if (!relPath.toLowerCase().endsWith(ext.toLowerCase())) {
            relPath += ext;
        }

        File target = new File(pDestDir, relPath);

        if (pSkipExisting && target.exists()) {
            log("跳过已存在的文件: " + target.getName());
            return Collections.emptyList();
        }

        Map<String, String> extraParams = new HashMap<>();
        if (pClean) {
            extraParams.put("cleanSource", "true");
        }
        if (pCreatePlaylists) {
            extraParams.put("createPlaylist", "true");
            extraParams.put("playlistName", pPlaylistName.isEmpty() ? meta.getAlbum() : pPlaylistName);
        }

        return Lists.newArrayList(new ChangeRecord(rec.getOriginalName(), target.getName(), rec.getFileHandle(), true,
                target.getAbsolutePath(), OperationType.MOVE, extraParams, ExecStatus.PENDING));
    }

    private String preserveOriginalStructure(File sourceFile) {
        File rootDir = new File(pDestDir);
        File sourceParent = sourceFile.getParentFile();
        
        if (sourceParent == null) {
            return sourceFile.getName();
        }
        
        String relativePath = sourceParent.getAbsolutePath();
        String rootPath = new File(".").getAbsolutePath();
        
        if (relativePath.startsWith(rootPath)) {
            relativePath = relativePath.substring(rootPath.length());
        }
        
        relativePath = relativePath.replace(File.separatorChar, '/');
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        
        return relativePath + "/" + sourceFile.getName();
    }
}
