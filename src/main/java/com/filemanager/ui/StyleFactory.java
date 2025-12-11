package com.filemanager.ui;

import com.filemanager.model.ThemeConfig;
import com.jfoenix.controls.JFXButton;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * UI 组件样式工厂
 * 负责生成风格统一的界面元素
 */
public class StyleFactory {
    
    private final ThemeConfig theme;

    public StyleFactory(ThemeConfig theme) {
        this.theme = theme;
    }

    public Label createLabel(String text, int size, boolean bold) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.web(theme.getTextColor()));
        return l;
    }

    public Label createHeader(String text) {
        return createLabel(text, 16, true);
    }

    public Label createNormalLabel(String text) {
        return createLabel(text, 12, false);
    }

    public Label createInfoLabel(String text) {
        Label l = createLabel(text, 10, false);
        l.setTextFill(Color.GRAY); 
        return l;
    }

    public JFXButton createActionButton(String text, String colorOverride, Runnable action) {
        JFXButton btn = new JFXButton(text);
        String color = colorOverride != null ? colorOverride : theme.getAccentColor();
        btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: %.1f; -fx-cursor: hand;", 
            color, theme.getCornerRadius()));
        if (action != null) btn.setOnAction(e -> action.run());
        return btn;
    }

    public JFXButton createButton(String t, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        return this.createActionButton(t, null, () -> h.handle(null));
    }

    /**
     * [新增] 创建行内图标按钮 (如删除、上移下移)
     */
    public JFXButton createIconButton(String iconText, String colorHex, Runnable action) {
        JFXButton btn = new JFXButton(iconText);
        String textColor = colorHex != null ? colorHex : "#555";
        
        // 基础样式
        String baseStyle = String.format("-fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-radius: 3; -fx-padding: 2 6 2 6; -fx-font-size: 10px; -fx-text-fill: %s;", textColor);
        // 悬停样式
        String hoverStyle = String.format("-fx-background-color: #eee; -fx-border-color: #999; -fx-border-radius: 3; -fx-padding: 2 6 2 6; -fx-font-size: 10px; -fx-text-fill: %s;", textColor);
        
        btn.setStyle(baseStyle);
        
        btn.setOnAction(e -> {
            if (action != null) action.run();
            e.consume(); // 防止事件冒泡选中列表行
        });
        
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        
        return btn;
    }

    public VBox createGlassPane() {
        VBox p = new VBox();
        p.getStyleClass().add("glass-pane");
        p.setStyle(String.format("-fx-background-color: rgba(255,255,255,%.2f); -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s;", 
            theme.getGlassOpacity(), theme.getCornerRadius(), theme.getTextColor()));
        return p;
    }
    
    public VBox createSectionHeader(String title, String subtitle) {
        VBox v = new VBox(2);
        v.getChildren().addAll(createHeader(title), createInfoLabel(subtitle));
        return v;
    }

    public void forceDarkText(Node node) {
        if (node instanceof Labeled) ((Labeled)node).setTextFill(Color.web(theme.getTextColor()));
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) forceDarkText(child);
        }
    }

    // [新增] 通用：创建统一风格的微型图标按钮
    public JFXButton createSmallIconButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        JFXButton btn = new JFXButton(text);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-radius: 3; -fx-padding: 2 6 2 6; -fx-font-size: 10px;");
        btn.setTextFill(Color.web("#555"));
        btn.setOnAction(e -> {
            handler.handle(e);
            e.consume(); // 防止事件冒泡触发 ListCell 选中
        });
        // Hover 效果
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #eee; -fx-border-color: #999; -fx-border-radius: 3; -fx-padding: 2 6 2 6; -fx-font-size: 10px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-border-color: #ccc; -fx-border-radius: 3; -fx-padding: 2 6 2 6; -fx-font-size: 10px;"));
        return btn;
    }
}