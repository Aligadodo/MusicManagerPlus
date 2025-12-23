package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.google.common.collect.Lists;
import javafx.scene.Node;

import java.io.File;
import java.util.*;

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
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        Set<String> sourceExts = new HashSet<>(Arrays.asList("dsf", "dff", "dts", "ape", "wav", "flac",
                "m4a", "iso", "dfd", "tak", "tta", "wv", "mp3", "aac", "ogg", "wma"));
        File virtualInput = new File(rec.getNewPath());
        String name = virtualInput.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex == -1) {
            return Collections.emptyList();
        }
        String fileExt = name.substring(dotIndex + 1);
        if (!sourceExts.contains(fileExt)) {
            return Collections.emptyList();
        }
        Map<String, String> param = getParams(virtualInput.getParentFile(), name);
        String newName = name.substring(0, dotIndex) + "." + param.get("format");
        File targetFile = new File(param.get("parentPath"), newName);
        ExecStatus status = ExecStatus.PENDING;
        boolean targetExists = targetFile.exists();
        if (targetExists && !pOverwrite) {
            return Collections.emptyList();
        }
        if (param.containsKey("doubleCheckParentPath")) {
            File doubleCheckTargetFile = new File(param.get("doubleCheckParentPath"), newName);
            if (doubleCheckTargetFile.exists() && !pOverwrite) {
                return Collections.emptyList();
            }
        }
        return Lists.newArrayList(new ChangeRecord(rec.getOriginalName(), targetFile.getName(),
                rec.getFileHandle(), true, targetFile.getAbsolutePath(), OperationType.CONVERT, param, status));
    }
}