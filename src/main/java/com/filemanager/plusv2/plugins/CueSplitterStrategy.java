package com.filemanager.plusv2.plugins;

import com.filemanager.plusv2.AppStrategy;
import com.filemanager.plusv2.model.ChangeRecord;
import com.filemanager.plusv2.type.ExecStatus;
import com.filemanager.plusv2.type.OperationType;
import com.filemanager.plusv2.type.ScanTarget;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
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

import java.io.File;
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

public class CueSplitterStrategy extends AppStrategy {
    private final JFXComboBox<String> cbTargetFormat;
    private final JFXComboBox<String> cbOutputDirMode;
    private final TextField txtRelativePath;
    private final Spinner<Integer> spThreads;
    private final TextField txtFFmpegPath;

    private String pFormat;
    private String pMode;
    private String pRelPath;
    private int pThreads;
    private String pFFmpeg;

    public CueSplitterStrategy() {
        cbTargetFormat = new JFXComboBox<>(FXCollections.observableArrayList("FLAC", "WAV", "MP3", "ALAC"));
        cbTargetFormat.getSelectionModel().select(0);
        cbOutputDirMode = new JFXComboBox<>(FXCollections.observableArrayList("原目录 (Source)", "子目录 (Sub-folder)"));
        cbOutputDirMode.getSelectionModel().select(1);
        txtRelativePath = new TextField("Split");
        txtRelativePath.visibleProperty().bind(cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录 (Source)"));
        spThreads = new Spinner<>(1, 32, 2);
        txtFFmpegPath = new TextField("ffmpeg");
    }

    @Override
    public String getName() {
        return "CUE 分轨";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
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
    public void saveConfig(Properties props) {
        props.setProperty("cue_format", cbTargetFormat.getValue());
        props.setProperty("cue_outMode", cbOutputDirMode.getValue());
        props.setProperty("cue_threads", String.valueOf(spThreads.getValue()));
        props.setProperty("cue_ffmpeg", txtFFmpegPath.getText());
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
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        HBox ff = new HBox(10, new Label("FFmpeg:"), txtFFmpegPath);
        JFXButton b = new JFXButton("...");
        b.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) txtFFmpegPath.setText(f.getAbsolutePath());
        });
        ff.getChildren().add(b);
        box.getChildren().addAll(new Label("目标格式:"), cbTargetFormat, new Label("输出:"), cbOutputDirMode, txtRelativePath, new Label("并发:"), spThreads, ff);
        return box;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.SPLIT) return;
        File target = new File(rec.getNewPath());
        if (!target.getParentFile().exists()) target.getParentFile().mkdirs();

        Map<String, String> p = rec.getExtraParams();
        FFmpeg ffmpeg = new FFmpeg(p.getOrDefault("ffmpegPath", "ffmpeg"));
        FFmpegBuilder builder = new FFmpegBuilder().setInput(p.get("audioSource"));

        FFmpegOutputBuilder out = builder.addOutput(target.getAbsolutePath())
                .setStartOffset(Long.parseLong(p.get("startTime")), TimeUnit.SECONDS)
                .setFormat(p.get("format"))
                .setAudioCodec(p.get("codec"))
                .addExtraArgs("-map", "0:a:0");

        if (p.containsKey("duration")) out.setDuration(Long.parseLong(p.get("duration")), TimeUnit.SECONDS);
        out.addMetaTag("title", p.get("title"));
        out.addMetaTag("artist", p.get("artist"));

        new FFmpegExecutor(ffmpeg).createJob(builder).run();
    }

    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        List<ChangeRecord> result = new ArrayList<>();
        List<ChangeRecord> cues = inputRecords.stream()
                .filter(r -> r.getFileHandle().getName().toLowerCase().endsWith(".cue"))
                .collect(Collectors.toList());
        List<ChangeRecord> nonCues = inputRecords.stream()
                .filter(r -> !r.getFileHandle().getName().toLowerCase().endsWith(".cue"))
                .collect(Collectors.toList());
        result.addAll(nonCues);

        AtomicInteger count = new AtomicInteger(0);
        int total = cues.size();

        List<ChangeRecord> tracks = cues.parallelStream().flatMap(cueRec -> {
            File cueFile = cueRec.getFileHandle();
            CueInfo info = parseCue(cueFile);
            if (info == null || info.tracks.isEmpty()) return Stream.empty();

            File audio = new File(cueFile.getParentFile(), info.audioFilename);
            if (!audio.exists()) {
                String base = cueFile.getName().substring(0, cueFile.getName().lastIndexOf("."));
                for (String ext : new String[]{".wav", ".flac", ".ape", ".dff"}) {
                    File t = new File(cueFile.getParentFile(), base + ext);
                    if (t.exists()) {
                        audio = t;
                        break;
                    }
                }
            }
            if (!audio.exists()) return Stream.empty();

            String ext = pFormat.toLowerCase();
            File outDir = "子目录".equals(pMode) ? new File(cueFile.getParentFile(), pRelPath.isEmpty() ? "Split" : pRelPath) : cueFile.getParentFile();

            List<ChangeRecord> ts = new ArrayList<>();
            for (int i = 0; i < info.tracks.size(); i++) {
                CueTrack t = info.tracks.get(i);
                double dur = (i < info.tracks.size() - 1) ? info.tracks.get(i + 1).startTime - t.startTime : 0;
                String name = String.format("%02d - %s.%s", t.number, t.title.replaceAll("[\\\\/:*?\"<>|]", "_"), ext);
                File target = new File(outDir, name);

                Map<String, String> pm = new HashMap<>();
                pm.put("audioSource", audio.getAbsolutePath());
                pm.put("startTime", String.format(Locale.US, "%.4f", t.startTime));
                if (dur > 0) pm.put("duration", String.format(Locale.US, "%.4f", dur));
                pm.put("title", t.title);
                pm.put("artist", t.performer != null ? t.performer : info.albumPerformer);
                pm.put("format", ext);
                pm.put("codec", "mp3".equals(ext) ? "libmp3lame" : "flac");
                pm.put("ffmpegPath", pFFmpeg);

                ts.add(new ChangeRecord("分轨: " + t.title, name, cueFile, true, target.getAbsolutePath(), OperationType.SPLIT, pm, ExecStatus.PENDING));
            }
            if (progressReporter != null) {
                int c = count.incrementAndGet();
                Platform.runLater(() -> progressReporter.accept((double) c / total, "解析 CUE..."));
            }
            return ts.stream();
        }).collect(Collectors.toList());

        result.addAll(tracks);
        return result;
    }

    private CueInfo parseCue(File file) {
        CueInfo info = new CueInfo();
        try {
            List<String> lines;
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            try {
                lines = Arrays.asList(new String(fileBytes, StandardCharsets.UTF_8).split("\\r?\\n"));
            } catch (Exception e) {
                lines = Arrays.asList(new String(fileBytes, Charset.forName("GBK")).split("\\r?\\n"));
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
                if ((m = pFile.matcher(line)).find()) info.audioFilename = m.group(1);
                else if ((m = pTitle.matcher(line)).find()) {
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

    @Data
    class CueInfo {
        String albumPerformer = "";
        String albumTitle = "";
        String audioFilename = "";
        List<CueTrack> tracks = new ArrayList<>();
    }

    @Data
    class CueTrack {
        int number;
        String title = "";
        String performer;
        double startTime;
    }
}