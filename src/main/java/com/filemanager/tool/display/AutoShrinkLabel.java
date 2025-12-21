package com.filemanager.tool.display;

import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class AutoShrinkLabel extends StackPane {
    private Label label;

    public AutoShrinkLabel(String text) {
        label = new Label(text);
        
        // 1. 禁止 Label 自动加省略号
        label.setEllipsisString("");
        label.setMinWidth(Label.USE_PREF_SIZE);

        // 2. 将 Label 放入 Group 中
        // Group 的特性是：其边界会随子组件缩放而变化，适合用于做缩放效果
        Group group = new Group(label);
        getChildren().add(group);

        // 3. 监听容器宽度变化，动态计算缩放比例
        widthProperty().addListener((obs, oldVal, newVal) -> {
            updateScale();
        });
    }

    private void updateScale() {
        double containerWidth = getWidth();
        double labelWidth = label.getLayoutBounds().getWidth();

        if (labelWidth > containerWidth && containerWidth > 0) {
            // 计算缩放比例
            double scale = containerWidth / labelWidth;
            label.setScaleX(scale);
            label.setScaleY(scale);
        } else {
            // 空间足够时恢复原状
            label.setScaleX(1.0);
            label.setScaleY(1.0);
        }
    }
}