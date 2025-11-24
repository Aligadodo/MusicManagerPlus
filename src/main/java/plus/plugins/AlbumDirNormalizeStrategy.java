package plus.plugins;

import javafx.scene.Node;
import javafx.scene.control.Label;
import plus.AppStrategy;
import plus.model.ChangeRecord;
import plus.type.ScanTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * demo 未实现
 */
public class AlbumDirNormalizeStrategy extends AppStrategy {
    @Override
    public String getName() {
        return "专辑目录标准化";
    }

    @Override
    public Node getConfigNode() {
        return new Label("自动识别底层文件夹内的歌曲信息并重命名文件夹。");
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
        List<ChangeRecord> records = new ArrayList<>();
        Map<File, List<File>> folderGroups = files.stream().collect(Collectors.groupingBy(File::getParentFile));

        for (Map.Entry<File, List<File>> entry : folderGroups.entrySet()) {
            File folder = entry.getKey();
            if (rootDirs.contains(folder)) continue;

            String artist = "Unknown";
            String album = folder.getName();
            String year = "XXXX";
            String type = "MP3";

            if (entry.getValue().stream().anyMatch(f -> f.getName().endsWith(".flac"))) type = "FLAC";
            if (folder.getName().contains("U87")) {
                artist = "陈奕迅";
                year = "2005";
                album = "U87";
            }

            String newFolderName = String.format("%s - %s - %s - %s", artist, year, album, type);

            if (!folder.getName().equals(newFolderName)) {
                String newPath = folder.getParent() + File.separator + newFolderName;
                records.add(new ChangeRecord(folder.getName(), newFolderName, folder, true, newPath, false));
            }
        }
        return records;
    }
}