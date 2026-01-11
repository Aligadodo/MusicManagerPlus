package com.filemanager.app.components;

import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.app.tools.display.ThemeConfig;
import com.filemanager.app.base.IAppController;
import com.jfoenix.controls.JFXButton;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AppearanceManager {
    private final IAppController app;
    private final ThemeConfig currentTheme;
    private final ImageView backgroundImageView;
    private final Region backgroundOverlay;
    
    public AppearanceManager(IAppController app, ThemeConfig currentTheme,
                           ImageView backgroundImageView, Region backgroundOverlay) {
        this.app = app;
        this.currentTheme = currentTheme;
        this.backgroundImageView = backgroundImageView;
        this.backgroundOverlay = backgroundOverlay;
    }
    
    public void showAppearanceDialog() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("设置");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.setPadding(new Insets(20));
        ColorPicker cp = new ColorPicker(Color.web(currentTheme.getAccentColor()));
        Slider sl = new Slider(0.1, 1.0, currentTheme.getGlassOpacity());
        CheckBox chk = new CheckBox("Dark Mode");
        chk.setSelected(currentTheme.isDarkBackground());
        TextField tp = new TextField(currentTheme.getBgImagePath());
        JFXButton bp = new JFXButton("背景...");
        bp.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) {
                tp.setText(f.getAbsolutePath());
                currentTheme.setBgImagePath(f.getAbsolutePath());
                applyAppearance();
            }
        });
        g.add(StyleFactory.createChapter("Color:"), 0, 0);
        g.add(cp, 1, 0);
        g.add(StyleFactory.createChapter("Opacity:"), 0, 1);
        g.add(sl, 1, 1);
        g.add(chk, 1, 2);
        g.add(StyleFactory.createChapter("BG:"), 0, 3);
        g.add(new HBox(5, tp, bp), 1, 3);
        d.getDialogPane().setContent(g);
        d.setResultConverter(b -> b);
        d.showAndWait().ifPresent(b -> {
            if (b == ButtonType.OK) {
                currentTheme.setAccentColor(String.format("#%02X%02X%02X", 
                        (int) (cp.getValue().getRed() * 255), 
                        (int) (cp.getValue().getGreen() * 255), 
                        (int) (cp.getValue().getBlue() * 255)));
                currentTheme.setGlassOpacity(sl.getValue());
                currentTheme.setDarkBackground(chk.isSelected());
                applyAppearance();
            }
        });
    }
    
    public void applyAppearance() {
        backgroundOverlay.setStyle("-fx-background-color: rgba(" + 
                (currentTheme.isDarkBackground() ? "0,0,0" : "255,255,255") + 
                ", " + (1 - currentTheme.getGlassOpacity()) + ");");
        if (!currentTheme.getBgImagePath().isEmpty()) {
            try {
                backgroundImageView.setImage(new Image(Files.newInputStream(Paths.get(currentTheme.getBgImagePath()))));
            } catch (Exception e) {
                app.logError("背景图加载失败：" + e.getMessage());
            }
        }
        app.refreshComposeView();
    }
}