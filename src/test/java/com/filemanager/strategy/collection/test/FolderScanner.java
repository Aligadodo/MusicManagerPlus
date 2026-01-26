package com.filemanager.strategy.collection.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件夹扫描器，用于扫描指定路径下的文件夹
 */
public class FolderScanner {
    
    /**
     * 扫描指定路径下的文件夹
     * @param path 扫描路径
     * @return 文件夹名称列表
     */
    public List<String> scanFolders(String path) {
        List<String> folderNames = new ArrayList<>();
        
        File directory = new File(path);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("路径不存在或不是目录: " + path);
            return folderNames;
        }
        
        File[] files = directory.listFiles(File::isDirectory);
        if (files != null) {
            for (File folder : files) {
                folderNames.add(folder.getName());
            }
        }
        
        System.out.println("扫描完成，找到 " + folderNames.size() + " 个文件夹");
        return folderNames;
    }
    
    /**
     * 递归扫描指定路径下的所有文件夹
     * @param path 扫描路径
     * @return 文件夹名称列表
     */
    public List<String> scanFoldersRecursive(String path) {
        List<String> folderNames = new ArrayList<>();
        scanFoldersRecursive(new File(path), folderNames);
        return folderNames;
    }
    
    private void scanFoldersRecursive(File directory, List<String> folderNames) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    folderNames.add(file.getName());
                    scanFoldersRecursive(file, folderNames);
                }
            }
        }
    }
}
