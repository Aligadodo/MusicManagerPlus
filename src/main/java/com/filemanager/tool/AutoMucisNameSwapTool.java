package com.filemanager.tool;

import com.filemanager.model.FileStatisticInfo;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import com.filemanager.util.FileUtil;
import com.filemanager.util.MusicNameParserUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 简易的交换歌手和歌名的工具
 */
public class AutoMucisNameSwapTool {

    private static final String[] music_types = new String[]{".mp3", ".flac", ".wav"};

    public static boolean isMusicFile(File file) {
        return StringUtils.endsWithAny(file.getName(), music_types);
    }

    public static void main(String[] args) {
        System.out.println("begin !");
        renameFiles("H:\\8-待整理");
//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void renameFiles(String rootDir) {

        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(rootDir), new ArrayList<>(), dirs);
        System.out.println(String.format("dir has  %d dirs ", dirs.size()));
        dirs.forEach(
                dir -> {
                    if (dir.listFiles() == null) {
                        return;
                    }
                    List<File> files = Arrays.asList(dir.listFiles()).stream().filter(file->{
                        return AutoMucisNameSwapTool.isMusicFile(file)&&file.getName().contains("-");
                    }).collect(Collectors.toList());
                    long count = files.stream().filter(file -> {
                        String art = MusicNameParserUtil.getDirNameFromMusicFile(file);
                        FileStatisticInfo musicInfo = FileStatisticInfo.create(file);
                        return art != null && StringUtils.endsWithIgnoreCase(musicInfo.classicName,art);
                    }).count();
                    if (!files.isEmpty() && count * 4 > files.size()) {
                        files.forEach(file -> tryRename(file,"-"));
                    }
                }
        );

    }


    private static void tryRename(File file, String seperator) {
        // 移动文件
        try {
            String oriName = file.getName();
            oriName = oriName.substring(0, oriName.lastIndexOf('.'));
            List<String> strs = Arrays.asList(oriName.split(seperator));
            Collections.reverse(strs);
            String songName = StringUtils.join(strs, seperator);
            System.out.println("try covert "+oriName+" to "+songName);
            FileUtil.renameFile(file, songName);
        } catch (Exception e) {
            // ignore
            if (e instanceof FileExistsException) {
                try {
                    FileUtils.delete(file);
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
        }

    }


}
