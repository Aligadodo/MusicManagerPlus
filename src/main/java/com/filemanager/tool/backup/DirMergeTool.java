/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.tool.backup;

import org.apache.commons.io.FileExistsException;
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
public class DirMergeTool {

    private static final String music_types = "mp3,flac,wav";
    private static final List<String> rules = new ArrayList<>();

    static {
        rules.add("(1)");
        rules.add("（1）");
    }

    public static void main(String[] args) {
        System.out.println("begin !");
        renameFiles("D:\\下载\\");
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
        dirs.forEach(
                file -> {
                    String fix = rules.stream().filter(item -> file.getName().contains(item)).findFirst().orElse(null);
                    if (fix != null) {
                        executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                count.addAndGet(1);
                                tryRename(file, fix);
                                count.decrementAndGet();
                            }
                        });
                    }
                }
        );
        while (count.get() > 0) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                //
            }
        }

    }


    private static void tryRename(File file, String fix) {
        // 移动文件
        try {
            FileUtil.renameDir(file, file.getName().replace(fix, ""));
        } catch (Exception e) {
            // ignore
            if (e instanceof FileExistsException) {
                FileUtil.delete(file);
            } else {
                e.printStackTrace();
            }
        }

    }


}
