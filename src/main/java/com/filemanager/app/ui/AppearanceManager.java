/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.ui;

import java.io.File;
import java.nio.file.Files;

import com.filemanager.app.base.IAppController;
import com.filemanager.app.tools.display.FontManager;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.app.tools.display.StyleTemplateManager;
import com.filemanager.app.tools.display.ThemeConfig;
import com.filemanager.app.tools.display.ThemeManager;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXTabPane;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

public class AppearanceManager {
    private final IAppController app;
    private final ThemeConfig currentTheme;
    private final ImageView backgroundImageView;
    private final Region backgroundOverlay;
    private final ThemeManager themeManager;
    private final StyleTemplateManager templateManager;
    private Tab finalPresetTab;
    private JFXTabPane tpAppearance;
    
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
        
        // 应用玻璃效果透明度
        String bgColor = currentTheme.getPanelBgColor();
        if (bgColor.startsWith("#") && bgColor.length() == 7) {
            int alpha = (int) (currentTheme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            bgColor = bgColor + alphaHex;
        }
        mainContainer.setStyle(String.format("-fx-background-color: %s;", bgColor));
        mainContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        
        // 创建TabPane来组织不同的设置类别
        JFXTabPane tabPane = StyleFactory.createTabPane(true);
        // 保存TabPane引用
        this.tpAppearance = tabPane;
       
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
        VBox imageSettingsBox = new VBox(10);
        HBox imagePathBox = new HBox(10);
        TextField bgImagePath = new TextField(currentTheme.getBgImagePath());
        bgImagePath.setPrefWidth(400);
        bgImagePath.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius(), currentTheme.getTextPrimaryColor(), 
                currentTheme.getBorderColor(), currentTheme.getBorderWidth()
        ));
        
        // 背景图策略选择器
        HBox strategyBox = new HBox(10);
        strategyBox.setAlignment(Pos.CENTER_LEFT);
        Label strategyLabel = new Label("显示策略：");
        strategyLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold;", currentTheme.getTextPrimaryColor()));
        
        ComboBox<ThemeConfig.BackgroundStrategy> strategyCombo = new ComboBox<>();
        strategyCombo.getItems().addAll(ThemeConfig.BackgroundStrategy.values());
        strategyCombo.setValue(currentTheme.getBgImageStrategy());
        strategyCombo.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius(), currentTheme.getTextPrimaryColor(), 
                currentTheme.getBorderColor(), currentTheme.getBorderWidth()
        ));
        strategyCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentTheme.setBgImageStrategy(newVal);
            isThemeModified = true;
            applyAppearance();
        });
        
        strategyBox.getChildren().addAll(strategyLabel, strategyCombo);
        
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
        
        imagePathBox.getChildren().addAll(bgImagePath, browseBtn, clearBtn);
        imageSettingsBox.getChildren().addAll(imagePathBox, strategyBox);
        content.getChildren().add(imageSettingsBox);
        
        // 删除了旧的背景图显示模式选择部分，改用新的策略选择器
        
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
        
        // 应用玻璃效果透明度
        String panelBgColor = currentTheme.getPanelBgColor();
        if (panelBgColor.startsWith("#") && panelBgColor.length() == 7) {
            int alpha = (int) (currentTheme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            panelBgColor = panelBgColor + alphaHex;
        }
        
        bgImagePanel.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 8; -fx-border-color: %s; -fx-border-width: 1px;",
                panelBgColor, currentTheme.getBorderColor()
        ));
        
        Label bgImagePanelTitle = new Label("选择背景图");
        bgImagePanelTitle.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s;",
                currentTheme.getFontFamily(), currentTheme.getTextPrimaryColor()
        ));
        bgImagePanelTitle.setPadding(new Insets(10, 0, 0, 10));
        bgImagePanel.getChildren().add(bgImagePanelTitle);
        
        // 预览尺寸选择器
        HBox previewSizeBox = new HBox(20);
        previewSizeBox.setPadding(new Insets(10, 0, 0, 10));
        previewSizeBox.setAlignment(Pos.CENTER_LEFT);
        Label previewSizeLabel = StyleFactory.createLabel("预览尺寸", 14, false);
        
        // 定义预览尺寸选项
        ObservableList<String> previewSizes = FXCollections.observableArrayList("小", "中", "大");
        JFXComboBox<String> previewSizeCb = new JFXComboBox<>(previewSizes);
        previewSizeCb.setValue("中"); // 默认选择中等尺寸
        previewSizeCb.setPrefWidth(100);
        
        // 设置下拉框样式
        previewSizeCb.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: %.1f; -fx-text-fill: %s; -fx-border-color: %s; -fx-border-width: %.1f;",
                currentTheme.getPanelBgColor(), currentTheme.getCornerRadius(), currentTheme.getTextPrimaryColor(), 
                currentTheme.getBorderColor(), currentTheme.getBorderWidth()
        ));
        
        // 将标签和下拉框添加到容器中
        previewSizeBox.getChildren().addAll(previewSizeLabel, previewSizeCb);
        
        // 保存当前预览尺寸
        String[] currentPreviewSize = {"中"};
        
        // 添加预览尺寸变化监听器
        previewSizeCb.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentPreviewSize[0] = newVal;
                // 重新加载整个背景图片面板
                if (tpAppearance != null) {
                    // 找到背景设置选项卡并重新加载
                    for (int i = 0; i < tpAppearance.getTabs().size(); i++) {
                        Tab tab = tpAppearance.getTabs().get(i);
                        if (tab.getText().equals("背景设置")) {
                            tpAppearance.getTabs().set(i, new Tab("背景设置", createBackgroundTabContent()));
                            break;
                        }
                    }
                }
            }
        });
        bgImagePanel.getChildren().add(previewSizeBox);
        
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
                        // 根据当前预览尺寸设置不同的图片大小
                        int width, height;
                        if (currentPreviewSize[0].equals("小")) {
                            width = 100;
                            height = 75;
                        } else if (currentPreviewSize[0].equals("大")) {
                            width = 200;
                            height = 150;
                        } else {
                            width = 150;
                            height = 110;
                        }
                        
                        // 创建缩略图
                        Image image = new Image(imageFile.toURI().toURL().toExternalForm());
                        ImageView imageView = new ImageView(image);
                        imageView.setPreserveRatio(true);
                        imageView.setFitWidth(width);
                        imageView.setFitHeight(height);
                        imageView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0px;");
                        
                        // 添加图片加载错误处理
                        image.errorProperty().addListener((obs, oldVal, newVal) -> {
                            if (newVal) {
                                System.err.println("预览图加载失败: " + imageFile.getAbsolutePath());
                                // 显示错误占位图
                                imageView.setImage(null);
                                imageView.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ff0000; -fx-border-width: 1px;");
                            }
                        });
                        
                        // 根据当前预览尺寸设置不同的按钮大小
                        int buttonWidth, buttonHeight;
                        if (currentPreviewSize[0].equals("小")) {
                            buttonWidth = 120;
                            buttonHeight = 95;
                        } else if (currentPreviewSize[0].equals("大")) {
                            buttonWidth = 220;
                            buttonHeight = 170;
                        } else {
                            buttonWidth = 170;
                            buttonHeight = 130;
                        }
                        
                        // 创建按钮
                        Button imageButton = new Button();
                        imageButton.setGraphic(imageView);
                        imageButton.setPrefSize(buttonWidth, buttonHeight);
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
                        
                        // 保存图片路径，用于双击查看大图
                        final String finalImagePath = imageFile.getAbsolutePath();
                        imageButton.setOnMouseClicked(event -> {
                            if (event.getClickCount() == 2) {
                                try {
                                    // 显示大图对话框
                                    Dialog<Void> dialog = new Dialog<>();
                                    dialog.setTitle("背景图预览");
                                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                                    dialog.getDialogPane().setPrefSize(1000, 800);
                                    
                                    StackPane previewPane = new StackPane();
                                    Image fullSizeImage = new Image(new File(finalImagePath).toURI().toURL().toExternalForm());
                                    ImageView fullSizeImageView = new ImageView(fullSizeImage);
                                    fullSizeImageView.setPreserveRatio(true);
                                    fullSizeImageView.setFitWidth(950);
                                    fullSizeImageView.setFitHeight(750);
                                    
                                    previewPane.getChildren().add(fullSizeImageView);
                                    dialog.getDialogPane().setContent(previewPane);
                                    
                                    dialog.showAndWait();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                        
                        bgImageFlow.getChildren().add(imageButton);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 根据当前预览尺寸设置不同的按钮大小
                        int buttonWidth, buttonHeight;
                        if (currentPreviewSize[0].equals("小")) {
                            buttonWidth = 120;
                            buttonHeight = 95;
                        } else if (currentPreviewSize[0].equals("大")) {
                            buttonWidth = 220;
                            buttonHeight = 170;
                        } else {
                            buttonWidth = 170;
                            buttonHeight = 130;
                        }
                        
                        // 创建错误占位按钮
                        Button errorButton = new Button("图片加载失败");
                        errorButton.setPrefSize(buttonWidth, buttonHeight);
                        errorButton.setStyle(String.format(
                                "-fx-background-color: #f0f0f0; -fx-background-radius: 4; -fx-border-color: #ff0000; -fx-border-width: 1px; -fx-text-fill: #ff0000;",
                                currentTheme.getBorderColor()
                        ));
                        bgImageFlow.getChildren().add(errorButton);
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
        
        // 按钮示例更新完成，无需重新应用整个界面样式
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
        
        // 检查当前主题是否为选中状态
        boolean isCurrentTheme = theme.getTemplateName().equals(currentTheme.getTemplateName());
        
        // 设置卡片样式，当前选中的主题使用特殊边框
        if (isCurrentTheme) {
            card.setStyle(
                    "-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0); " +
                    "-fx-border-color: #3498db; -fx-border-width: 3px; -fx-border-radius: 12;"
            );
        } else {
            card.setStyle(
                    "-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0);"
            );
        }
        
        // 主题名称
        Label nameLabel = new Label(theme.getTemplateName());
        nameLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333;",
                theme.getFontFamily()
        ));
        nameLabel.setAlignment(Pos.CENTER);
        card.getChildren().add(nameLabel);
        
        // 主题描述
        Label descLabel = new Label(theme.getTemplateDescription());
        descLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 12px; -fx-text-fill: #666666;",
                theme.getFontFamily()
        ));
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(170);
        card.getChildren().add(descLabel);
        
        // 分隔线
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #e0e0e0;");
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
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: #666666;",
                theme.getFontFamily()
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
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: #666666;",
                theme.getFontFamily()
        ));
        panelLabel.setAlignment(Pos.CENTER);
        Rectangle panelColor = new Rectangle(35, 20);
        panelColor.setFill(Color.web(validateColor(theme.getPanelBgColor(), "#ffffff")));
        panelColor.setArcWidth(4);
        panelColor.setArcHeight(4);
        panelColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", validateColor(theme.getBorderColor(), "#e0e0e0")));
        panelColorBox.getChildren().addAll(panelLabel, panelColor);
        colorGrid.add(panelColorBox, 1, 0);
        
        // 主题色
        VBox accentColorBox = new VBox(3);
        Label accentLabel = new Label("主题色");
        accentLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: #666666;",
                theme.getFontFamily()
        ));
        accentLabel.setAlignment(Pos.CENTER);
        Rectangle accentColor = new Rectangle(35, 20);
        accentColor.setFill(Color.web(validateColor(theme.getAccentColor(), "#3498db")));
        accentColor.setArcWidth(4);
        accentColor.setArcHeight(4);
        accentColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", validateColor(theme.getBorderColor(), "#e0e0e0")));
        accentColorBox.getChildren().addAll(accentLabel, accentColor);
        colorGrid.add(accentColorBox, 0, 1);
        
        // 文本色
        VBox textColorBox = new VBox(3);
        Label textLabel = new Label("文本色");
        textLabel.setStyle(String.format(
                "-fx-font-family: %s; -fx-font-size: 10px; -fx-text-fill: #666666;",
                theme.getFontFamily()
        ));
        textLabel.setAlignment(Pos.CENTER);
        Rectangle textColor = new Rectangle(35, 20);
        textColor.setFill(Color.web(validateColor(theme.getTextPrimaryColor(), "#2c3e50")));
        textColor.setArcWidth(4);
        textColor.setArcHeight(4);
        textColor.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 1px;", validateColor(theme.getBorderColor(), "#e0e0e0")));
        textColorBox.getChildren().addAll(textLabel, textColor);
        colorGrid.add(textColorBox, 1, 1);
        
        card.getChildren().add(colorGrid);
        
        // 应用主题背景色
        String cardBgColor = theme.getPanelBgColor();
        if (cardBgColor.startsWith("#") && cardBgColor.length() == 7) {
            int alpha = (int) (theme.getGlassOpacity() * 255);
            String alphaHex = String.format("%02x", alpha);
            cardBgColor = cardBgColor + alphaHex;
        }
        
        // 设置卡片样式
        card.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0); -fx-border-color: %s; -fx-border-width: 1px; -fx-padding: 15;",
                cardBgColor, theme.getBorderColor()
        ));
        
        return card;
    }
    
    /**
     * 检查指定主题是否为当前主题
     */
    private boolean isCurrentTheme(ThemeConfig theme) {
        return theme.getTemplateName().equals(currentTheme.getTemplateName());
    }
    
    /**
     * 应用指定主题到界面
     */
    private void applyTheme(ThemeConfig newTheme) {
        // 更新当前主题配置
        currentTheme.setBgImagePath(newTheme.getBgImagePath());
        currentTheme.setBgColor(newTheme.getBgColor());
        currentTheme.setAccentColor(newTheme.getAccentColor());
        currentTheme.setTextPrimaryColor(newTheme.getTextPrimaryColor());
        currentTheme.setTextSecondaryColor(newTheme.getTextSecondaryColor());
        currentTheme.setTextTertiaryColor(newTheme.getTextTertiaryColor());
        currentTheme.setTextDisabledColor(newTheme.getTextDisabledColor());
        currentTheme.setGlassOpacity(newTheme.getGlassOpacity());
        currentTheme.setDarkBackground(newTheme.isDarkBackground());
        currentTheme.setPanelBgColor(newTheme.getPanelBgColor());
        currentTheme.setFontFamily(newTheme.getFontFamily());
        currentTheme.setFontSize(newTheme.getFontSize());
        currentTheme.setButtonLargeSize(newTheme.getButtonLargeSize());
        currentTheme.setButtonSmallSize(newTheme.getButtonSmallSize());
        
        // 应用主题到界面
        applyAppearance();
    }
    
    /**
     * 应用当前主题到界面
     */
    public void applyAppearance() {
        // 更新背景图片
        String bgImagePath = currentTheme.getBgImagePath();
        if (bgImagePath != null && !bgImagePath.isEmpty()) {
            // 检查路径是否已经是URL格式
            String imageUrl;
            if (!bgImagePath.startsWith("file://") && !bgImagePath.startsWith("http://") && !bgImagePath.startsWith("https://")) {
                // 将本地文件路径转换为URL格式
                File imageFile = new File(bgImagePath);
                imageUrl = imageFile.toURI().toString();
            } else {
                imageUrl = bgImagePath;
            }
            Image newBgImage = new Image(imageUrl);
            backgroundImageView.setImage(newBgImage);
            
            // 设置图片显示策略
            switch (currentTheme.getBgImageStrategy()) {
                case STRETCH:
                    backgroundImageView.setPreserveRatio(false);
                    break;
                case CONTAIN:
                    backgroundImageView.setPreserveRatio(true);
                    break;
                case COVER:
                    backgroundImageView.setPreserveRatio(true);
                    break;
                case CENTER:
                    backgroundImageView.setPreserveRatio(true);
                    break;
            }
        } else {
            backgroundImageView.setImage(null);
        }
        
        // 更新背景覆盖层
        backgroundOverlay.setStyle(String.format(
                "-fx-background-color: rgba(%s, %.2f);",
                currentTheme.isDarkBackground() ? "0,0,0" : "255,255,255",
                1 - currentTheme.getGlassOpacity()
        ));
        
        // 刷新按钮示例样式
        updateButtonExamples();
    }
    
    /**
     * 刷新颜色选择器
     */
    private void refreshColorPickers() {
        // 此方法用于在主题变更时刷新颜色选择器的显示
        // 具体实现会根据UI组件的实际情况进行调整
    }
}