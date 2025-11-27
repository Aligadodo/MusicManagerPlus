package tool;

import domain.FileStatisticInfo;
import org.apache.commons.io.FileExistsException;
import util.FileUtil;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 按照关键词去除重复的文件夹
 */
public class DirTagUpdateTool {

    private static final String music_types = "mp3,flac,wav";
    private static final String full_music_types = "mp3,flac,wav,aiff,ape,dfd,dsf,iso,dts,dff";
    private static final List<String> rules = new ArrayList<>();

    static {
        rules.add("(1)");
        rules.add("（1）");
    }

    public static void main(String[] args) {
        System.out.println("begin !");
//        renameFiles("X:\\0 - 专辑系列");
//        renameFiles("X:\\8 - 待整理");
        renameFiles("X:\\8 - 待整理");
//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void renameFiles(String rootDir) {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(rootDir), files, dirs);
        System.out.printf("dir has %d files and %d dirs %n", files.size(), dirs.size());
        Collections.reverse(dirs);
        dirs.forEach(
                dir -> {
                    if (dir.listFiles() == null) return;
                    Map<String, Integer> typeMap = getMusicTypesCount(dir);
                    long countDirs = Arrays.stream(dir.listFiles()).filter(File::isDirectory).count();
                    if (typeMap.size() == 1 && countDirs < 4) {
                        String type = typeMap.keySet().iterator().next().toUpperCase();
                        if (!dir.getName().toUpperCase().contains(type)) {
                            FileUtil.renameDir(dir, dir.getName() + " - " + type);
                        }
                    }
                }
        );

    }


    private static Map<String, Integer> getMusicTypesCount(File dir) {
        Map<String, Integer> map = new HashMap<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    FileStatisticInfo fileStatisticInfo = FileStatisticInfo.create(file);
                    if (full_music_types.contains(fileStatisticInfo.type)) {
                        map.put(fileStatisticInfo.type, map.getOrDefault(fileStatisticInfo.type, 0) + 1);
                    }
                }
            }
        }
        return map;
    }


}
