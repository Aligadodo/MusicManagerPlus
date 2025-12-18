package com.filemanager.strategy;

import com.filemanager.app.IManagerAppInterface;
import com.filemanager.model.ChangeRecord;
import com.filemanager.model.RuleCondition;
import com.filemanager.model.RuleConditionGroup;
import com.filemanager.type.ScanTarget;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;

@Getter
public abstract class AppStrategy {
    protected IManagerAppInterface app;
    // 通用条件配置接口 (UI调用)
    // 通用前置条件 (所有策略都支持)
    @Getter
    protected List<RuleCondition> globalConditions = new ArrayList<>();
    @Getter
    // [修改] 升级为条件组列表 (OR关系)
    protected List<RuleConditionGroup> conditionGroups = new ArrayList<>();

    protected static Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#333333"));
        return label;
    }

    public void setContext(IManagerAppInterface app) {
        this.app = app;
    }

    protected void log(String msg) {
        if (app != null) app.log(msg);
    }

    // [新增] 便捷日志方法 (子类可直接调用 log("xxx"))
    protected void invalidatePreview() {
        if (app != null) app.invalidatePreview("组件触发");
    }

    public abstract String getName();

    public abstract Node getConfigNode(); // 策略特有的配置UI

    // 核心分析逻辑
    public abstract List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter);

    // 核心执行逻辑
    public abstract void execute(ChangeRecord rec) throws Exception;

    // 配置存取
    public abstract void saveConfig(Properties props);

    public abstract void loadConfig(Properties props);


    // [修改] 校验逻辑：组间为 OR，只要有一个组满足即可
    protected boolean checkConditions(File f) {
        if (conditionGroups.isEmpty() && globalConditions.isEmpty()) return true; // 无条件则通过
        for (RuleConditionGroup group : conditionGroups) {
            if (group.test(f)) return true; // 只要有一组满足 (组内是AND)，则通过
        }
        for (RuleCondition c : globalConditions) {
            if (!c.test(f)) return false;
        }
        return false; // 所有组都不满足
    }

    public abstract ScanTarget getTargetType();

    public void captureParams() {
    }

    public abstract String getDescription();

    protected ChangeRecord getTargetFile(File file, Collection<ChangeRecord> changeRecords) {
        return changeRecords.stream().filter(changeRecord -> changeRecord.getFileHandle().equals(file) &&
                file.getName().equals(changeRecord.getFileHandle().getName())).findFirst().orElse(null);
    }

    protected HBox createParamPair(String labelText, Node control) {
        HBox hb = new HBox(5);
        hb.setAlignment(Pos.CENTER_LEFT);
        hb.getChildren().addAll(new Label(labelText), control);
        return hb;
    }
}