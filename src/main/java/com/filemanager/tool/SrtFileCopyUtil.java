package com.filemanager.tool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SrtFileCopyUtil {
    private static final String music_types = "mp3,flac,wav";

    public static void main(String[] args) {
        System.out.println("begin !");
        renameFiles("M:\\");
        renameFiles("N:\\");
//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void renameFiles(String rootDir) {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        Arrays.stream(new File(rootDir).listFiles()).forEach(i -> {
            if (i.isDirectory()) {
                dirs.add(i);
            } else {
                files.add(i);
            }
        });
        System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));

        dirs.forEach(dir -> {
            if(dir.listFiles()==null){
                return;
            }
            List<File> subFiles =Arrays.asList(Objects.requireNonNull(dir.listFiles()));
            File srt = subFiles.stream().filter(file -> file.isDirectory() && StringUtils.containsAnyIgnoreCase(file.getName(), "字幕","SRT.")).findFirst().orElse(null);
            File av = subFiles.stream().filter(file -> file.isDirectory() && StringUtils.containsAnyIgnoreCase(file.getName(), "watermark","video")).findFirst().orElse(null);
            if(srt==null||av==null){
                return;
            }
            try {
                FileUtils.copyDirectory(srt, av);
                int i=0;
            } catch (IOException e) {
                //throw new RuntimeException(e);
            }
        });


    }

}
