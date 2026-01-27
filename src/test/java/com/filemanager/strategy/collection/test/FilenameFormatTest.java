package com.filemanager.strategy.collection.test;

import org.junit.Test;

/**
 * 测试文件名格式
 */
public class FilenameFormatTest {
    
    @Test
    public void testFilenameFormat() {
        System.out.println("=== 测试文件名格式 ===");
        
        String file1 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶";
        String file2 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤钢琴弄潮篇-爱情故事";
        String file3 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑥华夏风情篇-睡莲";
        String file4 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑦-异国风情篇-美丽的梭罗河";
        
        System.out.println("文件1:");
        System.out.println("  长度: " + file1.length());
        System.out.println("  最后一个字符: '" + file1.charAt(file1.length() - 1) + "'");
        System.out.println("  是否包含右括号: " + file1.contains("]"));
        System.out.println("  最后一个右括号位置: " + file1.lastIndexOf("]"));
        
        System.out.println("\n文件2:");
        System.out.println("  长度: " + file2.length());
        System.out.println("  最后一个字符: '" + file2.charAt(file2.length() - 1) + "'");
        System.out.println("  是否包含右括号: " + file2.contains("]"));
        System.out.println("  最后一个右括号位置: " + file2.lastIndexOf("]"));
        
        System.out.println("\n文件3:");
        System.out.println("  长度: " + file3.length());
        System.out.println("  最后一个字符: '" + file3.charAt(file3.length() - 1) + "'");
        System.out.println("  是否包含右括号: " + file3.contains("]"));
        System.out.println("  最后一个右括号位置: " + file3.lastIndexOf("]"));
        
        System.out.println("\n文件4:");
        System.out.println("  长度: " + file4.length());
        System.out.println("  最后一个字符: '" + file4.charAt(file4.length() - 1) + "'");
        System.out.println("  是否包含右括号: " + file4.contains("]"));
        System.out.println("  最后一个右括号位置: " + file4.lastIndexOf("]"));
    }
}
