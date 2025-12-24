package com.filemanager.tool.display;

import com.filemanager.type.TaskStatus;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * @author 28667
 */
public class ProgressBarDisplay extends Application {

    @Override
    public void start(Stage stage) {
        ProgressBar progressBar = new ProgressBar(0.6); // 设置 60% 进度
        progressBar.setPrefWidth(300);

        // 模拟状态切换逻辑
        updateProgressStatus(progressBar, TaskStatus.RUNNING);

        VBox root = new VBox(20, progressBar);
        root.setStyle("-fx-padding: 50; -fx-alignment: center;");
        
        stage.setScene(new Scene(root, 400, 200));
        stage.show();
    }

    /**
     * 根据状态更新进度条颜色的核心方法
     */
    public static void updateProgressStatus(ProgressBar pb, TaskStatus status) {
        switch (status) {
            case RUNNING:
                pb.setStyle("-fx-accent: #BDE0FE;"); // 马卡龙蓝
                break;
            case SUCCESS:
                pb.setStyle("-fx-accent: #B9FBC0;"); // 马卡龙绿
                break;
            case FAILURE:
                pb.setStyle("-fx-accent: #FFADAD;"); // 马卡龙红
                break;
            case CANCELED:
                pb.setStyle("-fx-accent: #FDFFB6;"); // 马卡龙黄
                break;
        }
    }



    public static void main(String[] args) { launch(args); }
}