package com.filemanager.tool.backup;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import com.filemanager.rule.Rule;
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
public class NovelRenameTool {

    private static final String music_types = "txt";
    private static final List<Rule> rules = new ArrayList<>();

    static {

        rules.add(new Rule("", music_types));
//        rules.add(new Rule("",music_types).regex(".*[\\u4e00-\\u9fa5]{2,}.*"));
    }

    public static void main(String[] args) {
        System.out.println("begin !");
        renameFiles("D:\\小说\\阅读存档");
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

            FileUtil.renameFile(file, getFormatedName(file));

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

    private static String getFormatedName(File file) {
        String filename = file.getName();
        filename = filename.substring(0, filename.lastIndexOf('.'));
        String original = filename;
        filename = ZhConverterUtil.toSimple(filename);
        if(filename.indexOf("《")>2){
            filename=filename.substring(filename.indexOf("《"));
        }
        filename = filename.replaceAll("[（(【\\[]?[.0-9A-Za-z]{4,}com[]）)】]?","");
        filename = filename.replaceAll("[（(【\\[]?[.0-9A-Za-z]{4,}org[]）)】]?","");
        filename = filename.replaceAll("[（(【\\[].*版本[]）)】]","");
        filename = filename.replaceAll("[（(【\\[].*版[]）)】]","");
        filename = filename.replaceAll("[（(【\\[].*网址.*[]）)】]","");
        filename = filename.replaceAll("[（(【\\[].*搜书吧.*[]）)】]","");
        filename = filename.replaceAll("搜书吧网址","");
        filename = filename.replaceAll("搜书吧","");
        filename = filename.replaceAll("[（(【\\[].*全本.*[]）)】]","");
        filename = filename.replaceAll("[（(【\\[].*校对.*[]）)】]","");
        filename = filename.replaceAll("[（(【\\[].*[0-9]{1,3}.*[\\\\-_].*[0-9]{2,4}.*[]）)】]","");
        filename = filename.replaceAll("[0-9]{1,3}[_-].*[0-9]{2,4}.*$","");
        if(filename.indexOf("作者")>3){
            filename = filename.substring(0, filename.indexOf("作者"));
        }
        filename = filename.trim();
        while (filename.endsWith("-")){
            filename=filename.substring(0,filename.length()-1);
            filename = filename.trim();
        }
        filename = filename.replaceAll("《","");
        filename = filename.replaceAll("》","");
        if(filename.length()>3||(original.length()-filename.length()<4)) {
            return filename;
        }else{
            return original;
        }
    }

}
