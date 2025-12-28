package com.filemanager.strategy;

import com.filemanager.base.IAppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.file.FileTypeUtil;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
import javafx.scene.Node;
import javafx.scene.control.TextField;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * demo
 */
public class AlbumDirNormalizeStrategy extends IAppStrategy {
    private final TextField txtTemplate;
    private String pTemplate;

    public AlbumDirNormalizeStrategy() {
        txtTemplate = new TextField("%artist% - %year% - %album%");
        txtTemplate.setPromptText("例如: %year% %album% 或 %artist%/[%year%] %album%");
    }

    @Override
    public String getName() {
        return "专辑目录命名标准化(施工中)";
    }

    @Override
    public void captureParams() {
        pTemplate = txtTemplate.getText();
    }

    @Override
    public String getDescription() {
        return "按照特定的格式对目录名称进行标准化，如：%artist% - %year% - %album%";
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("adn_template", txtTemplate.getText());
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("adn_template")) txtTemplate.setText(props.getProperty("adn_template"));
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public Node getConfigNode() {
        return StyleFactory.createVBoxPanel(
                StyleFactory.createParamPairLine("目录命名模板:", txtTemplate)
        );
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() == OperationType.ALBUM_RENAME) {
            File s = rec.getFileHandle();
            File t = new File(rec.getNewPath());
            if (s.equals(t)) return;
            if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
            Files.move(s.toPath(), t.toPath());
        }
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord changeRecord, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        if (changeRecord.getFileHandle().isFile()) {
            return Collections.emptyList();
        }
        String parentPath = changeRecord.getFileHandle().getParentFile().getAbsolutePath();
        List<ChangeRecord> dirFiles = getFilesUnderDir(changeRecord.getFileHandle(), inputRecords).stream()
                .filter(file -> FileTypeUtil.isMusicFile(file.getFileHandle())).collect(Collectors.toList());
        File parentDir = new File(parentPath);

        Map<String, Integer> artists = new HashMap<>();
        Map<String, Integer> albums = new HashMap<>();
        Map<String, Integer> years = new HashMap<>();

        for (ChangeRecord rec : dirFiles) {
            MetadataHelper.AudioMeta meta = MetadataHelper.getSmartMetadata(rec.getFileHandle(), false);
            artists.merge(meta.getArtist(), 1, Integer::sum);
            albums.merge(meta.getAlbum(), 1, Integer::sum);
            if (!meta.getYear().isEmpty()) {
                years.merge(meta.getYear(), 1, Integer::sum);
            }
        }

        String bestArtist = getTopKey(artists, "Unknown Artist");
        String bestAlbum = getTopKey(albums, parentDir.getName());
        String bestYear = getTopKey(years, "");

        MetadataHelper.AudioMeta consensus = new MetadataHelper.AudioMeta();
        consensus.setArtist(bestArtist);
        consensus.setAlbum(bestAlbum);
        consensus.setYear(bestYear);

        String newDirName = MetadataHelper.format(pTemplate, consensus).replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (newDirName.endsWith(" - ")) newDirName = newDirName.substring(0, newDirName.length() - 3);

        if (!parentDir.getName().equals(newDirName)) {
            changeRecord.setChanged(true);
            changeRecord.setOpType(OperationType.ALBUM_RENAME);
            changeRecord.setNewPath(newDirName);
            changeRecord.setStatus(ExecStatus.PENDING);
        }
        return Collections.emptyList();
    }

    private String getTopKey(Map<String, Integer> map, String def) {
        return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(def);
    }
}