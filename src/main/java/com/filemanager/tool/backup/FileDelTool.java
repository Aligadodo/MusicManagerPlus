package com.filemanager.tool.backup;

import com.filemanager.rule.Rule;
import com.filemanager.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 按照匹配规则删除文件
 */
public class FileDelTool {

    private static final String music_types = "mp3";
    private static final List<Rule> rules = new ArrayList<>();
    static {
        rules.add(new Rule("-",music_types).maxFileSize(5000));
    }

    public static void main(String[] args) {
        System.out.println("begin !");
//        scanFiles("H:\\", rules);
        scanFiles("H:\\0-网易云歌单系列", rules);
        scanFiles("I:\\网易云歌单系列", rules);
        scanFiles("Q:\\精选音乐备份\\网易云歌单系列", rules);
        System.out.println("done !");
    }

    public static void scanFiles(String rootDir,List<Rule> rules){
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(rootDir), files, dirs);
        System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));
        ExecutorService executorService = Executors.newFixedThreadPool(12);
        AtomicLong count = new AtomicLong();
        files.forEach(
                file -> {
                    Rule rule = rules.stream().filter(fileTransRule -> fileTransRule.isApply(file)).findFirst().orElse(null);
                    if(rule != null){
                        System.out.println("del "+file.getName());
                        try {
                            FileUtil.delete(file);
                        } catch (Exception e) {
                            //
                        }
                    }
                }
        );
        dirs.stream().sorted(Collections.reverseOrder()).forEach(
                dir -> {
                    File[] filesCheck = dir.listFiles();
                    if (filesCheck==null||filesCheck.length == 0) {
                        try {
                            FileUtil.delete(dir);

                        } catch (Exception e) {
                            e.printStackTrace() ;
                        }
                    }
                }
        );


    }





}
