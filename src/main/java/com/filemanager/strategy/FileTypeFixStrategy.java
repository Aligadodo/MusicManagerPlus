package com.filemanager.strategy;

import cn.hutool.core.map.MapUtil;
import com.filemanager.base.IAppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.tool.file.AudioTypeInspector;
import com.filemanager.tool.file.FileTypeUtil;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 文件类型修复工具
 *
 * @author 28667
 */
public class FileTypeFixStrategy extends IAppStrategy {
    private final CheckBox fixSoundOnly;
    private boolean pSoundOnly;

    public FileTypeFixStrategy() {
        fixSoundOnly = new CheckBox("只处理音频文件修复");
        fixSoundOnly.setSelected(true);
    }

    @Override
    public String getName() {
        return "文件类型修复工具";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public void captureParams() {
        pSoundOnly = fixSoundOnly.isSelected();
    }

    @Override
    public String getDescription() {
        return "一些网上下载的音频文件类型和实际类型不符，因此通过该工具智能进行修复。" +
                "本工具默认只处理音频文件的修复，其他类型的文件理论也支持。" +
                "底层使用的Apache Tika是目前 Java 生态中最成熟、准确度最高的文件类型检测方案，它基于文件内容的“魔数”（Magic Bytes）来判断类型，完全不依赖文件名。 ";
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("sound_only", String.valueOf(fixSoundOnly.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("sound_only")) {
            fixSoundOnly.setSelected(Boolean.parseBoolean(props.getProperty("fms_clean")));
        }
    }

    @Override
    public Node getConfigNode() {
        return StyleFactory.createVBoxPanel(
                StyleFactory.createHBox(fixSoundOnly));
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        // 复用重命名节点的实现即可，无需重复实现
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        if (pSoundOnly && !FileTypeUtil.isMusicFile(rec.getFileHandle())) {
            return Collections.emptyList();
        }
        AudioTypeInspector.FileTypeCheckResult checkResult = AudioTypeInspector.inspect(rec.getFileHandle());
        if (!checkResult.needsFix) {
            return Collections.emptyList();
        }
        String filename = rec.getFileHandle().getName();
        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        File newFile = new File(rec.getFileHandle().getParent(), nameWithoutExt + checkResult.suggestedExtension);
        rec.setChanged(true);
        rec.setOpType(OperationType.RENAME);
        rec.setNewPath(newFile.getAbsolutePath());
        rec.setExtraParams(MapUtil.of("来源", "文件类型修复"));
        return Collections.emptyList();
    }
}