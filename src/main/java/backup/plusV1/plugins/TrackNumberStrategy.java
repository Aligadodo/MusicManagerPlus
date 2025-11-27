package backup.plusV1.plugins;

import javafx.scene.Node;
import backup.plusV1.OldAppStrategy;
import backup.plusV1.model.ChangeRecord;
import backup.plusV1.type.ScanTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TrackNumberStrategy extends OldAppStrategy {
    @Override public String getName() { return "歌曲序号补全"; }
    @Override public Node getConfigNode() { return null; }
    @Override public ScanTarget getTargetType() { return ScanTarget.FILES_ONLY; }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
        List<ChangeRecord> records = new ArrayList<>();
        Map<File, List<File>> folderGroups = files.stream().collect(Collectors.groupingBy(File::getParentFile));

        for (List<File> folderFiles : folderGroups.values()) {
            folderFiles.sort(Comparator.comparing(File::getName));
            for (int i = 0; i < folderFiles.size(); i++) {
                File f = folderFiles.get(i);
                String oldName = f.getName();
                String ext = oldName.contains(".") ? oldName.substring(oldName.lastIndexOf(".")) : "";

                String title = oldName.replace(ext, "").replaceAll("^\\d+[.\\s-]*", "").trim();
                String newName = String.format("%02d. %s%s", (i + 1), title, ext);

                String newPath = f.getParent() + File.separator + newName;
                records.add(new ChangeRecord(oldName, newName, f, !oldName.equals(newName), newPath, false));
            }
        }
        return records;
    }
}