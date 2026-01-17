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

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.LanguageUtil;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.TagException;

/**
 * 文件清理策略
 * 支持：文件去重、文件夹去重、空目录清理
 * 删除方式：直接删除、伪删除（归档到垃圾箱）
 */
public class FileCleanupStrategy extends IAppStrategy {
    // 常见媒体类型定义，用于同类比较
    private static final Set<String> EXT_AUDIO = new HashSet<>(Arrays.asList("mp3", "flac", "wav", "aac", "m4a", "ogg", "wma", "ape", "alac", "aiff", "dsf", "dff"));
    private static final Set<String> EXT_VIDEO = new HashSet<>(Arrays.asList("mp4", "mkv", "avi", "mov", "wmv", "flv", "m4v", "mpg"));
    private static final Set<String> EXT_IMAGE = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "bmp", "gif", "webp", "tiff"));
    // --- UI Components ---
    private final JFXComboBox<CleanupMode> cbMode;
    private final JFXComboBox<DeleteMethod> cbMethod;
    private final TextField txtTrashPath; // 回收站路径（支持相对或绝对）
    private final CheckBox chkKeepLargest;
    private final CheckBox chkKeepEarliest;
    private final TextField txtKeepExt;
    // 文件名预处理选项
    private final CheckBox chkPreprocessLower;
    private final CheckBox chkPreprocessUpper;
    private final CheckBox chkPreprocessSimplified;
    // 文件大小范围选择
    private final JFXComboBox<FileSizeRange> cbSizeRange;
    // 音频特殊处理
    private final CheckBox chkAudioSpecial;
    // --- Runtime Params ---
    private CleanupMode pMode;
    private DeleteMethod pMethod;
    private String pTrashPath;
    private boolean pKeepLargest;
    private boolean pKeepEarliest;
    private String pKeepExt;
    // 预处理参数
    private boolean pPreprocessLower;
    private boolean pPreprocessUpper;
    private boolean pPreprocessSimplified;
    // 文件大小范围参数
    private FileSizeRange pSizeRange;
    // 音频特殊处理参数
    private boolean pAudioSpecial;

    public FileCleanupStrategy() {
        cbMode = new JFXComboBox<>(FXCollections.observableArrayList(CleanupMode.values()));
        cbMode.getSelectionModel().select(CleanupMode.DEDUP_FILES);
        cbMode.setTooltip(new Tooltip("选择清理的逻辑规则"));

        cbMethod = new JFXComboBox<>(FXCollections.observableArrayList(DeleteMethod.values()));
        cbMethod.getSelectionModel().select(DeleteMethod.PSEUDO_DELETE);
        cbMethod.setTooltip(new Tooltip("选择删除的方式"));

        txtTrashPath = new TextField(".EchoTrash");
        txtTrashPath.setPromptText("回收站位置");
        txtTrashPath.setTooltip(new Tooltip("输入相对名称（如 .del）将在各盘根目录创建；输入绝对路径（如 D:/Trash）则统一移动到该处。"));

        chkKeepLargest = new CheckBox("保留体积/质量最佳的副本");
        chkKeepLargest.setSelected(true);
        chkKeepLargest.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkKeepLargest.setTooltip(new Tooltip("勾选：保留最大的文件；不勾选：保留名字最短（通常是原件）的文件"));

        chkKeepEarliest = new CheckBox("保留日期最早/最晚的副本");
        chkKeepEarliest.setSelected(true);
        // 直接清理模式不需要显示日期保留选项
        BooleanBinding showKeepEarliest = cbMode.getSelectionModel().selectedItemProperty().isNotEqualTo(CleanupMode.REMOVE_EMPTY_DIRS)
                .and(cbMode.getSelectionModel().selectedItemProperty().isNotEqualTo(CleanupMode.DIRECT_CLEANUP));
        chkKeepEarliest.visibleProperty().bind(showKeepEarliest);
        chkKeepEarliest.setTooltip(new Tooltip("勾选：保留日期最早的文件(夹)；不勾选：保留最新的文件(夹)"));

        txtKeepExt = new TextField("wav");
        txtKeepExt.setPromptText("优先保留后缀");
        txtKeepExt.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        
        // 文件名预处理选项初始化
        chkPreprocessLower = new CheckBox("文件名转小写");
        chkPreprocessLower.setSelected(true);
        chkPreprocessLower.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkPreprocessLower.setTooltip(new Tooltip("将文件名转换为小写后进行比较"));
        
        chkPreprocessUpper = new CheckBox("文件名转大写");
        chkPreprocessUpper.setSelected(false);
        chkPreprocessUpper.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkPreprocessUpper.setTooltip(new Tooltip("将文件名转换为大写后进行比较"));
        
        // 实现大小写转换的互斥逻辑
        chkPreprocessLower.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                chkPreprocessUpper.setSelected(false);
            }
        });
        
        chkPreprocessUpper.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                chkPreprocessLower.setSelected(false);
            }
        });
        
        chkPreprocessSimplified = new CheckBox("文件名转简体中文");
        chkPreprocessSimplified.setSelected(false);
        chkPreprocessSimplified.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkPreprocessSimplified.setTooltip(new Tooltip("将文件名中的繁体中文转换为简体中文后进行比较"));
        
        // 文件大小范围选择初始化
        cbSizeRange = new JFXComboBox<>(FXCollections.observableArrayList(FileSizeRange.values()));
        cbSizeRange.getSelectionModel().select(FileSizeRange.ALL);
        // 去重文件和直接清理模式都需要显示文件大小范围选择
        BooleanBinding showSizeRange = cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES)
                .or(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DIRECT_CLEANUP));
        cbSizeRange.visibleProperty().bind(showSizeRange);
        cbSizeRange.setTooltip(new Tooltip("选择要处理的文件大小范围"));
        
        // 音频特殊处理选项初始化
        chkAudioSpecial = new CheckBox("音频文件特殊处理");
        chkAudioSpecial.setSelected(true);
        chkAudioSpecial.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkAudioSpecial.setTooltip(new Tooltip("对音频文件进行特殊处理，确保时间长度一致时优先保留质量较高的文件"));

    }

    @Override
    public String getName() {
        return "文件清理与去重";
    }

    @Override
    public String getDescription() {
        return "智能识别重复文件/文件夹、清理空目录。支持按盘符结构伪删除。";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.ALL;
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("清理模式:"), 0, 0);
        grid.add(cbMode, 1, 0);
        grid.add(new Label("删除方式:"), 0, 1);
        grid.add(cbMethod, 1, 1);

        // 动态配置区
        VBox dynamicArea = new VBox(8);
        dynamicArea.setStyle("-fx-background-color: rgba(0,0,0,0.03); -fx-padding: 10; -fx-background-radius: 5;");

        // 回收站配置
        HBox trashBox = new HBox(10);
        trashBox.setAlignment(Pos.CENTER_LEFT);
        JFXButton btnPickTrash = new JFXButton("浏览...");
        btnPickTrash.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtTrashPath.setText(f.getAbsolutePath());
        });
        trashBox.getChildren().addAll(new Label("回收站路径:"), txtTrashPath, btnPickTrash);
        
        // 伪删除和可回滚删除都需要显示回收站路径配置
        BooleanBinding showTrashPath = cbMethod.getSelectionModel().selectedItemProperty().isEqualTo(DeleteMethod.PSEUDO_DELETE)
                .or(cbMethod.getSelectionModel().selectedItemProperty().isEqualTo(DeleteMethod.ROLLBACKABLE_DELETE));
        txtTrashPath.visibleProperty().bind(showTrashPath);
        trashBox.visibleProperty().bind(showTrashPath);
        trashBox.managedProperty().bind(trashBox.visibleProperty());

        // 去重配置
        VBox dedupBox = new VBox(8);
        
        // 分组标题：基本去重选项
        Label lblBasicOptions = new Label("基本去重选项:");
        lblBasicOptions.setStyle("-fx-font-weight: bold;");
        VBox basicOptionsBox = new VBox(5);
        basicOptionsBox.setPadding(new javafx.geometry.Insets(0, 0, 0, 5));
        
        HBox keepRow1 = new HBox(10, new Label("优先后缀:"), txtKeepExt);
        keepRow1.setAlignment(Pos.CENTER_LEFT);
        HBox keepRow2 = new HBox(10, chkKeepLargest);
        keepRow2.setAlignment(Pos.CENTER_LEFT);
        HBox keepRow3 = new HBox(10, chkKeepEarliest);
        keepRow3.setAlignment(Pos.CENTER_LEFT);
        
        basicOptionsBox.getChildren().addAll(keepRow1, keepRow2, keepRow3);
        
        // 分组标题：文件名预处理
        Label lblPreprocess = new Label("文件名预处理:");
        lblPreprocess.setStyle("-fx-font-weight: bold;");
        VBox preprocessBox = new VBox(3);
        preprocessBox.setPadding(new javafx.geometry.Insets(5, 0, 5, 20));
        preprocessBox.getChildren().addAll(chkPreprocessLower, chkPreprocessUpper, chkPreprocessSimplified);
        
        // 分组标题：文件范围与特殊处理
        Label lblAdvancedOptions = new Label("高级选项:");
        lblAdvancedOptions.setStyle("-fx-font-weight: bold;");
        VBox advancedOptionsBox = new VBox(5);
        advancedOptionsBox.setPadding(new javafx.geometry.Insets(0, 0, 0, 5));
        
        // 文件大小范围选择
        HBox sizeRangeRow = new HBox(10, new Label("文件大小范围:"), cbSizeRange);
        sizeRangeRow.setAlignment(Pos.CENTER_LEFT);
        
        // 音频特殊处理选项
        HBox audioSpecialRow = new HBox(10, chkAudioSpecial);
        audioSpecialRow.setAlignment(Pos.CENTER_LEFT);
        
        advancedOptionsBox.getChildren().addAll(sizeRangeRow, audioSpecialRow);
        
        // 添加分隔线
        javafx.scene.control.Separator separator1 = new javafx.scene.control.Separator();
        javafx.scene.control.Separator separator2 = new javafx.scene.control.Separator();
        
        // 提示信息
        Label lblHint = new Label("提示：去重仅在同类型文件（如音频vs音频）间进行，会自动忽略 '(1)', 'Copy' 等后缀。");
        lblHint.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        dedupBox.getChildren().addAll(
            lblBasicOptions, basicOptionsBox,
            separator1,
            lblPreprocess, preprocessBox,
            separator2,
            lblAdvancedOptions, advancedOptionsBox,
            lblHint
        );
        dedupBox.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        dedupBox.managedProperty().bind(dedupBox.visibleProperty());

        dynamicArea.getChildren().addAll(trashBox, dedupBox);

        box.getChildren().addAll(grid, dynamicArea);
        return box;
    }

    @Override
    public void captureParams() {
        pMode = cbMode.getValue();
        pMethod = cbMethod.getValue();
        pTrashPath = txtTrashPath.getText();
        if (pTrashPath == null || pTrashPath.trim().isEmpty()) pTrashPath = ".EchoTrash";
        pKeepLargest = chkKeepLargest.isSelected();
        pKeepEarliest = chkKeepEarliest.isSelected();
        pKeepExt = txtKeepExt.getText();
        
        // 捕获预处理参数
        pPreprocessLower = chkPreprocessLower.isSelected();
        pPreprocessUpper = chkPreprocessUpper.isSelected();
        pPreprocessSimplified = chkPreprocessSimplified.isSelected();
        
        // 捕获文件大小范围参数
        pSizeRange = cbSizeRange.getValue();
        
        // 捕获音频特殊处理参数
        pAudioSpecial = chkAudioSpecial.isSelected();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("clean_mode", pMode.name());
        props.setProperty("clean_method", pMethod.name());
        props.setProperty("clean_trash", pTrashPath);
        props.setProperty("clean_keepLarge", String.valueOf(pKeepLargest));
        props.setProperty("clean_keepEarly", String.valueOf(pKeepEarliest));
        props.setProperty("clean_keepExt", pKeepExt);
        
        // 保存预处理参数
        props.setProperty("clean_preprocessLower", String.valueOf(pPreprocessLower));
        props.setProperty("clean_preprocessUpper", String.valueOf(pPreprocessUpper));
        props.setProperty("clean_preprocessSimplified", String.valueOf(pPreprocessSimplified));
        
        // 保存文件大小范围参数
        props.setProperty("clean_sizeRange", pSizeRange.name());
        
        // 保存音频特殊处理参数
        props.setProperty("clean_audioSpecial", String.valueOf(pAudioSpecial));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("clean_mode"))
            cbMode.getSelectionModel().select(CleanupMode.valueOf(props.getProperty("clean_mode")));
        if (props.containsKey("clean_method"))
            cbMethod.getSelectionModel().select(DeleteMethod.valueOf(props.getProperty("clean_method")));
        if (props.containsKey("clean_trash")) txtTrashPath.setText(props.getProperty("clean_trash"));
        if (props.containsKey("clean_keepLarge"))
            chkKeepLargest.setSelected(Boolean.parseBoolean(props.getProperty("clean_keepLarge")));
        if (props.containsKey("clean_keepEarly"))
            chkKeepEarliest.setSelected(Boolean.parseBoolean(props.getProperty("clean_keepEarly")));
        if (props.containsKey("clean_keepExt")) txtKeepExt.setText(props.getProperty("clean_keepExt"));
        
        // 加载预处理参数
        if (props.containsKey("clean_preprocessLower"))
            chkPreprocessLower.setSelected(Boolean.parseBoolean(props.getProperty("clean_preprocessLower")));
        if (props.containsKey("clean_preprocessUpper"))
            chkPreprocessUpper.setSelected(Boolean.parseBoolean(props.getProperty("clean_preprocessUpper")));
        if (props.containsKey("clean_preprocessSimplified"))
            chkPreprocessSimplified.setSelected(Boolean.parseBoolean(props.getProperty("clean_preprocessSimplified")));
        
        // 加载文件大小范围参数
        if (props.containsKey("clean_sizeRange"))
            cbSizeRange.getSelectionModel().select(FileSizeRange.valueOf(props.getProperty("clean_sizeRange")));
        
        // 加载音频特殊处理参数
        if (props.containsKey("clean_audioSpecial"))
            chkAudioSpecial.setSelected(Boolean.parseBoolean(props.getProperty("clean_audioSpecial")));
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        // 1. 根据模式选择分析器
        File f = rec.getFileHandle();

        if (pMode == CleanupMode.REMOVE_EMPTY_DIRS) {
            if (isDirectoryEmpty(f)) {
                return Collections.singletonList(createDeleteRecord(f, "空文件夹 (无子文件)"));
            }
            return Collections.emptyList();
        } else if (pMode == CleanupMode.DEDUP_FOLDERS) {
            File[] files = f.listFiles();
            if (f.isFile() || files == null || files.length < 2) {
                return Collections.emptyList();
            }
            return analyzeDuplicateFolders(Arrays.asList(files));
        } else if (pMode == CleanupMode.DIRECT_CLEANUP) {
            // 直接清理模式：直接删除文件（应用大小范围过滤）
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                if (files == null) {
                    return Collections.emptyList();
                }
                List<ChangeRecord> results = new ArrayList<>();
                for (File file : files) {
                    if (file.isFile() && pSizeRange.isInRange(file.length())) {
                        results.add(createDeleteRecord(file, "直接清理文件"));
                    }
                }
                return results;
            } else {
                // 如果是单个文件，直接检查是否符合大小范围并删除
                if (pSizeRange.isInRange(f.length())) {
                    return Collections.singletonList(createDeleteRecord(f, "直接清理文件"));
                }
                return Collections.emptyList();
            }
        } else {
            File[] files = f.listFiles();
            if (f.isFile() || files == null || files.length < 2) {
                return Collections.emptyList();
            }
            // 应用文件大小范围过滤
            List<File> filteredFiles = Arrays.stream(files)
                    .filter(file -> file.isFile() && pSizeRange.isInRange(file.length()))
                    .collect(Collectors.toList());
            if (filteredFiles.size() < 2) {
                return Collections.emptyList();
            }
            return analyzeDuplicateFiles(filteredFiles);
        }
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.DELETE) return;

        File file = rec.getFileHandle();
        if (!file.exists()) return; // 可能已经被处理了

        String method = rec.getExtraParams().get("method");

        if ("DIRECT".equals(method)) {
            // 递归删除（如果是文件夹）
            if (file.isDirectory()) deleteDirectoryRecursively(file);
            else Files.delete(file.toPath());
        } else {
            // 伪删除：移动到 Trash
            String trashPath = rec.getNewPath();
            if (trashPath != null && !trashPath.isEmpty()) {
                File trashFile = new File(trashPath);
                if (!trashFile.getParentFile().exists()) trashFile.getParentFile().mkdirs();
                // 尝试移动
                try {
                    Files.move(file.toPath(), trashFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    // 跨盘移动 fallback
                    if (file.isDirectory()) copyDirectory(file, trashFile);
                    else Files.copy(file.toPath(), trashFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    deleteDirectoryRecursively(file); // 移完后删源
                }
            }
        }
    }

    private void deleteDirectoryRecursively(File file) throws IOException {
        Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // --- 具体分析逻辑 ---

    /**
     * 1. 智能文件去重
     */
    private List<ChangeRecord> analyzeDuplicateFiles(List<File> files) {
        List<ChangeRecord> result = new ArrayList<>();
        Map<File, List<File>> dirMap = files.stream().filter(File::isFile).collect(Collectors.groupingBy(File::getParentFile));

        // 正则：提取文件名核心 (忽略 (1), - Copy 等)
        Pattern normPattern = Pattern.compile("^(.+?)(\\s*[\\(\\[（].*?[\\)\\]）])?(\\s*-\\s*(副本|Copy))?(\\s*\\(\\d+\\))?(\\.[^.]+)?$");

        for (Map.Entry<File, List<File>> entry : dirMap.entrySet()) {
            // 二级分组：CoreName -> List<File>
            Map<String, List<File>> nameGroup = entry.getValue().stream().collect(Collectors.groupingBy(f -> {
                String name = f.getName();
                Matcher m = normPattern.matcher(name);
                String core = m.find() ? m.group(1).trim() : name;
                // 应用文件名预处理
                core = preprocessFilename(core);
                String ext = getExt(name);
                String typeTag = getMediaType(ext);
                return core + "::" + typeTag; // Key: "song::AUDIO"
            }));

            for (List<File> group : nameGroup.values()) {
                if (group.size() < 2) continue; // 无重复

                // 检查是否是音频文件组
                String ext = getExt(group.get(0).getName());
                boolean isAudioGroup = EXT_AUDIO.contains(ext);
                
                List<File> filesToProcess = new ArrayList<>(group);
                
                if (isAudioGroup && pAudioSpecial) {
                    // 音频文件特殊处理：按持续时间二次分组，仅对时间一致的文件去重
                    Map<Long, List<File>> durationGroups = new HashMap<>();
                    
                    for (File file : group) {
                        Map<String, Long> metadata = getAudioMetadata(file);
                        if (metadata != null) {
                            long duration = metadata.get("duration");
                            // 精确匹配持续时间（毫秒级）
                            durationGroups.computeIfAbsent(duration, k -> new ArrayList<>()).add(file);
                        } else {
                            // 无法读取元数据的文件单独处理
                            durationGroups.computeIfAbsent(-1L, k -> new ArrayList<>()).add(file);
                        }
                    }
                    
                    // 对每个持续时间组进行处理
                    for (List<File> durationGroup : durationGroups.values()) {
                        if (durationGroup.size() < 2) continue;
                        
                        // 音频文件特殊选择逻辑：优先保留高质量文件
                        File keeper = Collections.max(durationGroup, (f1, f2) -> {
                            // 1. 优先后缀匹配
                            if (pKeepExt != null && !pKeepExt.isEmpty()) {
                                boolean k1 = f1.getName().toLowerCase().endsWith("." + pKeepExt.toLowerCase());
                                boolean k2 = f2.getName().toLowerCase().endsWith("." + pKeepExt.toLowerCase());
                                if (k1 != k2) {
                                    return k1 ? 1 : -1;
                                }
                            }
                            
                            // 2. 优先比较码率（音频质量）
                            Map<String, Long> meta1 = getAudioMetadata(f1);
                            Map<String, Long> meta2 = getAudioMetadata(f2);
                            
                            if (meta1 != null && meta2 != null) {
                                long bitrate1 = meta1.getOrDefault("bitrate", 0L);
                                long bitrate2 = meta2.getOrDefault("bitrate", 0L);
                                if (bitrate1 != bitrate2) {
                                    return Long.compare(bitrate1, bitrate2);
                                }
                            }
                            
                            // 3. 体积优先
                            if (pKeepLargest) {
                                int sizeCmp = Long.compare(f1.length(), f2.length());
                                if (sizeCmp != 0) {
                                    return sizeCmp;
                                }
                            }
                            
                            // 4. 变更时间优先
                            if (pKeepEarliest) {
                                int timeCmp = Long.compare(f2.lastModified(), f1.lastModified());
                                if (timeCmp != 0) {
                                    return timeCmp;
                                }
                                
                                try {
                                    BasicFileAttributes attributes = Files.readAttributes(Paths.get(f1.getPath()), BasicFileAttributes.class);
                                    BasicFileAttributes attributes2 = Files.readAttributes(Paths.get(f2.getPath()), BasicFileAttributes.class);
                                    if (attributes2.lastModifiedTime().compareTo(attributes.lastModifiedTime()) != 0) {
                                        return attributes2.lastModifiedTime().compareTo(attributes.lastModifiedTime());
                                    }
                                    if (attributes2.creationTime().compareTo(attributes.creationTime()) != 0) {
                                        return attributes2.creationTime().compareTo(attributes.creationTime());
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            
                            // 5. 默认：名字短的优先 (通常不带 (1) 的是原件)
                            int lenCmp = Integer.compare(f2.getName().length(), f1.getName().length());
                            if (lenCmp != 0) {
                                return lenCmp;
                            }
                            
                            // 6. 默认：名字排序靠前的优先 (通常是大写)
                            return StringUtils.compare(f2.getName(), f1.getName(), true);
                        });
                        
                        // 标记要删除的文件
                        for (File f : durationGroup) {
                            if (f == keeper) continue;
                            result.add(createDeleteRecord(f, "重复副本 (与 " + keeper.getName() + " 内容重复)"));
                        }
                    }
                } else {
                    // 非音频文件或未启用音频特殊处理：使用原有逻辑
                    File keeper = Collections.max(group, (f1, f2) -> {
                        // 1. 优先后缀匹配
                        if (pKeepExt != null && !pKeepExt.isEmpty()) {
                            boolean k1 = f1.getName().toLowerCase().endsWith("." + pKeepExt.toLowerCase());
                            boolean k2 = f2.getName().toLowerCase().endsWith("." + pKeepExt.toLowerCase());
                            if (k1 != k2) {
                                return k1 ? 1 : -1;
                            }
                        }

                        // 2. 体积优先
                        if (pKeepLargest) {
                            int sizeCmp = Long.compare(f1.length(), f2.length());
                            if (sizeCmp != 0) {
                                return sizeCmp;
                            }
                        }

                        // 3. 变更时间优先
                        if (pKeepEarliest) {
                            int sizeCmp = Long.compare(f2.lastModified(), f1.lastModified());
                            if (sizeCmp != 0) {
                                return sizeCmp;
                            }

                            try {
                                BasicFileAttributes attributes = Files.readAttributes(Paths.get(f1.getPath()), BasicFileAttributes.class);
                                BasicFileAttributes attributes2 = Files.readAttributes(Paths.get(f2.getPath()), BasicFileAttributes.class);
                                if (attributes2.lastModifiedTime().compareTo(attributes.lastModifiedTime()) != 0) {
                                    return attributes2.lastModifiedTime().compareTo(attributes.lastModifiedTime());
                                }
                                if (attributes2.creationTime().compareTo(attributes.creationTime()) != 0) {
                                    return attributes2.creationTime().compareTo(attributes.creationTime());
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        // 4. 默认：名字短的优先 (通常不带 (1) 的是原件)
                        int compLen = Integer.compare(f2.getName().length(), f1.getName().length());
                        if (compLen != 0) {
                            return compLen;
                        }

                        // 5. 默认：名字排序靠前的优先 (通常是大写)
                        return StringUtils.compare(f2.getName(), f1.getName(), true);
                    });

                    // 严格检查：必须确保 keeper 存在于 group 中，且不被删除
                    for (File f : group) {
                        if (f == keeper) {
                            continue; // 保留
                        }
                        result.add(createDeleteRecord(f, "重复副本 (与 " + keeper.getName() + " 内容重复)"));
                    }
                }
            }
        }
        return result;
    }

    private String getExt(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(i + 1).toLowerCase() : "";
    }

    private String getMediaType(String ext) {
        if (EXT_AUDIO.contains(ext)) return "AUDIO";
        if (EXT_VIDEO.contains(ext)) return "VIDEO";
        if (EXT_IMAGE.contains(ext)) return "IMAGE";
        return "OTHER_" + ext;
    }
    
    /**
     * 对文件名进行预处理
     * @param filename 原始文件名
     * @return 预处理后的文件名
     */
    private String preprocessFilename(String filename) {
        String processed = filename;
        
        // 繁简中文转换
        if (pPreprocessSimplified) {
            try {
                // 使用LanguageUtil进行中文转换
                processed = LanguageUtil.toSimpleChinese(processed);
            } catch (Exception e) {
                // 如果转换失败，保持原样
                e.printStackTrace();
            }
        }
        
        // 大小写转换（优先级：大写优先于小写）
        if (pPreprocessUpper) {
            processed = processed.toUpperCase();
        } else if (pPreprocessLower) {
            processed = processed.toLowerCase();
        }
        
        return processed;
    }
    
    /**
     * 获取音频文件的元数据
     * @param file 音频文件
     * @return 包含持续时间（毫秒）和码率（kbps）的Map，无法读取时返回null
     */
    private Map<String, Long> getAudioMetadata(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader audioHeader = audioFile.getAudioHeader();
            
            Map<String, Long> metadata = new HashMap<>();
            // 获取持续时间（毫秒）
            metadata.put("duration", audioHeader.getTrackLength() * 1000L);
            // 获取码率（kbps）
            metadata.put("bitrate", audioHeader.getBitRateAsNumber());
            
            return metadata;
        } catch (Exception e) {
            // 忽略无法读取的音频文件
            return null;
        }
    }

    /**
     * 2. 文件夹去重 (内容一致性检查)
     */
    private List<ChangeRecord> analyzeDuplicateFolders(List<File> files) {
        List<ChangeRecord> result = new ArrayList<>();
        List<File> dirs = files.stream().filter(File::isDirectory).collect(Collectors.toList());
        Map<File, List<File>> parentMap = dirs.stream().filter(f -> f.getParentFile() != null).collect(Collectors.groupingBy(File::getParentFile));
        for (List<File> siblings : parentMap.values()) {
            // 计算指纹（包含递归内容）
            Map<File, String> fingerprints = new HashMap<>();
            for (File dir : siblings) {
                fingerprints.put(dir, calculateRecursiveDirFingerprint(dir));
            }

            // 按指纹分组
            Map<String, List<File>> dupeGroups = siblings.stream().collect(Collectors.groupingBy(fingerprints::get));

            for (Map.Entry<String, List<File>> entry : dupeGroups.entrySet()) {
                if (entry.getKey().isEmpty()) {
                    continue; // 忽略空指纹或无法读取的
                }
                List<File> group = entry.getValue();
                if (group.size() < 2) {
                    continue;
                }

                // 保留名字最短的
                group.sort(Comparator.comparingInt((File f) -> f.getName().length()));
                File keeper = group.get(0);

                for (int i = 1; i < group.size(); i++) {
                    File toDelete = group.get(i);
                    String sizeStr = formatSize(getDirSize(toDelete));
                    result.add(createDeleteRecord(toDelete, "文件夹内容重复 (同: " + keeper.getName() + ", 大小: " + sizeStr + ")"));
                }
            }
        }
        return result;
    }

    /**
     * 递归计算文件夹指纹：相对路径 + 文件大小
     * 只有目录结构和文件大小完全一致才视为重复
     */
    private String calculateRecursiveDirFingerprint(File dir) {
        try {
            StringBuilder sb = new StringBuilder();
            Files.walk(dir.toPath()).sorted() // 确保顺序一致
                    .forEach(path -> {
                        File f = path.toFile();
                        String relPath = dir.toPath().relativize(path).toString();
                        sb.append(relPath).append(":");
                        if (f.isFile()) sb.append(f.length());
                        else sb.append("D");
                        sb.append("|");
                    });
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 递归计算文件夹大小
     */
    private long getDirSize(File dir) {
        try {
            return Files.walk(dir.toPath()).filter(p -> p.toFile().isFile()).mapToLong(p -> p.toFile().length()).sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private boolean isDirectoryEmpty(File directory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory.toPath())) {
            return !dirStream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    // --- 路径计算核心 ---

    private ChangeRecord createDeleteRecord(File f, String reason) {
        String newPath;
        String newName;
        Map<String, String> params = new HashMap<>();

        if (pMethod == DeleteMethod.DIRECT_DELETE) {
            newPath = "PERMANENT_DELETE";
            newName = "[删除] " + f.getName();
            params.put("method", "DIRECT");
        } else {
            // 伪删除或可回滚删除路径计算
            File trashRoot;
            Path sourcePath = f.toPath();
            Path root = sourcePath.getRoot();
            
            // 基础回收站路径
            if (Paths.get(pTrashPath).isAbsolute()) {
                // 模式 B: 固定绝对路径
                File fixedTrash = new File(pTrashPath);
                String driveTag = root.toString().replace(":", "").replace(File.separator, "") + "_Drive";
                Path relativeToRoot = root.relativize(sourcePath).getParent();
                trashRoot = new File(fixedTrash, driveTag);
                if (relativeToRoot != null) trashRoot = new File(trashRoot, relativeToRoot.toString());
            } else {
                // 模式 A: 相对路径
                File driveRoot = root.toFile();
                trashRoot = new File(driveRoot, pTrashPath);
                Path relativeToRoot = root.relativize(sourcePath).getParent();
                if (relativeToRoot != null) trashRoot = new File(trashRoot, relativeToRoot.toString());
            }
            
            // 可回滚删除：添加时间戳子目录
            if (pMethod == DeleteMethod.ROLLBACKABLE_DELETE) {
                // 使用应用启动时间戳作为统一的时间戳 (格式: yyyyMMdd_HHmmss)
                long taskStartTimestamp = app != null ? app.getTaskStartTimStamp() : System.currentTimeMillis();
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(taskStartTimestamp));
                trashRoot = new File(trashRoot, timestamp);
            }

            File trashFile = new File(trashRoot, f.getName());
            newPath = trashFile.getAbsolutePath();
            newName = "[回收] " + f.getName();
            params.put("method", pMethod == DeleteMethod.ROLLBACKABLE_DELETE ? "ROLLBACKABLE" : "PSEUDO");
        }
        params.put("reason", reason);

        // 在 ChangeRecord 中复用 RENAME 只是为了显示，OpType 必须是 DELETE
        return new ChangeRecord(f.getName(), newName, f, true, newPath, OperationType.DELETE, params, ExecStatus.PENDING);
    }

    private void copyDirectory(File source, File target) throws IOException {
        Files.walk(source.toPath()).forEach(sourcePath -> {
            Path targetPath = target.toPath().resolve(source.toPath().relativize(sourcePath));
            try {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public enum CleanupMode {
        DEDUP_FILES("同名/近似文件去重"), 
        DEDUP_FOLDERS("重复内容文件夹去重"), 
        REMOVE_EMPTY_DIRS("删除空文件夹"),
        DIRECT_CLEANUP("直接清理文件");
        private final String label;

        CleanupMode(String l) {
            label = l;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum DeleteMethod {
        PSEUDO_DELETE("伪删除 (保留目录结构到回收站)"), 
        DIRECT_DELETE("直接物理删除 (不可恢复)"),
        ROLLBACKABLE_DELETE("可回滚删除 (保留到按时间命名的子目录)");
        private final String label;

        DeleteMethod(String l) {
            label = l;
        }

        @Override
        public String toString() {
            return label;
        }
    }
    
    /**
     * 文件大小范围枚举
     */
    public enum FileSizeRange {
        ALL("不区分大小", 0, Long.MAX_VALUE),
        SMALL("小文件 (< 10MB)", 0, 10 * 1024 * 1024),
        MEDIUM("中等文件 (10MB - 100MB)", 10 * 1024 * 1024, 100 * 1024 * 1024),
        LARGE("大文件 (> 100MB)", 100 * 1024 * 1024, Long.MAX_VALUE);
        
        private final String label;
        private final long minSize;
        private final long maxSize;
        
        FileSizeRange(String label, long minSize, long maxSize) {
            this.label = label;
            this.minSize = minSize;
            this.maxSize = maxSize;
        }
        
        public boolean isInRange(long size) {
            return size >= minSize && size < maxSize;
        }
        
        @Override
        public String toString() {
            return label;
        }
    }

}