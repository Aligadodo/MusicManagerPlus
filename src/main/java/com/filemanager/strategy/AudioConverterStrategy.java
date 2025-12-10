package com.filemanager.strategy;

import com.filemanager.model.*;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 音频转换策略 (v19.6 CD Mode Fix)
 * 优化点：
 * 1. 修复了 Lambda 表达式中变量非 effectively final 的编译错误。
 * 2. 完善了 CD 模式的参数锁定逻辑，防止被通用参数覆盖。
 */
public class AudioConverterStrategy extends AppStrategy {

    // --- UI 组件 ---
    private final JFXComboBox<String> cbTargetFormat;
    private final JFXComboBox<String> cbOutputDirMode;
    private final TextField txtRelativePath;

    // 覆盖控制
    private final CheckBox chkOverwrite;

    // FFmpeg 参数控制
    private final Spinner<Integer> spFfmpegThreads;
    private final JFXComboBox<String> cbSampleRate;
    private final JFXComboBox<String> cbChannels;

    private final TextField txtFFmpegPath;
    private final CheckBox chkEnableCache;
    private final TextField txtCacheDir;
    private final CheckBox chkForceFilenameMeta;

    // --- 运行时参数 ---
    private String pFormat;
    private String pMode;
    private String pRelPath;
    private boolean pOverwrite;
    private String pFFmpeg;
    private boolean pUseCache;
    private String pCacheDir;
    private boolean pForceMeta;

    private int pInnerThreads;
    private String pSampleRate;
    private String pChannels;

    public AudioConverterStrategy() {
        cbTargetFormat = new JFXComboBox<>(FXCollections.observableArrayList("FLAC", "WAV", "WAV (CD标准)", "MP3", "ALAC", "AAC", "OGG"));
        cbTargetFormat.getSelectionModel().select("FLAC");

        cbOutputDirMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "原目录 (Source)",
                "子目录 (Sub-folder)",
                "同级目录 (Sibling folder)",
                "自定义相对路径"
        ));
        cbOutputDirMode.getSelectionModel().select(1);

        txtRelativePath = new TextField("");
        updateDefaultPathPrompt("FLAC");

        cbSampleRate = new JFXComboBox<>(FXCollections.observableArrayList("保持原样 (Original)", "44100", "48000", "88200", "96000", "192000"));
        cbSampleRate.getSelectionModel().select(0);

        cbChannels = new JFXComboBox<>(FXCollections.observableArrayList("保持原样 (Original)", "1 (Mono)", "2 (Stereo)", "6 (5.1)"));
        cbChannels.getSelectionModel().select(0);

        cbTargetFormat.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateDefaultPathPrompt(newVal);
            invalidatePreview();
            // 如果选中 CD 标准，自动禁用采样率和声道选择，避免误导
            boolean isCD = "WAV (CD标准)".equals(newVal);
            cbSampleRate.setDisable(isCD);
            cbChannels.setDisable(isCD);
        });

        txtRelativePath.visibleProperty().bind(cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录 (Source)"));

        chkOverwrite = new CheckBox("强制覆盖已存在的目标文件");
        chkOverwrite.setSelected(false);
        chkOverwrite.setTooltip(new Tooltip("如果不勾选，遇到已存在的目标文件将跳过处理。"));

        chkForceFilenameMeta = new CheckBox("忽略原Tag，强制用文件名重构元数据");
        chkForceFilenameMeta.setTooltip(new Tooltip("勾选此项可解决严重的乱码问题，完全丢弃源文件信息，仅依据文件名和目录名生成Tag。"));
        chkForceFilenameMeta.setSelected(false);

        spFfmpegThreads = new Spinner<>(1, 16, 4);
        spFfmpegThreads.setEditable(true);

        txtFFmpegPath = new TextField("ffmpeg");
        txtFFmpegPath.setPromptText("Path to ffmpeg executable");

        chkEnableCache = new CheckBox("启用 SSD 缓存暂存 (解决IO瓶颈)");
        txtCacheDir = new TextField();
        txtCacheDir.setPromptText("SSD 缓存目录路径");
    }

    private void updateDefaultPathPrompt(String format) {
        if (format == null) return;
        String cleanFormat = format.split(" ")[0].toUpperCase();
        txtRelativePath.setPromptText("默认: Converted - " + cleanFormat);
    }

    @Override public String getName() { return "音频格式转换 (Pro)"; }
    @Override public String getDescription() { return "高品质音频转换。支持参数微调、乱码修复及智能覆盖检测。"; }
    @Override public ScanTarget getTargetType() { return ScanTarget.FILES_ONLY; }

    @Override
    public Node getConfigNode() {
        VBox rootBox = new VBox(15);
        rootBox.setPadding(new Insets(5, 0, 5, 0));

        // 1. 输出设置
        GridPane outputGrid = new GridPane();
        outputGrid.setHgap(10); outputGrid.setVgap(10);
        outputGrid.add(new Label("目标格式:"), 0, 0); outputGrid.add(cbTargetFormat, 1, 0);
        outputGrid.add(new Label("输出模式:"), 0, 1); outputGrid.add(cbOutputDirMode, 1, 1); outputGrid.add(txtRelativePath, 2, 1);

        ColumnConstraints colLabel = new ColumnConstraints(); colLabel.setMinWidth(70);
        ColumnConstraints colMid = new ColumnConstraints(); colMid.setPrefWidth(160);
        ColumnConstraints colGrow = new ColumnConstraints(); colGrow.setHgrow(Priority.ALWAYS);
        outputGrid.getColumnConstraints().addAll(colLabel, colMid, colGrow);

        // 2. 音频参数 (FlowPane)
        VBox audioParamsBox = new VBox(5);
        audioParamsBox.getChildren().add(new Label("音频参数详解:"));
        FlowPane paramsFlow = new FlowPane();
        paramsFlow.setHgap(20); paramsFlow.setVgap(10);
        paramsFlow.getChildren().addAll(
                createParamPair("FFmpeg线程:", spFfmpegThreads),
                createParamPair("采样率(Hz):", cbSampleRate),
                createParamPair("声道数:", cbChannels)
        );
        audioParamsBox.getChildren().add(paramsFlow);

        // 3. 处理选项
        VBox optionsBox = new VBox(8);
        optionsBox.getChildren().addAll(chkOverwrite, chkForceFilenameMeta, chkEnableCache);

        // 4. 路径与缓存
        GridPane pathGrid = new GridPane();
        pathGrid.setHgap(10); pathGrid.setVgap(10);
        pathGrid.getColumnConstraints().addAll(colLabel, colGrow, new ColumnConstraints());

        Label lblCache = new Label("缓存目录:");
        lblCache.disableProperty().bind(chkEnableCache.selectedProperty().not());
        txtCacheDir.disableProperty().bind(chkEnableCache.selectedProperty().not());
        JFXButton btnPickCache = new JFXButton("选择");
        btnPickCache.disableProperty().bind(chkEnableCache.selectedProperty().not());
        btnPickCache.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtCacheDir.setText(f.getAbsolutePath());
        });
        pathGrid.add(lblCache, 0, 0); pathGrid.add(txtCacheDir, 1, 0); pathGrid.add(btnPickCache, 2, 0);

        JFXButton btnPickFFmpeg = new JFXButton("浏览");
        btnPickFFmpeg.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if(f!=null) txtFFmpegPath.setText(f.getAbsolutePath());
        });
        pathGrid.add(new Label("FFmpeg:"), 0, 1); pathGrid.add(txtFFmpegPath, 1, 1); pathGrid.add(btnPickFFmpeg, 2, 1);

        rootBox.getChildren().addAll(new Label("输出设置:"), outputGrid, new Separator(), audioParamsBox, new Separator(), new Label("处理选项:"), optionsBox, pathGrid);
        return rootBox;
    }

    private HBox createParamPair(String labelText, Node control) {
        HBox hb = new HBox(5); hb.setAlignment(Pos.CENTER_LEFT); hb.getChildren().addAll(new Label(labelText), control); return hb;
    }

    @Override
    public void captureParams() {
        pFormat = cbTargetFormat.getValue();
        pMode = cbOutputDirMode.getValue();
        pRelPath = txtRelativePath.getText();
        pOverwrite = chkOverwrite.isSelected();
        pFFmpeg = txtFFmpegPath.getText();
        pUseCache = chkEnableCache.isSelected();
        pCacheDir = txtCacheDir.getText();
        pForceMeta = chkForceFilenameMeta.isSelected();
        pInnerThreads = spFfmpegThreads.getValue();
        pSampleRate = cbSampleRate.getValue();
        pChannels = cbChannels.getValue();
    }

    @Override
    public void saveConfig(Properties props) {
        if (cbTargetFormat.getValue() != null) props.setProperty("ac_format", cbTargetFormat.getValue());
        if (cbOutputDirMode.getValue() != null) props.setProperty("ac_outMode", cbOutputDirMode.getValue());
        if (txtRelativePath.getText() != null) props.setProperty("ac_relPath", txtRelativePath.getText());
        if (txtFFmpegPath.getText() != null) props.setProperty("ac_ffmpeg", txtFFmpegPath.getText());
        props.setProperty("ac_useCache", String.valueOf(chkEnableCache.isSelected()));
        if (txtCacheDir.getText() != null) props.setProperty("ac_cacheDir", txtCacheDir.getText());
        props.setProperty("ac_forceMeta", String.valueOf(chkForceFilenameMeta.isSelected()));
        props.setProperty("ac_overwrite", String.valueOf(chkOverwrite.isSelected()));
        props.setProperty("ac_innerThreads", String.valueOf(spFfmpegThreads.getValue()));
        props.setProperty("ac_sampleRate", cbSampleRate.getValue());
        props.setProperty("ac_channels", cbChannels.getValue());
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("ac_format")) cbTargetFormat.getSelectionModel().select(props.getProperty("ac_format"));
        if (props.containsKey("ac_outMode")) cbOutputDirMode.getSelectionModel().select(props.getProperty("ac_outMode"));
        if (props.containsKey("ac_relPath")) txtRelativePath.setText(props.getProperty("ac_relPath"));
        if (props.containsKey("ac_ffmpeg")) txtFFmpegPath.setText(props.getProperty("ac_ffmpeg"));
        if (props.containsKey("ac_useCache")) chkEnableCache.setSelected(Boolean.parseBoolean(props.getProperty("ac_useCache")));
        if (props.containsKey("ac_cacheDir")) txtCacheDir.setText(props.getProperty("ac_cacheDir"));
        if (props.containsKey("ac_forceMeta")) chkForceFilenameMeta.setSelected(Boolean.parseBoolean(props.getProperty("ac_forceMeta")));
        if (props.containsKey("ac_overwrite")) chkOverwrite.setSelected(Boolean.parseBoolean(props.getProperty("ac_overwrite")));
        if (props.containsKey("ac_innerThreads")) { try { spFfmpegThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("ac_innerThreads"))); } catch (Exception e) {} }
        if (props.containsKey("ac_sampleRate")) cbSampleRate.getSelectionModel().select(props.getProperty("ac_sampleRate"));
        if (props.containsKey("ac_channels")) cbChannels.getSelectionModel().select(props.getProperty("ac_channels"));
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.CONVERT) return;

        File source = rec.getFileHandle();
        File target = new File(rec.getNewPath());
        File finalTarget = target;
        File stagingFile = null;

        if (!Boolean.parseBoolean(rec.getExtraParams().get("overwrite")) && target.exists()) {
            return;
        }

        if (rec.getExtraParams().containsKey("stagingPath")) {
            stagingFile = new File(rec.getExtraParams().get("stagingPath"));
            if (!stagingFile.getParentFile().exists()) stagingFile.getParentFile().mkdirs();
            target = stagingFile;
        } else {
            if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
        }

        convertAudioFile(source, target, rec.getExtraParams());

        if (stagingFile != null && stagingFile.exists()) {
            if (!finalTarget.getParentFile().exists()) finalTarget.getParentFile().mkdirs();
            Files.move(stagingFile.toPath(), finalTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void convertAudioFile(File source, File target, Map<String, String> params) throws IOException {
        String ffmpegPath = params.getOrDefault("ffmpegPath", "ffmpeg");
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        boolean forceMeta = "true".equals(params.get("forceMeta"));

        try {
            if (forceMeta) {
                runFFmpegJob(ffmpeg, source, target, params, false);
            } else {
                runFFmpegJob(ffmpeg, source, target, params, true);
            }
        } catch (IOException e) {
            if (!forceMeta) {
                try {
                    if (target.exists()) target.delete();
                    runFFmpegJob(ffmpeg, source, target, params, false);
                } catch (IOException retryEx) {
                    throw new IOException("重试失败: " + retryEx.getMessage(), retryEx);
                }
            } else {
                throw e;
            }
        }
    }

    private void runFFmpegJob(FFmpeg ffmpeg, File source, File target, Map<String, String> params, boolean mapMetadata) throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .overrideOutputFiles(true);

        if (params.containsKey("innerThreads")) {
            builder.addExtraArgs("-threads", params.get("innerThreads"));
        }

        FFmpegOutputBuilder outputBuilder = builder.addOutput(target.getAbsolutePath())
                .setFormat(params.getOrDefault("format", "flac"))
                .setAudioCodec(params.getOrDefault("codec", "flac"));

        outputBuilder.addExtraArgs("-map", "0:a:0");

        if (mapMetadata) {
            outputBuilder.addExtraArgs("-map_metadata", "0");
            if (target.getName().toLowerCase().endsWith(".mp3")) {
                outputBuilder.addExtraArgs("-id3v2_version", "3");
            }
        } else {
            outputBuilder.addExtraArgs("-map_metadata", "-1");
            MetadataHelper.AudioMeta meta = MetadataHelper.getSmartMetadata(source, true);
            if (!meta.getTitle().isEmpty()) outputBuilder.addMetaTag("title", meta.getTitle());
            if (!meta.getArtist().isEmpty()) outputBuilder.addMetaTag("artist", meta.getArtist());
            if (!meta.getAlbum().isEmpty()) outputBuilder.addMetaTag("album", meta.getAlbum());
            if (!meta.getYear().isEmpty()) outputBuilder.addMetaTag("date", meta.getYear());
            if (!meta.getTrack().isEmpty()) outputBuilder.addMetaTag("track", meta.getTrack());
            outputBuilder.addMetaTag("comment", "Processed by Echo Music Manager");
        }

        // 统一参数读取：params 中的 key 已经是 sampleRate 和 channels
        if (params.containsKey("sampleRate")) {
            try {
                outputBuilder.setAudioSampleRate(Integer.parseInt(params.get("sampleRate")));
            } catch (NumberFormatException ignored) {}
        }

        if (params.containsKey("channels")) {
            try {
                outputBuilder.setAudioChannels(Integer.parseInt(params.get("channels")));
            } catch (NumberFormatException ignored) {}
        }

        new FFmpegExecutor(ffmpeg).createJob(builder).run();
    }

    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        String selectedFormat = pFormat;
        final String[] mode = {pMode};
        String relPath = pRelPath;
        boolean overwrite = pOverwrite;
        String ffmpeg = pFFmpeg;
        boolean useCache = pUseCache;
        String cacheDir = pCacheDir;
        boolean forceMeta = pForceMeta;

        // 捕获 UI 参数到本地变量
        int innerThreads = pInnerThreads;
        String uiSampleRate = pSampleRate;
        String uiChannels = pChannels;

        // [核心优化] 默认目录名逻辑
        if (relPath == null || relPath.trim().isEmpty()) {
            String cleanFormat = selectedFormat != null ? selectedFormat.split(" ")[0].toUpperCase() : "UNK";
            relPath = "Converted - " + cleanFormat;
        }
        final String finalRelPath = relPath;

        if (useCache && (cacheDir == null || cacheDir.trim().isEmpty() || !new File(cacheDir).isDirectory())) {
            useCache = false;
        }

        // [核心优化] 确定最终参数：如果是 CD 模式，强制覆盖 UI 选项
        final boolean isCdMode = "WAV (CD标准)".equals(selectedFormat);
        final String extension = isCdMode ? "wav" : (selectedFormat != null ? selectedFormat.toLowerCase() : "flac");

        // 预处理音频参数字符串
        final String targetSampleRate;
        final String targetChannels;

        if (isCdMode) {
            targetSampleRate = "44100";
            targetChannels = "2";
        } else {
            // 非 CD 模式：从 UI 读取
            targetSampleRate = (uiSampleRate != null && !uiSampleRate.contains("Original")) ? uiSampleRate.split(" ")[0] : null;

            String ch = null;
            if (uiChannels != null && !uiChannels.contains("Original")) {
                if (uiChannels.startsWith("1")) ch = "1";
                else if (uiChannels.startsWith("2")) ch = "2";
                else if (uiChannels.startsWith("6")) ch = "6";
            }
            targetChannels = ch;
        }

        Set<String> sourceExts = new HashSet<>(Arrays.asList("dsf", "dff", "dts", "ape", "wav", "flac", "m4a", "iso", "dfd", "tak", "tta", "wv", "mp3", "aac", "ogg", "wma"));
        int total = inputRecords.size();
        AtomicInteger processed = new AtomicInteger(0);

        boolean finalUseCache = useCache;
        return inputRecords.parallelStream()
                .map(rec -> {
                    File virtualInput = new File(rec.getNewPath());
                    if (!checkConditions(virtualInput)) return rec;

                    String name = virtualInput.getName().toLowerCase();
                    int dotIndex = name.lastIndexOf(".");
                    if (dotIndex == -1) return rec;
                    String fileExt = name.substring(dotIndex + 1);

                    if (!sourceExts.contains(fileExt)) return rec;

                    int curr = processed.incrementAndGet();
                    if (progressReporter != null && curr % 50 == 0) {
                        double p = (double) curr / total;
                        Platform.runLater(() -> progressReporter.accept(p, "分析音频: " + curr + "/" + total));
                    }

                    String newName = name.substring(0, dotIndex) + "." + extension;
                    File parent = virtualInput.getParentFile();
                    File targetFile;

                    if (mode[0] == null) mode[0] = "原目录";
                    if (mode[0].startsWith("原目录")) {
                        targetFile = new File(parent, newName);
                    } else if (mode[0].startsWith("子目录")) {
                        targetFile = new File(new File(parent, finalRelPath), newName);
                    } else if (mode[0].startsWith("同级目录")) {
                        targetFile = new File(new File(parent.getParentFile(), finalRelPath), newName);
                    } else {
                        targetFile = new File(new File(parent, finalRelPath), newName);
                    }

                    ExecStatus status = ExecStatus.PENDING;
                    boolean targetExists = targetFile.exists();
                    if (targetExists && !overwrite) {
                        status = ExecStatus.SKIPPED;
                    }

                    Map<String, String> params = new HashMap<>();
                    params.put("format", extension);
                    params.put("ffmpegPath", ffmpeg);
                    params.put("forceMeta", String.valueOf(forceMeta));
                    params.put("overwrite", String.valueOf(overwrite));

                    params.put("innerThreads", String.valueOf(innerThreads));

                    // 放入计算好的最终音频参数
                    if (targetSampleRate != null) params.put("sampleRate", targetSampleRate);
                    if (targetChannels != null) params.put("channels", targetChannels);

                    if ("mp3".equals(extension)) params.put("codec", "libmp3lame");
                    else if ("flac".equals(extension)) params.put("codec", "flac");
                    else if ("wav".equals(extension)) params.put("codec", "pcm_s24le"); // 默认24bit，如果是CD模式会被后续覆盖吗？
                    // 注意：wav 格式下 codec 默认值问题。
                    // 如果是 CD 模式，runFFmpegJob 会被 isCdMode 逻辑控制吗？
                    // 现在的 runFFmpegJob 逻辑是通用的，它只看 params。
                    // 我们需要在 analyze 阶段把 CD 模式的特殊 codec 也写死。

                    if (isCdMode) {
                        params.put("codec", "pcm_s16le");
                    } else if ("wav".equals(extension)) {
                        params.put("codec", "pcm_s24le");
                    } else if ("alac".equals(extension)) {
                        params.put("codec", "alac");
                    } else if ("aac".equals(extension)) {
                        params.put("codec", "aac");
                    }

                    if (finalUseCache && status != ExecStatus.SKIPPED) {
                        String tempFileName = UUID.randomUUID().toString() + "_" + newName;
                        File stagingFile = new File(cacheDir, tempFileName);
                        params.put("stagingPath", stagingFile.getAbsolutePath());
                    }

                    return new ChangeRecord(rec.getOriginalName(), targetFile.getName(), rec.getFileHandle(), true, targetFile.getAbsolutePath(), OperationType.CONVERT, params, status);
                })
                .collect(Collectors.toList());
    }
}