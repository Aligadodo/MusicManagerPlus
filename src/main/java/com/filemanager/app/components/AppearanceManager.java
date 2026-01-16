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
import javafx.scene.shape.Circle;
import javafx.scene.Node;
import javafx.geometry.Point2D;
import java.util.function.BiConsumer;
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
    
    // 保存示例按钮引用，用于样式更新
    private Button largeButtonExample;
    private Button smallButtonExample;
    
    // 跟踪主题是否被修改
    private boolean isThemeModified = false;
    
    public AppearanceManager(IAppController app, ThemeConfig currentTheme,
                           ImageView backgroundImageView, Region backgroundOverlay) {
        this.app = app;
        this.currentTheme = currentTheme;
        this.backgroundImageView = backgroundImageView;
        this.backgroundOverlay = backgroundOverlay;
        this.themeManager = ThemeManager.getInstance();
        this.templateManager = StyleTemplateManager.getInstance();
    }
    
    /**
     * 获取界面设置内容
     */
    public Node getAppearanceSettingsContent() {
        // 创建主容器
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setStyle(String.format("-fx-background-color: %s;", currentTheme.getBgColor()));
        mainContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        
        // 创建TabPane来组织不同的设置类别
        TabPane tabPane = new TabPane();
        tabPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-tab-min-height: 40; -fx-tab-max-height: 40; -fx-tab-min-width: 100; -fx-tab-max-width: 200;\n" +
                ".tab-pane > .tab-header-area {\n" +
                "    -fx-background-color: transparent;\n" +
                "}\n" +
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
                "    -fx-cursor: hand;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab:hover {\n" +
                "    -fx-background-color: %s;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab:selected {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-border-color: %s %s %s %s;\n" +
                "    -fx-border-width: %.1f %.1f 0 %.1f;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab > .tab-container > .tab-label {\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-font-family: %s;\n" +
                "    -fx-font-size: 14px;\n" +
                "    -fx-font-weight: normal;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab:hover > .tab-container > .tab-label {\n" +
                "    -fx-text-fill: %s;\n" +
                "}\n" +
                ".tab-pane > .tab-header-area > .headers-region > .tab:selected > .tab-container > .tab-label {\n" +
                "    -fx-text-fill: %s;\n" +
                "    -fx-font-weight: bold;\n" +
                "}\n" +
                ".tab-pane > .tab-content-area {\n" +
                "    -fx-background-color: %s;\n" +
                "    -fx-border-color: %s;\n" +
                "    -fx-border-width: %.1f;\n" +
                "    -fx-border-radius: 0 %.1f %.1f %.1f;\n" +
                "}",
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor(), currentTheme.getBorderWidth(),
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor(), currentTheme.getBorderWidth(),
                currentTheme.getBgColor(), currentTheme.getBorderColor(), currentTheme.getBorderWidth(), currentTheme.getBorderWidth(), currentTheme.getBorderWidth(), currentTheme.getCornerRadius(), currentTheme.getCornerRadius(),
                currentTheme.getPanelHoverColor(),
                currentTheme.getPanelBgColor(), currentTheme.getAccentColor(), currentTheme.getAccentColor(), currentTheme.getPanelBgColor(), currentTheme.getAccentColor(), currentTheme.getBorderWidth(), currentTheme.getBorderWidth(), currentTheme.getBorderWidth(),
                currentTheme.getTextSecondaryColor(), currentTheme.getFontFamily(),
                currentTheme.getTextPrimaryColor(),
                currentTheme.getAccentColor(),
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor(), currentTheme.getBorderWidth(), currentTheme.getCornerRadius(), currentTheme.getCornerRadius(), currentTheme.getCornerRadius()
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
        styleManageTab.setContent(createStyleTabContent());
        tabPane.getTabs().add(styleManageTab);
        
        mainContainer.getChildren().add(tabPane);
        
        // 添加滚动容器
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        return scrollPane;
    }
    
    public void showAppearanceDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("外观设置");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(900);
        dialog.getDialogPane().setPrefHeight(700);
        
        // 设置对话框内容
        dialog.getDialogPane().setContent(getAppearanceSettingsContent());
        
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
                
                // 添加列表样式属性的更新
                theme.setListBgColor(currentTheme.getListBgColor());
                theme.setListRowEvenBgColor(currentTheme.getListRowEvenBgColor());
                theme.setListRowOddBgColor(currentTheme.getListRowOddBgColor());
                theme.setListRowSelectedBgColor(currentTheme.getListRowSelectedBgColor());
                theme.setListRowSelectedTextColor(currentTheme.getListRowSelectedTextColor());
                theme.setListRowHoverBgColor(currentTheme.getListRowHoverBgColor());
                theme.setListBorderColor(currentTheme.getListBorderColor());
                theme.setListHeaderBgColor(currentTheme.getListHeaderBgColor());
                theme.setListHeaderTextColor(currentTheme.getListHeaderTextColor());
            });
            
            // 如果主题被修改，自动保存为自定义主题
            if (isThemeModified) {
                // 创建自定义主题
                ThemeConfig customTheme = currentTheme.clone();
                customTheme.setTemplateName("自定义主题");
                customTheme.setTemplateDescription("用户自定义的主题");
                
                // 保存自定义主题
                templateManager.saveTemplate(customTheme);
                
                // 更新主题预设选项卡的内容，显示最新的主题列表
                if (finalPresetTab != null) {
                    finalPresetTab.setContent(createPresetTabContent());
                }
                
                // 重置修改标志
                isThemeModified = false;
            }
            
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
        grid.setHgap(20); // 减小水平间距
        grid.setVgap(20); // 减小垂直间距
        grid.setPadding(new Insets(10, 0, 10, 0));
        
        // 保存所有主题卡片的引用，用于后续更新选中状态
        final java.util.List<VBox> themeCards = new java.util.ArrayList<>();
        final java.util.List<ThemeConfig> themes = new java.util.ArrayList<>();
        
        // 创建主题卡片并保存引用
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
        }
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        // 动态调整每行显示的卡片数量
        scrollPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            // 根据滚动面板宽度动态计算每行显示的卡片数量
            double cardWidth = 180 + grid.getHgap(); // 卡片宽度 + 间距
            int maxCols = (int) Math.floor((newValue.doubleValue() - 40) / cardWidth); // 40是左右边距
            if (maxCols < 1) maxCols = 1;
            
            // 重新布局卡片
            grid.getChildren().clear();
            int col = 0;
            int row = 0;
            for (int i = 0; i < themeCards.size(); i++) {
                grid.add(themeCards.get(i), col, row);
                col++;
                if (col >= maxCols) {
                    col = 0;
                    row++;
                }
            }
        });
        
        // 初始布局
        double initialWidth = 800; // 估计初始宽度
        double cardWidth = 180 + grid.getHgap();
        int initialCols = (int) Math.floor((initialWidth - 40) / cardWidth);
        if (initialCols < 1) initialCols = 1;
        
        int col = 0;
        int row = 0;
        for (int i = 0; i < themeCards.size(); i++) {
            grid.add(themeCards.get(i), col, row);
            col++;
            if (col >= initialCols) {
                col = 0;
                row++;
            }
        }
        
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
            
            // 重新应用预设主题的样式，而不是硬编码为白色
            if (isCurrentTheme(theme)) {
                // 当前主题卡片，添加选中效果
                card.setStyle(String.format(
                        "-fx-background-color: %s; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 15, 0, 0, 0); -fx-border-color: %s; -fx-border-width: 2px;",
                        theme.getPanelBgColor(), theme.getAccentColor()
                ));
            } else {
                // 非当前主题卡片，应用预设主题的基本样式
                card.setStyle(String.format(
                        "-fx-background-color: %s; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0);",
                        theme.getPanelBgColor()
                ));
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
        ColorPicker accentPicker = new ColorPicker(Color.web(validateColor(currentTheme.getAccentColor(), "#3498db")));
        accentPicker.setOpacity(1.0); // 主题色通常不透明
        accentPicker.setOnAction(e -> {
            Color color = accentPicker.getValue();
            currentTheme.setAccentColor(String.format("#%02X%02X%02X%02X", 
                    (int) (color.getRed() * 255), 
                    (int) (color.getGreen() * 255), 
                    (int) (color.getBlue() * 255),
                    (int) (color.getOpacity() * 255)));
            isThemeModified = true;
            applyAppearance();
        });
        colorGrid.add(accentLabel, 0, 0);
        colorGrid.add(accentPicker, 1, 0);
        
        // 文本颜色
        Label textLabel = StyleFactory.createLabel("文本颜色", 14, false);
        ColorPicker textPicker = new ColorPicker(Color.web(validateColor(currentTheme.getTextPrimaryColor(), "#2c3e50")));
        textPicker.setOpacity(1.0); // 文本颜色通常不透明
        textPicker.setOnAction(e -> {
            Color color = textPicker.getValue();
            currentTheme.setTextPrimaryColor(String.format("#%02X%02X%02X%02X", 
                    (int) (color.getRed() * 255), 
                    (int) (color.getGreen() * 255), 
                    (int) (color.getBlue() * 255),
                    (int) (color.getOpacity() * 255)));
            isThemeModified = true;
            applyAppearance();
        });
        colorGrid.add(textLabel, 2, 0);
        colorGrid.add(textPicker, 3, 0);
        
        // 背景色
        Label bgLabel = StyleFactory.createLabel("背景色", 14, false);
        ColorPicker bgPicker = new ColorPicker(Color.web(validateColor(currentTheme.getBgColor(), "#f5f5f5")));
        bgPicker.setOnAction(e -> {
            Color color = bgPicker.getValue();
            currentTheme.setBgColor(String.format("#%02X%02X%02X%02X", 
                    (int) (color.getRed() * 255), 
                    (int) (color.getGreen() * 255), 
                    (int) (color.getBlue() * 255),
                    (int) (color.getOpacity() * 255)));
            isThemeModified = true;
            applyAppearance();
        });
        colorGrid.add(bgLabel, 0, 1);
        colorGrid.add(bgPicker, 1, 1);
        
        // 面板背景色
        Label panelLabel = StyleFactory.createLabel("面板背景色", 14, false);
        ColorPicker panelPicker = new ColorPicker(Color.web(validateColor(currentTheme.getPanelBgColor(), "#ffffff")));
        panelPicker.setOnAction(e -> {
            Color color = panelPicker.getValue();
            currentTheme.setPanelBgColor(String.format("#%02X%02X%02X%02X", 
                    (int) (color.getRed() * 255), 
                    (int) (color.getGreen() * 255), 
                    (int) (color.getBlue() * 255),
                    (int) (color.getOpacity() * 255)));
            isThemeModified = true;
            applyAppearance();
        });
        colorGrid.add(panelLabel, 2, 1);
        colorGrid.add(panelPicker, 3, 1);
        
        // 边框颜色
        Label borderLabel = StyleFactory.createLabel("边框颜色", 14, false);
        ColorPicker borderPicker = new ColorPicker(Color.web(validateColor(currentTheme.getBorderColor(), "#e0e0e0")));
        borderPicker.setOnAction(e -> {
            Color color = borderPicker.getValue();
            currentTheme.setBorderColor(String.format("#%02X%02X%02X%02X", 
                    (int) (color.getRed() * 255), 
                    (int) (color.getGreen() * 255), 
                    (int) (color.getBlue() * 255),
                    (int) (color.getOpacity() * 255)));
            isThemeModified = true;
            applyAppearance();
        });
        colorGrid.add(borderLabel, 0, 2);
        colorGrid.add(borderPicker, 1, 2);
        
        // 悬停颜色
        Label hoverLabel = StyleFactory.createLabel("悬停颜色", 14, false);
        ColorPicker hoverPicker = new ColorPicker(Color.web(validateColor(currentTheme.getHoverColor(), "#f5f5f5")));
        hoverPicker.setOnAction(e -> {
            Color color = hoverPicker.getValue();
            currentTheme.setHoverColor(String.format("#%02X%02X%02X%02X", 
                    (int) (color.getRed() * 255), 
                    (int) (color.getGreen() * 255), 
                    (int) (color.getBlue() * 255),
                    (int) (color.getOpacity() * 255)));
            isThemeModified = true;
            applyAppearance();
        });
        colorGrid.add(hoverLabel, 2, 2);
        colorGrid.add(hoverPicker, 3, 2);
        
        content.getChildren().add(colorGrid);
        return content;
    }
    
    /**
     * 验证颜色值是否有效，如果无效则返回默认值
     */
    private String validateColor(String color, String defaultValue) {
        if (color == null || color.isEmpty()) {
            return defaultValue;
        }
        
        // 确保颜色值以#开头
        if (!color.startsWith("#")) {
            color = "#" + color;
        }
        
        // 确保颜色值有正确的长度 (7位: #RRGGBB 或 9位: #RRGGBBAA)
        if (color.length() != 7 && color.length() != 9) {
            return defaultValue;
        }
        
        // 验证颜色格式是否正确
        try {
            Color.web(color);
            return color;
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
    
    /**
     * 复制背景图到style/themes/images目录，并返回复制后的文件路径
     * @param originalPath 原始背景图路径
     * @return 复制后的背景图路径
     */
    private String copyBackgroundImage(String originalPath) {
        if (originalPath == null || originalPath.isEmpty()) {
            return "";
        }
        
        // 检查文件是否已经在style/themes/images目录下
        if (originalPath.contains("style/themes/images")) {
            return originalPath;
        }
        
        try {
            File originalFile = new File(originalPath);
            if (!originalFile.exists()) {
                return originalPath;
            }
            
            // 创建目标目录
            File targetDir = new File("style/themes/images");
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            
            // 创建目标文件
            String fileName = originalFile.getName();
            File targetFile = new File(targetDir, fileName);
            
            // 如果文件已经存在，检查内容是否相同
            if (targetFile.exists()) {
                byte[] originalBytes = Files.readAllBytes(originalFile.toPath());
                byte[] targetBytes = Files.readAllBytes(targetFile.toPath());
                
                if (java.util.Arrays.equals(originalBytes, targetBytes)) {
                    // 文件内容相同，直接返回目标路径
                    return targetFile.getAbsolutePath();
                } else {
                    // 文件内容不同，生成新的文件名
                    String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
                    String ext = fileName.substring(fileName.lastIndexOf('.'));
                    int counter = 1;
                    while (targetFile.exists()) {
                        fileName = nameWithoutExt + "_" + counter + ext;
                        targetFile = new File(targetDir, fileName);
                        counter++;
                    }
                }
            }
            
            // 复制文件
            Files.copy(originalFile.toPath(), targetFile.toPath());
            
            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return originalPath;
        }
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
                String copiedPath = copyBackgroundImage(f.getAbsolutePath());
                bgImagePath.setText(copiedPath);
                currentTheme.setBgImagePath(copiedPath);
                isThemeModified = true;
                applyAppearance();
            }
        });
        
        JFXButton clearBtn = StyleFactory.createActionButton("清除背景", "#e74c3c", () -> {
            bgImagePath.clear();
            currentTheme.setBgImagePath("");
            backgroundImageView.setImage(null);
            isThemeModified = true;
            applyAppearance();
        });
        
        imageBox.getChildren().addAll(bgImagePath, browseBtn, clearBtn);
        content.getChildren().add(imageBox);
        
        // 背景图显示模式选择
        VBox displayModeBox = new VBox(15);
        displayModeBox.setPadding(new Insets(10, 0, 0, 0));
        displayModeBox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: %.1f; -fx-border-color: %s; -fx-border-width: %.1f; -fx-padding: 20;",
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius(), currentTheme.getBorderColor(), currentTheme.getBorderWidth()));
        
        Label displayModeLabel = StyleFactory.createLabel("背景图显示模式", 16, true);
        displayModeBox.getChildren().add(displayModeLabel);
        
        HBox modeButtons = new HBox(10);
        
        JFXButton stretchBtn = StyleFactory.createActionButton("拉伸", "#3498db", () -> {
            // 设置背景图拉伸模式
            backgroundImageView.setPreserveRatio(false);
            backgroundImageView.setStyle("-fx-background-size: stretch; -fx-background-position: center; -fx-background-repeat: no-repeat;");
            applyAppearance();
        });
        
        JFXButton cropBtn = StyleFactory.createActionButton("裁剪", "#3498db", () -> {
            // 设置背景图裁剪模式
            backgroundImageView.setPreserveRatio(true);
            backgroundImageView.setStyle("-fx-background-size: cover; -fx-background-position: center; -fx-background-repeat: no-repeat;");
            applyAppearance();
        });
        
        JFXButton centerBtn = StyleFactory.createActionButton("居中", "#3498db", () -> {
            // 设置背景图居中模式
            backgroundImageView.setPreserveRatio(true);
            backgroundImageView.setStyle("-fx-background-size: contain; -fx-background-position: center; -fx-background-repeat: no-repeat;");
            applyAppearance();
        });
        
        modeButtons.getChildren().addAll(stretchBtn, cropBtn, centerBtn);
        displayModeBox.getChildren().add(modeButtons);
        
        // 背景图裁切功能
        JFXButton cropImageBtn = StyleFactory.createActionButton("裁切背景图", "#e67e22", () -> {
            if (!currentTheme.getBgImagePath().isEmpty()) {
                // 显示裁切对话框
                showCropBackgroundDialog();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText(null);
                alert.setContentText("请先选择一张背景图");
                alert.showAndWait();
            }
        });
        displayModeBox.getChildren().add(cropImageBtn);
        
        content.getChildren().add(displayModeBox);
        
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
        
        // 背景图选择面板
        VBox bgImagePanel = new VBox(15);
        bgImagePanel.setPadding(new Insets(20, 0, 0, 0));
        bgImagePanel.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-width: 1px;",
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor()
        ));
        
        Label bgImagePanelTitle = new Label("选择背景图");
        bgImagePanelTitle.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s;",
                currentTheme.getFontFamily(), currentTheme.getTextPrimaryColor()
        ));
        bgImagePanelTitle.setPadding(new Insets(10, 0, 0, 10));
        bgImagePanel.getChildren().add(bgImagePanelTitle);
        
        // 背景图显示区域
        FlowPane bgImageFlow = new FlowPane();
        bgImageFlow.setPadding(new Insets(10));
        bgImageFlow.setHgap(15);
        bgImageFlow.setVgap(15);
        
        // 确保style/themes/images目录存在
        File imagesDir = new File("style/themes/images");
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
        
        // 加载style/themes/images目录下的所有背景图
        if (imagesDir.exists() && imagesDir.isDirectory()) {
            File[] imageFiles = imagesDir.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
                       lowerName.endsWith(".png") || lowerName.endsWith(".svg") || 
                       lowerName.endsWith(".gif");
            });
            
            if (imageFiles != null && imageFiles.length > 0) {
                for (File imageFile : imageFiles) {
                    try {
                        // 创建缩略图
                        Image image = new Image(imageFile.toURI().toURL().toExternalForm(), 120, 80, true, true);
                        ImageView imageView = new ImageView(image);
                        imageView.setPreserveRatio(true);
                        imageView.setFitWidth(120);
                        imageView.setFitHeight(80);
                        imageView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0px;");
                        
                        // 创建按钮
                        Button imageButton = new Button();
                        imageButton.setGraphic(imageView);
                        imageButton.setPrefSize(130, 90);
                        imageButton.setStyle(String.format(
                                "-fx-background-color: transparent; -fx-background-radius: 4; -fx-border-color: %s; -fx-border-width: 1px; -fx-content-display: graphic-only; -fx-padding: 5px;",
                                currentTheme.getBorderColor()
                        ));
                        imageButton.setAlignment(Pos.CENTER);
                        imageButton.setOnAction(e -> {
                            String imagePath = imageFile.getAbsolutePath();
                            bgImagePath.setText(imagePath);
                            currentTheme.setBgImagePath(imagePath);
                            isThemeModified = true;
                            applyAppearance();
                        });
                        
                        bgImageFlow.getChildren().add(imageButton);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // 如果没有图片，显示提示信息
                Label noImagesLabel = StyleFactory.createLabel("style/themes/images目录下没有图片文件，请先添加背景图片", 14, false);
                noImagesLabel.setWrapText(true);
                noImagesLabel.setMaxWidth(400);
                noImagesLabel.setStyle("-fx-text-fill: #e74c3c;");
                bgImageFlow.getChildren().add(noImagesLabel);
            }
        }
        
        bgImagePanel.getChildren().add(bgImageFlow);
        content.getChildren().add(bgImagePanel);
        
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
        JFXSlider fontSizeSlider = new JFXSlider(10, 22, Math.round(currentTheme.getFontSize()));
        fontSizeSlider.setPrefWidth(400);
        fontSizeSlider.setMajorTickUnit(1);
        fontSizeSlider.setMinorTickCount(0);
        fontSizeSlider.setShowTickLabels(true);
        fontSizeSlider.setShowTickMarks(false);
        fontSizeSlider.setSnapToTicks(true); // 自动对齐到整数刻度
        
        Label fontSizeValue = StyleFactory.createLabel(String.format("%d", Math.round(currentTheme.getFontSize())), 14, false);
        fontSizeValue.setMinWidth(50);
        
        fontSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int intValue = (int) Math.round(newVal.doubleValue());
            currentTheme.setFontSize(intValue);
            fontSizeValue.setText(String.format("%d", intValue));
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
        
        // 按钮颜色设置
        Label buttonColorTitle = new Label("按钮颜色设置");
        buttonColorTitle.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: %s;",
                currentTheme.getFontFamily(), currentTheme.getTextPrimaryColor()
        ));
        content.getChildren().add(buttonColorTitle);
        
        // 按钮背景色
        HBox buttonBgColorBox = new HBox(20);
        buttonBgColorBox.setAlignment(Pos.CENTER_LEFT);
        Label buttonBgColorLabel = StyleFactory.createLabel("按钮背景色", 14, false);
        ColorPicker buttonBgColorPicker = new ColorPicker(Color.web(validateColor(currentTheme.getButtonBgColor(), "#3498db")));
        buttonBgColorPicker.setOnAction(e -> {
            Color color = buttonBgColorPicker.getValue();
            currentTheme.setButtonBgColor(String.format("#%02X%02X%02X%02X", 
                    (int) (color.getRed() * 255), 
                    (int) (color.getGreen() * 255), 
                    (int) (color.getBlue() * 255),
                    (int) (color.getOpacity() * 255)));
            isThemeModified = true;
            updateButtonExamples(); // 更新示例按钮
        });
        buttonBgColorBox.getChildren().addAll(buttonBgColorLabel, buttonBgColorPicker);
        content.getChildren().add(buttonBgColorBox);
        
        // 按钮文字颜色
        HBox buttonTextColorBox = new HBox(20);
        buttonTextColorBox.setAlignment(Pos.CENTER_LEFT);
        Label buttonTextColorLabel = StyleFactory.createLabel("按钮文字颜色", 14, false);
        ColorPicker buttonTextColorPicker = new ColorPicker(Color.web(validateColor(currentTheme.getButtonTextColor(), "#ffffff")));
        buttonTextColorPicker.setOnAction(e -> {
            Color color = buttonTextColorPicker.getValue();
            currentTheme.setButtonTextColor(String.format("#%02X%02X%02X%02X", 
                    (int) (color.getRed() * 255), 
                    (int) (color.getGreen() * 255), 
                    (int) (color.getBlue() * 255),
                    (int) (color.getOpacity() * 255)));
            isThemeModified = true;
            updateButtonExamples(); // 更新示例按钮
        });
        buttonTextColorBox.getChildren().addAll(buttonTextColorLabel, buttonTextColorPicker);
        content.getChildren().add(buttonTextColorBox);
        
        // 按钮边框颜色
        HBox buttonBorderColorBox = new HBox(20);
        buttonBorderColorBox.setAlignment(Pos.CENTER_LEFT);
        Label buttonBorderColorLabel = StyleFactory.createLabel("按钮边框颜色", 14, false);
        ColorPicker buttonBorderColorPicker = new ColorPicker(Color.web(validateColor(currentTheme.getButtonBorderColor(), "#2980b9")));
        buttonBorderColorPicker.setOnAction(e -> {
            Color color = buttonBorderColorPicker.getValue();
            currentTheme.setButtonBorderColor(String.format("#%02X%02X%02X%02X", 
                    (int) (color.getRed() * 255), 
                    (int) (color.getGreen() * 255), 
                    (int) (color.getBlue() * 255),
                    (int) (color.getOpacity() * 255)));
            isThemeModified = true;
            updateButtonExamples(); // 更新示例按钮
        });
        buttonBorderColorBox.getChildren().addAll(buttonBorderColorLabel, buttonBorderColorPicker);
        content.getChildren().add(buttonBorderColorBox);
        
        // 按钮大小设置
        Label buttonSizeTitle = new Label("按钮大小设置");
        buttonSizeTitle.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: %s;",
                currentTheme.getFontFamily(), currentTheme.getTextPrimaryColor()
        ));
        content.getChildren().add(buttonSizeTitle);
        
        // 大按钮大小设置
        HBox largeButtonBox = new HBox(20);
        largeButtonBox.setAlignment(Pos.CENTER_LEFT);
        Label largeButtonLabel = StyleFactory.createLabel("大按钮大小", 14, false);
        JFXSlider largeButtonSlider = new JFXSlider(60, 100, Math.round(currentTheme.getButtonLargeSize()));
        largeButtonSlider.setPrefWidth(400);
        largeButtonSlider.setMajorTickUnit(5);
        largeButtonSlider.setMinorTickCount(0);
        largeButtonSlider.setShowTickLabels(true);
        largeButtonSlider.setShowTickMarks(false);
        largeButtonSlider.setSnapToTicks(true); // 自动对齐到刻度
        
        Label largeButtonValue = StyleFactory.createLabel(String.format("%d", Math.round(currentTheme.getButtonLargeSize())), 14, false);
        largeButtonValue.setMinWidth(50);
        
        largeButtonSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int intValue = (int) Math.round(newVal.doubleValue());
            currentTheme.setButtonLargeSize(intValue);
            largeButtonValue.setText(String.format("%d", intValue));
            updateButtonExamples(); // 更新示例按钮
        });
        
        largeButtonBox.getChildren().addAll(largeButtonLabel, largeButtonSlider, largeButtonValue);
        content.getChildren().add(largeButtonBox);
        
        // 小按钮大小设置
        HBox smallButtonBox = new HBox(20);
        smallButtonBox.setAlignment(Pos.CENTER_LEFT);
        Label smallButtonLabel = StyleFactory.createLabel("小按钮大小", 14, false);
        JFXSlider smallButtonSlider = new JFXSlider(40, 80, Math.round(currentTheme.getButtonSmallSize()));
        smallButtonSlider.setPrefWidth(400);
        smallButtonSlider.setMajorTickUnit(5);
        smallButtonSlider.setMinorTickCount(0);
        smallButtonSlider.setShowTickLabels(true);
        smallButtonSlider.setShowTickMarks(false);
        smallButtonSlider.setSnapToTicks(true); // 自动对齐到刻度
        
        Label smallButtonValue = StyleFactory.createLabel(String.format("%d", Math.round(currentTheme.getButtonSmallSize())), 14, false);
        smallButtonValue.setMinWidth(50);
        
        smallButtonSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int intValue = (int) Math.round(newVal.doubleValue());
            currentTheme.setButtonSmallSize(intValue);
            smallButtonValue.setText(String.format("%d", intValue));
            updateButtonExamples(); // 更新示例按钮
        });
        
        smallButtonBox.getChildren().addAll(smallButtonLabel, smallButtonSlider, smallButtonValue);
        content.getChildren().add(smallButtonBox);
        
        // 按钮示例展示
        Label buttonExampleTitle = new Label("按钮示例");
        buttonExampleTitle.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: %s;",
                currentTheme.getFontFamily(), currentTheme.getTextPrimaryColor()
        ));
        content.getChildren().add(buttonExampleTitle);
        
        HBox buttonExamplesBox = new HBox(30);
        buttonExamplesBox.setAlignment(Pos.CENTER_LEFT);
        buttonExamplesBox.setPadding(new Insets(10));
        buttonExamplesBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-width: 1px;",
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor()
        ));
        
        // 大按钮示例
        Button largeButtonExample = new Button("大按钮示例");
        largeButtonExample.setPrefWidth(150);
        largeButtonExample.setPrefHeight(Math.round(currentTheme.getButtonLargeSize()));
        largeButtonExample.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: 14px; -fx-background-radius: %.1f; -fx-border-color: %s; -fx-border-width: %.1f; -fx-cursor: hand;",
                currentTheme.getButtonBgColor(), currentTheme.getButtonTextColor(), currentTheme.getFontFamily(),
                currentTheme.getCornerRadius(), currentTheme.getButtonBorderColor(), currentTheme.getBorderWidth()
        ));
        buttonExamplesBox.getChildren().add(largeButtonExample);
        
        // 小按钮示例
        Button smallButtonExample = new Button("小按钮示例");
        smallButtonExample.setPrefWidth(120);
        smallButtonExample.setPrefHeight(Math.round(currentTheme.getButtonSmallSize()));
        smallButtonExample.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: 12px; -fx-background-radius: %.1f; -fx-border-color: %s; -fx-border-width: %.1f; -fx-cursor: hand;",
                currentTheme.getButtonBgColor(), currentTheme.getButtonTextColor(), currentTheme.getFontFamily(),
                currentTheme.getCornerRadius(), currentTheme.getButtonBorderColor(), currentTheme.getBorderWidth()
        ));
        buttonExamplesBox.getChildren().add(smallButtonExample);
        
        content.getChildren().add(buttonExamplesBox);
        
        // 保存示例按钮引用，用于后续更新
        this.largeButtonExample = largeButtonExample;
        this.smallButtonExample = smallButtonExample;
        
        return content;
    }
    
    /**
     * 更新按钮示例样式
     */
    private void updateButtonExamples() {
        if (largeButtonExample != null) {
            largeButtonExample.setPrefHeight(Math.round(currentTheme.getButtonLargeSize()));
            largeButtonExample.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: 14px; -fx-background-radius: %.1f; -fx-border-color: %s; -fx-border-width: %.1f; -fx-cursor: hand;",
                    currentTheme.getButtonBgColor(), currentTheme.getButtonTextColor(), currentTheme.getFontFamily(),
                    currentTheme.getCornerRadius(), currentTheme.getButtonBorderColor(), currentTheme.getBorderWidth()
            ));
        }
        
        if (smallButtonExample != null) {
            smallButtonExample.setPrefHeight(Math.round(currentTheme.getButtonSmallSize()));
            smallButtonExample.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-family: %s; -fx-font-size: 12px; -fx-background-radius: %.1f; -fx-border-color: %s; -fx-border-width: %.1f; -fx-cursor: hand;",
                    currentTheme.getButtonBgColor(), currentTheme.getButtonTextColor(), currentTheme.getFontFamily(),
                    currentTheme.getCornerRadius(), currentTheme.getButtonBorderColor(), currentTheme.getBorderWidth()
            ));
        }
        
        // 应用新的按钮样式到整个界面
        applyAppearance();
    }
    
    /**
     * 创建样式管理选项卡内容
     */
    private Node createStyleTabContent() {
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
        card.setPrefWidth(180); // 调整卡片宽度，使其更紧凑
        card.setPrefHeight(250); // 调整卡片高度，使其更紧凑
        card.setPadding(new Insets(12)); // 减小内边距
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
        bgColor.setFill(Color.web(validateColor(theme.getBgColor(), "#f5f5f5")));
        bgColor.setArcWidth(4);
        bgColor.setArcHeight(4);
        bgColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", validateColor(theme.getBorderColor(), "#e0e0e0")));
        bgColorBox.getChildren().addAll(bgLabel, bgColor);
        colorGrid.add(bgColorBox, 0, 0);
        
        // 面板背景色
        VBox panelColorBox = new VBox(3);
        Label panelLabel = new Label("面板");
        panelLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getFontFamily(), validateColor(theme.getTextSecondaryColor(), "#546e7a")
        ));
        panelLabel.setAlignment(Pos.CENTER);
        Rectangle panelColor = new Rectangle(35, 20);
        panelColor.setFill(Color.web(validateColor(theme.getPanelBgColor(), "#ffffff")));
        panelColor.setArcWidth(4);
        panelColor.setArcHeight(4);
        panelColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", validateColor(theme.getBorderColor(), "#e0e0e0")));
        panelColorBox.getChildren().addAll(panelLabel, panelColor);
        colorGrid.add(panelColorBox, 1, 0);
        
        // 主色调
        VBox accentColorBox = new VBox(3);
        Label accentLabel = new Label("主色");
        accentLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getFontFamily(), validateColor(theme.getTextSecondaryColor(), "#546e7a")
        ));
        accentLabel.setAlignment(Pos.CENTER);
        Rectangle accentColor = new Rectangle(35, 20);
        accentColor.setFill(Color.web(validateColor(theme.getAccentColor(), "#3498db")));
        accentColor.setArcWidth(4);
        accentColor.setArcHeight(4);
        accentColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", validateColor(theme.getBorderColor(), "#e0e0e0")));
        accentColorBox.getChildren().addAll(accentLabel, accentColor);
        colorGrid.add(accentColorBox, 2, 0);
        
        // 文本主色
        VBox textColorBox = new VBox(3);
        Label textLabel = new Label("文本");
        textLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: %s;",
                theme.getFontFamily(), validateColor(theme.getTextSecondaryColor(), "#546e7a")
        ));
        textLabel.setAlignment(Pos.CENTER);
        Rectangle textColor = new Rectangle(35, 20);
        textColor.setFill(Color.web(validateColor(theme.getTextPrimaryColor(), "#2c3e50")));
        textColor.setArcWidth(4);
        textColor.setArcHeight(4);
        textColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", validateColor(theme.getBorderColor(), "#e0e0e0")));
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
                    "-fx-background-color: %s; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 15, 0, 0, 0); -fx-border-color: %s; -fx-border-width: 3px;",
                    theme.getPanelBgColor(), theme.getAccentColor()
            ));
            
            // 添加明显的选中标记
            StackPane checkmarkPane = new StackPane();
            checkmarkPane.setPrefSize(30, 30);
            checkmarkPane.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 0);",
                    theme.getAccentColor()
            ));
            
            Label checkmarkLabel = new Label("✓");
            checkmarkLabel.setStyle(String.format(
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;"
            ));
            checkmarkPane.getChildren().add(checkmarkLabel);
            
            // 将标记放置在卡片右上角
            StackPane.setAlignment(checkmarkPane, Pos.TOP_RIGHT);
            card.getChildren().add(checkmarkPane);
            // 调整标记位置
            StackPane.setMargin(checkmarkPane, new Insets(-5, -5, 0, 0));
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
        
        // 添加列表样式属性的更新
        currentTheme.setListBgColor(theme.getListBgColor());
        currentTheme.setListRowEvenBgColor(theme.getListRowEvenBgColor());
        currentTheme.setListRowOddBgColor(theme.getListRowOddBgColor());
        currentTheme.setListRowSelectedBgColor(theme.getListRowSelectedBgColor());
        currentTheme.setListRowSelectedTextColor(theme.getListRowSelectedTextColor());
        currentTheme.setListRowHoverBgColor(theme.getListRowHoverBgColor());
        currentTheme.setListBorderColor(theme.getListBorderColor());
        currentTheme.setListHeaderBgColor(theme.getListHeaderBgColor());
        currentTheme.setListHeaderTextColor(theme.getListHeaderTextColor());
        
        // 添加按钮样式属性的更新
        currentTheme.setButtonPrimaryBgColor(theme.getButtonPrimaryBgColor());
        currentTheme.setButtonPrimaryTextColor(theme.getButtonPrimaryTextColor());
        currentTheme.setButtonPrimaryBorderColor(theme.getButtonPrimaryBorderColor());
        currentTheme.setButtonPrimaryHoverColor(theme.getButtonPrimaryHoverColor());
        currentTheme.setButtonPrimaryPressedColor(theme.getButtonPrimaryPressedColor());
        currentTheme.setButtonSecondaryBgColor(theme.getButtonSecondaryBgColor());
        currentTheme.setButtonSecondaryTextColor(theme.getButtonSecondaryTextColor());
        currentTheme.setButtonSecondaryBorderColor(theme.getButtonSecondaryBorderColor());
        currentTheme.setButtonSecondaryHoverColor(theme.getButtonSecondaryHoverColor());
        currentTheme.setButtonSecondaryPressedColor(theme.getButtonSecondaryPressedColor());
        currentTheme.setButtonSuccessBgColor(theme.getButtonSuccessBgColor());
        currentTheme.setButtonSuccessTextColor(theme.getButtonSuccessTextColor());
        currentTheme.setButtonSuccessBorderColor(theme.getButtonSuccessBorderColor());
        currentTheme.setButtonSuccessHoverColor(theme.getButtonSuccessHoverColor());
        currentTheme.setButtonWarningBgColor(theme.getButtonWarningBgColor());
        currentTheme.setButtonWarningTextColor(theme.getButtonWarningTextColor());
        currentTheme.setButtonWarningBorderColor(theme.getButtonWarningBorderColor());
        currentTheme.setButtonWarningHoverColor(theme.getButtonWarningHoverColor());
        currentTheme.setButtonErrorBgColor(theme.getButtonErrorBgColor());
        currentTheme.setButtonErrorTextColor(theme.getButtonErrorTextColor());
        currentTheme.setButtonErrorBorderColor(theme.getButtonErrorBorderColor());
        currentTheme.setButtonErrorHoverColor(theme.getButtonErrorHoverColor());
        
        // 设置背景图
        currentTheme.setBgImagePath(theme.getBgImagePath());
        
        applyAppearance();
    }
    
    /**
     * 刷新颜色选择器
     */
    private void refreshColorPickers() {
        // 由于颜色选择器是在对话框中动态创建的，这里不需要实现具体逻辑
        // 实际应用中可以通过绑定或事件监听来实现
    }
    
    /**
     * 显示背景图裁切对话框
     */
    private void showCropBackgroundDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("背景图裁切");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(800);
        dialog.getDialogPane().setPrefHeight(600);
        
        // 创建主容器
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setStyle(String.format("-fx-background-color: %s;", currentTheme.getBgColor()));
        
        // 背景图预览区域
        StackPane previewContainer = new StackPane();
        previewContainer.setPrefSize(750, 400);
        previewContainer.setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: %.1f; -fx-background-radius: %.1f;",
                currentTheme.getPanelBgColor(), currentTheme.getBorderColor(), currentTheme.getBorderWidth(), currentTheme.getCornerRadius()));
        
        // 加载当前背景图
        ImageView originalImage = new ImageView(backgroundImageView.getImage());
        originalImage.setPreserveRatio(true);
        originalImage.setFitWidth(700);
        originalImage.setFitHeight(350);
        
        // 创建裁切矩形
        Rectangle cropRect = new Rectangle();
        cropRect.setWidth(400);
        cropRect.setHeight(300);
        cropRect.setStroke(Color.RED);
        cropRect.setStrokeWidth(2);
        cropRect.setFill(Color.TRANSPARENT);
        
        // 添加拖动手柄
        Circle topLeftHandle = new Circle(5);
        topLeftHandle.setFill(Color.WHITE);
        topLeftHandle.setStroke(Color.RED);
        topLeftHandle.setStrokeWidth(2);
        topLeftHandle.setCursor(Cursor.NW_RESIZE);
        
        Circle topRightHandle = new Circle(5);
        topRightHandle.setFill(Color.WHITE);
        topRightHandle.setStroke(Color.RED);
        topRightHandle.setStrokeWidth(2);
        topRightHandle.setCursor(Cursor.NE_RESIZE);
        
        Circle bottomLeftHandle = new Circle(5);
        bottomLeftHandle.setFill(Color.WHITE);
        bottomLeftHandle.setStroke(Color.RED);
        bottomLeftHandle.setStrokeWidth(2);
        bottomLeftHandle.setCursor(Cursor.SW_RESIZE);
        
        Circle bottomRightHandle = new Circle(5);
        bottomRightHandle.setFill(Color.WHITE);
        bottomRightHandle.setStroke(Color.RED);
        bottomRightHandle.setStrokeWidth(2);
        bottomRightHandle.setCursor(Cursor.SE_RESIZE);
        
        // 更新手柄位置
        updateCropHandles(cropRect, topLeftHandle, topRightHandle, bottomLeftHandle, bottomRightHandle);
        
        // 添加手柄拖拽功能
        addHandleDragListeners(cropRect, topLeftHandle, topRightHandle, bottomLeftHandle, bottomRightHandle);
        
        previewContainer.getChildren().addAll(originalImage, cropRect, topLeftHandle, topRightHandle, bottomLeftHandle, bottomRightHandle);
        
        // 尺寸调整控件
        VBox sizeControls = new VBox(15);
        sizeControls.setPadding(new Insets(0, 0, 0, 10));
        sizeControls.setStyle("-fx-background-color: transparent;");
        
        Label sizeLabel = StyleFactory.createLabel("裁切区域尺寸", 14, false);
        sizeControls.getChildren().add(sizeLabel);
        
        // 宽度滑块
        HBox widthBox = new HBox(15);
        Label widthLabel = StyleFactory.createLabel("宽度:", 12, false);
        TextField widthField = new TextField(String.valueOf((int) cropRect.getWidth()));
        widthField.setPrefWidth(80);
        widthField.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius(), currentTheme.getTextPrimaryColor(), 
                currentTheme.getBorderColor(), currentTheme.getBorderWidth()
        ));
        
        JFXSlider widthSlider = new JFXSlider(100, 700, cropRect.getWidth());
        widthSlider.setPrefWidth(200);
        widthSlider.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f;",
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius()
        ));
        
        widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            cropRect.setWidth(newVal.doubleValue());
            widthField.setText(String.valueOf((int) newVal.doubleValue()));
            updateCropHandles(cropRect, topLeftHandle, topRightHandle, bottomLeftHandle, bottomRightHandle);
        });
        
        widthBox.getChildren().addAll(widthLabel, widthField, widthSlider);
        sizeControls.getChildren().add(widthBox);
        
        // 高度滑块
        HBox heightBox = new HBox(15);
        Label heightLabel = StyleFactory.createLabel("高度:", 12, false);
        TextField heightField = new TextField(String.valueOf((int) cropRect.getHeight()));
        heightField.setPrefWidth(80);
        heightField.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius(), currentTheme.getTextPrimaryColor(), 
                currentTheme.getBorderColor(), currentTheme.getBorderWidth()
        ));
        
        JFXSlider heightSlider = new JFXSlider(100, 350, cropRect.getHeight());
        heightSlider.setPrefWidth(200);
        heightSlider.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f;",
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius()
        ));
        
        heightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            cropRect.setHeight(newVal.doubleValue());
            heightField.setText(String.valueOf((int) newVal.doubleValue()));
            updateCropHandles(cropRect, topLeftHandle, topRightHandle, bottomLeftHandle, bottomRightHandle);
        });
        
        heightBox.getChildren().addAll(heightLabel, heightField, heightSlider);
        sizeControls.getChildren().add(heightBox);
        
        mainContainer.getChildren().addAll(previewContainer, sizeControls);
        dialog.getDialogPane().setContent(mainContainer);
        
        // 应用裁切
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.APPLY) {
                // 这里可以添加实际的裁切逻辑
                // 目前只是保存裁切设置
                backgroundImageView.setPreserveRatio(true);
                backgroundImageView.setStyle("-fx-background-size: cover; -fx-background-position: center; -fx-background-repeat: no-repeat;");
                applyAppearance();
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    /**
     * 更新裁切手柄位置
     */
    private void updateCropHandles(Rectangle cropRect, Circle topLeft, Circle topRight, Circle bottomLeft, Circle bottomRight) {
        topLeft.setCenterX(cropRect.getX());
        topLeft.setCenterY(cropRect.getY());
        
        topRight.setCenterX(cropRect.getX() + cropRect.getWidth());
        topRight.setCenterY(cropRect.getY());
        
        bottomLeft.setCenterX(cropRect.getX());
        bottomLeft.setCenterY(cropRect.getY() + cropRect.getHeight());
        
        bottomRight.setCenterX(cropRect.getX() + cropRect.getWidth());
        bottomRight.setCenterY(cropRect.getY() + cropRect.getHeight());
    }
    
    /**
     * 添加手柄拖拽监听器
     */
    private void addHandleDragListeners(Rectangle cropRect, Circle topLeft, Circle topRight, Circle bottomLeft, Circle bottomRight) {
        // 为每个手柄添加拖拽监听器
        addHandleDragListener(topLeft, cropRect, (deltaX, deltaY) -> {
            cropRect.setX(cropRect.getX() + deltaX);
            cropRect.setY(cropRect.getY() + deltaY);
            cropRect.setWidth(cropRect.getWidth() - deltaX);
            cropRect.setHeight(cropRect.getHeight() - deltaY);
        });
        
        addHandleDragListener(topRight, cropRect, (deltaX, deltaY) -> {
            cropRect.setY(cropRect.getY() + deltaY);
            cropRect.setWidth(cropRect.getWidth() + deltaX);
            cropRect.setHeight(cropRect.getHeight() - deltaY);
        });
        
        addHandleDragListener(bottomLeft, cropRect, (deltaX, deltaY) -> {
            cropRect.setX(cropRect.getX() + deltaX);
            cropRect.setWidth(cropRect.getWidth() - deltaX);
            cropRect.setHeight(cropRect.getHeight() + deltaY);
        });
        
        addHandleDragListener(bottomRight, cropRect, (deltaX, deltaY) -> {
            cropRect.setWidth(cropRect.getWidth() + deltaX);
            cropRect.setHeight(cropRect.getHeight() + deltaY);
        });
    }
    
    /**
     * 添加单个手柄的拖拽监听器
     */
    private void addHandleDragListener(Circle handle, Rectangle cropRect, BiConsumer<Double, Double> resizeAction) {
        final boolean[] isDragging = {false};
        final Point2D[] lastMousePosition = {null};
        
        handle.setOnMousePressed(e -> {
            isDragging[0] = true;
            lastMousePosition[0] = new Point2D(e.getSceneX(), e.getSceneY());
        });
        
        handle.setOnMouseDragged(e -> {
            if (isDragging[0] && lastMousePosition[0] != null) {
                double deltaX = e.getSceneX() - lastMousePosition[0].getX();
                double deltaY = e.getSceneY() - lastMousePosition[0].getY();
                
                // 执行尺寸调整操作
                resizeAction.accept(deltaX, deltaY);
                
                // 确保尺寸不小于最小值
                if (cropRect.getWidth() < 50) cropRect.setWidth(50);
                if (cropRect.getHeight() < 50) cropRect.setHeight(50);
                
                // 更新手柄位置
                Circle topLeft = (Circle) ((Pane) handle.getParent()).getChildren().get(2);
                Circle topRight = (Circle) ((Pane) handle.getParent()).getChildren().get(3);
                Circle bottomLeft = (Circle) ((Pane) handle.getParent()).getChildren().get(4);
                Circle bottomRight = (Circle) ((Pane) handle.getParent()).getChildren().get(5);
                updateCropHandles(cropRect, topLeft, topRight, bottomLeft, bottomRight);
                
                lastMousePosition[0] = new Point2D(e.getSceneX(), e.getSceneY());
            }
        });
        
        handle.setOnMouseReleased(e -> {
            isDragging[0] = false;
            lastMousePosition[0] = null;
        });
    }
    
    public void applyAppearance() {
        // 重新初始化样式工厂
        StyleFactory.initStyleFactory(currentTheme);
        
        // 更新背景覆盖层
        backgroundOverlay.setStyle("-fx-background-color: rgba(" + 
                (currentTheme.isDarkBackground() ? "0,0,0" : "255,255,255") + 
                ", " + (1 - currentTheme.getGlassOpacity()) + ");");
        
        // 检查是否有背景图
        boolean hasBackgroundImage = !currentTheme.getBgImagePath().isEmpty();
        
        // 如果有背景图，将面板背景色设置为透明
        if (hasBackgroundImage) {
            currentTheme.setPanelBgColor("transparent");
            currentTheme.setPanelBorderColor("transparent");
            currentTheme.setPanelHoverColor("transparent");
        }
        
        // 更新背景图片
        if (hasBackgroundImage) {
            try {
                File bgImageFile = new File(currentTheme.getBgImagePath());
                String imageUrl;
                
                // 检查文件是否存在（绝对路径或相对路径）
                if (bgImageFile.exists() && bgImageFile.isFile()) {
                    // 本地文件，使用file:// URL方式加载
                    imageUrl = bgImageFile.toURI().toURL().toExternalForm();
                } else {
                    // 尝试从资源路径加载（相对路径）
                    java.net.URL resourceUrl = getClass().getClassLoader().getResource(currentTheme.getBgImagePath());
                    if (resourceUrl != null) {
                        imageUrl = resourceUrl.toExternalForm();
                    } else {
                        app.logError("背景图文件不存在：" + currentTheme.getBgImagePath());
                        backgroundImageView.setImage(null);
                        return;
                    }
                }
                
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
                backgroundImageView.setStyle("-fx-background-size: cover; -fx-background-position: center; -fx-background-repeat: no-repeat;");
                
                backgroundImageView.setImage(image);
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