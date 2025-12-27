package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.CueSheet;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.file.CueParserUtil;
import javafx.scene.Node;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.*;

/**
 * CUE 分轨策略 (专业版)
 * 功能：解析 CUE 文件，智能定位音频源，基于时间戳调用 FFmpeg 精确切割，并写入元数据。
 */
public class CueSplitterStrategy extends AbstractFfmpegStrategy {
    public CueSplitterStrategy() {
        super();
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
        }
        return tracks;
    }

}