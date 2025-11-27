package backup.plusV1.plugins;

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
import backup.plusV1.OldAppStrategy;
import backup.plusV1.model.ChangeRecord;
import backup.plusV1.type.ExecStatus;
import backup.plusV1.type.OperationType;
import backup.plusV1.type.ScanTarget;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CueSplitterStrategy extends OldAppStrategy {
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
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
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

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
        return Collections.emptyList();
    }

    private CueInfo parseCue(File file) {
        CueInfo info = new CueInfo();
        try {
            // 修复 MalformedInputException: 手动探测编码
            List<String> lines;
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            try {
                // 优先尝试 UTF-8
                lines = Arrays.asList(new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8).split("\\r?\\n"));
            } catch (Exception e) {
                // 失败则回退到 GBK (中文环境常见)
                lines = Arrays.asList(new String(fileBytes, java.nio.charset.Charset.forName("GBK")).split("\\r?\\n"));
            }

            CueTrack currentTrack = null;

            // 优化正则：支持灵活的空格和 INDEX 格式
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
                        // 通常 INDEX 01 是音轨开始，INDEX 00 是 Pre-gap
                        if (indexType == 1) {
                            int mm = Integer.parseInt(m.group(2));
                            int ss = Integer.parseInt(m.group(3));
                            int ff = Integer.parseInt(m.group(4));
                            // CUE 时间格式：分:秒:帧 (1秒=75帧)
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

    // --- Robust CUE Parser ---
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