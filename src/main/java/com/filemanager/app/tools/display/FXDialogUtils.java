/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.app.tools.display;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Optional;

public class FXDialogUtils {

    public enum ToastType { INFO, SUCCESS, ERROR }

    public static void showToast(Stage owner, String msg, ToastType type) {
        final Stage toastStage = new Stage();
        toastStage.initOwner(owner);
        toastStage.initStyle(StageStyle.TRANSPARENT);

        // 1. 创建文本标签
        Label label = new Label(msg);
        label.getStyleClass().add("toast-text");
        label.setMaxWidth(400); // 限制最大宽度，防止长文本无限横向延伸
        label.setWrapText(true); // 允许换行
        label.setAlignment(Pos.CENTER); // Label 内部对齐

        // 2. 创建容器并设置对齐
        StackPane root = new StackPane(label);
        root.getStyleClass().add("toast-root");
        StackPane.setAlignment(label, Pos.CENTER); // 确保 Label 在 StackPane 中居中

        switch (type) {
            case SUCCESS: root.getStyleClass().add("toast-success"); break;
            case ERROR:   root.getStyleClass().add("toast-error");   break;
            default:      root.getStyleClass().add("toast-info");    break;
        }

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        try {
            String cssPath = FXDialogUtils.class.getResource("/style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("未找到 style.css 文件");
        }

        toastStage.setScene(scene);

        // 3. 动态定位逻辑
        // 必须先 show，JavaFX 才会计算 root 的 PrefWidth/Height
        toastStage.show();
        
        // 再次强制刷新布局以获取准确宽高
        root.applyCss();
        root.layout();

        double x = owner.getX() + (owner.getWidth() - toastStage.getWidth()) / 2;
        double y = owner.getY() + (owner.getHeight() * 0.8) - (toastStage.getHeight() / 2);
        
        toastStage.setX(x);
        toastStage.setY(y);

        // 4. 动画
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        final FadeTransition fadeOut = new FadeTransition(Duration.millis(500), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(2.0));
        fadeOut.setOnFinished(event -> toastStage.close());

        fadeIn.setOnFinished(event -> fadeOut.play());
        fadeIn.play();
    }

    /**
     * 显示普通消息对话框
     */
    public static void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        // 注入 CSS 优化外观
        DialogPane dialogPane = alert.getDialogPane();
        String cssPath = FXDialogUtils.class.getResource("/style.css").toExternalForm();
        dialogPane.getStylesheets().add(cssPath);

        alert.showAndWait();
    }

    /**
     * 显示确认对话框 (返回 true/false)
     */
    public static boolean showConfirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        // 自定义按钮文字 (JDK 8 写法)
        ButtonType btnYes = new ButtonType("确定", ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType("取消", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == btnYes;
    }
}