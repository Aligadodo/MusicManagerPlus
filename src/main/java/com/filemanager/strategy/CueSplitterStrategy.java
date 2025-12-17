package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.CueSheet;
import com.filemanager.tool.file.CueParser;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CUE 分轨策略 (专业版)
 * 功能：解析 CUE 文件，智能定位音频源，基于时间戳调用 FFmpeg 精确切割，并写入元数据。
 */
public class CueSplitterStrategy extends AppStrategy {

    // --- UI Components ---
    private final JFXComboBox<String> cbTargetFormat;
    private final JFXComboBox<String> cbOutputDirMode;
    private final TextField txtRelativePath;
    private final TextField txtFFmpegPath;
    private final CheckBox chkIgnorePregap;

    // --- Runtime Params ---
    private String pFormat;
    private String pMode;
    private String pRelPath;
    private String pFFmpeg;
    private boolean pIgnorePregap;

    public CueSplitterStrategy() {
        cbTargetFormat = new JFXComboBox<>(FXCollections.observableArrayList("FLAC", "WAV", "MP3", "ALAC", "AAC"));
        cbTargetFormat.getSelectionModel().select("FLAC");
        cbTargetFormat.setTooltip(new Tooltip("分轨后的输出格式"));

        cbOutputDirMode = new JFXComboBox<>(FXCollections.observableArrayList("原目录 (Source)", "子目录 (Sub-folder)", "自定义相对路径"));
        cbOutputDirMode.getSelectionModel().select(1); // 默认子目录

        txtRelativePath = new TextField("Split");
        txtRelativePath.setPromptText("文件夹名称");
        txtRelativePath.visibleProperty().bind(cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录 (Source)"));

        txtFFmpegPath = new TextField("ffmpeg");
        txtFFmpegPath.setPromptText("FFmpeg 可执行文件路径");

        chkIgnorePregap = new CheckBox("忽略索引 00 (Pregap)");
        chkIgnorePregap.setSelected(true);
        chkIgnorePregap.setTooltip(new Tooltip("通常 INDEX 00 是静音或上一轨的残留，勾选后从 INDEX 01 开始切割"));
    }

    @Override
    public String getName() {
        return "CUE 整轨自动切割 (CUE Splitter)";
    }

    @Override
    public String getDescription() {
        return "解析 .cue 索引文件，将整轨音频无损切割为单曲。支持预览详细的歌曲清单与时长信息。";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("目标格式:"), 0, 0);
        grid.add(cbTargetFormat, 1, 0);
        grid.add(new Label("输出位置:"), 0, 1);
        HBox outBox = new HBox(5, cbOutputDirMode, txtRelativePath);
        HBox.setHgrow(cbOutputDirMode, Priority.ALWAYS);
        HBox.setHgrow(txtRelativePath, Priority.ALWAYS);
        grid.add(outBox, 1, 1);

        HBox ffBox = new HBox(10, txtFFmpegPath);
        HBox.setHgrow(txtFFmpegPath, Priority.ALWAYS);
        JFXButton btnPick = new JFXButton("浏览");
        btnPick.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) txtFFmpegPath.setText(f.getAbsolutePath());
        });
        ffBox.getChildren().add(btnPick);
        grid.add(new Label("FFmpeg:"), 0, 3);
        grid.add(ffBox, 1, 3);

        box.getChildren().addAll(grid, new Separator(), chkIgnorePregap);
        return box;
    }

    @Override
    public void captureParams() {
        pFormat = cbTargetFormat.getValue();
        pMode = cbOutputDirMode.getValue();
        pRelPath = txtRelativePath.getText();
        pFFmpeg = txtFFmpegPath.getText();
        pIgnorePregap = chkIgnorePregap.isSelected();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("cue_format", cbTargetFormat.getValue());
        props.setProperty("cue_outMode", cbOutputDirMode.getValue());
        props.setProperty("cue_ffmpeg", txtFFmpegPath.getText());
        props.setProperty("cue_pregap", String.valueOf(chkIgnorePregap.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("cue_format")) cbTargetFormat.getSelectionModel().select(props.getProperty("cue_format"));
        if (props.containsKey("cue_outMode"))
            cbOutputDirMode.getSelectionModel().select(props.getProperty("cue_outMode"));
        if (props.containsKey("cue_ffmpeg")) txtFFmpegPath.setText(props.getProperty("cue_ffmpeg"));
        if (props.containsKey("cue_pregap"))
            chkIgnorePregap.setSelected(Boolean.parseBoolean(props.getProperty("cue_pregap")));
    }

    // --- 核心逻辑：分析 ---
    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        List<ChangeRecord> result = new ArrayList<>();

        // 1. 筛选 CUE 文件
        List<ChangeRecord> cueRecords = inputRecords.stream()
                .filter(r -> r.getFileHandle().getName().toLowerCase().endsWith(".cue"))
                .collect(Collectors.toList());

        // 非 CUE 文件原样传递
        List<ChangeRecord> others = inputRecords.stream()
                .filter(r -> !r.getFileHandle().getName().toLowerCase().endsWith(".cue"))
                .collect(Collectors.toList());
        result.addAll(others);

        if (cueRecords.isEmpty()) return result;

        String ext = pFormat.toLowerCase();
        AtomicInteger processed = new AtomicInteger(0);
        int total = cueRecords.size();

        // 2. 解析 CUE 并生成分轨任务
        List<ChangeRecord> splitTasks = cueRecords.parallelStream().flatMap(cueRec -> {
            File cueFile = cueRec.getFileHandle();

            // 进度通知
            int c = processed.incrementAndGet();
            if (progressReporter != null)
                Platform.runLater(() -> progressReporter.accept((double) c / total, "解析 CUE: " + cueFile.getName()));

            // 解析
            CueSheet cueSheet = CueParser.parse(cueFile.toPath());
            if (cueSheet == null || cueSheet.tracks.isEmpty()) return Stream.empty();

            // 定位音频源文件
            File sourceAudio = locateAudioFile(cueFile, cueSheet.fileName);
            if (sourceAudio == null) return Stream.empty();

            // 确定输出目录
            File outputDir;
            if (pMode.startsWith("原目录")) outputDir = cueFile.getParentFile();
            else if (pMode.startsWith("子目录"))
                outputDir = new File(cueFile.getParentFile(), pRelPath.isEmpty() ? "Split" : pRelPath);
            else outputDir = new File(cueFile.getParentFile(), pRelPath);

            List<ChangeRecord> tracks = new ArrayList<>();
            List<CueSheet.CueTrack> cueTracks = cueSheet.tracks;

            for (int i = 0; i < cueTracks.size(); i++) {
                CueSheet.CueTrack t = cueTracks.get(i);

                // 计算起止时间
                long startTime = t.startTimeMs;

                long duration = 0L;
                if (i < cueTracks.size() - 1) {
                    CueSheet.CueTrack next = cueTracks.get(i + 1);
                    duration = next.startTimeMs - startTime;
                }

                // 构建文件名
                String trackTitle = t.title.isEmpty() ? "Unknown" : t.title;
                String safeTitle = trackTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
                String trackName = String.format("%02d - %s.%s", t.number, safeTitle, ext);
                File targetFile = new File(outputDir, trackName);

                // 继承全局信息
                String artist = t.performer;
                if (artist == null || artist.isEmpty()) artist = cueSheet.albumPerformer;
                String album = cueSheet.albumTitle;

                // 格式化时长用于展示 (MM:SS)
                String durationStr = "??:??";
                if (duration != 0L) {
                    int totalSec = Math.round(duration / 1000);
                    durationStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60);
                }

                // [核心优化] 丰富预览信息：[01] 歌名 - 歌手 [04:20]
                String displayInfo = String.format("[%02d] %s - %s [%s]", t.number, trackTitle, artist, durationStr);

                Map<String, String> params = new HashMap<>();
                params.put("source", sourceAudio.getAbsolutePath());
                // 存入双精度秒数
                params.put("start", String.format(Locale.US, "%d", startTime));
                if (duration != 0) params.put("duration", String.format(Locale.US, "%d", duration));

                params.put("meta_title", trackTitle);
                params.put("meta_artist", artist);
                params.put("meta_album", album);
                params.put("meta_track", String.valueOf(t.number));

                params.put("format", ext);
                params.put("ffmpeg", pFFmpeg);
                // Codec mapping
                if ("mp3".equals(ext)) params.put("codec", "libmp3lame");
                else if ("aac".equals(ext)) params.put("codec", "aac");
                else if ("flac".equals(ext)) params.put("codec", "flac");
                else if ("alac".equals(ext)) params.put("codec", "alac");
                else params.put("codec", "pcm_s16le");

                ChangeRecord trackRec = new ChangeRecord(
                        displayInfo, // 使用富信息作为源展示
                        trackName,
                        cueFile,
                        true,
                        targetFile.getAbsolutePath(),
                        OperationType.SPLIT,
                        params,
                        ExecStatus.PENDING
                );
                tracks.add(trackRec);
            }
            return tracks.stream();
        }).collect(Collectors.toList());

        result.addAll(splitTasks);
        return result;
    }

    // --- 核心逻辑：执行 ---
    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.SPLIT) return;

        File target = new File(rec.getNewPath());
        if (!target.getParentFile().exists()) target.getParentFile().mkdirs();

        Map<String, String> p = rec.getExtraParams();
        String ffmpegPath = p.getOrDefault("ffmpeg", "ffmpeg");

        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        FFmpegBuilder builder = new FFmpegBuilder();

        // 输入
        builder.setInput(p.get("source"));

        // [修复] 时间转换：秒(String) -> 毫秒(long)
        long startMillis = Long.parseLong(p.get("start"));

        // 输出配置
        FFmpegOutputBuilder out = builder.addOutput(target.getAbsolutePath())
                .setStartOffset(startMillis, TimeUnit.MILLISECONDS)
                .setFormat(p.get("format"))
                .setAudioCodec(p.get("codec"))
                .addExtraArgs("-map_metadata", "-1");

        if (p.containsKey("duration")) {
            long durationMillis = Long.parseLong(p.get("duration"));
            out.setDuration(durationMillis, TimeUnit.MILLISECONDS);
        }

        // 写入元数据
        if (p.containsKey("meta_title")) out.addMetaTag("title", p.get("meta_title"));
        if (p.containsKey("meta_artist")) out.addMetaTag("artist", p.get("meta_artist"));
        if (p.containsKey("meta_album")) out.addMetaTag("album", p.get("meta_album"));
        if (p.containsKey("meta_track")) out.addMetaTag("track", p.get("meta_track"));
        if (p.containsKey("meta_genre")) out.addMetaTag("genre", p.get("meta_genre"));
        if (p.containsKey("meta_date")) out.addMetaTag("date", p.get("meta_date"));

        new FFmpegExecutor(ffmpeg).createJob(builder).run();
    }

    // ==================== CUE 解析与辅助 ====================

    private File locateAudioFile(File cueFile, String declaredName) {
        File dir = cueFile.getParentFile();
        if (declaredName != null && !declaredName.isEmpty()) {
            File f = new File(dir, declaredName);
            if (f.exists()) return f;
        }

        String baseName = cueFile.getName();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) baseName = baseName.substring(0, dot);

        String[] exts = {".flac", ".wav", ".ape", ".m4a", ".dsf", ".dff", ".tak", ".tta", ".wv"};
        for (String ext : exts) {
            File f = new File(dir, baseName + ext);
            if (f.exists()) return f;
            if (declaredName != null) {
                int d2 = declaredName.lastIndexOf('.');
                if (d2 > 0) {
                    File f2 = new File(dir, declaredName.substring(0, d2) + ext);
                    if (f2.exists()) return f2;
                }
            }
        }
        return null;
    }
}