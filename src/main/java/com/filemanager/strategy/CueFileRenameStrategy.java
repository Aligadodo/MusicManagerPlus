package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * demo
 */
public class CueFileRenameStrategy extends AppStrategy {
    private final TextField txtTemplate;
    private String pTemplate;

    public CueFileRenameStrategy() {
        txtTemplate = new TextField("%artist% - %year% - %album%");
        txtTemplate.setPromptText("例如: %year% %album% 或 %artist%/[%year%] %album%");
    }

    @Override
    public String getName() {
        return "专辑目录标准化";
    }

    @Override
    public void captureParams() {
        pTemplate = txtTemplate.getText();
    }

    @Override
    public String getDescription() {
        return "为了解决cue文件在部分软件下，由于中文命名导致的无法加载的问题，支持统一调整cue及对应的音频文件命名。";
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
        VBox box = new VBox(10);
        box.getChildren().addAll(createStyledLabel("目录命名模板:"), txtTemplate, createStyledLabel("变量: %artist%, %album%, %year%"));
        return box;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() == OperationType.ALBUM_RENAME) {
            File s = rec.getFileHandle();
            File t = new File(rec.getNewPath());
            if (s.equals(t)) return;
            if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
            Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        Map<String, List<ChangeRecord>> dirGroups = inputRecords.stream()
                .collect(Collectors.groupingBy(rec -> new File(rec.getNewPath()).getParent()));

        List<ChangeRecord> results = new ArrayList<>();
        int total = dirGroups.size();
        AtomicInteger processed = new AtomicInteger(0);

        for (Map.Entry<String, List<ChangeRecord>> entry : dirGroups.entrySet()) {
            String parentPath = entry.getKey();
            List<ChangeRecord> dirFiles = entry.getValue();
            File parentDir = new File(parentPath);

            Map<String, Integer> artists = new HashMap<>();
            Map<String, Integer> albums = new HashMap<>();
            Map<String, Integer> years = new HashMap<>();

            for (ChangeRecord rec : dirFiles) {
                MetadataHelper.AudioMeta meta = MetadataHelper.getSmartMetadata(rec.getFileHandle(), false);
                artists.merge(meta.getArtist(), 1, Integer::sum);
                albums.merge(meta.getAlbum(), 1, Integer::sum);
                if (!meta.getYear().isEmpty()) years.merge(meta.getYear(), 1, Integer::sum);
                results.add(rec);
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
                File targetDir = new File(parentDir.getParentFile(), newDirName);
                ChangeRecord folderRec = new ChangeRecord(parentDir.getName(), newDirName, parentDir, true, targetDir.getAbsolutePath(), OperationType.ALBUM_RENAME, new HashMap<>(), ExecStatus.PENDING);
                results.add(folderRec);
            }

            int curr = processed.incrementAndGet();
            if (progressReporter != null && curr % 10 == 0)
                Platform.runLater(() -> progressReporter.accept((double) curr / total, "分析目录: " + parentDir.getName()));
        }
        return results;
    }

    private String getTopKey(Map<String, Integer> map, String def) {
        return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(def);
    }
}