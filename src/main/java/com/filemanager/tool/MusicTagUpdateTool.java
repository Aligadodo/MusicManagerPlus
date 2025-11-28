package com.filemanager.tool;

import com.filemanager.domain.FileStatisticInfo;
import com.filemanager.domain.MusicInfo;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.flac.FlacFileReader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.wav.WavInfoTag;
import org.jaudiotagger.tag.wav.WavTag;
import com.filemanager.rule.Rule;
import com.filemanager.util.AudioTagUtils;
import com.filemanager.util.FileUtil;

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

    private static final String music_types = "wav";
//    private static final String music_types = "flac,wav,aiff,ape,dfd,dsf,iso";
    private static final List<Rule> rules = new ArrayList<>();

    static {

        rules.add(new Rule("", music_types));
//        rules.add(new Rule("",music_types).regex(".*[\\u4e00-\\u9fa5]{2,}.*"));
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("begin !");
//        renameFiles("H:\\8-待整理");
        renameFiles("X:\\0 - 专辑系列\\0 - 华语专辑\\C - 蔡琴\\");
//
//        renameFiles("I:\\");
//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void renameFiles(String rootDir) throws InterruptedException {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(rootDir), files, dirs);
        System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        AtomicLong count = new AtomicLong();
        files.forEach(
                file -> executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            count.incrementAndGet();
                            Rule rule = rules.stream().filter(fileClassifyRule -> fileClassifyRule.isApply(file)).findFirst().orElse(null);
                            if (rule != null) {
                                tryRename(file, true);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }finally {
                            count.decrementAndGet();
                        }
                    }
                })
        );
        while (count.get()>0){
            System.out.println("wait finish...");
            Thread.sleep(1000);
        }
        executorService.shutdown();
    }


    private static void tryRename(File file, boolean updateFileTags) {
        // 移动文件
        try {
            FileStatisticInfo fileStatisticInfo = FileStatisticInfo.create(file);
            if (fileStatisticInfo.isMusic()) {
                MusicInfo musicInfo = MusicRenameTool.queryMusicFileTag(file);
                updateMusicFileTag(musicInfo, fileStatisticInfo);
            }
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
        if (fileStatisticInfo.isMusic()) {
            updateFileTag(musicInfo, fileStatisticInfo);
        } else {
            updateOtherFileTag(musicInfo, fileStatisticInfo);
        }
    }


    private static void updateFileTag(MusicInfo musicInfo, FileStatisticInfo fileStatisticInfo) {
        File file = fileStatisticInfo.file;
        try {
            if (StringUtils.isBlank(musicInfo.songName)) {
                musicInfo.songName = fileStatisticInfo.getClassicName();
            }
            AudioFile audioFile = fileStatisticInfo.getAudioFileReader().read(file);
            audioFile.setExt(fileStatisticInfo.type);
            Tag fileTags = audioFile.getTag();
//            fileTags.setEncoding(Charset.());
            String artist = fileTags.getFirst(FieldKey.ARTIST);// 歌手名
            String album = fileTags.getFirst(FieldKey.ALBUM);// 專輯名
            String songName = fileTags.getFirst(FieldKey.TITLE);// 歌名
            System.out.println(file.getName() + " album: " + album +" --- "+musicInfo.album); // 專輯名
            System.out.println(file.getName() + " singer: " + artist +" --- "+musicInfo.artist); // 歌手名
            System.out.println(file.getName() + " songName: " + songName +" --- "+musicInfo.songName); // 歌名
            boolean anyUpdate = false;
            if (artist.contains("?") || StringUtils.isBlank(artist)) {
                if (musicInfo.artist != null) {
                    fileTags.setField(FieldKey.ARTIST, musicInfo.artist);
                    anyUpdate = true;
                }
            }
            if (songName.contains("?") || StringUtils.isBlank(songName)) {
                if (musicInfo.songName != null) {
                    fileTags.setField(FieldKey.TITLE, musicInfo.songName);
                    anyUpdate = true;
                }
            }
            if (album.contains("?")) {
                fileTags.setField(FieldKey.ALBUM, "");
                anyUpdate = true;
            }
            if (fileTags instanceof WavTag) {

                WavInfoTag fileTagsV2 = ((WavTag) fileTags).getInfoTag();
                String artistV2 = fileTagsV2.getFirst(FieldKey.ARTIST);// 歌手名
                String albumV2 = fileTagsV2.getFirst(FieldKey.ALBUM);// 專輯名
                String songNameV2 = fileTagsV2.getFirst(FieldKey.TITLE);// 歌名
                boolean needReset = true;
                if (artistV2.contains("?")) {
                    needReset = true;
                }
                if (songNameV2.contains("?") || songNameV2.contains("[") || songNameV2.contains("]") || StringUtils.isBlank(songNameV2)) {
                    needReset = true;
                }
                if (albumV2.contains("?")) {
                    needReset = true;
                }
                if (needReset) {
                    AudioTagUtils.fixAudioTags(file, (StringUtils.isBlank(musicInfo.songName) ?
                            songName : musicInfo.songName), (StringUtils.isBlank(musicInfo.artist) ? artist : musicInfo.artist), StringUtils.isBlank(musicInfo.album) ? album : musicInfo.album);
                    return;
                }
            }
            if (anyUpdate) {
                audioFile.commit();
                System.out.println("Metadata updated successfully.");
            }
        } catch (CannotReadException e) {
            // ignore
            int i = 0;
        } catch (Exception e) {
            // ignore
            int i = 0;
        }
    }

    private static void updateOtherFileTag(MusicInfo musicInfo, FileStatisticInfo fileStatisticInfo) {
        File file = fileStatisticInfo.file;
        try {
            AudioFile mp3File = new FlacFileReader().read(file);
            AudioFile audioFile = AudioFileIO.read(file);
            Tag fileTags = mp3File.getTag();
            String artist = fileTags.getFirst(FieldKey.ARTIST);// 歌手名
            String album = fileTags.getFirst(FieldKey.ALBUM);// 專輯名
            String songName = fileTags.getFirst(FieldKey.TITLE);// 歌名
            System.out.println(file.getName() + " album: " + album); // 專輯名
            System.out.println(file.getName() + " singer: " + artist); // 歌手名
            System.out.println(file.getName() + " songName: " + songName); // 歌名

        } catch (CannotReadException e) {
            // ignore
            int i = 0;
        } catch (Exception e) {
            // ignore
            int i = 0;
        }
    }


}
