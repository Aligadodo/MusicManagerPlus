package com.filemanager.tool.backup;

import com.filemanager.rule.Rule;
import com.filemanager.util.FileUtil;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MusicToMp3Util {
    private static final String music_types = "flac,wav";
    private static final List<Rule> rulesOfTrans = new ArrayList<>();
    private static final List<Rule> rulesOfCopy = new ArrayList<>();

    static {

        rulesOfTrans.add(new Rule("-", music_types));
        rulesOfCopy.add(new Rule("-", "mp3,lrc"));
//        rules.add(new Rule("",music_types).regex(".*[\\u4e00-\\u9fa5]{2,}.*"));
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("begin !");
        syncDir("Q:\\音乐存档（MP3格式）\\","H:\\" );
//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void syncDir(String destDir, String... rootDirs) throws InterruptedException {
        ExecutorService executorService = new ThreadPoolExecutor(5, 10, 60L, TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<Runnable>());
        // 设置全局默认异常处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Unhandled exception in thread: " + thread.getName());
            System.err.println("Exception: " + throwable.getMessage());
            throwable.printStackTrace();
        });
        AtomicLong countRunning = new AtomicLong();
        for (String rootDir:rootDirs) {
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println("-----------------------------------           BEGIN         ------------------------------------");


            List<File> files = new ArrayList<>();
            List<File> dirs = new ArrayList<>();
            FileUtil.listFiles(0, new File(rootDir), files, dirs);
            System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));


            files.forEach(
                    file -> {
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    countRunning.incrementAndGet();
                                    System.out.println(file.getName() + " process begin ");
                                    Rule rule = rulesOfTrans.stream().filter(fileTransRule -> fileTransRule.isApply(file)).findFirst().orElse(null);
                                    Rule rule2 = rulesOfCopy.stream().filter(fileTransRule -> fileTransRule.isApply(file)).findFirst().orElse(null);
                                    if(rule2==null&&rule==null){
                                        return;
                                    }
                                    if(file.getPath().contains("8-待整理")){
                                        return;
                                    }
                                    File destDirectory = new File(file.getParentFile().getPath().replace("H:\\",destDir)+File.separator);
                                    File destFile = new File(file.getParentFile().getPath().replace("H:\\",destDir)
                                            +File.separator+file.getName().substring(0,file.getName().lastIndexOf('.'))+".mp3");
                                    if(destFile.exists()){
                                        return;
                                    }
                                    if(rule != null){
                                        System.out.println("trans from  "+file.getName() + " to "+ destFile.getPath());
                                        try {
                                            audioConvert2Mp3(file, destFile);
                                            return;
                                        } catch (Exception e) {
                                            //
                                        }
                                    }
                                    if(rule2 != null){
                                        System.out.println("copy from  "+file.getName() + " to "+ destDirectory.getPath());
                                        try {
                                            FileUtil.copyTo(file, destDirectory, true);
                                            return;
                                        } catch (Exception e) {
                                            //
                                        }
                                    }
                                    System.out.println(file.getName() + " process done ");
                                } catch (Throwable e ) {
                                    System.out.println(file.getName() + " process fail " + e.getMessage());
                                } finally {
                                    countRunning.decrementAndGet();
                                }
                            }
                        });

                    }
            );

            System.out.println("-----------------------------------           END         ------------------------------------");
            System.out.println();
            System.out.println();
            System.out.println();
        }

        while (countRunning.get()>0){
            System.out.println("wait finish...");
            Thread.sleep(1000);
        }
        executorService.shutdown();

    }

    /**
     * 将音频转换为mp3
     *
     * @param source 源音频文件
     * @param target 输出的音频文件
     */
    public static void audioConvert2Mp3(File source, File target)
    {
        try
        {
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(128000);
            audio.setChannels(2);
            audio.setSamplingRate(44100);
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);
            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(source), target, attrs);
        }
        catch(Exception ex)
        {
            System.out.println("ERROR "+ex.getMessage());
        }
    }
}
