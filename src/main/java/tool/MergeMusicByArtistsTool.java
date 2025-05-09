package tool;

import com.alibaba.fastjson.JSONObject;
import domain.FileStatisticInfo;
import org.apache.commons.lang3.StringUtils;
import util.FileUtil;
import util.MusicNameParserUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 将目录下的文件，如果歌手能匹配目标空间的，全部移动到目标空间去，避免零碎分布的歌曲
 */
public class MergeMusicByArtistsTool {

    private static final String full_music_types = "mp3,flac,wav,aiff,ape,dfd,dsf,iso";

    public static void main(String[] args) {
        System.out.println("begin !");
//        renameFiles("H:\\8-待整理");
        renameFiles("H:\\8-待整理");
//        renameFiles("I:\\中文歌手");

//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void renameFiles(String rootDir) {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(rootDir), files, dirs);
        System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));

        Map<String,Integer> countFind = new HashMap<>();

        files.forEach(
                file -> {
                    if (file.getName().indexOf('-') < 2) {
                        return;
                    }
                    FileStatisticInfo fileStatisticInfo = FileStatisticInfo.create(file);
                    if(!full_music_types.contains(fileStatisticInfo.type)){
                        return;
                    }
                    String dir = MusicNameParserUtil.getDirNameFromMusicFile(file, false);
                    if (MusicNameParserUtil.existingDirMapping.containsKey(dir)) {
                        System.out.println(file + " moved to " + MusicNameParserUtil.existingDirMapping.get(dir));
                        FileUtil.transferTo(file, MusicNameParserUtil.existingDirMapping.get(dir));
                    }else{
                        if(dir!=null&&dir.length()<10){
                            countFind.put(dir, countFind.getOrDefault(dir, 0)+1);
                        }
                    }
                }
        );

        // 输出出现多于一定次数的歌手名
        Set<String> newDirS = countFind.entrySet().stream().filter(i->i.getValue()>=2
                        && !StringUtils.containsAny(i.getKey(),"CD", "音轨", "Track", "Record"
                        ,"群星","Artist","DTS","惊叹号","跨时代","七里香","我很忙","彼得","神笛","标题","IA","中孝介",
                        "WAV","FLAC","MP3"))

                .map(Map.Entry::getKey).collect(Collectors.toSet());

        System.out.println(JSONObject.toJSONString(newDirS));
        //自动创建目录
//        FileUtil.batchCreateDirUnder(newDirS, new File(rootDir));
//        FileUtil.batchCreateDirUnder(newDirS, new File("H:\\0-中文歌手"));
        //FileUtil.batchCreateDirUnder(newDirS, new File("H:\\0-日韩歌手"));
        //FileUtil.batchCreateDirUnder(newDirS, new File("H:\\0-日韩歌手"));

    }


}
