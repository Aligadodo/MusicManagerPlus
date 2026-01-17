/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.CueSheet;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.LanguageUtil;
import com.filemanager.util.file.CueParserUtil;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.DirectoryChooser;
import org.apache.commons.lang3.exception.ExceptionUtils;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.filemanager.app.tools.display.StyleFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * CUE 分轨策略 (专业版)
 * 功能：解析 CUE 文件，智能定位音频源，基于时间戳调用 FFmpeg 精确切割，并写入元数据。
 */
public class CueSplitterStrategy extends AbstractFfmpegStrategy {
    // --- 切分完成后执行选项 UI 组件 ---
    protected final JFXComboBox<String> cbAfterSplitAction;
    protected final CheckBox chkEnableArchive;
    protected final TextField txtArchiveDir;
    protected final JFXButton btnPickArchiveDir;
    
    // --- 运行时参数 ---
    protected String pAfterSplitAction;
    protected boolean pEnableArchive;
    protected String pArchiveDir;
    
    // 用于跟踪每个cue文件的处理状态
    private final Map<String, Set<String>> cueTrackProcessingStatus = new HashMap<>();

    public CueSplitterStrategy() {
        super();
        
        // 初始化切分完成后执行选项
        cbAfterSplitAction = new JFXComboBox<>(FXCollections.observableArrayList(
                "什么都不做 (默认)",
                "删除原始文件",
                "归档原始文件"
        ));
        cbAfterSplitAction.setTooltip(new Tooltip("选择切分完成后对原始文件的处理方式"));
        cbAfterSplitAction.getSelectionModel().select(0);
        
        chkEnableArchive = new CheckBox("启用归档目录");
        chkEnableArchive.setTooltip(new Tooltip("启用时，将原始文件移动到指定的归档目录"));
        chkEnableArchive.setSelected(false);
        
        txtArchiveDir = new TextField();
        txtArchiveDir.setPromptText("归档目录路径");
        
        btnPickArchiveDir = StyleFactory.createActionButton("选择路径", "", () -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtArchiveDir.setText(f.getAbsolutePath());
        });
        
        // 控制归档目录输入框的可用性
        txtArchiveDir.disableProperty().bind(chkEnableArchive.selectedProperty().not());
        btnPickArchiveDir.disableProperty().bind(chkEnableArchive.selectedProperty().not());
        
        // 当选择归档原始文件时自动启用归档目录选项
        cbAfterSplitAction.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isArchiveSelected = "归档原始文件".equals(newVal);
            chkEnableArchive.setSelected(isArchiveSelected);
        });
    }
    
    /**
     * 初始化指定cue文件的音轨列表
     * @param cueFilePath cue文件的绝对路径
     * @param trackIds 该cue文件对应的所有音轨ID列表
     */
    private void initializeCueTracks(String cueFilePath, List<String> trackIds) {
        cueTrackProcessingStatus.computeIfAbsent(cueFilePath, k -> new HashSet<>()).addAll(trackIds);
    }
    
    /**
     * 标记指定cue文件的指定音轨为已完成
     * @param cueFilePath cue文件的绝对路径
     * @param trackId 音轨ID
     */
    private void markTrackAsCompleted(String cueFilePath, String trackId) {
        Set<String> trackIds = cueTrackProcessingStatus.get(cueFilePath);
        if (trackIds != null) {
            trackIds.remove(trackId);
        }
    }
    
    /**
     * 检查指定cue文件的所有音轨是否都已完成切分
     * @param cueFilePath cue文件的绝对路径
     * @return 如果所有音轨都已完成，返回true；否则返回false
     */
    private boolean isAllTracksCompleted(String cueFilePath) {
        Set<String> trackIds = cueTrackProcessingStatus.get(cueFilePath);
        return trackIds != null && trackIds.isEmpty();
    }
    
    /**
     * 切分完成后执行选择的操作
     * @param cueFilePath cue文件的绝对路径
     * @param audioFilePath 原始音频文件的绝对路径
     */
    private void afterSplitProcess(String cueFilePath, String audioFilePath) {
        // 检查所有音轨是否都已完成
        if (!isAllTracksCompleted(cueFilePath)) {
            return;
        }
        
        // 根据选择的操作执行相应的处理
        if ("什么都不做 (默认)".equals(pAfterSplitAction)) {
            log("已完成所有音轨切分，选择：什么都不做");
        } else if ("删除原始文件".equals(pAfterSplitAction)) {
            deleteOriginalFiles(cueFilePath, audioFilePath);
        } else if ("归档原始文件".equals(pAfterSplitAction)) {
            if (pEnableArchive && pArchiveDir != null && !pArchiveDir.isEmpty()) {
                archiveOriginalFiles(cueFilePath, audioFilePath);
            } else {
                log("已完成所有音轨切分，但归档目录未设置或未启用，将什么都不做");
            }
        }
    }
    
    /**
     * 删除原始文件
     * @param cueFilePath cue文件的绝对路径
     * @param audioFilePath 原始音频文件的绝对路径
     */
    private void deleteOriginalFiles(String cueFilePath, String audioFilePath) {
        try {
            // 删除cue文件
            File cueFile = new File(cueFilePath);
            if (cueFile.exists() && cueFile.delete()) {
                log("已删除原始cue文件: " + cueFilePath);
            }
            
            // 删除原始音频文件
            File audioFile = new File(audioFilePath);
            if (audioFile.exists() && audioFile.delete()) {
                log("已删除原始音频文件: " + audioFilePath);
            }
            
            // 清理处理状态记录
            cueTrackProcessingStatus.remove(cueFilePath);
        } catch (Exception e) {
            logError("删除原始文件时出错: " + e.getMessage());
        }
    }
    
    /**
     * 归档原始文件
     * @param cueFilePath cue文件的绝对路径
     * @param audioFilePath 原始音频文件的绝对路径
     */
    private void archiveOriginalFiles(String cueFilePath, String audioFilePath) {
        try {
            // 确保归档目录存在
            Path archiveDirPath = Paths.get(pArchiveDir);
            if (!Files.exists(archiveDirPath)) {
                Files.createDirectories(archiveDirPath);
            }
            
            // 归档cue文件
            Path sourceCuePath = Paths.get(cueFilePath);
            Path targetCuePath = archiveDirPath.resolve(sourceCuePath.getFileName());
            Files.move(sourceCuePath, targetCuePath, StandardCopyOption.REPLACE_EXISTING);
            log("已归档cue文件: " + cueFilePath + " -> " + targetCuePath.toString());
            
            // 归档原始音频文件
            Path sourceAudioPath = Paths.get(audioFilePath);
            Path targetAudioPath = archiveDirPath.resolve(sourceAudioPath.getFileName());
            Files.move(sourceAudioPath, targetAudioPath, StandardCopyOption.REPLACE_EXISTING);
            log("已归档音频文件: " + audioFilePath + " -> " + targetAudioPath.toString());
            
            // 归档其他相关文件（同目录下非音频分轨目标路径的文件）
            File cueParentDir = sourceCuePath.getParent().toFile();
            if (cueParentDir.isDirectory()) {
                File[] files = cueParentDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        // 跳过已归档的文件和目录
                        if (file.equals(sourceCuePath.toFile()) || file.equals(sourceAudioPath.toFile()) || file.isDirectory()) {
                            continue;
                        }
                        
                        // 跳过已切分的音轨文件
                        boolean isSplitTrack = false;
                        for (String trackPattern : Arrays.asList("*.wav", "*.flac", "*.mp3", "*.alac", "*.aac", "*.ogg")) {
                            if (file.getName().toLowerCase().matches(trackPattern.toLowerCase().replace("*", ".*"))) {
                                isSplitTrack = true;
                                break;
                            }
                        }
                        
                        if (!isSplitTrack) {
                            Path sourceFilePath = file.toPath();
                            Path targetFilePath = archiveDirPath.resolve(sourceFilePath.getFileName());
                            Files.move(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                            log("已归档相关文件: " + sourceFilePath.toString() + " -> " + targetFilePath.toString());
                        }
                    }
                }
            }
            
            // 清理处理状态记录
            cueTrackProcessingStatus.remove(cueFilePath);
        } catch (IOException e) {
            logError("归档原始文件时出错: " + e.getMessage());
        }
    }

    @Override
    public String getDefaultDirPrefix() {
        return "Split";
    }

    @Override
    public String getName() {
        return "CUE整轨自动切割";
    }

    @Override
    public String getDescription() {
        return "解析 .cue 索引文件，将整轨音频无损切割为单曲。" +
                "支持预览详细的歌曲清单与时长信息。只需要扫描cue文件。" +
                "同一个音轨在切分时不会同时执行，避免文件锁出现，会分成多轮任务逐个完成切分。" +
                "如果音频存储在机械盘，可以使用缓存目录或者镜像目录（挂载到SSD盘下）进行处理加速，提升5-10倍的处理效率。";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public Node getConfigNode() {
        Node parentConfig = super.getConfigNode();
        
        // 创建切分完成后执行选项的配置面板
        Node afterSplitOptions = StyleFactory.createVBoxPanel(
                StyleFactory.createChapter("切分完成后执行选项"),
                StyleFactory.createParamPairLine("执行选项:", cbAfterSplitAction),
                chkEnableArchive,
                StyleFactory.createHBox(
                        StyleFactory.createParamLabel("归档目录:"),
                        txtArchiveDir,
                        btnPickArchiveDir
                )
        );
        
        // 将新的配置面板添加到父配置面板中
        return StyleFactory.createVBoxPanel(
                parentConfig,
                StyleFactory.createSeparator(),
                afterSplitOptions
        );
    }

    @Override
    public void captureParams() {
        super.captureParams();
        
        // 捕获切分完成后执行选项的参数
        pAfterSplitAction = cbAfterSplitAction.getValue();
        pEnableArchive = chkEnableArchive.isSelected();
        pArchiveDir = txtArchiveDir.getText();
    }

    @Override
    public void saveConfig(Properties props) {
        super.saveConfig(props);
        
        // 保存切分完成后执行选项的配置
        props.setProperty("cue_after_split_action", pAfterSplitAction);
        props.setProperty("cue_enable_archive", String.valueOf(pEnableArchive));
        props.setProperty("cue_archive_dir", pArchiveDir);
    }

    @Override
    public void loadConfig(Properties props) {
        super.loadConfig(props);
        
        // 加载切分完成后执行选项的配置
        if (props.containsKey("cue_after_split_action")) {
            cbAfterSplitAction.getSelectionModel().select(props.getProperty("cue_after_split_action"));
        }
        if (props.containsKey("cue_enable_archive")) {
            chkEnableArchive.setSelected(Boolean.parseBoolean(props.getProperty("cue_enable_archive")));
        }
        if (props.containsKey("cue_archive_dir")) {
            txtArchiveDir.setText(props.getProperty("cue_archive_dir"));
        }
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.SPLIT) return;
        
        // 获取cue文件路径和音轨ID
        String cueFilePath = rec.getExtraParams().get("cueFilePath");
        String trackId = rec.getExtraParams().get("trackId");
        String sourceAudioPath = rec.getExtraParams().get("source");
        
        // 执行切分操作
        super.execute(rec);
        
        // 标记当前音轨为已完成
        if (cueFilePath != null && trackId != null) {
            markTrackAsCompleted(cueFilePath, trackId);
            
            // 检查是否所有音轨都已完成切分，如果是，则执行选择的操作
            afterSplitProcess(cueFilePath, sourceAudioPath);
        }
    }

    // --- 核心逻辑：分析 ---
    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        if (!rec.getFileHandle().getName().toLowerCase().endsWith(".cue") || rec.getFileHandle().isDirectory()) {
            return Collections.emptyList();
        }
        File cueFile = rec.getFileHandle();
        // 解析
        CueSheet cueSheet = null;
        try {
            cueSheet = CueParserUtil.parse(cueFile.toPath());
        } catch (Exception e) {
            logError("Cue文件解析失败，跳过：: " + cueFile.toPath() + "，错误详情：" + ExceptionUtils.getStackTrace(e));
        }
        if (cueSheet == null || cueSheet.getTracks().isEmpty()) return Collections.emptyList();

        // 分轨的cue文件无需再切分
        if (cueSheet.getCountFiles() == cueSheet.getTracks().size()) {
            log("自动忽略。已切分的分轨文件，无需重新切分，直接用格式转换组件即可处理：" + cueFile.getAbsolutePath());
            return Collections.emptyList();
        }

        // 定位音频源文件
        File sourceAudio = CueParserUtil.locateAudioFile(cueFile, cueSheet.getAlbumFileName());
        if (sourceAudio == null) return Collections.emptyList();
        List<ChangeRecord> tracks = new ArrayList<>();
        List<CueSheet.CueTrack> cueTracks = cueSheet.getTracks();

        // 用于存储当前cue文件的所有音轨ID
        List<String> trackIds = new ArrayList<>();
        
        for (int i = 0; i < cueTracks.size(); i++) {
            CueSheet.CueTrack t = cueTracks.get(i);
            Map<String, String> params = getParams(sourceAudio.getParentFile(), "Track-" + t.getNumber());
            sourceAudio = CueParserUtil.locateAudioFile(cueFile, t.getFormatedFileName());
            if (sourceAudio == null) {
                continue;
            }
            // 起止时间
            long startTime = t.getSoundStartTimeMs();
            long duration = t.getDuration();

            // 构建文件名
            String trackName = t.getFormatedTrackName(params.get("format"));

            // 自动格式化目标文件名
            if (Boolean.parseBoolean(params.getOrDefault("autoFormatFilename", "true"))) {
                trackName = LanguageUtil.toSimpleChinese(trackName).trim();
            }

            // 继承全局信息
            String artist = t.getPerformer();
            String album = cueSheet.getAlbumTitle();
            // [核心优化] 丰富预览信息：[01] 歌名 - 歌手 [04:20]
            String displayInfo = t.getDisplayInfo();
            File targetFile = new File(params.get("parentPath"), trackName);
            if (params.containsKey("doubleCheckParentPath")) {
                File doubleCheckTargetFile = new File(params.get("doubleCheckParentPath"), trackName);
                if (doubleCheckTargetFile.exists() && !pOverwrite) {
                    continue;
                }
            }
            // 忽略已存在的文件
            boolean targetExists = targetFile.exists();
            if (targetExists && !pOverwrite) {
                continue;
            }
            params.put("source", sourceAudio.getAbsolutePath());
            // 存入毫秒数
            params.put("start", startTime + "");
            if (duration != 0) {
                params.put("duration", String.format(Locale.US, "%d", duration));
            }
            if (t.getTitle() != null) {
                params.put("meta_title", t.getTitle());
                params.put("meta_artist", artist);
                params.put("meta_album", album);
                params.put("meta_track", String.valueOf(t.getNumber()));
            }
            // 为ChangeRecord添加cue文件信息，用于跟踪
            params.put("cueFilePath", cueFile.getAbsolutePath());
            params.put("trackId", trackName);
            
            ChangeRecord trackRec = new ChangeRecord(
                    // 使用富信息作为源展示
                    displayInfo,
                    trackName,
                    sourceAudio,
                    true,
                    targetFile.getAbsolutePath(),
                    OperationType.SPLIT,
                    params,
                    ExecStatus.PENDING
            );
            tracks.add(trackRec);
            
            // 添加到音轨ID列表
            trackIds.add(trackName);
        }
        
        // 初始化当前cue文件的音轨列表，用于跟踪处理状态
        initializeCueTracks(cueFile.getAbsolutePath(), trackIds);
        
        return tracks;
    }

}