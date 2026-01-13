/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.components;

import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.app.tools.display.ThemeConfig;
import com.filemanager.app.tools.display.ThemeManager;
import com.filemanager.app.tools.display.StyleTemplateManager;
import java.util.List;
import com.filemanager.app.base.IAppController;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXComboBox;
import com.filemanager.app.tools.display.FontManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.Node;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class AppearanceManager {
    private final IAppController app;
    private final ThemeConfig currentTheme;
    private final ImageView backgroundImageView;
    private final Region backgroundOverlay;
    private final ThemeManager themeManager;
    private final StyleTemplateManager templateManager;
    private Tab finalPresetTab;
    
    public AppearanceManager(IAppController app, ThemeConfig currentTheme,
                           ImageView backgroundImageView, Region backgroundOverlay) {
        this.app = app;
        this.currentTheme = currentTheme;
        this.backgroundImageView = backgroundImageView;
        this.backgroundOverlay = backgroundOverlay;
        this.themeManager = ThemeManager.getInstance();
        this.templateManager = StyleTemplateManager.getInstance();
    }
    
    public void showAppearanceDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("外观设置");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(900);
        dialog.getDialogPane().setPrefHeight(700);
        
        // 创建主容器
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setStyle(String.format("-fx-background-color: %s;", currentTheme.getBgColor()));
        mainContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        

        
        // 创建TabPane来组织不同的设置类别
        TabPane tabPane = new TabPane();
        tabPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: 40; -fx-tab-max-height: 40; -fx-tab-min-width: 100; -fx-tab-max-width: 200; -fx-text-fill: %s;\n" +
                ".tab-pane > .tab-header-area > .tab-header-background {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-border-color: %s;\n" +
                "    -fx-border-width: 0 0 %.1f 0;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-border-color: %s;\n" +
                "    -fx-border-width: %.1f %.1f 0 %.1f;\n" +
                "    -fx-border-radius: %.1f %.1f 0 0;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab:selected {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-border-color: %s;\n" +
                "    -fx-border-width: %.1f %.1f 0 %.1f;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab > .tab-container > .tab-label {\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-font-family: %s;\n" +
                "    -fx-font-size: 14px;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab:selected > .tab-container > .tab-label {\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-font-weight: bold;\n" +
                "}",
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor(), currentTheme.getBorderWidth(), currentTheme.getTextPrimaryColor(),
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor(), currentTheme.getBorderWidth(),
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor(), currentTheme.getBorderWidth(), currentTheme.getBorderWidth(), currentTheme.getBorderWidth(), currentTheme.getCornerRadius(), currentTheme.getCornerRadius(),
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor(), currentTheme.getBorderWidth(), currentTheme.getBorderWidth(), currentTheme.getBorderWidth(),
                currentTheme.getTextSecondaryColor(), currentTheme.getFontFamily(),
                currentTheme.getTextPrimaryColor()
        ));
        
        // 主题预设选项卡
        Tab presetTab = new Tab("主题预设");
        presetTab.setContent(createPresetTabContent());
        tabPane.getTabs().add(presetTab);
        
        // 保存对主题预设选项卡的引用，用于后续更新
        finalPresetTab = presetTab;
        
        // 颜色设置选项卡
        Tab colorTab = new Tab("颜色设置");
        colorTab.setContent(createColorTabContent());
        tabPane.getTabs().add(colorTab);
        
        // 背景设置选项卡
        Tab backgroundTab = new Tab("背景设置");
        backgroundTab.setContent(createBackgroundTabContent());
        tabPane.getTabs().add(backgroundTab);
        
        // 字体设置选项卡
        Tab fontTab = new Tab("字体设置");
        fontTab.setContent(createFontTabContent());
        tabPane.getTabs().add(fontTab);
        
        // 按钮设置选项卡
        Tab buttonTab = new Tab("按钮设置");
        buttonTab.setContent(createButtonTabContent());
        tabPane.getTabs().add(buttonTab);
        
        // 样式管理选项卡
        Tab styleManageTab = new Tab("样式管理");
        styleManageTab.setContent(createStyleManageTabContent());
        tabPane.getTabs().add(styleManageTab);
        
        mainContainer.getChildren().add(tabPane);
        
        // 添加滚动容器
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        dialog.getDialogPane().setContent(scrollPane);
        
        // 应用按钮逻辑
        Button applyButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.APPLY);
        applyButton.setOnAction(e -> {
            // 保存当前主题到ThemeManager
            themeManager.updateCurrentTheme(theme -> {
                theme.setBgImagePath(currentTheme.getBgImagePath());
                theme.setBgColor(currentTheme.getBgColor());
                theme.setAccentColor(currentTheme.getAccentColor());
                theme.setTextPrimaryColor(currentTheme.getTextPrimaryColor());
                theme.setTextSecondaryColor(currentTheme.getTextSecondaryColor());
                theme.setTextTertiaryColor(currentTheme.getTextTertiaryColor());
                theme.setTextDisabledColor(currentTheme.getTextDisabledColor());
                theme.setGlassOpacity(currentTheme.getGlassOpacity());
                theme.setDarkBackground(currentTheme.isDarkBackground());
                theme.setPanelBgColor(currentTheme.getPanelBgColor());
                theme.setFontFamily(currentTheme.getFontFamily());
                theme.setFontSize(currentTheme.getFontSize());
                theme.setButtonLargeSize(currentTheme.getButtonLargeSize());
                theme.setButtonSmallSize(currentTheme.getButtonSmallSize());
            });
            dialog.close();
        });
        
        dialog.showAndWait();
    }
    
    /**
     * 创建主题预设选项卡内容
     */
    private Node createPresetTabContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: transparent;");
        
        // 获取所有样式模板
        templateManager.loadAllTemplates();
        java.util.List<ThemeConfig> templates = templateManager.getAllTemplates();
        
        // 使用网格布局展示主题卡片
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(30);
        grid.setPadding(new Insets(10, 0, 10, 0));
        
        // 保存所有主题卡片的引用，用于后续更新选中状态
        final java.util.List<VBox> themeCards = new java.util.ArrayList<>();
        final java.util.List<ThemeConfig> themes = new java.util.ArrayList<>();
        
        int col = 0;
        int row = 0;
        for (ThemeConfig template : templates) {
            // 创建主题预览卡片
            VBox themeCard = createThemeCard(template);
            themeCard.setCursor(Cursor.HAND);
            
            // 保存引用
            themeCards.add(themeCard);
            themes.add(template);
            
            // 设置点击事件
            final int index = themeCards.size() - 1;
            themeCard.setOnMouseClicked(e -> {
                // 应用新主题
                applyTheme(themes.get(index));
                // 刷新颜色选择器
                refreshColorPickers();
                // 更新所有主题卡片的选中状态
                updateAllThemeCardSelection(themeCards, themes);
            });
            
            grid.add(themeCard, col, row);
            col++;
            if (col > 2) { // 每行显示3个卡片，确保在有限宽度内完整显示
                col = 0;
                row++;
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        content.getChildren().add(scrollPane);
        return content;
    }
    
    /**
     * 更新所有主题卡片的选中状态
     */
    private void updateAllThemeCardSelection(java.util.List<VBox> themeCards, java.util.List<ThemeConfig> themes) {
        for (int i = 0; i < themeCards.size(); i++) {
            VBox card = themeCards.get(i);
            ThemeConfig theme = themes.get(i);
            
            // 重置卡片样式
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 0);");
            
            // 如果是当前主题，添加选中效果
            if (isCurrentTheme(theme)) {
                card.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0); -fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 10;");
            }
        }
    }
    
    /**
     * 创建颜色设置选项卡内容
     */
    private Node createColorTabContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: transparent;");
        
        // 创建颜色设置网格
        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(25);
        colorGrid.setVgap(20);
        colorGrid.setPadding(new Insets(10, 0, 10, 0));
        
        // 主题色
        Label accentLabel = StyleFactory.createLabel("主题色", 14, false);
        ColorPicker accentPicker = new ColorPicker(Color.web(currentTheme.getAccentColor()));
        accentPicker.setOnAction(e -> {
            currentTheme.setAccentColor(String.format("#%02X%02X%02X", 
                    (int) (accentPicker.getValue().getRed() * 255), 
                    (int) (accentPicker.getValue().getGreen() * 255), 
                    (int) (accentPicker.getValue().getBlue() * 255)));
            applyAppearance();
        });
        colorGrid.add(accentLabel, 0, 0);
        colorGrid.add(accentPicker, 1, 0);
        
        // 文本颜色
        Label textLabel = StyleFactory.createLabel("文本颜色", 14, false);
        ColorPicker textPicker = new ColorPicker(Color.web(currentTheme.getTextPrimaryColor()));
        textPicker.setOnAction(e -> {
            currentTheme.setTextPrimaryColor(String.format("#%02X%02X%02X", 
                    (int) (textPicker.getValue().getRed() * 255), 
                    (int) (textPicker.getValue().getGreen() * 255), 
                    (int) (textPicker.getValue().getBlue() * 255)));
            applyAppearance();
        });
        colorGrid.add(textLabel, 2, 0);
        colorGrid.add(textPicker, 3, 0);
        
        // 背景色
        Label bgLabel = StyleFactory.createLabel("背景色", 14, false);
        ColorPicker bgPicker = new ColorPicker(Color.web(currentTheme.getBgColor()));
        bgPicker.setOnAction(e -> {
            currentTheme.setBgColor(String.format("#%02X%02X%02X", 
                    (int) (bgPicker.getValue().getRed() * 255), 
                    (int) (bgPicker.getValue().getGreen() * 255), 
                    (int) (bgPicker.getValue().getBlue() * 255)));
            applyAppearance();
        });
        colorGrid.add(bgLabel, 0, 1);
        colorGrid.add(bgPicker, 1, 1);
        
        // 面板背景色
        Label panelLabel = StyleFactory.createLabel("面板背景色", 14, false);
        ColorPicker panelPicker = new ColorPicker(Color.web(currentTheme.getPanelBgColor()));
        panelPicker.setOnAction(e -> {
            currentTheme.setPanelBgColor(String.format("#%02X%02X%02X", 
                    (int) (panelPicker.getValue().getRed() * 255), 
                    (int) (panelPicker.getValue().getGreen() * 255), 
                    (int) (panelPicker.getValue().getBlue() * 255)));
            applyAppearance();
        });
        colorGrid.add(panelLabel, 2, 1);
        colorGrid.add(panelPicker, 3, 1);
        
        // 边框颜色
        Label borderLabel = StyleFactory.createLabel("边框颜色", 14, false);
        ColorPicker borderPicker = new ColorPicker(Color.web(currentTheme.getBorderColor()));
        borderPicker.setOnAction(e -> {
            currentTheme.setBorderColor(String.format("#%02X%02X%02X", 
                    (int) (borderPicker.getValue().getRed() * 255), 
                    (int) (borderPicker.getValue().getGreen() * 255), 
                    (int) (borderPicker.getValue().getBlue() * 255)));
            applyAppearance();
        });
        colorGrid.add(borderLabel, 0, 2);
        colorGrid.add(borderPicker, 1, 2);
        
        // 悬停颜色
        Label hoverLabel = StyleFactory.createLabel("悬停颜色", 14, false);
        ColorPicker hoverPicker = new ColorPicker(Color.web(currentTheme.getHoverColor()));
        hoverPicker.setOnAction(e -> {
            currentTheme.setHoverColor(String.format("#%02X%02X%02X", 
                    (int) (hoverPicker.getValue().getRed() * 255), 
                    (int) (hoverPicker.getValue().getGreen() * 255), 
                    (int) (hoverPicker.getValue().getBlue() * 255)));
            applyAppearance();
        });
        colorGrid.add(hoverLabel, 2, 2);
        colorGrid.add(hoverPicker, 3, 2);
        
        content.getChildren().add(colorGrid);
        return content;
    }
    
    /**
     * 创建背景设置选项卡内容
     */
    private Node createBackgroundTabContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: transparent;");
        
        // 背景图片选择
        HBox imageBox = new HBox(10);
        TextField bgImagePath = new TextField(currentTheme.getBgImagePath());
        bgImagePath.setPrefWidth(400);
        bgImagePath.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius(), currentTheme.getTextPrimaryColor(), 
                currentTheme.getBorderColor(), currentTheme.getBorderWidth()
        ));
        
        JFXButton browseBtn = StyleFactory.createActionButton("浏览背景图", "#3498db", () -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) {
                bgImagePath.setText(f.getAbsolutePath());
                currentTheme.setBgImagePath(f.getAbsolutePath());
                applyAppearance();
            }
        });
        
        JFXButton clearBtn = StyleFactory.createActionButton("清除背景", "#e74c3c", () -> {
            bgImagePath.clear();
            currentTheme.setBgImagePath("");
            backgroundImageView.setImage(null);
            applyAppearance();
        });
        
        imageBox.getChildren().addAll(bgImagePath, browseBtn, clearBtn);
        content.getChildren().add(imageBox);
        
        // 玻璃效果透明度
        HBox opacityBox = new HBox(20);
        opacityBox.setAlignment(Pos.CENTER_LEFT);
        Label opacityLabel = StyleFactory.createLabel("玻璃效果透明度", 14, false);
        JFXSlider opacitySlider = new JFXSlider(0.1, 1.0, currentTheme.getGlassOpacity());
        opacitySlider.setPrefWidth(400);
        opacitySlider.setMajorTickUnit(0.1);
        opacitySlider.setMinorTickCount(0);
        opacitySlider.setShowTickLabels(true);
        opacitySlider.setShowTickMarks(false);
        
        Label opacityValue = StyleFactory.createLabel(String.format("%.2f", currentTheme.getGlassOpacity()), 14, false);
        opacityValue.setMinWidth(50);
        
        // 添加延迟更新机制，减少频繁UI更新导致的卡顿
        javafx.animation.PauseTransition pauseTransition = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentTheme.setGlassOpacity(newVal.doubleValue());
            opacityValue.setText(String.format("%.2f", newVal.doubleValue()));
            
            // 取消之前的过渡
            pauseTransition.stop();
            // 设置新的过渡
            pauseTransition.setOnFinished(e -> {
                // 直接更新背景覆盖层样式，避免调用完整的applyAppearance方法
                backgroundOverlay.setStyle("-fx-background-color: rgba(" + 
                        (currentTheme.isDarkBackground() ? "0,0,0" : "255,255,255") + 
                        ", " + (1 - newVal.doubleValue()) + ");");
            });
            // 开始新的过渡
            pauseTransition.play();
        });
        
        opacityBox.getChildren().addAll(opacityLabel, opacitySlider, opacityValue);
        content.getChildren().add(opacityBox);
        
        // 深色模式
        HBox darkModeBox = new HBox(10);
        darkModeBox.setPadding(new Insets(10, 0, 0, 0));
        CheckBox darkModeChk = new CheckBox("深色模式");
        darkModeChk.setSelected(currentTheme.isDarkBackground());
        darkModeChk.setOnAction(e -> {
            currentTheme.setDarkBackground(darkModeChk.isSelected());
            applyAppearance();
        });
        darkModeBox.getChildren().add(darkModeChk);
        content.getChildren().add(darkModeBox);
        
        return content;
    }
    
    /**
     * 创建字体设置选项卡内容
     */
    private Node createFontTabContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: transparent;");
        
        // 字体选择
        HBox fontFamilyBox = new HBox(20);
        fontFamilyBox.setAlignment(Pos.CENTER_LEFT);
        Label fontFamilyLabel = StyleFactory.createLabel("字体家族", 14, false);
        
        FontManager fontManager = FontManager.getInstance();
        ObservableList<String> fontFamilies = FXCollections.observableArrayList(fontManager.getFilteredFonts());
        JFXComboBox<String> fontFamilyCb = new JFXComboBox<>(fontFamilies);
        fontFamilyCb.setValue(currentTheme.getFontFamily());
        fontFamilyCb.setPrefWidth(400);
        fontFamilyCb.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius(), currentTheme.getTextPrimaryColor(), 
                currentTheme.getBorderColor(), currentTheme.getBorderWidth()
        ));
        fontFamilyCb.setOnAction(e -> {
            currentTheme.setFontFamily(fontFamilyCb.getValue());
            applyAppearance();
        });
        
        fontFamilyBox.getChildren().addAll(fontFamilyLabel, fontFamilyCb);
        content.getChildren().add(fontFamilyBox);
        
        // 字体大小
        HBox fontSizeBox = new HBox(20);
        fontSizeBox.setAlignment(Pos.CENTER_LEFT);
        Label fontSizeLabel = StyleFactory.createLabel("字体大小", 14, false);
        JFXSlider fontSizeSlider = new JFXSlider(10, 22, currentTheme.getFontSize());
        fontSizeSlider.setPrefWidth(400);
        fontSizeSlider.setMajorTickUnit(1);
        fontSizeSlider.setMinorTickCount(0);
        fontSizeSlider.setShowTickLabels(true);
        fontSizeSlider.setShowTickMarks(false);
        
        Label fontSizeValue = StyleFactory.createLabel(String.format("%.1f", currentTheme.getFontSize()), 14, false);
        fontSizeValue.setMinWidth(50);
        
        fontSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentTheme.setFontSize(newVal.doubleValue());
            fontSizeValue.setText(String.format("%.1f", newVal.doubleValue()));
            applyAppearance();
        });
        
        fontSizeBox.getChildren().addAll(fontSizeLabel, fontSizeSlider, fontSizeValue);
        content.getChildren().add(fontSizeBox);
        
        return content;
    }
    
    /**
     * 创建按钮设置选项卡内容
     */
    private Node createButtonTabContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: transparent;");
        
        // 大按钮大小设置
        HBox largeButtonBox = new HBox(20);
        largeButtonBox.setAlignment(Pos.CENTER_LEFT);
        Label largeButtonLabel = StyleFactory.createLabel("大按钮大小", 14, false);
        JFXSlider largeButtonSlider = new JFXSlider(60, 100, currentTheme.getButtonLargeSize());
        largeButtonSlider.setPrefWidth(400);
        largeButtonSlider.setMajorTickUnit(5);
        largeButtonSlider.setMinorTickCount(0);
        largeButtonSlider.setShowTickLabels(true);
        largeButtonSlider.setShowTickMarks(false);
        
        Label largeButtonValue = StyleFactory.createLabel(String.format("%.1f", currentTheme.getButtonLargeSize()), 14, false);
        largeButtonValue.setMinWidth(50);
        
        largeButtonSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentTheme.setButtonLargeSize(newVal.doubleValue());
            largeButtonValue.setText(String.format("%.1f", newVal.doubleValue()));
            applyAppearance();
        });
        
        largeButtonBox.getChildren().addAll(largeButtonLabel, largeButtonSlider, largeButtonValue);
        content.getChildren().add(largeButtonBox);
        
        // 小按钮大小设置
        HBox smallButtonBox = new HBox(20);
        smallButtonBox.setAlignment(Pos.CENTER_LEFT);
        Label smallButtonLabel = StyleFactory.createLabel("小按钮大小", 14, false);
        JFXSlider smallButtonSlider = new JFXSlider(40, 80, currentTheme.getButtonSmallSize());
        smallButtonSlider.setPrefWidth(400);
        smallButtonSlider.setMajorTickUnit(5);
        smallButtonSlider.setMinorTickCount(0);
        smallButtonSlider.setShowTickLabels(true);
        smallButtonSlider.setShowTickMarks(false);
        
        Label smallButtonValue = StyleFactory.createLabel(String.format("%.1f", currentTheme.getButtonSmallSize()), 14, false);
        smallButtonValue.setMinWidth(50);
        
        smallButtonSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentTheme.setButtonSmallSize(newVal.doubleValue());
            smallButtonValue.setText(String.format("%.1f", newVal.doubleValue()));
            applyAppearance();
        });
        
        smallButtonBox.getChildren().addAll(smallButtonLabel, smallButtonSlider, smallButtonValue);
        content.getChildren().add(smallButtonBox);
        
        return content;
    }
    
    /**
     * 创建样式管理选项卡内容
     */
    private Node createStyleManageTabContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: transparent;");
        
        // 保存模板区域
        VBox saveTemplateBox = new VBox(15);
        saveTemplateBox.setPadding(new Insets(10));
        saveTemplateBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-width: 1px;",
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor()
        ));
        
        Label saveTitle = new Label("保存自定义模板");
        saveTitle.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s;",
                currentTheme.getFontFamily(), currentTheme.getTextPrimaryColor()
        ));
        
        GridPane saveGrid = new GridPane();
        saveGrid.setHgap(15);
        saveGrid.setVgap(15);
        
        // 模板名称
        Label nameLabel = new Label("模板名称:");
        nameLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 14px; -fx-text-fill: %s;",
                currentTheme.getFontFamily(), currentTheme.getTextPrimaryColor()
        ));
        TextField nameField = new TextField();
        nameField.setPromptText("输入模板名称");
        nameField.setPrefWidth(200);
        nameField.setStyle(String.format(
                "-fx-background-color: #ffffff; -fx-background-radius: 4; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: 1px;",
                currentTheme.getTextPrimaryColor(), currentTheme.getBorderColor()
        ));
        
        // 模板描述
        Label descLabel = new Label("模板描述:");
        descLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 14px; -fx-text-fill: %s;",
                currentTheme.getFontFamily(), currentTheme.getTextPrimaryColor()
        ));
        TextField descField = new TextField();
        descField.setPromptText("输入模板描述");
        descField.setPrefWidth(300);
        descField.setStyle(String.format(
                "-fx-background-color: #ffffff; -fx-background-radius: 4; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: 1px;",
                currentTheme.getTextPrimaryColor(), currentTheme.getBorderColor()
        ));
        
        // 保存按钮
        JFXButton saveBtn = StyleFactory.createActionButton("保存为新模板", "#27ae60", () -> {
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText(null);
                alert.setContentText("请输入模板名称");
                alert.showAndWait();
                return;
            }
            
            // 创建新模板
            ThemeConfig newTemplate = currentTheme.clone();
            newTemplate.setTemplateName(nameField.getText().trim());
            newTemplate.setTemplateDescription(descField.getText());
            
            // 保存模板
            if (templateManager.saveTemplate(newTemplate)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("成功");
                alert.setHeaderText(null);
                alert.setContentText("模板保存成功");
                alert.showAndWait();
                
                // 清空输入
                nameField.clear();
                descField.clear();
                
                // 更新主题预设选项卡的内容，显示最新的主题列表
                finalPresetTab.setContent(createPresetTabContent());
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText(null);
                alert.setContentText("模板保存失败");
                alert.showAndWait();
            }
        });
        
        saveGrid.add(nameLabel, 0, 0);
        saveGrid.add(nameField, 1, 0);
        saveGrid.add(descLabel, 0, 1);
        saveGrid.add(descField, 1, 1, 2, 1);
        saveGrid.add(saveBtn, 1, 2);
        
        saveTemplateBox.getChildren().addAll(saveTitle, saveGrid);
        
        // 模板管理区域
        VBox manageTemplateBox = new VBox(15);
        manageTemplateBox.setPadding(new Insets(10));
        manageTemplateBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-width: 1px;",
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor()
        ));
        
        Label manageTitle = new Label("管理模板");
        manageTitle.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s;",
                currentTheme.getFontFamily(), currentTheme.getTextPrimaryColor()
        ));
        
        // 模板列表
        ListView<String> templateListView = new ListView<>();
        templateListView.setPrefHeight(200);
        templateListView.setStyle(String.format(
                "-fx-background-color: #ffffff; -fx-background-radius: 4; -fx-border-color: %s; -fx-border-width: 1px;",
                currentTheme.getBorderColor()
        ));
        
        // 刷新模板列表
        javafx.collections.ObservableList<String> templateNames = javafx.collections.FXCollections.observableArrayList();
        templateManager.loadAllTemplates();
        templateNames.addAll(templateManager.getAllTemplateNames());
        templateListView.setItems(templateNames);
        
        // 操作按钮
        HBox manageButtons = new HBox(15);
        manageButtons.setAlignment(Pos.CENTER);
        
        JFXButton useBtn = StyleFactory.createActionButton("应用模板", "#3498db", () -> {
            String selectedName = templateListView.getSelectionModel().getSelectedItem();
            if (selectedName != null) {
                ThemeConfig template = templateManager.getTemplate(selectedName);
                if (template != null) {
                    applyTheme(template);
                    refreshColorPickers();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("成功");
                    alert.setHeaderText(null);
                    alert.setContentText("模板已应用");
                    alert.showAndWait();
                }
            }
        });
        
        JFXButton deleteBtn = StyleFactory.createActionButton("删除模板", "#e74c3c", () -> {
            String selectedName = templateListView.getSelectionModel().getSelectedItem();
            if (selectedName != null) {
                // 确认删除
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("确认删除");
                alert.setHeaderText(null);
                alert.setContentText("确定要删除此模板吗？");
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        if (templateManager.deleteTemplate(selectedName)) {
                            // 刷新列表
                            templateNames.clear();
                            templateManager.loadAllTemplates();
                            templateNames.addAll(templateManager.getAllTemplateNames());
                            templateListView.setItems(templateNames);
                            
                            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                            successAlert.setTitle("成功");
                            successAlert.setHeaderText(null);
                            successAlert.setContentText("模板已删除");
                            successAlert.showAndWait();
                        } else {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                            errorAlert.setTitle("错误");
                            errorAlert.setHeaderText(null);
                            errorAlert.setContentText("无法删除内置模板");
                            errorAlert.showAndWait();
                        }
                    }
                });
            }
        });
        
        JFXButton refreshBtn = StyleFactory.createActionButton("刷新列表", "#95a5a6", () -> {
            templateNames.clear();
            templateManager.loadAllTemplates();
            templateNames.addAll(templateManager.getAllTemplateNames());
            templateListView.setItems(templateNames);
        });
        
        manageButtons.getChildren().addAll(useBtn, deleteBtn, refreshBtn);
        
        manageTemplateBox.getChildren().addAll(manageTitle, templateListView, manageButtons);
        
        content.getChildren().addAll(saveTemplateBox, manageTemplateBox);
        return content;
    }
    
    /**
     * 创建主题预览卡片
     */
    private VBox createThemeCard(ThemeConfig theme) {
        VBox card = new VBox(10);
        card.setPrefWidth(200); // 增加卡片宽度，显示更多内容
        card.setPrefHeight(280); // 增加卡片高度
        card.setPadding(new Insets(15));
        card.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0);",
                theme.getPanelBgColor()
        ));
        
        // 主题名称
        Label nameLabel = new Label(theme.getTemplateName());
        nameLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s;",
                theme.getFontFamily(), theme.getTextPrimaryColor()
        ));
        nameLabel.setAlignment(Pos.CENTER);
        card.getChildren().add(nameLabel);
        
        // 主题描述
        Label descLabel = new Label(theme.getTemplateDescription());
        descLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 12px; -fx-text-fill: %s;",
                theme.getFontFamily(), theme.getTextSecondaryColor()
        ));
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(170);
        card.getChildren().add(descLabel);
        
        // 分隔线
        Separator separator = new Separator();
        separator.setStyle(String.format("-fx-background-color: %s;", theme.getBorderColor()));
        card.getChildren().add(separator);
        
        // 颜色预览区
        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(8);
        colorGrid.setVgap(8);
        colorGrid.setPadding(new Insets(5, 0, 5, 0));
        
        // 背景色
        VBox bgColorBox = new VBox(3);
        Label bgLabel = new Label("背景色");
        bgLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getFontFamily(), theme.getTextSecondaryColor()
        ));
        bgLabel.setAlignment(Pos.CENTER);
        Rectangle bgColor = new Rectangle(35, 20);
        bgColor.setFill(Color.web(theme.getBgColor()));
        bgColor.setArcWidth(4);
        bgColor.setArcHeight(4);
        bgColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", theme.getBorderColor()));
        bgColorBox.getChildren().addAll(bgLabel, bgColor);
        colorGrid.add(bgColorBox, 0, 0);
        
        // 面板背景色
        VBox panelColorBox = new VBox(3);
        Label panelLabel = new Label("面板");
        panelLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getFontFamily(), theme.getTextSecondaryColor()
        ));
        panelLabel.setAlignment(Pos.CENTER);
        Rectangle panelColor = new Rectangle(35, 20);
        panelColor.setFill(Color.web(theme.getPanelBgColor()));
        panelColor.setArcWidth(4);
        panelColor.setArcHeight(4);
        panelColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", theme.getBorderColor()));
        panelColorBox.getChildren().addAll(panelLabel, panelColor);
        colorGrid.add(panelColorBox, 1, 0);
        
        // 主色调
        VBox accentColorBox = new VBox(3);
        Label accentLabel = new Label("主色");
        accentLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getFontFamily(), theme.getTextSecondaryColor()
        ));
        accentLabel.setAlignment(Pos.CENTER);
        Rectangle accentColor = new Rectangle(35, 20);
        accentColor.setFill(Color.web(theme.getAccentColor()));
        accentColor.setArcWidth(4);
        accentColor.setArcHeight(4);
        accentColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", theme.getBorderColor()));
        accentColorBox.getChildren().addAll(accentLabel, accentColor);
        colorGrid.add(accentColorBox, 2, 0);
        
        // 文本主色
        VBox textColorBox = new VBox(3);
        Label textLabel = new Label("文本");
        textLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getFontFamily(), theme.getTextSecondaryColor()
        ));
        textLabel.setAlignment(Pos.CENTER);
        Rectangle textColor = new Rectangle(35, 20);
        textColor.setFill(Color.web(theme.getTextPrimaryColor()));
        textColor.setArcWidth(4);
        textColor.setArcHeight(4);
        textColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", theme.getBorderColor()));
        textColorBox.getChildren().addAll(textLabel, textColor);
        colorGrid.add(textColorBox, 3, 0);
        
        // 按钮样式预览
        VBox buttonPreviewBox = new VBox(5);
        Label buttonLabel = new Label("按钮样式");
        buttonLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 12px; -fx-text-fill: %s; -fx-font-weight: bold;",
                theme.getFontFamily(), theme.getTextPrimaryColor()
        ));
        buttonLabel.setAlignment(Pos.CENTER);
        
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER);
        
        // 主要按钮
        Button primaryButton = new Button("主按钮");
        primaryButton.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 4; -fx-font-size: 10px; -fx-min-width: 60; -fx-min-height: 25;",
                theme.getButtonPrimaryBgColor(), theme.getButtonPrimaryTextColor()
        ));
        
        // 次要按钮
        Button secondaryButton = new Button("次按钮");
        secondaryButton.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-border-color: %s; -fx-background-radius: 4; -fx-font-size: 10px; -fx-min-width: 60; -fx-min-height: 25;",
                theme.getButtonSecondaryBgColor(), theme.getButtonSecondaryTextColor(), theme.getButtonSecondaryBorderColor()
        ));
        
        buttonBox.getChildren().addAll(primaryButton, secondaryButton);
        buttonPreviewBox.getChildren().addAll(buttonLabel, buttonBox);
        
        // 列表样式预览
        VBox listPreviewBox = new VBox(5);
        Label listLabel = new Label("列表样式");
        listLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 12px; -fx-text-fill: %s; -fx-font-weight: bold;",
                theme.getFontFamily(), theme.getTextPrimaryColor()
        ));
        listLabel.setAlignment(Pos.CENTER);
        
        VBox listPreview = new VBox();
        listPreview.setPrefWidth(160);
        listPreview.setPrefHeight(40);
        listPreview.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1px; -fx-background-radius: 4;",
                theme.getListBgColor(), theme.getBorderColor()
        ));
        
        // 列表行预览
        HBox listRow1 = new HBox(5);
        listRow1.setPadding(new Insets(3, 5, 3, 5));
        listRow1.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: transparent transparent %s transparent; -fx-border-width: 0 0 1px 0;",
                theme.getListRowOddBgColor(), theme.getBorderColor()
        ));
        Label listItem1 = new Label("列表项 1");
        listItem1.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getFontFamily(), theme.getTextPrimaryColor()
        ));
        listRow1.getChildren().add(listItem1);
        
        HBox listRow2 = new HBox(5);
        listRow2.setPadding(new Insets(3, 5, 3, 5));
        listRow2.setStyle(String.format(
                "-fx-background-color: %s;",
                theme.getListRowEvenBgColor()
        ));
        Label listItem2 = new Label("列表项 2");
        listItem2.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getFontFamily(), theme.getTextPrimaryColor()
        ));
        listRow2.getChildren().add(listItem2);
        
        listPreview.getChildren().addAll(listRow1, listRow2);
        listPreviewBox.getChildren().addAll(listLabel, listPreview);
        
        // 添加到卡片
        card.getChildren().addAll(colorGrid, buttonPreviewBox, listPreviewBox);
        
        // 添加选中效果
        if (isCurrentTheme(theme)) {
            card.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 15, 0, 0, 0); -fx-border-color: %s; -fx-border-width: 2px;",
                    theme.getPanelBgColor(), theme.getAccentColor()
            ));
        }
        
        return card;
    }
    
    /**
     * 检查是否为当前主题
     */
    private boolean isCurrentTheme(ThemeConfig theme) {
        // 比较更多关键属性以确保准确性
        return theme.getAccentColor().equals(currentTheme.getAccentColor()) &&
               theme.getBgColor().equals(currentTheme.getBgColor()) &&
               theme.getPanelBgColor().equals(currentTheme.getPanelBgColor()) &&
               theme.getTextPrimaryColor().equals(currentTheme.getTextPrimaryColor()) &&
               theme.isDarkBackground() == currentTheme.isDarkBackground();
    }
    
    /**
     * 应用主题
     */
    private void applyTheme(ThemeConfig theme) {
        currentTheme.setBgColor(theme.getBgColor());
        currentTheme.setAccentColor(theme.getAccentColor());
        currentTheme.setTextPrimaryColor(theme.getTextPrimaryColor());
        currentTheme.setTextSecondaryColor(theme.getTextSecondaryColor());
        currentTheme.setTextTertiaryColor(theme.getTextTertiaryColor());
        currentTheme.setTextDisabledColor(theme.getTextDisabledColor());
        currentTheme.setGlassOpacity(theme.getGlassOpacity());
        currentTheme.setDarkBackground(theme.isDarkBackground());
        currentTheme.setCornerRadius(theme.getCornerRadius());
        currentTheme.setBorderColor(theme.getBorderColor());
        currentTheme.setPanelBgColor(theme.getPanelBgColor());
        currentTheme.setPanelBorderColor(theme.getPanelBorderColor());
        currentTheme.setHoverColor(theme.getHoverColor());
        currentTheme.setSelectedColor(theme.getSelectedColor());
        currentTheme.setDisabledColor(theme.getDisabledColor());
        currentTheme.setProgressBarColor(theme.getProgressBarColor());
        currentTheme.setSuccessColor(theme.getSuccessColor());
        currentTheme.setWarningColor(theme.getWarningColor());
        currentTheme.setErrorColor(theme.getErrorColor());
        currentTheme.setInfoColor(theme.getInfoColor());
        
        applyAppearance();
    }
    
    /**
     * 刷新颜色选择器
     */
    private void refreshColorPickers() {
        // 由于颜色选择器是在对话框中动态创建的，这里不需要实现具体逻辑
        // 实际应用中可以通过绑定或事件监听来实现
    }
    
    public void applyAppearance() {
        // 重新初始化样式工厂
        StyleFactory.initStyleFactory(currentTheme);
        
        // 更新背景覆盖层
        backgroundOverlay.setStyle("-fx-background-color: rgba(" + 
                (currentTheme.isDarkBackground() ? "0,0,0" : "255,255,255") + 
                ", " + (1 - currentTheme.getGlassOpacity()) + ");");
        
        // 更新背景图片
        if (!currentTheme.getBgImagePath().isEmpty()) {
            try {
                File bgImageFile = new File(currentTheme.getBgImagePath());
                if (bgImageFile.exists() && bgImageFile.isFile()) {
                    // 使用file:// URL方式加载图片，处理特殊字符和空格
                    String imageUrl = bgImageFile.toURI().toURL().toExternalForm();
                    
                    // 创建图片对象并设置加载参数
                    Image image = new Image(imageUrl, true); // 启用异步加载
                    
                    // 设置图片加载完成和错误处理回调
                    image.errorProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal) {
                            app.logError("背景图加载失败：图片文件可能损坏或格式不支持");
                            backgroundImageView.setImage(null);
                        }
                    });
                    
                    // 设置图片缩放模式
                    backgroundImageView.setPreserveRatio(false);
                    backgroundImageView.setFitWidth(-1); // 自动适应容器宽度
                    backgroundImageView.setFitHeight(-1); // 自动适应容器高度
                    backgroundImageView.setStyle("-fx-background-size: cover; -fx-background-position: center; -fx-background-repeat: no-repeat;");
                    
                    backgroundImageView.setImage(image);
                } else {
                    app.logError("背景图文件不存在：" + currentTheme.getBgImagePath());
                    backgroundImageView.setImage(null);
                }
            } catch (Exception e) {
                app.logError("背景图加载失败：" + e.getMessage());
                backgroundImageView.setImage(null);
            }
        } else {
            backgroundImageView.setImage(null);
        }
        
        // 递归更新所有界面元素的样式
        if (app != null && app instanceof com.filemanager.app.base.IUIElementProvider) {
            com.filemanager.app.base.IUIElementProvider uiElementProvider = (com.filemanager.app.base.IUIElementProvider) app;
            javafx.scene.layout.StackPane rootContainer = uiElementProvider.getRootContainer();
            if (rootContainer != null) {
                com.filemanager.app.tools.display.StyleFactory.updateNodeStyle(rootContainer);
            }
        }
        
        // 调用所有IAutoReloadAble组件的reload方法
        if (app != null && app instanceof com.filemanager.app.base.IAutoReloadAbleProvider) {
            com.filemanager.app.base.IAutoReloadAbleProvider reloadableProvider = (com.filemanager.app.base.IAutoReloadAbleProvider) app;
            List<com.filemanager.app.base.IAutoReloadAble> reloadableNodes = reloadableProvider.getAutoReloadNodes();
            if (reloadableNodes != null) {
                for (com.filemanager.app.base.IAutoReloadAble reloadable : reloadableNodes) {
                    reloadable.reload();
                }
            }
        }
    }
}