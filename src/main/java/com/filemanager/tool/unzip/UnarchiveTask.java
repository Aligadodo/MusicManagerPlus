package com.filemanager.tool.unzip;

/**
 * 解压任务实体
 * @author 28667
 */
public class UnarchiveTask {
    public String archivePath;
    public String targetDir;
    public boolean overwrite = true;
    public String password;
    public int threadCount = 0;
    public ProgressListener listener; // 根据此字段决定是否开启流读取

    public UnarchiveTask(String archivePath, String targetDir) {
        this.archivePath = archivePath;
        this.targetDir = targetDir;
    }
}