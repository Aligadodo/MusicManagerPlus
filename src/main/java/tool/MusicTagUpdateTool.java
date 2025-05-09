package tool;

import domain.FileStatisticInfo;
import domain.MusicInfo;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.flac.FlacFileReader;
import org.jaudiotagger.audio.mp3.MP3FileReader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import rule.Rule;
import util.FileUtil;
import util.MusicNameParserUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分场景重命名歌曲，标准化命名，补充tag信息等；
 */
public class MusicTagUpdateTool {

    //private static final String music_types = "mp3,flac,wav";
    private static final String music_types = "mp3,flac,wav,aiff,ape,dfd,dsf,iso";
    private static final List<Rule> rules = new ArrayList<>();

    static {

        rules.add(new Rule("", music_types));
//        rules.add(new Rule("",music_types).regex(".*[\\u4e00-\\u9fa5]{2,}.*"));
    }

    public static void main(String[] args) {
        System.out.println("begin !");
//        renameFiles("H:\\8-待整理");
        renameFiles("C:\\Users\\28667\\Downloads\\VipSongsDownload");
//
//        renameFiles("I:\\");
//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void renameFiles(String rootDir) {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(rootDir), files, dirs);
        System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        AtomicLong count = new AtomicLong();
        files.forEach(
                file -> {
                    Rule rule = rules.stream().filter(fileClassifyRule -> fileClassifyRule.isApply(file)).findFirst().orElse(null);
                    if (rule != null) {
                        tryRename(file, true);
                    }
                }
        );

    }


    private static void tryRename(File file, boolean updateFileTags) {
        // 移动文件
        try {
            FileStatisticInfo fileStatisticInfo = FileStatisticInfo.create(file);
            updateMusicFileTag(null, fileStatisticInfo);
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

    private static void updateMusicFileTag(MusicInfo musicInfo, FileStatisticInfo fileStatisticInfo) {
        updateMp3FileTag(musicInfo, fileStatisticInfo);
    }


    private static void updateMp3FileTag(MusicInfo musicInfo, FileStatisticInfo fileStatisticInfo) {
        File file = fileStatisticInfo.file;
        try {
            if(!fileStatisticInfo.file.getName().contains("mp3")){
                return;
            }
            AudioFile mp3File = new FlacFileReader().read(file);
            Tag fileTags = mp3File.getTag();
            if(fileTags!=null){
                file.renameTo(new File(file.getParentFile(), file.getName().replace("mp3","flac")));
            }
            String artist = fileTags.getFirst(FieldKey.ARTIST);// 歌手名
            String album = fileTags.getFirst(FieldKey.ALBUM);// 專輯名
            String songName = fileTags.getFirst(FieldKey.TITLE);// 歌名
            System.out.println(file.getName() + " album: " + album); // 專輯名
            System.out.println(file.getName() + " singer: " + artist); // 歌手名
            System.out.println(file.getName() + " songName: " + songName); // 歌名

        } catch (CannotReadException e) {
            //
        } catch (Exception e) {
            // ignore
        }
    }





}
