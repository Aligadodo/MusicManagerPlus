package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import javafx.application.Platform;
import javafx.scene.Node;

import java.io.File;
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
public class AudioConverterStrategy extends AbstractFfmpegStrategy {

    public AudioConverterStrategy() {
        super();
    }
    @Override
    public String getDefaultDirPrefix() {
        return "Convert";
    }
    @Override
    public String getName() {
        return "音频格式转换";
    }

    @Override
    public String getDescription() {
        return "高品质音频转换。支持参数微调、乱码修复及智能覆盖检测等。";
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
        if (rec.getOpType() != OperationType.CONVERT) return;
        super.execute(rec);
    }

    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        Set<String> sourceExts = new HashSet<>(Arrays.asList("dsf", "dff", "dts", "ape", "wav", "flac", "m4a", "iso", "dfd", "tak", "tta", "wv", "mp3", "aac", "ogg", "wma"));
        int total = inputRecords.size();
        AtomicInteger processed = new AtomicInteger(0);
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
                    Map<String, String> param = getParams(virtualInput.getParentFile(), name);
                    String newName = name.substring(0, dotIndex) + "." + param.get("format");
                    File targetFile = new File(param.get("parentPath"), newName);
                    ExecStatus status = ExecStatus.PENDING;
                    boolean targetExists = targetFile.exists();
                    if (targetExists && !pOverwrite) {
                        return rec;
                    }
                    return new ChangeRecord(rec.getOriginalName(), targetFile.getName(), rec.getFileHandle(), true, targetFile.getAbsolutePath(), OperationType.CONVERT, param, status);
                })
                .collect(Collectors.toList());
    }
}