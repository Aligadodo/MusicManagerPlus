package plusv2.plugins;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.Data;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import plusv2.AppStrategyV2;
import plusv2.model.ChangeRecord;
import plusv2.type.ExecStatus;
import plusv2.type.OperationType;
import plusv2.type.ScanTarget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CueSplitterStrategy extends AppStrategyV2 {
    // ... existing fields and constructor ...
    private final JFXComboBox<String> cbTargetFormat;
    private final JFXComboBox<String> cbOutputDirMode;
    private final TextField txtRelativePath;
    private final Spinner<Integer> spThreads;
    private final TextField txtFFmpegPath;

    // Cached Params
    private String pFormat;
    private String pMode;
    private String pRelPath;
    private int pThreads;
    private String pFFmpeg;

    public CueSplitterStrategy() {
        cbTargetFormat = new JFXComboBox<>(FXCollections.observableArrayList("FLAC", "WAV", "MP3", "ALAC"));
        cbTargetFormat.getSelectionModel().select("FLAC");

        cbOutputDirMode = new JFXComboBox<>(FXCollections.observableArrayList("原目录 (Source)", "子目录 (Sub-folder)", "自定义相对路径"));
        cbOutputDirMode.getSelectionModel().select(1); // 默认子目录，比较整洁

        txtRelativePath = new TextField("Split");
        txtRelativePath.visibleProperty().bind(cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录 (Source)"));

        spThreads = new Spinner<>(1, 32, 2); // 分轨比较耗IO，线程不宜过多
        txtFFmpegPath = new TextField("ffmpeg");
    }

    @Override
    public String getName() {
        return "CUE 分轨 (Splitter)";
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
    public void captureParams() {
        pFormat = cbTargetFormat.getValue();
        pMode = cbOutputDirMode.getValue();
        pRelPath = txtRelativePath.getText();
        pThreads = spThreads.getValue();
        pFFmpeg = txtFFmpegPath.getText();
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
        box.getChildren().addAll(new Label("目标格式:"), cbTargetFormat, new Label("输出位置:"), new HBox(10, cbOutputDirMode, txtRelativePath), new Separator(), new HBox(15, new Label("并发线程:"), spThreads), ffmpegBox);
        return box;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.SPLIT) return;

        File target = new File(rec.getNewPath());
        if (!target.getParentFile().exists()) target.getParentFile().mkdirs();

        Map<String, String> params = rec.getExtraParams();
        String audioSourcePath = params.get("audioSource");
        File audioSource = new File(audioSourcePath);
        if (!audioSource.exists()) throw new FileNotFoundException("源音频文件不存在: " + audioSourcePath);

        String startTime = params.get("startTime");
        String duration = params.get("duration");
        String ffmpegPath = params.getOrDefault("ffmpegPath", "ffmpeg");

        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        FFmpegBuilder builder = new FFmpegBuilder();
        builder.setInput(audioSourcePath);

        FFmpegOutputBuilder outputBuilder = builder.addOutput(rec.getNewPath())
                .setStartOffset(Integer.parseInt(startTime), TimeUnit.SECONDS)
                .setFormat(params.getOrDefault("format", "flac"))
                .setAudioCodec(params.getOrDefault("codec", "flac"))
                .addExtraArgs("-map_metadata", "-1");

        if (duration != null) {
            outputBuilder.setDuration(Integer.parseInt(duration), TimeUnit.SECONDS);
        }

        if (params.containsKey("title")) outputBuilder.addMetaTag("title", params.get("title"));
        if (params.containsKey("artist")) outputBuilder.addMetaTag("artist", params.get("artist"));
        if (params.containsKey("album")) outputBuilder.addMetaTag("album", params.get("album"));
        if (params.containsKey("track")) outputBuilder.addMetaTag("track", params.get("track"));

        new FFmpegExecutor(ffmpeg).createJob(builder).run();
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        // ... existing analyze implementation ...
        List<ChangeRecord> records = new ArrayList<>();
        String extension = pFormat.toLowerCase();
        String mode = pMode;
        String relPath = pRelPath;
        String ffmpeg = pFFmpeg;

        // 1. 筛选 .cue 文件
        List<File> cueFiles = files.stream().filter(f -> f.getName().toLowerCase().endsWith(".cue")).collect(Collectors.toList());

        int total = cueFiles.size();
        AtomicInteger processed = new AtomicInteger(0);

        // 2. 并行处理每个 CUE
        return cueFiles.parallelStream().flatMap(cueFile -> {
            List<ChangeRecord> trackRecords = new ArrayList<>();
            try {
                // 进度
                int curr = processed.incrementAndGet();
                if (progressReporter != null)
                    Platform.runLater(() -> progressReporter.accept((double) curr / total, "解析 CUE: " + cueFile.getName()));

                // 解析 CUE (修复编码异常)
                CueInfo cueInfo = parseCue(cueFile);
                if (cueInfo == null || cueInfo.tracks.isEmpty()) return Stream.empty();

                // 确认音频文件存在
                File sourceAudio = null;
                if (cueInfo.audioFilename != null) {
                    sourceAudio = new File(cueFile.getParentFile(), cueInfo.audioFilename);
                    if (!sourceAudio.exists()) sourceAudio = null;
                }

                // 如果指定文件不存在，尝试找同名不同后缀的
                if (sourceAudio == null) {
                    String baseName = cueFile.getName().substring(0, cueFile.getName().lastIndexOf("."));
                    for (String ext : new String[]{".wav", ".flac", ".ape", ".dff", ".dsf", ".tak", ".tta"}) {
                        File alt = new File(cueFile.getParentFile(), baseName + ext);
                        if (alt.exists()) {
                            sourceAudio = alt;
                            break;
                        }
                    }
                }

                // 仍然找不到音频文件，跳过
                if (sourceAudio == null) return Stream.empty();

                File outputDir;
                if (mode.startsWith("原目录")) outputDir = cueFile.getParentFile();
                else if (mode.startsWith("子目录"))
                    outputDir = new File(cueFile.getParentFile(), relPath.isEmpty() ? "Split" : relPath);
                else outputDir = new File(cueFile.getParentFile(), relPath);

                // 为每个 Track 生成 Record
                for (int i = 0; i < cueInfo.tracks.size(); i++) {
                    CueTrack track = cueInfo.tracks.get(i);
                    // 计算时长：下一轨开始时间 - 当前轨开始时间
                    Double duration = null;
                    if (i < cueInfo.tracks.size() - 1) {
                        duration = cueInfo.tracks.get(i + 1).startTime - track.startTime;
                    }

                    // 文件名格式：01 - Title.flac
                    String safeTitle = track.title.replaceAll("[\\\\/:*?\"<>|]", "_");
                    String trackName = String.format("%02d - %s.%s", track.number, safeTitle, extension);
                    File targetFile = new File(outputDir, trackName);

                    Map<String, String> params = new HashMap<>();
                    params.put("audioSource", sourceAudio.getAbsolutePath());
                    // 使用 Locale.US 确保小数点格式正确
                    params.put("startTime", String.format(Locale.US, "%.4f", track.startTime));
                    if (duration != null) params.put("duration", String.format(Locale.US, "%.4f", duration));
                    params.put("title", track.title);
                    params.put("artist", track.performer != null ? track.performer : cueInfo.albumPerformer);
                    params.put("album", cueInfo.albumTitle);
                    params.put("track", String.valueOf(track.number));
                    params.put("format", extension);
                    params.put("ffmpegPath", ffmpeg);

                    if ("mp3".equals(extension)) params.put("codec", "libmp3lame");
                    else if ("flac".equals(extension)) params.put("codec", "flac");
                    else params.put("codec", "pcm_s16le"); // wav default

                    ChangeRecord rec = new ChangeRecord("分轨: " + track.title, trackName, cueFile, true, targetFile.getAbsolutePath(), OperationType.SPLIT, params, ExecStatus.PENDING);
                    trackRecords.add(rec);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return trackRecords.stream();
        }).collect(Collectors.toList());
    }

    private CueInfo parseCue(File file) {
        CueInfo info = new CueInfo();
        try {
            List<String> lines;
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            try {
                lines = Arrays.asList(new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8).split("\\r?\\n"));
            } catch (Exception e) {
                lines = Arrays.asList(new String(fileBytes, java.nio.charset.Charset.forName("GBK")).split("\\r?\\n"));
            }

            CueTrack currentTrack = null;
            Pattern pFile = Pattern.compile("FILE\\s+\"(.*)\"");
            Pattern pTrack = Pattern.compile("TRACK\\s+(\\d+)\\s+AUDIO");
            Pattern pTitle = Pattern.compile("TITLE\\s+\"(.*)\"");
            Pattern pPerf = Pattern.compile("PERFORMER\\s+\"(.*)\"");
            Pattern pIndex = Pattern.compile("INDEX\\s+(\\d+)\\s+(\\d+):(\\d+):(\\d+)");

            for (String line : lines) {
                line = line.trim();
                Matcher m;

                if ((m = pFile.matcher(line)).find()) {
                    info.audioFilename = m.group(1);
                } else if ((m = pTitle.matcher(line)).find()) {
                    if (currentTrack != null) currentTrack.title = m.group(1);
                    else info.albumTitle = m.group(1);
                } else if ((m = pPerf.matcher(line)).find()) {
                    if (currentTrack != null) currentTrack.performer = m.group(1);
                    else info.albumPerformer = m.group(1);
                } else if ((m = pTrack.matcher(line)).find()) {
                    currentTrack = new CueTrack();
                    currentTrack.number = Integer.parseInt(m.group(1));
                    info.tracks.add(currentTrack);
                } else if ((m = pIndex.matcher(line)).find()) {
                    if (currentTrack != null) {
                        int indexType = Integer.parseInt(m.group(1));
                        if (indexType == 1) {
                            int mm = Integer.parseInt(m.group(2));
                            int ss = Integer.parseInt(m.group(3));
                            int ff = Integer.parseInt(m.group(4));
                            currentTrack.startTime = mm * 60 + ss + (ff / 75.0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return info;
    }

    private void runFFmpegJob(FFmpeg ffmpeg, File source, File target, Map<String, String> params, boolean mapMetadata) throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder().setInput(source.getAbsolutePath()).overrideOutputFiles(true);
        if (source.getName().toLowerCase().endsWith(".ape")) builder.setFormat("ape");

        FFmpegOutputBuilder outputBuilder = builder.addOutput(target.getAbsolutePath())
                .setFormat(params.getOrDefault("format", "flac"))
                .setAudioCodec(params.getOrDefault("codec", "flac"));

        // [修复] 将 -map 0:a:0 移至 outputBuilder 的 extraArgs
        outputBuilder.addExtraArgs("-map", "0:a:0");

        if (mapMetadata) outputBuilder.addExtraArgs("-map_metadata", "0");
        if (params.containsKey("sample_rate"))
            outputBuilder.setAudioSampleRate(Integer.parseInt(params.get("sample_rate")));
        if (params.containsKey("channels")) outputBuilder.setAudioChannels(Integer.parseInt(params.get("channels")));

        new FFmpegExecutor(ffmpeg).createJob(builder).run();
    }

    // ... CueInfo, CueTrack, parseCue ...
    @Data
    class CueInfo {
        String albumPerformer = "Unknown";
        String albumTitle = "Unknown";
        String audioFilename;
        List<CueTrack> tracks = new ArrayList<>();
    }

    @Data
    class CueTrack {
        int number;
        String title = "Unknown";
        String performer;
        double startTime;
    }
}