package plus;

import java.io.File;
import java.util.List;

import javafx.scene.Node;
import plus.model.ChangeRecord;
import plus.type.ScanTarget;

public abstract class AppStrategy {
    public abstract String getName();

    public abstract Node getConfigNode();

    public abstract ScanTarget getTargetType();

    public int getPreferredThreadCount() {
        return 1;
    }

    public abstract List<ChangeRecord> analyze(List<File> files, List<File> rootDirs);
}