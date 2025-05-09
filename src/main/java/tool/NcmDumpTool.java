package tool;

import dump.NcmDump;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NcmDumpTool {

    public static void main(String[] args) throws IOException {
        runWithThreads();
//        if (args.length == 0) {
//            ErrorUtils.error("No input .ncm File");
//        } else {
//            for (String arg : args) {
//                File file = new File(arg);
//                NcmDump ncmDump = new NcmDump(file);
//                ncmDump.execute();
//            }
//        }
    }


    private static void runWithThreads() throws IOException {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                1,  //核心线程数
                5,             //最大线程数
                60,            //临时线程的最长空闲时间
                TimeUnit.SECONDS,   //空闲时间的单位
                new ArrayBlockingQueue<>(100),  //传递任务的阻塞队列
                Executors.defaultThreadFactory(),   //用于创建新线程的线程工厂
                new ThreadPoolExecutor.CallerRunsPolicy()    //超出负荷时的拒绝策略
        );

        //File folder = new File("D:\\音乐\\Music");
        //L:\网易云
        File folder = new File("C:\\Users\\28667\\Downloads\\VipSongsDownload");
        File[] files = folder.listFiles();


        List<File> filesToTrans = new ArrayList<>();
        List<String> existingFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.getName().toLowerCase().endsWith(".ncm")) {
                    if(file.getTotalSpace()>10000&&file.getName().lastIndexOf('.')>0) {
                        existingFiles.add(file.getName().substring(0, file.getName().lastIndexOf('.')) + ".ncm");
                    }else{
                        Files.delete(file.toPath());
                        file.delete();
                    }
                }
            }
            for (File file : files) {
                if (file.isFile() && file.exists() && file.getName().toLowerCase().endsWith(".ncm")) {
                    if(existingFiles.contains(file.getName())) {
                        Files.delete(file.toPath());
                        System.out.println("delete "+file.getName());
                    }else{
                        filesToTrans.add(file);
                    }
                }
            }
        }

        AtomicInteger countRunning = new AtomicInteger();
        for (File file : filesToTrans) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        countRunning.incrementAndGet();
                        System.out.println(file.getName() + " process begin ");
                        NcmDump ncmDump = new NcmDump(file);
                        ncmDump.execute();
                        file.delete();
                        System.out.println(file.getName() + " process done ");
                    } catch (Throwable e) {
                        System.out.println(file.getName() + " process fail " + e.getMessage());
                    } finally {
                        countRunning.decrementAndGet();
                    }
                }
            });
        }

        try {
            while (countRunning.get()>0) {
                Thread.sleep(1000);
            }
            //关闭线程池
            threadPool.shutdown();
            System.out.println("all process done ...");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
