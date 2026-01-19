package com.filemanager.tool.unzip;

/**
 * 解压引擎通用接口
 */
public interface UnarchiveEngine {
    boolean extract(UnarchiveTask task);
    boolean isAvailable();
}