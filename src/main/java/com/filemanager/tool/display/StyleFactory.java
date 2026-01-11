package com.filemanager.tool.display;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.ThemeConfig;
import com.jfoenix.controls.JFXButton;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

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

    public static Node createSeparator() {
        Separator separator = new Separator();
        return separator;
    }

    /**
     * 渐变分割线
     *
     * @param isVertical
     * @return
     */
    public static Node createSeparatorWithChange(boolean isVertical) {
        if (isVertical) {
            // 水平渐变分割线
            Region hDivider = new Region();
            hDivider.setPrefHeight(1); // 线条粗细
            hDivider.setStyle(
                    "-fx-background-color: linear-gradient(to right, transparent, #D6E9FF 50%, transparent);"
            );
            return hDivider;
        }
        // 垂直渐变分割线
        Region vDivider = new Region();
        vDivider.setPrefWidth(1);
        vDivider.setStyle(
                "-fx-background-color: linear-gradient(to bottom, transparent, #D6E9FF 50%, transparent);"
        );
        return vDivider;
    }

    /**
     * 带提示词的分割线
     *
     * @param desc
     * @return
     */
    public static HBox createSeparatorWithDesc(String desc) {
        // HBox 容器实现：[线条] 文字 [线条]
        HBox labelDivider = new HBox(10);
        labelDivider.setAlignment(Pos.CENTER);

        Label label = new Label(desc);
        label.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 11px;");

        Region line1 = new Region();
        HBox.setHgrow(line1, Priority.ALWAYS);
        line1.setPrefHeight(1);
        line1.setStyle("-fx-background-color: #E5E5E5;");

        Region line2 = new Region();
        HBox.setHgrow(line2, Priority.ALWAYS);
        line2.setPrefHeight(1);
        line2.setStyle("-fx-background-color: #E5E5E5;");

        labelDivider.getChildren().addAll(line1, label, line2);
        return labelDivider;
    }

    /**
     * 自动把其他组件排挤到左右两侧
     *
     * @return
     */
    public static Node createSpacer() {
        Region spacer = new Region();
        spacer.setStyle("-fx-background-color: transparent;");
        spacer.getStyleClass().add("glass-pane");
        // 关键核心：设置其在 HBox 中始终自动扩展
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static Label createHeader(String text) {
        Label label = createLabel(text, 18, true);
        label.minWidth(30);
        return label;
    }

    public static Label createChapter(String text) {
        Label label = createLabel(text, 16, true);
        label.minWidth(30);
        return label;
    }

    public static Label createDescLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#333333"));
        return label;
    }

    public static AutoShrinkLabel createParamLabel(String text) {
        AutoShrinkLabel label = new AutoShrinkLabel(text);
        label.minWidth(70);
        label.maxWidth(70);
        return label;
    }

    public static HBox createParamPairLine(String labelText, Node... controls) {
        HBox hBox = createHBox(createParamLabel(labelText), createSpacer());
        hBox.getChildren().addAll(controls);
        hBox.setSpacing(3);
        return hBox;
    }

    public static Label createInfoLabel(String text, int maxWidth) {
        Label l = createLabel(text, 10, false);
        l.setTextFill(Color.GRAY);
        l.setMaxWidth(maxWidth);
        l.setWrapText(true);
        return l;
    }

    public static TextArea createTextArea() {
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.getStyleClass().add("glass-pane");
        logArea.setStyle(String.format("-fx-background-color: #e6dfe3; -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s;",
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
     * 创建透明的横向容器
     *
     * @return
     */
    public static VBox createVBox(Node... subNodes) {
        VBox p = new VBox();
        p.setStyle("-fx-background-color: transparent;");
        p.getStyleClass().add("glass-pane");
        for (Node subNode : subNodes) {
            p.getChildren().add(subNode);
        }
        return p;
    }

    /**
     * 创建透明的竖向容器
     *
     * @return
     */
    public static VBox createVBoxPanel(Node... subNodes) {
        VBox p = createVBox(subNodes);
        p.setStyle(String.format("-fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s;",
                theme.getGlassOpacity(), theme.getCornerRadius(), theme.getTextColor()));
        p.setSpacing(5);
        return p;
    }

    /**
     * 创建透明的横向容器
     *
     * @return
     */
    public static HBox createHBox(Node... subNodes) {
        HBox p = new HBox();
        p.setStyle("-fx-background-color: transparent;");
        p.getStyleClass().add("glass-pane");
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
        HBox p = createHBox(subNodes);
        p.setStyle(String.format("-fx-background-color: rgba(255,255,255,%.2f); -fx-background-radius: %.1f; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-text-fill: %s;",
                theme.getGlassOpacity(), theme.getCornerRadius(), theme.getTextColor()));
        p.setPadding(new Insets(5, 5, 5, 5));
        p.setSpacing(5);
        return p;
    }

    public static VBox createSectionHeader(String title, String subtitle) {
        VBox v = new VBox(2);
        v.getChildren().addAll(createHeader(title), createInfoLabel(subtitle, 400));
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
        column.setStyle("-fx-border-color: #eee; -fx-border-radius: 1; -fx-padding: 2 6 2 6; -fx-font-size: 10px;");
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


    public static HBox createTreeItemMenu(EventHandler<ActionEvent> open, EventHandler<ActionEvent> up, EventHandler<ActionEvent> down, EventHandler<ActionEvent> del) {
        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER_RIGHT);
        // 策略操作：上移、下移、删除
        // (注：配置详情通过列表选中触发，这里不需要额外按钮，或者可以加一个 '⚙' 指示)
        if (open != null) {
            JFXButton openUp = StyleFactory.createSmallIconButton("📂", open);
            actions.getChildren().add(openUp);
        }
        if (up != null) {
            JFXButton btnUp = StyleFactory.createSmallIconButton("▲", up);
            actions.getChildren().add(btnUp);
        }
        if (down != null) {
            JFXButton btnDown = StyleFactory.createSmallIconButton("▼", down);
            actions.getChildren().add(btnDown);
        }
        if (del != null) {
            JFXButton btnDel = StyleFactory.createSmallIconButton("✕", del);
            btnDel.setTextFill(Color.web("#e74c3c"));
            actions.getChildren().add(btnDel);
        }
        return actions;
    }


    /**
     * 更新列表行选中的样式
     *
     * @param node
     * @param selected
     */
    public static void updateTreeItemStyle(Node node, boolean selected) {
        if (selected) {
            // 选中样式：淡蓝色背景 + 左侧/底部蓝色边框
            node.setStyle("-fx-background-color: rgba(52, 152, 219, 0.15); -fx-border-color: #3498db; -fx-border-width: 0 0 1 0;");
        } else {
            // 默认样式
            node.setStyle("-fx-background-color: transparent; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
        }
    }

    public static Button createRefreshButton(EventHandler<ActionEvent> handler) {
        // 1. 创建刷新图标的 SVG 路径 (一个圆圈箭头)
        SVGPath refreshIcon = new SVGPath();
        refreshIcon.setContent("M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z");
        refreshIcon.setFill(javafx.scene.paint.Color.WHITE);

        // 2. 创建按钮并设置样式
        Button btn = new Button();
        btn.setGraphic(refreshIcon); // 将 SVG 设置为按钮图标
        btn.setStyle(
                "-fx-background-color: #BDE0FE;" + // 马卡龙蓝
                        "-fx-background-radius: 50;" +      // 圆形边框
                        "-fx-min-width: 20px;" +
                        "-fx-min-height: 20px;" +
                        "-fx-cursor: hand;"
        );

        // 3. 添加旋转动画（点击时触发）
        RotateTransition rt = new RotateTransition(Duration.millis(600), refreshIcon);
        rt.setByAngle(360); // 旋转 360 度
        rt.setCycleCount(1);
        rt.setInterpolator(Interpolator.EASE_BOTH); // 柔和的启动和停止

        btn.setOnAction(e -> {
            handler.handle(e);
            rt.playFromStart();
        });
        return btn;
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
    
    /**
     * 创建统一风格的进度条
     * @param initialValue 初始进度值（0.0-1.0，-1.0表示不确定）
     * @param prefWidth 首选宽度
     * @return 配置好的进度条
     */
    public static ProgressBar createProgressBar(double initialValue, double prefWidth) {
        ProgressBar progressBar = new ProgressBar(initialValue);
        progressBar.setPrefHeight(25);
        progressBar.setPrefWidth(prefWidth);
        progressBar.setStyle("-fx-accent: #27ae60;");
        return progressBar;
    }
    
    /**
     * 创建主进度条（占满宽度）
     * @param initialValue 初始进度值
     * @return 配置好的主进度条
     */
    public static ProgressBar createMainProgressBar(double initialValue) {
        ProgressBar progressBar = createProgressBar(initialValue, 10000.0);
        return progressBar;
    }
    
    /**
     * 创建根路径进度条（固定宽度）
     * @param initialValue 初始进度值
     * @return 配置好的根路径进度条
     */
    public static ProgressBar createRootPathProgressBar(double initialValue) {
        return createProgressBar(initialValue, 200);
    }
}
