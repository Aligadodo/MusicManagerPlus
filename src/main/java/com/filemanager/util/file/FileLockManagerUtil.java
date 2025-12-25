package com.filemanager.util.file;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟文件锁管理类 (基于内存)
 */
public class FileLockManagerUtil {

    // 使用线程安全的 Set 来存储已加锁的文件路径
    private static final Set<String> lockedFiles = ConcurrentHashMap.newKeySet();

    /**
     * 尝试对文件加锁
     * @param file 文件
     * @return 如果加锁成功返回 true；如果文件已被锁，返回 false
     */
    public static boolean lock(File file) {
        // add 方法如果元素已存在会返回 false，不存在则添加并返回 true
        return lockedFiles.add(file.getAbsolutePath());
    }

    /**
     * 释放文件锁
     * @param file 文件路径
     * @return 如果解锁成功返回 true；如果文件本来就没有锁，返回 false
     */
    public static boolean unlock(File file) {
        return lockedFiles.remove(file.getAbsolutePath());
    }

    /**
     * 检查文件是否已被锁
     * @param file 文件路径
     * @return 是否已被锁
     */
    public static boolean isLocked(File file) {
        return lockedFiles.contains(file.getAbsolutePath());
    }

    /**
     * 清除所有锁
     */
    public static void clearAllLocks() {
        lockedFiles.clear();
    }
}