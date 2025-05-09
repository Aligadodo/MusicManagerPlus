package tool;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import rule.Rule;
import util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class FileDunplicateTool {

    private static final String music_types = "mp3,flac,wav,ncm,ape";
    private static final List<Rule> rules = new ArrayList<>();

    static {
        rules.add(new Rule("-", music_types));
    }

    public static void main(String[] args) {
        System.out.println("begin !");
        scanFiles("I:\\", rules);
//        scanFiles("D:\\小说\\阅读存档", rules);
//        scanFiles("H:\\0-中文歌手", rules);
//        scanFiles("H:\\0-欧美歌手", rules);
//        scanFiles("Q:\\", rules);
        System.out.println("done !");
    }

    public static void scanFiles(String rootDir, List<Rule> rules) {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(rootDir), files, dirs);
        System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));
        ExecutorService executorService = Executors.newFixedThreadPool(12);
        AtomicLong count = new AtomicLong();
        Map<String, File> fileMap = new HashMap<>();
        files.forEach(file -> {
            Rule rule = rules.stream().filter(fileTransRule -> fileTransRule.isApply(file)).findFirst().orElse(null);
            if (rule != null) {
                try {
                    String filename = getFormatedName(file);
                    if (StringUtils.containsAny(filename, ".", "CD", "音轨", "Track", "Record")) {
                        return;
                    }
                    if (fileMap.containsKey(filename)) {
                        File former = fileMap.get(filename);
                        if (former.getTotalSpace() > file.getTotalSpace()) {
                            FileUtil.delete(file);
                            System.out.println("del " + file.getName());
                        } else {
                            FileUtil.delete(former);
                            fileMap.put(filename, file);
                            System.out.println("del " + file.getName());
                        }
                    } else {
                        fileMap.put(filename, file);
                    }

                } catch (Exception e) {
                    //
                }
            }
        });


    }



    private static String getFormatedName(File file) {
        String filename = file.getName();
        filename = filename.substring(0, filename.lastIndexOf('.'));
        filename = filename.replaceAll(" ", "");
        filename = filename.toUpperCase();
        filename = ZhConverterUtil.toSimple(filename);
        filename = filename.replaceAll("（.*全本.*）","");
        if(filename.indexOf("作者")>3){
            filename = filename.substring(filename.indexOf("作者"));
        }
        filename = filename.replaceAll("（.*校对.*）","");
        filename = filename.replaceAll("【.*全本.*】","");
        return filename;
    }


}
