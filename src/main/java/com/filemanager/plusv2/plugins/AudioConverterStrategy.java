package com.filemanager.plusv2.plugins; // --- Strategies ---

import com.filemanager.plusv2.AppStrategy;
import com.filemanager.plusv2.model.ChangeRecord;
import com.filemanager.plusv2.type.ExecStatus;
import com.filemanager.plusv2.type.OperationType;
import com.filemanager.plusv2.type.ScanTarget;
import com.filemanager.plusv2.util.MetadataHelper;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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

public class AudioConverterStrategy extends AppStrategy {

    private final JFXComboBox<String> cbTargetFormat;
    private final JFXComboBox<String> cbOutputDirMode;
    private final TextField txtRelativePath;
    private final CheckBox chkSkipExisting;
    private final Spinner<Integer> spThreads;
    private final TextField txtFFmpegPath;
    private final CheckBox chkEnableCache;
    private final TextField txtCacheDir;
    private final CheckBox chkForceFilenameMeta;

    private String pFormat;
    private String pMode;
    private String pRelPath;
    private boolean pSkip;
    private String pFFmpeg;
    private boolean pUseCache;
    private String pCacheDir;
    private boolean pForceMeta;

    public AudioConverterStrategy() {
        cbTargetFormat = new JFXComboBox<>(FXCollections.observableArrayList("FLAC", "WAV", "WAV (CD标准)", "MP3", "ALAC", "AAC"));
        cbTargetFormat.getSelectionModel().select("FLAC");

        cbOutputDirMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "原目录 (Source)", "子目录 (Sub-folder)", "同级目录 (Sibling folder)", "自定义相对路径"
        ));
        cbOutputDirMode.getSelectionModel().select(0);

        txtRelativePath = new TextField("converted");
        txtRelativePath.visibleProperty().bind(cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录 (Source)"));

        chkSkipExisting = new CheckBox("如果目标文件存在则跳过");
        chkSkipExisting.setSelected(true);

        chkForceFilenameMeta = new CheckBox("忽略原文件 Tag，强制使用文件名重构元数据");

        spThreads = new Spinner<>(1, 64, 4);
        txtFFmpegPath = new TextField("ffmpeg");

        chkEnableCache = new CheckBox("启用 SSD 缓存");
        txtCacheDir = new TextField();
    }

    @Override
    public String getName() {
        return "音频格式转换 (高并发/CD修复)";
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        HBox ffmpegBox = new HBox(10, new Label("FFmpeg:"), txtFFmpegPath);
        ffmpegBox.setAlignment(Pos.CENTER_LEFT);
        JFXButton btnPick = new JFXButton("浏览");
        btnPick.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) txtFFmpegPath.setText(f.getAbsolutePath());
        });
        ffmpegBox.getChildren().add(btnPick);
        HBox.setHgrow(txtFFmpegPath, Priority.ALWAYS);

        HBox cacheBox = new HBox(10, txtCacheDir);
        JFXButton btnPickCache = new JFXButton("选择SSD目录");
        btnPickCache.disableProperty().bind(chkEnableCache.selectedProperty().not());
        btnPickCache.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtCacheDir.setText(f.getAbsolutePath());
        });
        cacheBox.getChildren().add(btnPickCache);

        box.getChildren().addAll(
                new Label("目标格式:"), cbTargetFormat,
                new Label("输出位置:"), new HBox(10, cbOutputDirMode, txtRelativePath),
                new Separator(),
                chkForceFilenameMeta,
                new HBox(15, new Label("并发线程:"), spThreads),
                chkEnableCache, cacheBox,
                new Separator(), chkSkipExisting, ffmpegBox
        );
        return box;
    }

    @Override
    public void captureParams() {
        pFormat = cbTargetFormat.getValue();
        pMode = cbOutputDirMode.getValue();
        pRelPath = txtRelativePath.getText();
        pSkip = chkSkipExisting.isSelected();
        pFFmpeg = txtFFmpegPath.getText();
        pUseCache = chkEnableCache.isSelected();
        pCacheDir = txtCacheDir.getText();
        pForceMeta = chkForceFilenameMeta.isSelected();
    }

    @Override
    public void saveConfig(Properties props) {
        if (cbTargetFormat.getValue() != null) props.setProperty("ac_format", cbTargetFormat.getValue());
        if (cbOutputDirMode.getValue() != null) props.setProperty("ac_outMode", cbOutputDirMode.getValue());
        if (txtFFmpegPath.getText() != null) props.setProperty("ac_ffmpeg", txtFFmpegPath.getText());
        props.setProperty("ac_useCache", String.valueOf(chkEnableCache.isSelected()));
        if (txtCacheDir.getText() != null) props.setProperty("ac_cacheDir", txtCacheDir.getText());
        props.setProperty("ac_threads", String.valueOf(spThreads.getValue()));
        props.setProperty("ac_forceMeta", String.valueOf(chkForceFilenameMeta.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("ac_format")) cbTargetFormat.getSelectionModel().select(props.getProperty("ac_format"));
        if (props.containsKey("ac_outMode"))
            cbOutputDirMode.getSelectionModel().select(props.getProperty("ac_outMode"));
        if (props.containsKey("ac_ffmpeg")) txtFFmpegPath.setText(props.getProperty("ac_ffmpeg"));
        if (props.containsKey("ac_useCache"))
            chkEnableCache.setSelected(Boolean.parseBoolean(props.getProperty("ac_useCache")));
        if (props.containsKey("ac_cacheDir")) txtCacheDir.setText(props.getProperty("ac_cacheDir"));
        if (props.containsKey("ac_threads")) {
            try {
                spThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("ac_threads")));
            } catch (Exception e) {
            }
        }
        if (props.containsKey("ac_forceMeta"))
            chkForceFilenameMeta.setSelected(Boolean.parseBoolean(props.getProperty("ac_forceMeta")));
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.CONVERT) return;

        File source = rec.getFileHandle();
        File target = new File(rec.getNewPath());
        File finalTarget = target;
        File stagingFile = null;

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
            runFFmpegJob(ffmpeg, source, target, params, !forceMeta);
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
            if (!meta.getTrack().isEmpty()) outputBuilder.addMetaTag("track", meta.getTrack());
            outputBuilder.addMetaTag("comment", "Echo Music Manager");
        }

        if (params.containsKey("sample_rate"))
            outputBuilder.setAudioSampleRate(Integer.parseInt(params.get("sample_rate")));
        if (params.containsKey("channels")) outputBuilder.setAudioChannels(Integer.parseInt(params.get("channels")));

        new FFmpegExecutor(ffmpeg).createJob(builder).run();
    }

    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        String selectedFormat = pFormat;
        final String[] mode = {pMode};
        String relPath = pRelPath;
        boolean skipExisting = pSkip;
        String ffmpeg = pFFmpeg;
        boolean useCache = false;
        String cacheDir = pCacheDir;
        boolean forceMeta = pForceMeta;

        useCache = pUseCache;

        String extension;
        boolean isCdMode;
        if ("WAV (CD标准)".equals(selectedFormat)) {
            extension = "wav";
            isCdMode = true;
        } else {
            isCdMode = false;
            extension = selectedFormat != null ? selectedFormat.toLowerCase() : "flac";
        }

        Set<String> sourceExts = new HashSet<>(Arrays.asList("dsf", "dff", "dts", "ape", "wav", "flac", "m4a", "iso", "dfd", "tak", "tta", "wv", "mp3", "aac", "ogg", "wma"));
        int total = inputRecords.size();
        AtomicInteger processed = new AtomicInteger(0);

        boolean finalUseCache = useCache;
        return inputRecords.parallelStream().map(rec -> {
            File virtualInput = new File(rec.getNewPath());
            if (!checkConditions(virtualInput)) return rec;

            String name = virtualInput.getName().toLowerCase();
            int dotIndex = name.lastIndexOf(".");
            if (dotIndex == -1) return rec;
            String fileExt = name.substring(dotIndex + 1);

            if (!sourceExts.contains(fileExt)) return rec;
            if (fileExt.equals(extension) && mode[0] != null && mode[0].startsWith("原目录") && !isCdMode && !forceMeta)
                return rec;

            int curr = processed.incrementAndGet();
            if (progressReporter != null && curr % 50 == 0) {
                double p = (double) curr / total;
                Platform.runLater(() -> progressReporter.accept(p, "分析音频: " + curr + "/" + total));
            }

            String newName = name.substring(0, dotIndex) + "." + extension;
            File parent = virtualInput.getParentFile();
            File targetFile;

            if (mode[0] == null) mode[0] = "原目录";
            if (mode[0].startsWith("原目录")) targetFile = new File(parent, newName);
            else if (mode[0].startsWith("子目录"))
                targetFile = new File(new File(parent, relPath == null || relPath.isEmpty() ? "converted" : relPath), newName);
            else if (mode[0].startsWith("同级目录"))
                targetFile = new File(new File(parent.getParentFile(), relPath == null || relPath.isEmpty() ? parent.getName() + "_" + extension : relPath), newName);
            else targetFile = new File(new File(parent, relPath), newName);

            ExecStatus status = ExecStatus.PENDING;
            if (skipExisting && targetFile.exists()) status = ExecStatus.SKIPPED;

            Map<String, String> params = new HashMap<>();
            params.put("format", extension);
            params.put("ffmpegPath", ffmpeg);
            params.put("forceMeta", String.valueOf(forceMeta));

            if (finalUseCache && status != ExecStatus.SKIPPED) {
                String tempFileName = UUID.randomUUID() + "_" + newName;
                File stagingFile = new File(cacheDir, tempFileName);
                params.put("stagingPath", stagingFile.getAbsolutePath());
            }

            if (isCdMode) {
                params.put("codec", "pcm_s16le");
                params.put("sample_rate", "44100");
                params.put("channels", "2");
            } else {
                if ("mp3".equals(extension)) params.put("codec", "libmp3lame");
                else if ("flac".equals(extension)) params.put("codec", "flac");
                else if ("wav".equals(extension)) params.put("codec", "pcm_s24le");
            }

            return new ChangeRecord(rec.getOriginalName(), targetFile.getName(), rec.getFileHandle(), true, targetFile.getAbsolutePath(), OperationType.CONVERT, params, status);
        }).collect(Collectors.toList());
    }
}