package com.filemanager.tool;

import org.apache.commons.io.FileExistsException;
import com.filemanager.util.FileUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 按照关键词去除重复的文件夹
 */
public class DirSyncTool {


    public static void main(String[] args) {
        System.out.println("begin !");
        syncDir("H:\\","I:\\" );
//        renameFiles("L:\\", rules);
        System.out.println("done !");
    }

    public static void syncDir(String rootDir, String... otherDirs) {

        Map<File, File> dirAndParentMap = new HashMap<>();
        listFiles(0, new File(rootDir), dirAndParentMap);
        System.out.println(String.format("%s has  %d dirs ", rootDir,  dirAndParentMap.size()));

        for(String oneMoreDirRoot:otherDirs){
            Map<File, File> dirAndParentMap2 = new HashMap<>();
            listFiles(0, new File(oneMoreDirRoot), dirAndParentMap2);
            System.out.println();System.out.println();System.out.println();
            System.out.println("-----------------------------------           BEGIN         ------------------------------------");
            System.out.println(String.format("%s has  %d dirs ", oneMoreDirRoot,  dirAndParentMap2.size()));

            for(Map.Entry<File, File> entry:dirAndParentMap2.entrySet()){
                File dir = entry.getKey();
                File parentDir = entry.getValue();
                Map.Entry<File, File> sample = dirAndParentMap.entrySet().stream().filter(item -> item.getKey().getName().equals(dir.getName())).findFirst().orElse(null);
                if(sample==null){
                    System.out.println(String.format("%s has no %s in  %s  ", rootDir,  dir.getName(), oneMoreDirRoot));
                    continue;
                }
                if(sample.getValue().getPath().equals(rootDir)){
                    continue;
                }
                FileUtil.transferTo(dir, new File(sample.getValue().getPath().replace(rootDir, oneMoreDirRoot)));
            }

            System.out.println("-----------------------------------           END         ------------------------------------");
            System.out.println();System.out.println();System.out.println();
        }

    }

    // 递归方法遍历文件夹
    public static void listFiles(int depth, File folder, Map<File, File> dirAndParentMap) {
        // 获取文件夹下的所有文件和子文件夹
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        // 遍历文件和文件夹
        for (File file : files) {
            if(file.getName().startsWith("$")||file.getName().startsWith("del")||file.getName().startsWith("Sys")){
                continue;
            }
            if (file.isFile()) {
                continue;
            } else if (file.isDirectory()) {
                dirAndParentMap.put(file, folder);
                // 如果是文件夹,则递归调用listFiles方法
                listFiles(depth++, file, dirAndParentMap);
            }
        }
    }


    private static void tryRename(File file, String fix) {
        // 移动文件
        try {
            FileUtil.renameDir(file, file.getName().replace(fix, ""));
        } catch (Exception e) {
            // ignore
            if (e instanceof FileExistsException) {
                FileUtil.delete(file);
            } else {
                e.printStackTrace();
            }
        }

    }


}
