package com.filemanager.strategy;

import com.filemanager.model.*;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final Spinner<Integer> spThreads;
    private final TextField txtFFmpegPath;
    private final CheckBox chkIgnorePregap;

    // --- Runtime Params ---
    private String pFormat;
    private String pMode;
    private String pRelPath;
    private int pThreads;
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

        spThreads = new Spinner<>(1, 32, 2);
        spThreads.setTooltip(new Tooltip("并行分轨任务数（建议2-4，视磁盘IO性能而定）"));

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
    public int getPreferredThreadCount() {
        return spThreads.getValue();
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

        grid.add(new Label("并发任务:"), 0, 2);
        grid.add(spThreads, 1, 2);

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
        pThreads = spThreads.getValue();
        pFFmpeg = txtFFmpegPath.getText();
        pIgnorePregap = chkIgnorePregap.isSelected();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("cue_format", cbTargetFormat.getValue());
        props.setProperty("cue_outMode", cbOutputDirMode.getValue());
        props.setProperty("cue_threads", String.valueOf(spThreads.getValue()));
        props.setProperty("cue_ffmpeg", txtFFmpegPath.getText());
        props.setProperty("cue_pregap", String.valueOf(chkIgnorePregap.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("cue_format")) cbTargetFormat.getSelectionModel().select(props.getProperty("cue_format"));
        if (props.containsKey("cue_outMode"))
            cbOutputDirMode.getSelectionModel().select(props.getProperty("cue_outMode"));
        if (props.containsKey("cue_ffmpeg")) txtFFmpegPath.setText(props.getProperty("cue_ffmpeg"));
        if (props.containsKey("cue_threads")) {
            try {
                spThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("cue_threads")));
            } catch (Exception e) {
            }
        }
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
            CueSheet cueSheet = parseCueSheet(cueFile);
            if (cueSheet == null || cueSheet.getTracks().isEmpty()) return Stream.empty();

            // 定位音频源文件
            File sourceAudio = locateAudioFile(cueFile, cueSheet.getAudioFilename());
            if (sourceAudio == null) return Stream.empty();

            // 确定输出目录
            File outputDir;
            if (pMode.startsWith("原目录")) outputDir = cueFile.getParentFile();
            else if (pMode.startsWith("子目录"))
                outputDir = new File(cueFile.getParentFile(), pRelPath.isEmpty() ? "Split" : pRelPath);
            else outputDir = new File(cueFile.getParentFile(), pRelPath);

            List<ChangeRecord> tracks = new ArrayList<>();
            List<CueTrack> cueTracks = cueSheet.getTracks();

            for (int i = 0; i < cueTracks.size(); i++) {
                CueTrack t = cueTracks.get(i);

                // 计算起止时间
                double startTime = t.getIndex01();
                if (!pIgnorePregap && t.getIndex00() >= 0 && t.getIndex00() < t.getIndex01()) {
                    startTime = t.getIndex00();
                }

                Double duration = null;
                if (i < cueTracks.size() - 1) {
                    CueTrack next = cueTracks.get(i + 1);
                    double nextStart = next.getIndex01();
                    if (!pIgnorePregap && next.getIndex00() >= 0) {
                        nextStart = next.getIndex01();
                    }
                    duration = nextStart - startTime;
                }

                // 构建文件名
                String trackTitle = t.getTitle().isEmpty() ? "Unknown" : t.getTitle();
                String safeTitle = trackTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
                String trackName = String.format("%02d - %s.%s", t.getNumber(), safeTitle, ext);
                File targetFile = new File(outputDir, trackName);

                // 继承全局信息
                String artist = t.getPerformer();
                if (artist == null || artist.isEmpty()) artist = cueSheet.getPerformer();
                String album = cueSheet.getTitle();

                // 格式化时长用于展示 (MM:SS)
                String durationStr = "??:??";
                if (duration != null) {
                    int totalSec = (int) Math.round(duration);
                    durationStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60);
                }

                // [核心优化] 丰富预览信息：[01] 歌名 - 歌手 [04:20]
                String displayInfo = String.format("[%02d] %s - %s [%s]", t.getNumber(), trackTitle, artist, durationStr);

                Map<String, String> params = new HashMap<>();
                params.put("source", sourceAudio.getAbsolutePath());
                // 存入双精度秒数
                params.put("start", String.format(Locale.US, "%.4f", startTime));
                if (duration != null) params.put("duration", String.format(Locale.US, "%.4f", duration));

                params.put("meta_title", trackTitle);
                params.put("meta_artist", artist);
                params.put("meta_album", album);
                params.put("meta_track", String.valueOf(t.getNumber()));
                if (cueSheet.getGenre() != null) params.put("meta_genre", cueSheet.getGenre());
                if (cueSheet.getDate() != null) params.put("meta_date", cueSheet.getDate());

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
        double startSeconds = Double.parseDouble(p.get("start"));
        long startMillis = (long) (startSeconds * 1000);

        // 输出配置
        FFmpegOutputBuilder out = builder.addOutput(target.getAbsolutePath())
                .setStartOffset(startMillis, TimeUnit.MILLISECONDS)
                .setFormat(p.get("format"))
                .setAudioCodec(p.get("codec"))
                .addExtraArgs("-map_metadata", "-1");

        if (p.containsKey("duration")) {
            double durationSeconds = Double.parseDouble(p.get("duration"));
            long durationMillis = (long) (durationSeconds * 1000);
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

    private CueSheet parseCueSheet(File file) {
        CueSheet sheet = new CueSheet();
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String content;
            try {
                content = new String(bytes, StandardCharsets.UTF_8);
                if (content.contains("") && !isUtf8(bytes)) {
                    throw new Exception("Not UTF-8");
                }
            } catch (Exception e) {
                content = new String(bytes, Charset.forName("GBK"));
            }

            String[] lines = content.split("\\r?\\n");
            CueTrack currentTrack = null;

            Pattern pRem = Pattern.compile("^\\s*REM\\s+(\\w+)\\s+(.*)", Pattern.CASE_INSENSITIVE);
            Pattern pPerf = Pattern.compile("^\\s*PERFORMER\\s+\"(.*)\"", Pattern.CASE_INSENSITIVE);
            Pattern pTitle = Pattern.compile("^\\s*TITLE\\s+\"(.*)\"", Pattern.CASE_INSENSITIVE);
            Pattern pFile = Pattern.compile("^\\s*FILE\\s+\"(.*)\"", Pattern.CASE_INSENSITIVE);
            Pattern pTrack = Pattern.compile("^\\s*TRACK\\s+(\\d+)\\s+AUDIO", Pattern.CASE_INSENSITIVE);
            Pattern pIndex = Pattern.compile("^\\s*INDEX\\s+(\\d+)\\s+(\\d+):(\\d+):(\\d+)", Pattern.CASE_INSENSITIVE);

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                Matcher m;

                if ((m = pTrack.matcher(line)).find()) {
                    currentTrack = new CueTrack();
                    currentTrack.setNumber(Integer.parseInt(m.group(1)));
                    sheet.getTracks().add(currentTrack);
                } else if ((m = pIndex.matcher(line)).find()) {
                    if (currentTrack != null) {
                        int idx = Integer.parseInt(m.group(1));
                        int mm = Integer.parseInt(m.group(2));
                        int ss = Integer.parseInt(m.group(3));
                        int ff = Integer.parseInt(m.group(4));
                        // MM:SS:FF (1 sec = 75 frames)
                        double seconds = mm * 60 + ss + (ff / 75.0);
                        if (idx == 1) currentTrack.setIndex01(seconds);
                        else if (idx == 0) currentTrack.setIndex00(seconds);
                    }
                } else if ((m = pTitle.matcher(line)).find()) {
                    String t = m.group(1);
                    if (currentTrack != null) currentTrack.setTitle(t);
                    else sheet.setTitle(t);
                } else if ((m = pPerf.matcher(line)).find()) {
                    String p = m.group(1);
                    if (currentTrack != null) currentTrack.setPerformer(p);
                    else sheet.setPerformer(p);
                } else if ((m = pFile.matcher(line)).find()) {
                    sheet.setAudioFilename(m.group(1));
                } else if ((m = pRem.matcher(line)).find()) {
                    String key = m.group(1).toUpperCase();
                    String val = m.group(2).replace("\"", "").trim();
                    if ("DATE".equals(key)) sheet.setDate(val);
                    if ("GENRE".equals(key)) sheet.setGenre(val);
                }
            }
        } catch (Exception e) {
            System.err.println("CUE Parse Error: " + e.getMessage());
            return null;
        }
        return sheet;
    }

    private boolean isUtf8(byte[] raw) {
        return raw.length >= 3 && (raw[0] & 0xFF) == 0xEF && (raw[1] & 0xFF) == 0xBB && (raw[2] & 0xFF) == 0xBF;
    }

    @Data
    @NoArgsConstructor
    private static class CueSheet {
        private String title = "";
        private String performer = "Unknown Artist";
        private String audioFilename;
        private String date;
        private String genre;
        private List<CueTrack> tracks = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    private static class CueTrack {
        private int number;
        private String title = "";
        private String performer;
        private double index00 = -1;
        private double index01 = 0;
    }
}