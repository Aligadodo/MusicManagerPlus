package tool;

import domain.FileStatisticInfo;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import util.FileUtil;
import util.MusicNameParserUtil;

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
public class MusicNameSwapByDirTool {

    public static void main(String[] args) {
        System.out.println("begin !");
//        renameFiles("H:\\8-待整理");
        renameFiles("H:\\0-中文歌手");
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
                    List<File> files = Arrays.asList(dir.listFiles()).stream().filter(file -> {
                        return AutoMucisNameSwapTool.isMusicFile(file) && file.getName().contains("-");
                    }).collect(Collectors.toList());
                    files.forEach(file -> {
                        FileStatisticInfo statisticInfo = FileStatisticInfo.create(file);
                        if (statisticInfo.classicName.endsWith(dir.getName())) {
                            if (statisticInfo.oriName.startsWith(dir.getName() + "-")
                                    && statisticInfo.oriName.length() - statisticInfo.oriName.lastIndexOf("—") <= 5
                                    && statisticInfo.oriName.lastIndexOf("—") > 5) {
                                FileUtil.renameFile(file, statisticInfo.oriName.substring(0, statisticInfo.oriName.lastIndexOf("—")));
                            } else {
                                tryRename(file, "-");
                            }
                        }
                    });
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
            System.out.println("try covert " + oriName + " to " + songName);
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
