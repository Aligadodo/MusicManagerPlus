package com.filemanager.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileListSorter {
    public static void main(String[] args) {
        String directoryPath = "X:\\0 - 专辑系列\\2 - 索尼Hi-Res古典畅销专辑精选\\史诗交响 - 古典乐之新声"; // 替换为你的目录路径
        List<String> fileNames = new ArrayList<>();
        String music_types = "mp3,flac,wav,aiff,ape,dfd,dsf,iso";
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.getName().endsWith(".lrc")) {
                    fileNames.add(file.getName().substring(0,file.getName().lastIndexOf('.')));
                }
            }
        }

        Collections.sort(fileNames); // 对文件名进行排序
        for (String fileName : fileNames) {
            System.out.println(fileName);
        }
    }
}
