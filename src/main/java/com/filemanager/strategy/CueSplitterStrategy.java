package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.CueSheet;
import com.filemanager.util.file.CueParser;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import javafx.application.Platform;
import javafx.scene.Node;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CUE 分轨策略 (专业版)
 * 功能：解析 CUE 文件，智能定位音频源，基于时间戳调用 FFmpeg 精确切割，并写入元数据。
 */
public class CueSplitterStrategy extends AbstractFfmpegStrategy {
    public CueSplitterStrategy() {
        super();
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
        return super.getConfigNode();
    }

    @Override
    public void captureParams() {
        super.captureParams();
    }

    @Override
    public void saveConfig(Properties props) {
        super.saveConfig(props);
    }

    @Override
    public void loadConfig(Properties props) {
        super.loadConfig(props);
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.SPLIT) return;
        super.execute(rec);
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
            List<ChangeRecord> tracks = new ArrayList<>();
            List<CueSheet.CueTrack> cueTracks = cueSheet.tracks;

            Map<String, String> params = getParams(sourceAudio.getParentFile());
            try {
                for (int i = 0; i < cueTracks.size(); i++) {
                    CueSheet.CueTrack t = cueTracks.get(i);

                    // 计算起止时间
                    long startTime = t.soundStartTimeMs;

                    long duration = 0L;
                    if (i < cueTracks.size() - 1) {
                        CueSheet.CueTrack next = cueTracks.get(i + 1);
                        duration = next.soundStartTimeMs - startTime;
                    }
                    // 构建文件名
                    String trackTitle = t.title.isEmpty() ? "Unknown" : t.title;
                    String safeTitle = trackTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
                    String trackName = String.format("%02d - %s.%s", t.number, safeTitle, params.get("format"));

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
                    File targetFile = new File(params.get("parentPath"), trackName);
                    // 忽略已存在的文件
                    boolean targetExists = targetFile.exists();
                    if (targetExists && !pOverwrite) {
                        continue;
                    }
                    params.put("source", sourceAudio.getAbsolutePath());
                    // 存入双精度秒数
                    params.put("start", String.format(Locale.US, "%d", startTime));
                    if (duration != 0) {
                        params.put("duration", String.format(Locale.US, "%d", duration));
                    }
                    if (t.title != null) {
                        params.put("meta_title", t.title);
                        params.put("meta_artist", artist);
                        params.put("meta_album", album);
                        params.put("meta_track", String.valueOf(t.number));
                    }
                    ChangeRecord trackRec = new ChangeRecord(
                            displayInfo, // 使用富信息作为源展示
                            trackName,
                            sourceAudio,
                            true,
                            targetFile.getAbsolutePath(),
                            OperationType.SPLIT,
                            params,
                            ExecStatus.PENDING
                    );
                    tracks.add(trackRec);
                }
                return tracks.stream();
            } catch (Exception e) {
                log("Cue文件解析失败，跳过：: " + cueFile.toPath() + "，错误详情：" + ExceptionUtils.getStackTrace(e));
            }
            return Stream.empty();
        }).collect(Collectors.toList());

        result.addAll(splitTasks);
        return result;
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