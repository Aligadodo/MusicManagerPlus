package tool;

import rule.Rule;
import util.FileUtil;
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

public class MusicToWavUtil {

    private static final List<Rule> rulesOfCopy = new ArrayList<>();

    static {
        rulesOfCopy.add(new Rule("-", "wav"));
//        rules.add(new Rule("",music_types).regex(".*[\\u4e00-\\u9fa5]{2,}.*"));
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("begin !");
        syncDir( "K:\\音乐存档（WAV格式）\\","K:\\音乐存档（DSD格式）");
//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void syncDir(String destDir, String... rootDirs) throws InterruptedException {
        ExecutorService executorService = new ThreadPoolExecutor(2, 2, 60L, TimeUnit.SECONDS,
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
                                    Rule rule2 = rulesOfCopy.stream().filter(fileTransRule -> fileTransRule.isApply(file)).findFirst().orElse(null);
                                    if(rule2==null){
                                        return;
                                    }
                                    File destDirectory = new File(file.getParentFile().getPath().replace("K:\\",destDir)+File.separator);
                                    if(rule2 != null){
                                        System.out.println("copy from  "+file.getName() + " to "+ destDirectory.getPath());
                                        try {
                                            FileUtil.transferTo(file, destDirectory, true);
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
            attrs.setOutputFormat("flac");
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
