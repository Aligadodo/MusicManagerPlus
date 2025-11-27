package plusv2;

import javafx.scene.Node;
import lombok.Getter;
import plusv2.model.ChangeRecord;
import plusv2.model.RuleCondition;
import plusv2.type.ScanTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;

@Getter
public abstract class AppStrategy {
    protected MusicFileManagerApp app;
    // 通用前置条件 (所有策略都支持)
    protected List<RuleCondition> globalConditions = new ArrayList<>();

    public void setContext(MusicFileManagerApp app) { this.app = app; }
    protected void log(String msg) { if (app != null) app.log(msg); }

    // [新增] 便捷日志方法 (子类可直接调用 log("xxx"))
    protected void invalidatePreview() {
        if (app != null) app.invalidatePreview("组件触发");
    }

    public abstract String getName();
    public abstract Node getConfigNode(); // 策略特有的配置UI

    // 通用条件配置接口 (UI调用)
    public List<RuleCondition> getGlobalConditions() { return globalConditions; }

    // 核心分析逻辑
    public abstract List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter);

    // 核心执行逻辑
    public abstract void execute(ChangeRecord rec) throws Exception;

    // 配置存取
    public abstract void saveConfig(Properties props);
    public abstract void loadConfig(Properties props);

    // 辅助：检查通用条件
    protected boolean checkConditions(File f) {
        if (globalConditions.isEmpty()) return true;
        for (RuleCondition c : globalConditions) {
            if (!c.test(f)) return false;
        }
        return true;
    }

    public abstract ScanTarget getTargetType();

    public void captureParams() {
    }

    public String getDescription() {
        return getName();
    }
}