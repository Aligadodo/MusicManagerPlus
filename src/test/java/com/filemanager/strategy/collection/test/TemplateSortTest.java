package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 测试TEMPLATE策略的排序逻辑
 */
public class TemplateSortTest {
    
    @Test
    public void testTemplateSort() {
        System.out.println("=== 测试TEMPLATE策略的排序逻辑 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("[龙音海文版 CD-0221]茉莉芬芳-陈爱娟古筝独奏之一");
        filenames.add("[龙音海文版 CD-0176]小河淌水-陈爱娟古筝独奏之二");
        
        System.out.println("原始文件名列表:");
        for (String filename : filenames) {
            System.out.println("  - " + filename);
        }
        
        // 复制文件名列表并排序
        List<String> sortedFilenames = new ArrayList<>(filenames);
        Collections.sort(sortedFilenames);
        
        System.out.println("\n排序后的文件名列表:");
        for (int i = 0; i < sortedFilenames.size(); i++) {
            System.out.println("  " + i + ": " + sortedFilenames.get(i));
        }
        
        // 取中间的文件名
        int middleIndex = sortedFilenames.size() / 2;
        String middleFilename = sortedFilenames.get(middleIndex);
        
        System.out.println("\n中间的文件名（索引" + middleIndex + "）: " + middleFilename);
    }
}
