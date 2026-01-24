package com.filemanager.tool.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件夹合并工具类
 * 提供同名父子文件夹合并和嵌套文件夹合并功能
 */
public class FolderMergeUtil {

    /**
     * 合并同名父子文件夹
     * 将子文件夹的内容移动到父文件夹，然后删除子文件夹
     *
     * @param parentDir 父文件夹
     * @param childDir  子文件夹
     * @param overwrite 是否覆盖已有文件
     * @return 冲突的文件列表
     * @throws IOException 操作异常
     */
    public static List<File> mergeSameNameParentChild(File parentDir, File childDir, boolean overwrite) throws IOException {
        List<File> conflictingFiles = new ArrayList<>();

        // 确保父文件夹和子文件夹存在且是目录
        if (!parentDir.exists() || !parentDir.isDirectory() || !childDir.exists() || !childDir.isDirectory()) {
            throw new IllegalArgumentException("父文件夹或子文件夹不存在或不是目录");
        }

        // 检查父文件夹和子文件夹是否同名
        if (!parentDir.getName().equals(childDir.getName())) {
            throw new IllegalArgumentException("父文件夹和子文件夹名称不同，无法合并");
        }

        // 遍历子文件夹中的所有文件和子目录
        File[] subFiles = childDir.listFiles();
        if (subFiles != null) {
            for (File subFile : subFiles) {
                File destFile = new File(parentDir, subFile.getName());

                // 检查是否存在冲突
                if (destFile.exists()) {
                    if (!overwrite) {
                        conflictingFiles.add(subFile);
                        continue;
                    }
                }

                // 移动文件或文件夹
                if (subFile.isDirectory()) {
                    // 如果目标是目录，递归合并
                    if (destFile.exists() && destFile.isDirectory()) {
                        // 目标目录已存在，递归合并内容
                        List<File> subConflicts = mergeDirectoryContents(subFile, destFile, overwrite);
                        conflictingFiles.addAll(subConflicts);
                        // 删除已合并的子文件夹
                        deleteDirectoryRecursively(subFile);
                    } else {
                        // 移动整个目录
                        Files.move(subFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    // 移动文件
                    Files.move(subFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        // 删除空的子文件夹
        deleteDirectoryRecursively(childDir);

        return conflictingFiles;
    }

    /**
     * 合并目录内容
     * 将sourceDir的内容移动到destDir
     *
     * @param sourceDir 源目录
     * @param destDir   目标目录
     * @param overwrite 是否覆盖已有文件
     * @return 冲突的文件列表
     * @throws IOException 操作异常
     */
    private static List<File> mergeDirectoryContents(File sourceDir, File destDir, boolean overwrite) throws IOException {
        List<File> conflictingFiles = new ArrayList<>();

        File[] subFiles = sourceDir.listFiles();
        if (subFiles != null) {
            for (File subFile : subFiles) {
                File destFile = new File(destDir, subFile.getName());

                // 检查是否存在冲突
                if (destFile.exists()) {
                    if (!overwrite) {
                        conflictingFiles.add(subFile);
                        continue;
                    }
                }

                if (subFile.isDirectory()) {
                    // 如果目标是目录，递归合并
                    if (destFile.exists() && destFile.isDirectory()) {
                        List<File> subConflicts = mergeDirectoryContents(subFile, destFile, overwrite);
                        conflictingFiles.addAll(subConflicts);
                        deleteDirectoryRecursively(subFile);
                    } else {
                        Files.move(subFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    // 移动文件
                    Files.move(subFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        return conflictingFiles;
    }

    /**
     * 合并嵌套文件夹
     * 当父目录只有一个子目录文件夹且没有其他文件时，自动合并掉这些空的目录层次
     *
     * @param rootDir   根目录
     * @param overwrite 是否覆盖已有文件
     * @return 合并的文件夹数量
     * @throws IOException 操作异常
     */
    public static int mergeNestedFolders(File rootDir, boolean overwrite) throws IOException {
        int mergedCount = 0;

        // 确保根目录存在且是目录
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IllegalArgumentException("根目录不存在或不是目录");
        }

        // 遍历当前目录的所有子目录
        File[] subDirs = rootDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                // 递归处理子目录
                mergedCount += mergeNestedFolders(subDir, overwrite);

                // 检查当前目录是否只有一个子目录且没有其他文件
                File[] currentFiles = rootDir.listFiles();
                if (currentFiles != null && currentFiles.length == 1 && currentFiles[0].isDirectory()) {
                    File onlyChild = currentFiles[0];

                    // 检查子目录是否有内容
                    File[] childContents = onlyChild.listFiles();
                    if (childContents != null && childContents.length > 0) {
                        // 有内容，合并子目录的内容到当前目录
                        List<File> conflicts = mergeDirectoryContents(onlyChild, rootDir, overwrite);
                        if (conflicts.isEmpty()) {
                            // 没有冲突，删除子目录
                            deleteDirectoryRecursively(onlyChild);
                            mergedCount++;
                        }
                    }
                }
            }
        }

        return mergedCount;
    }

    /**
     * 递归删除目录
     *
     * @param directory 要删除的目录
     * @throws IOException 操作异常
     */
    public static void deleteDirectoryRecursively(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    Files.delete(file.toPath());
                }
            }
        }
        Files.delete(directory.toPath());
    }

    /**
     * 检查目录是否只有一个子目录且没有其他文件
     *
     * @param directory 要检查的目录
     * @return 如果只有一个子目录且没有其他文件，返回该子目录；否则返回null
     */
    public static File hasOnlyOneSubdirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null || files.length != 1) {
            return null;
        }

        File onlyFile = files[0];
        if (onlyFile.isDirectory()) {
            return onlyFile;
        }

        return null;
    }
}