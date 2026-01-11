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

import com.filemanager.util.FileUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 将特定目录下的文件，按照一定的规则合并到一个文件夹中
 * 执行合并后，会再做一次重命名操作
 */
public class DirCreateMergeTool {

    private static final String music_types = "mp3,flac,wav";

    public static void main(String[] args) {
        System.out.println("begin !");
        renameFiles("H:\\0-中文歌手");
//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void renameFiles(String rootDir) {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        Map<String, File> existingDirMapping = new HashMap<>();
        Map<String, Integer> noneExistingDirMapping = new HashMap<>();
        Arrays.stream(new File(rootDir).listFiles()).forEach(i -> {
            if (i.isDirectory()) {
                dirs.add(i);
                existingDirMapping.put(i.getName().toUpperCase(), i);
            } else {
                files.add(i);
            }
        });
        System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));

        // 批量创建文件夹
        files.forEach(
                file -> {
                    if (file.getName().indexOf('-') < 2) {
                        return;
                    }
                    String dir = MusicNameParserUtil.getDirNameFromMusicFile(file);
                    if (existingDirMapping.containsKey(dir)) {
                        return;
                    }
                    try {
                        noneExistingDirMapping.put(dir, noneExistingDirMapping.getOrDefault(dir, 0) + 1);
                        if (noneExistingDirMapping.get(dir) >= 2) {
                            Path path = new File(rootDir + File.separator + dir).toPath();
                            System.out.println("开始创建新文件夹 " + path);
                            Files.createDirectory(path);
                            existingDirMapping.put(dir, new File(rootDir + File.separator + dir));
                            System.out.println("成功创建新文件夹 " + path);
                        }
                    } catch (Throwable e) {
                        // do nothing
                    }
                }
        );

        files.forEach(
                file -> {
                    if (file.getName().indexOf('-') < 2) {
                        return;
                    }
                    String dir = MusicNameParserUtil.getDirNameFromMusicFile(file);
                    if (existingDirMapping.containsKey(dir)) {
                        FileUtil.transferTo(file, existingDirMapping.get(dir));
                    }
                }
        );


        MusicRenameTool.renameFiles(rootDir);


    }


}
