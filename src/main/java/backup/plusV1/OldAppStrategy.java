package backup.plusV1;

import javafx.scene.Node;
import backup.plusV1.model.ChangeRecord;
import backup.plusV1.type.ScanTarget;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class OldAppStrategy {
    public abstract String getName();

    public abstract Node getConfigNode();

    public abstract ScanTarget getTargetType();

    // 新增：UI线程捕获参数的方法，避免后台线程访问UI
    public void captureParams() {
    }

    public int getPreferredThreadCount() {
        return 1;
    }

    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        try {
            return analyze(files, rootDirs);
        }finally {
            progressReporter.accept(100.0, getName()+" analysis finished.");
        }
    }

    public abstract List<ChangeRecord> analyze(List<File> files, List<File> rootDirs);
}