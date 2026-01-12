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
    
    public AppearanceManager(IAppController app, ThemeConfig currentTheme,
                           ImageView backgroundImageView, Region backgroundOverlay) {
        this.app = app;
        this.currentTheme = currentTheme;
        this.backgroundImageView = backgroundImageView;
        this.backgroundOverlay = backgroundOverlay;
        this.themeManager = ThemeManager.getInstance();
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
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: 40; -fx-tab-max-height: 40;",
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor(), currentTheme.getBorderWidth()
        ));
        
        // 主题预设选项卡
        Tab presetTab = new Tab("主题预设");
        presetTab.setContent(createPresetTabContent());
        tabPane.getTabs().add(presetTab);
        
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
                theme.setTextColor(currentTheme.getTextColor());
                theme.setGlassOpacity(currentTheme.getGlassOpacity());
                theme.setDarkBackground(currentTheme.isDarkBackground());
                theme.setPanelBgColor(currentTheme.getPanelBgColor());
                theme.setFontFamily(currentTheme.getFontFamily());
                theme.setFontSize(currentTheme.getFontSize());
                theme.setLargeButtonSize(currentTheme.getLargeButtonSize());
                theme.setSmallButtonSize(currentTheme.getSmallButtonSize());
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
        
        // 获取所有主题预设
        Map<String, ThemeConfig> presets = themeManager.getThemePresets();
        
        // 使用网格布局展示主题卡片
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(10, 0, 10, 0));
        
        // 保存所有主题卡片的引用，用于后续更新选中状态
        final java.util.List<VBox> themeCards = new java.util.ArrayList<>();
        final java.util.List<ThemeConfig> themes = new java.util.ArrayList<>();
        
        int col = 0;
        int row = 0;
        for (Map.Entry<String, ThemeConfig> entry : presets.entrySet()) {
            String themeName = entry.getKey();
            ThemeConfig theme = entry.getValue();
            
            // 创建主题预览卡片
            VBox themeCard = createThemeCard(themeName, theme);
            themeCard.setCursor(Cursor.HAND);
            
            // 保存引用
            themeCards.add(themeCard);
            themes.add(theme);
            
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
            if (col > 4) { // 每行显示5个卡片
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
        ColorPicker textPicker = new ColorPicker(Color.web(currentTheme.getTextColor()));
        textPicker.setOnAction(e -> {
            currentTheme.setTextColor(String.format("#%02X%02X%02X", 
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
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius(), currentTheme.getTextColor(), 
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
        
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentTheme.setGlassOpacity(newVal.doubleValue());
            opacityValue.setText(String.format("%.2f", newVal.doubleValue()));
            applyAppearance();
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
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius(), currentTheme.getTextColor(), 
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
        JFXSlider largeButtonSlider = new JFXSlider(60, 100, currentTheme.getLargeButtonSize());
        largeButtonSlider.setPrefWidth(400);
        largeButtonSlider.setMajorTickUnit(5);
        largeButtonSlider.setMinorTickCount(0);
        largeButtonSlider.setShowTickLabels(true);
        largeButtonSlider.setShowTickMarks(false);
        
        Label largeButtonValue = StyleFactory.createLabel(String.format("%.1f", currentTheme.getLargeButtonSize()), 14, false);
        largeButtonValue.setMinWidth(50);
        
        largeButtonSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentTheme.setLargeButtonSize(newVal.doubleValue());
            largeButtonValue.setText(String.format("%.1f", newVal.doubleValue()));
            applyAppearance();
        });
        
        largeButtonBox.getChildren().addAll(largeButtonLabel, largeButtonSlider, largeButtonValue);
        content.getChildren().add(largeButtonBox);
        
        // 小按钮大小设置
        HBox smallButtonBox = new HBox(20);
        smallButtonBox.setAlignment(Pos.CENTER_LEFT);
        Label smallButtonLabel = StyleFactory.createLabel("小按钮大小", 14, false);
        JFXSlider smallButtonSlider = new JFXSlider(40, 80, currentTheme.getSmallButtonSize());
        smallButtonSlider.setPrefWidth(400);
        smallButtonSlider.setMajorTickUnit(5);
        smallButtonSlider.setMinorTickCount(0);
        smallButtonSlider.setShowTickLabels(true);
        smallButtonSlider.setShowTickMarks(false);
        
        Label smallButtonValue = StyleFactory.createLabel(String.format("%.1f", currentTheme.getSmallButtonSize()), 14, false);
        smallButtonValue.setMinWidth(50);
        
        smallButtonSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentTheme.setSmallButtonSize(newVal.doubleValue());
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
        
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
        JFXButton saveBtn = StyleFactory.createActionButton("保存当前样式", "#27ae60", () -> {
            // 实现保存样式功能
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存样式");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("样式文件", "*.json"));
            fileChooser.setInitialDirectory(new File("style"));
            
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                themeManager.saveThemeToFile(file, currentTheme);
            }
        });
        
        JFXButton loadBtn = StyleFactory.createActionButton("加载样式文件", "#3498db", () -> {
            // 实现加载样式功能
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("加载样式");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("样式文件", "*.json"));
            fileChooser.setInitialDirectory(new File("style"));
            
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                ThemeConfig theme = themeManager.loadThemeFromFile(file);
                if (theme != null) {
                    applyTheme(theme);
                    refreshColorPickers();
                }
            }
        });
        
        buttonBox.getChildren().addAll(saveBtn, loadBtn);
        content.getChildren().add(buttonBox);
        
        return content;
    }
    
    /**
     * 创建主题预览卡片
     */
    private VBox createThemeCard(String themeName, ThemeConfig theme) {
        VBox card = new VBox(8);
        card.setPrefWidth(100); // 减小卡片宽度
        card.setPrefHeight(130); // 减小卡片高度
        card.setPadding(new Insets(8)); // 减小内边距
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 0);");
        
        // 主题名称
        Label nameLabel = StyleFactory.createLabel(themeName, 12, true); // 减小字体大小
        nameLabel.setAlignment(Pos.CENTER);
        card.getChildren().add(nameLabel);
        
        // 主题颜色预览
        Rectangle accentColor = new Rectangle(80, 16); // 减小预览矩形尺寸
        accentColor.setFill(Color.web(theme.getAccentColor()));
        accentColor.setArcWidth(4);
        accentColor.setArcHeight(4);
        card.getChildren().add(accentColor);
        
        Rectangle bgColor = new Rectangle(80, 16); // 减小预览矩形尺寸
        bgColor.setFill(Color.web(theme.getBgColor()));
        bgColor.setArcWidth(4);
        bgColor.setArcHeight(4);
        card.getChildren().add(bgColor);
        
        Rectangle panelColor = new Rectangle(80, 16); // 减小预览矩形尺寸
        panelColor.setFill(Color.web(theme.getPanelBgColor()));
        panelColor.setArcWidth(5);
        panelColor.setArcHeight(5);
        card.getChildren().add(panelColor);
        
        // 添加选中效果
        if (isCurrentTheme(theme)) {
            card.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0); -fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 10;");
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
               theme.getTextColor().equals(currentTheme.getTextColor()) &&
               theme.isDarkBackground() == currentTheme.isDarkBackground();
    }
    
    /**
     * 应用主题
     */
    private void applyTheme(ThemeConfig theme) {
        currentTheme.setBgColor(theme.getBgColor());
        currentTheme.setAccentColor(theme.getAccentColor());
        currentTheme.setTextColor(theme.getTextColor());
        currentTheme.setLightTextColor(theme.getLightTextColor());
        currentTheme.setDisabledTextColor(theme.getDisabledTextColor());
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
                backgroundImageView.setImage(new Image(Files.newInputStream(Paths.get(currentTheme.getBgImagePath()))));
            } catch (Exception e) {
                app.logError("背景图加载失败：" + e.getMessage());
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
    }
}