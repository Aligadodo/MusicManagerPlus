package com.filemanager.tool.unzip;

/**
 * 进度监听回调
 * @author 28667
 */
@FunctionalInterface
public interface ProgressListener {
    void onProgress(int progress);
}



