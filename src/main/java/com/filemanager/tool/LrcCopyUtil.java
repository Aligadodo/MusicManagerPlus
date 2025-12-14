package com.filemanager.tool;

import com.filemanager.model.FileStatisticInfo;
import org.apache.commons.lang3.StringUtils;
import com.filemanager.util.FileUtil;
import com.filemanager.util.MusicNameParserUtil;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class LrcCopyUtil {


    public static Map<String, File> existingDirMapping = new HashMap<>();
    private static final Pattern eng = Pattern.compile("[0-9a-zA-Z\\s]*");

    static {
        scanArtistDirs("H:\\");

        System.out.println(String.format("find %d dirs ", existingDirMapping.size()));
    }


    private static void scanArtistDirs(String dir) {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(dir), files, dirs);
        files.forEach(i -> {
            if(i.getName().contains("-")&&MusicNameParserUtil.isMusicFile(i.getName())){
                FileStatisticInfo statisticInfo = FileStatisticInfo.create(i);
                existingDirMapping.put(statisticInfo.oriName, i.getParentFile());
            }
        });
    }

    public static void main(String[] args) {
        System.out.println("begin !");
        renameFiles("I:\\");
        System.out.println("done !");
    }

    public static void renameFiles(String rootDir) {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(rootDir), files, dirs);
        System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));

        files.forEach(file -> {
            FileStatisticInfo statisticInfo = FileStatisticInfo.create(file);
            if((existingDirMapping.containsKey(statisticInfo.oriName)) && StringUtils.equalsAnyIgnoreCase(statisticInfo.type, "lrc", "jpg")){
                FileUtil.copyTo(file, existingDirMapping.get(statisticInfo.oriName), false);
            }else if((existingDirMapping.containsKey(file.getParentFile().getName()+"-"+statisticInfo.oriName)) && StringUtils.equalsAnyIgnoreCase(statisticInfo.type, "lrc", "jpg")){
                FileUtil.copyTo(file, existingDirMapping.get(file.getParentFile().getName()+"-"+statisticInfo.oriName), false);
            }
        });


    }
}
