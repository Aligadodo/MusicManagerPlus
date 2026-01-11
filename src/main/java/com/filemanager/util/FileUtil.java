/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.util;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class FileUtil {
    private static boolean REAL_DELETE = false;

    public static void batchCreateDirUnder(Collection<String> dirs, File rootDir){
        for(String dir:dirs){
            try {
                FileUtils.forceMkdir(new File(rootDir.getPath()+File.separator+dir));
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void delete(File file){
        if(REAL_DELETE){
            try {
                if(file.isDirectory()){
                    FileUtils.deleteDirectory(file);
                    System.out.println(String.format("dir %s deleted! ", file.getPath()));
                }else{
                    FileUtils.delete(file);
                    System.out.println(String.format("file %s deleted! ", file.getPath()+file.getName()));
                }
            }catch (Exception e){

            }
        }else{
            String root = file.getPath().split(":")[0];
            transferTo(file, new File(root+":"+"del"+File.separator+file.getParentFile().getPath().split(":")[1]));
        }
    }


    public static String getFileType(File file) {
        if (file.isDirectory()) {
            return "directory";
        }
        String filename = file.getName();
        if (file.getName().indexOf('.') == -1) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public static double getFileSizeMB(File file) {
        if (file.isDirectory()) {
            return 0.0;
        }
        return (file.length() + 0.0) / (1024 * 1024);
    }


    // 递归方法遍历文件夹
    public static void listFiles(int depth, File folder, List<File> allFiles, List<File> allDirs) {
        // 获取文件夹下的所有文件和子文件夹
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        // 遍历文件和文件夹
        for (File file : files) {
            if(file.getName().startsWith("$")||file.getName().startsWith("del")||file.getName().startsWith("Sys")){
                continue;
            }
            // 判断是否是文件,如果是文件则输出文件名
            if (file.isFile()) {
                allFiles.add(file);
            } else if (file.isDirectory()) {
                allDirs.add(file);
                // 如果是文件夹,则递归调用listFiles方法
                listFiles(depth++, file, allFiles, allDirs);
            }
        }
    }


    public static void transferTo(File file, File destPath) {
        transferTo(file, destPath, false);
    }
    // 转移文件目录
    public static void transferTo(File file, File destPath, boolean isForce) {
        if(file.getParentFile().equals(destPath)){
            return;
        }
        // 子文件夹的东西不用移到外面
        if (!isForce&&file.getPath().startsWith(destPath.getPath())&&!destPath.getPath().startsWith(file.getPath())) {
            // System.out.println(String.format("file %s no need move ! ", file.getPath()));
            return;
        }
        // 移动文件
        try {
            System.out.println(file + " moving to " + destPath.getPath());
            if(file.isFile()) {
                if(!destPath.exists()){
                    FileUtils.createParentDirectories(destPath);
                }
                FileUtils.moveFileToDirectory(file, destPath, true);
                System.out.println(file + " moved to " + destPath.getPath());
            }else{
                FileUtils.moveDirectoryToDirectory(file, destPath, true);
                System.out.println(file + " moved to " + destPath.getPath());
            }
        } catch (IOException e) {
            // ignore
            if (e instanceof FileExistsException) {
                try {
                    System.out.println(file + " exists in " + destPath.getPath());
                    FileUtils.delete(new File(destPath.getPath() + File.separator + file.getName()));
                    FileUtils.moveFileToDirectory(file, destPath, false);
                    System.out.println(file + " overwrite to " + destPath.getPath());
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
        }

    }

    public static void copyTo(File file, File destPath, boolean isForce) {
        if(file.getParentFile().equals(destPath)){
            return;
        }
        // 子文件夹的东西不用移到外面
        if (!isForce&&file.getPath().startsWith(destPath.getPath())&&!destPath.getPath().startsWith(file.getPath())) {
            // System.out.println(String.format("file %s no need move ! ", file.getPath()));
            return;
        }
        // 移动文件
        try {
            System.out.println(file + " copy to " + destPath.getPath());
            if(file.isFile()) {
                if(new File(destPath.getPath()+File.separator+file.getName()).exists()){
                    System.out.println(file + " ignore copy to " + destPath.getPath());
                    return;
                }
//                FileUtils.createParentDirectories(new File(destPath.getPath()));
                FileUtils.copyFileToDirectory(file, destPath, true);
                System.out.println(file + " copy to " + destPath.getPath());
            }else{
                FileUtils.copyDirectoryToDirectory(file, destPath);
                System.out.println(file + " copy to " + destPath.getPath());
            }
        } catch (IOException e) {
            // ignore
            if (e instanceof FileExistsException) {
                try {
                    System.out.println(file + " exists in " + destPath.getPath());
                    FileUtils.delete(new File(destPath.getPath() + File.separator + file.getName()));
                    FileUtils.copyFileToDirectory(file, destPath, false);
                    System.out.println(file + " overwrite to " + destPath.getPath());
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
        }

    }

    // 转移文件目录
    public static void renameFile(File file, String filename) {
        if (file.getName().equals(filename) || StringUtils.isAllBlank(filename)) {
            // System.out.println(String.format("file %s no need move ! ", file.getPath()));
            return;
        }
        String oriName = file.getName();
        String newName = filename + oriName.substring(oriName.lastIndexOf('.'));
        // 修改文件名
        try {
            if (file.getName().equals(newName)) {
                return;
            }
            File targetFile = new File(file.getParentFile(), newName);
            if (file.renameTo(targetFile)) {
                System.out.println("文件名修改成功: ");
            } else {
                if(targetFile.exists()) {
                    System.out.println("目标文件存在，取较大的一个: ");
                    if (file.length() > targetFile.length()) {
                        delete(targetFile);
                        file.renameTo(new File(file.getParentFile(), newName));
                    } else {
                        FileUtils.delete(file);
                    }
                }
            }
            System.out.println(file + " ------>> " + filename);
        } catch (Exception e) {
            // ignore
        }

    }


    // 转移文件目录
    public static void renameDir(File dir, String filename) {
        if (dir.getName().equals(filename) || StringUtils.isAllBlank(filename)) {
            // System.out.println(String.format("file %s no need move ! ", file.getPath()));
            return;
        }
        // 修改文件名
        try {
            File[] files = dir.getParentFile().listFiles();
            File sameDir = null;
            for (File item : files) {
                if (item.isDirectory() && item.getName().equals(filename)) {
                    sameDir = item;
                }
            }
            if (sameDir == null) {
                if (dir.renameTo(new File(dir.getParentFile(), filename))) {
                    System.out.println("文件夹修改成功: ");
                } else {
                    System.out.println("文件夹修改失败: ");
                }
            } else {
                File[] subFiles = dir.listFiles();
                File finalSameDir = sameDir;
                Arrays.stream(subFiles).forEach(item -> transferTo(item, finalSameDir));
                FileUtils.deleteDirectory(dir);
            }
            System.out.println(dir + " ------>> " + filename);
        } catch (Exception e) {
            // ignore
            if (e instanceof FileExistsException) {
//                try {
//                    //FileUtils.delete(file);
//                } catch (IOException ex) {
//                    e.printStackTrace();
//                }
            } else {
                e.printStackTrace();
            }
        }

    }


}
