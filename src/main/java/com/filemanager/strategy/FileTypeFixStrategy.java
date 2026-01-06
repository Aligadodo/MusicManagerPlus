package com.filemanager.strategy;

import cn.hutool.core.map.MapUtil;
import com.filemanager.base.IAppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.tool.file.AudioTypeInspector;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 文件类型修复工具
 *
 * @author 28667
 */
public class FileTypeFixStrategy extends IAppStrategy {
    private final CheckBox isForce;
    private boolean pForce;

    public FileTypeFixStrategy() {
        isForce = new CheckBox("通过读取文件来识别文件类型（准确率更高但会变慢）");
    }

    @Override
    public String getName() {
        return "音频文件类型修复工具";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public void captureParams() {
        pForce = isForce.isSelected();
    }

    @Override
    public String getDescription() {
        return "一些网上下载的音频文件类型和实际类型不符，可以通过该工具智能进行修复。" +
                "底层使用的Apache Tika是目前 Java 生态中最成熟、准确度最高的文件类型检测方案，它基于文件内容的“魔数”（Magic Bytes）来判断类型，完全不依赖文件名。 ";
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("is_force", String.valueOf(isForce.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("is_force")) {
            isForce.setSelected(Boolean.parseBoolean(props.getProperty("is_force")));
        }
    }

    @Override
    public Node getConfigNode() {
        return StyleFactory.createVBoxPanel(
                StyleFactory.createHBox(isForce));
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        // 复用重命名节点的实现即可，无需重复实现
        File s = rec.getFileHandle();
        File t = new File(rec.getNewPath());
        if (s.equals(t)) {
            return;
        }
        if (!t.getParentFile().exists()) {
            t.getParentFile().mkdirs();
        }
        Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        AudioTypeInspector.FileTypeCheckResult checkResult = null;
        if (pForce) {
            checkResult = AudioTypeInspector.inspectHard(rec.getFileHandle());
        } else {
            checkResult = AudioTypeInspector.inspect(rec.getFileHandle());
        }
        if (!checkResult.success) {
            logError(checkResult.message);
            return Collections.emptyList();
        }
        if (!checkResult.needsFix) {
            return Collections.emptyList();
        }
        String filename = rec.getFileHandle().getName();
        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        File newFile = new File(rec.getFileHandle().getParent(), nameWithoutExt + checkResult.suggestedExtension);
        rec.setChanged(true);
        rec.setOpType(OperationType.FIX_TYPE);
        rec.setNewPath(newFile.getAbsolutePath());
        rec.setNewName(newFile.getName());
        rec.setExtraParams(MapUtil.of("来源", "文件类型修复"));
        return Collections.emptyList();
    }
}