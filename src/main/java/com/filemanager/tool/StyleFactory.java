package com.filemanager.tool;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.ThemeConfig;
import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * UI 组件样式工厂
 * 负责生成风格统一的界面元素
 *
 * @author 28667
 */
public class StyleFactory {

    private static ThemeConfig theme = null;
    String baseStyle = "-fx-background-color: transparent; -fx-border-radius: 3; ";
    // 悬停样式
    String hoverStyle = "-fx-background-color: #eee; -fx-border-radius: 3;  ";

    public static void initStyleFactory(ThemeConfig theme) {
        StyleFactory.theme = theme;
    }

    public static Label createLabel(String text, int size, boolean bold) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.web(theme.getTextColor()));
        return l;
    }

    public static Label createHeader(String text) {
        return createLabel(text, 16, true);
    }

    public static Label createNormalLabel(String text) {
        return createLabel(text, 12, false);
    }


    public static Label createDescLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#333333"));
        return label;
    }

    public static Label createParamLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#333333"));
        return label;
    }

    public static HBox createParamPair(String labelText, Node control) {
        return createHBoxPanel(createParamLabel(labelText), new Region(), control);
    }

    public static Label createInfoLabel(String text) {
        Label l = createLabel(text, 10, false);
        l.setTextFill(Color.GRAY);
        return l;
    }

    public static TextArea createTextArea() {
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.getStyleClass().add("glass-pane");
        logArea.setStyle(String.format("-fx-background-color: rgba(255,255,255,%.2f); -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s;",
                theme.getGlassOpacity(), theme.getCornerRadius(), theme.getTextColor()));
        return logArea;
    }

    private static JFXButton createButton(String text) {
        JFXButton btn = new JFXButton(text);
        return btn;
    }

    public static JFXButton createActionButton(String text, String colorOverride, Runnable action) {
        JFXButton btn = createButton(text);
        String color = colorOverride != null ? colorOverride : theme.getAccentColor();
        btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: %.1f; -fx-cursor: hand;",
                color, theme.getCornerRadius()));
        btn.setMinWidth(80);
        btn.setPadding(new Insets(5, 5, 5, 5));
        if (action != null) btn.setOnAction(e -> action.run());
        return btn;
    }

    public static JFXButton createButton(String t, EventHandler<ActionEvent> h) {
        return createActionButton(t, null, () -> h.handle(null));
    }

    /**
     * [新增] 创建行内图标按钮 (如删除、上移下移)
     */
    public static JFXButton createIconButton(String iconText, String colorHex, Runnable action) {
        JFXButton btn = createButton(iconText);
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

    /**
     * 创建透明的竖向容器
     *
     * @return
     */
    public static VBox createVBoxPanel(Node... subNodes) {
        VBox p = new VBox();
        p.getStyleClass().add("glass-pane");
        p.setStyle(String.format("-fx-background-color: rgba(255,255,255,%.2f); -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s;",
                theme.getGlassOpacity(), theme.getCornerRadius(), theme.getTextColor()));
        p.setSpacing(5);
        for (Node subNode : subNodes) {
            p.getChildren().add(subNode);
        }
        return p;
    }

    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static HBox createHBoxPanel(Node... subNodes) {
        HBox p = new HBox();
        p.getStyleClass().add("glass-pane");
        p.setStyle(String.format("-fx-background-color: rgba(255,255,255,%.2f); -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s;",
                theme.getGlassOpacity(), theme.getCornerRadius(), theme.getTextColor()));
        p.setPadding(new Insets(5, 5, 5, 5));
        p.setSpacing(5);
        for (Node subNode : subNodes) {
            p.getChildren().add(subNode);
        }
        return p;
    }

    public static VBox createSectionHeader(String title, String subtitle) {
        VBox v = new VBox(2);
        v.getChildren().addAll(createHeader(title), createInfoLabel(subtitle));
        return v;
    }

    public static void forceDarkText(Node node) {
        if (node instanceof Labeled) ((Labeled) node).setTextFill(Color.web(theme.getTextColor()));
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) forceDarkText(child);
        }
    }

    // [新增] 通用：创建统一风格的微型图标按钮
    public static JFXButton createSmallIconButton(String text, EventHandler<ActionEvent> handler) {
        JFXButton btn = createButton(text);
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

    public static TreeTableColumn<ChangeRecord, String> createTreeTableColumn(String text, boolean needToolTip, int prefWidth, int minWidth, int maxWidth) {
        TreeTableColumn<ChangeRecord, String> column = new TreeTableColumn<>(text);
        column.setPrefWidth(prefWidth);
        column.setMinWidth(minWidth);
        column.setMaxWidth(maxWidth);
        column.setStyle("-fx-background-color: #b2b2b2; -fx-border-color: #999; -fx-border-radius: 3; -fx-padding: 2 6 2 6; -fx-font-size: 10px;");
        column.setCellFactory(col -> {
            return new TreeTableCell<ChangeRecord, String>() {
                private final Tooltip tooltip = new Tooltip();
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setTooltip(null); // 必须清除，否则空行也会显示上一个内容的悬浮
                    } else {
                        setText(item);
                        // 设置悬浮内容
                        tooltip.setText("详情内容：\n" + item);
                        // 可选：设置换行宽度，防止详情太长变成一条直线
                        tooltip.setWrapText(true);
                        tooltip.setPrefWidth(300);
                        setTooltip(tooltip);
                    }
                }
            };
        });
        return column;
    }

    public static void setBasicStyle(Node node) {
//        node.setStyle(baseStyle);
//        if (node instanceof Labeled) {
//            ((Labeled) node).setTextFill(Color.web(theme.getTextColor()));
//        }
//        if (node instanceof Parent) {
//            for (Node c : ((Parent) node).getChildrenUnmodifiable()) {
//                setBasicStyle(c);
//            }
//        }
    }
}
