package com.filemanager.tool;

import com.filemanager.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 按照关键词去除重复的文件夹
 */
public class DirUnrapTool {


    public static void main(String[] args) {
        System.out.println("begin !");
        renameFiles("H:\\0-中文歌手\\周杰伦\\");
//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void renameFiles(String rootDir) {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(rootDir), files, dirs);
        System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        AtomicLong count = new AtomicLong();
        files.forEach(
                file -> {
                    FileUtil.transferTo(file, new File(rootDir), true);
                }
        );


    }





}
