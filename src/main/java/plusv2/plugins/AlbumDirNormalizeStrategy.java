package plusv2.plugins;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import plusv2.AppStrategyV2;
import plusv2.model.AudioMeta;
import plusv2.model.ChangeRecord;
import plusv2.type.OperationType;
import plusv2.type.ScanTarget;
import plusv2.util.MetadataHelper;

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
public class AlbumDirNormalizeStrategy extends AppStrategyV2 {
    private final TextField txtTemplate;
    private String pTemplate;

    public AlbumDirNormalizeStrategy() {
        txtTemplate = new TextField("%artist% - %year% - %album%");
        txtTemplate.setPromptText("例如: %year% %album% 或 %artist%/[%year%] %album%");
    }

    @Override public String getName() { return "专辑目录标准化 (整理文件夹)"; }
    @Override public ScanTarget getTargetType() { return ScanTarget.FILES_ONLY; } // Scan files to determine folder info
    @Override public void captureParams() { pTemplate = txtTemplate.getText(); }

    @Override public Node getConfigNode() {
        VBox box = new VBox(10);
        box.getChildren().addAll(new Label("目录命名模板:"), txtTemplate, new Label("可用变量: %artist%, %album%, %year%"));
        return box;
    }

    @Override public void saveConfig(Properties props) { props.setProperty("adn_template", txtTemplate.getText()); }
    @Override public void loadConfig(Properties props) { if(props.containsKey("adn_template")) txtTemplate.setText(props.getProperty("adn_template")); }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        List<ChangeRecord> records = new ArrayList<>();
        String template = pTemplate;
        // Group files by parent directory
        Map<File, List<File>> dirGroups = files.stream().collect(Collectors.groupingBy(File::getParentFile));

        int total = dirGroups.size();
        AtomicInteger processed = new AtomicInteger(0);

        for (Map.Entry<File, List<File>> entry : dirGroups.entrySet()) {
            File dir = entry.getKey();
            List<File> dirFiles = entry.getValue();

            if (rootDirs.contains(dir)) continue; // Don't rename root source dirs

            // Calculate "Consensus" Metadata
            Map<String, Integer> artists = new HashMap<>();
            Map<String, Integer> albums = new HashMap<>();
            Map<String, Integer> years = new HashMap<>();

            for (File f : dirFiles) {
                AudioMeta meta = MetadataHelper.getSmartMetadata(f, false);
                artists.merge(meta.artist, 1, Integer::sum);
                albums.merge(meta.album, 1, Integer::sum);
                if (!meta.year.isEmpty()) years.merge(meta.year, 1, Integer::sum);
            }

            AudioMeta consensus = new AudioMeta();
            consensus.artist = getTopKey(artists, "Unknown Artist");
            consensus.album = getTopKey(albums, dir.getName());
            consensus.year = getTopKey(years, "");

            // Generate new name
            String newName = MetadataHelper.format(template, consensus).replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            // Remove trailing " - " if year was empty
            if (newName.endsWith(" - ")) newName = newName.substring(0, newName.length() - 3);
            if (newName.contains(" -  - ")) newName = newName.replace(" -  - ", " - ");

            if (!dir.getName().equals(newName)) {
                File target = new File(dir.getParentFile(), newName);
                records.add(new ChangeRecord(dir.getName(), newName, dir, true, target.getAbsolutePath(), OperationType.RENAME));
            }

            if (progressReporter != null) {
                int curr = processed.incrementAndGet();
                if (curr % 10 == 0) Platform.runLater(() -> progressReporter.accept((double)curr/total, "分析目录: " + dir.getName()));
            }
        }
        return records;
    }

    private String getTopKey(Map<String, Integer> map, String def) {
        return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(def);
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File s = rec.getFileHandle(); File t = new File(rec.getNewPath());
        if(s.equals(t)) return;
        if(!t.getParentFile().exists()) t.getParentFile().mkdirs();
        Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}