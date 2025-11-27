package plusv2;

import javafx.scene.Node;
import plusv2.model.ChangeRecord;
import plusv2.type.ScanTarget;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;

public abstract class AppStrategyV2 {
    // [新增] 持有主程序引用
    protected MusicFileManagerAppV2 app;

    // [新增] 上下文注入方法 (在 initStrategies 中自动调用)
    public void setContext(MusicFileManagerAppV2 app) {
        this.app = app;
    }

    // [新增] 获取 App 实例 (供子类使用)
    public MusicFileManagerAppV2 getApp() {
        return app;
    }

    // [新增] 便捷日志方法 (子类可直接调用 log("xxx"))
    protected void log(String msg) {
        if (app != null) app.log(msg);
    }

    // [新增] 便捷日志方法 (子类可直接调用 log("xxx"))
    protected void invalidatePreview() {
        if (app != null) app.invalidatePreview();
    }

    public abstract String getName();

    public abstract Node getConfigNode();

    public abstract ScanTarget getTargetType();

    // 新增：UI线程捕获参数的方法，避免后台线程访问UI
    public void captureParams() {
    }

    public int getPreferredThreadCount() {
        return 1;
    }

    // [新增] 执行单个变更记录的逻辑
    public abstract void execute(ChangeRecord rec) throws Exception;

    // [新增] 配置持久化接口 (默认空实现)
    public void saveConfig(Properties props) {
    }

    public void loadConfig(Properties props) {
    }

    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        try {
            return analyze(files, rootDirs);
        } finally {
            progressReporter.accept(100.0, getName() + " analysis finished.");
        }
    }

    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
        return Collections.emptyList();
    }

}