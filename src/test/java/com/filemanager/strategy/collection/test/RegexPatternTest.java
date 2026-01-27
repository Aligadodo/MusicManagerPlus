package com.filemanager.strategy.collection.test;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试正则表达式匹配
 */
public class RegexPatternTest {
    
    @Test
    public void testRegexPattern() {
        System.out.println("=== 测试正则表达式匹配 ===");
        
        String file1 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶";
        String file2 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤钢琴弄潮篇-爱情故事";
        String file3 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑥华夏风情篇-睡莲";
        String file4 = "龙音唱片.-.[龙音文采华音版-轻舟随波系列⑦-异国风情篇-美丽的梭罗河";
        
        Pattern pattern = Pattern.compile("\\[([^\\]]*系列[^\\]]*)\\]");
        
        System.out.println("正则表达式: " + pattern.pattern());
        
        System.out.println("\n文件1:");
        Matcher matcher1 = pattern.matcher(file1);
        if (matcher1.find()) {
            System.out.println("  匹配成功: " + matcher1.group(1));
        } else {
            System.out.println("  匹配失败");
        }
        
        System.out.println("\n文件2:");
        Matcher matcher2 = pattern.matcher(file2);
        if (matcher2.find()) {
            System.out.println("  匹配成功: " + matcher2.group(1));
        } else {
            System.out.println("  匹配失败");
        }
        
        System.out.println("\n文件3:");
        Matcher matcher3 = pattern.matcher(file3);
        if (matcher3.find()) {
            System.out.println("  匹配成功: " + matcher3.group(1));
        } else {
            System.out.println("  匹配失败");
        }
        
        System.out.println("\n文件4:");
        Matcher matcher4 = pattern.matcher(file4);
        if (matcher4.find()) {
            System.out.println("  匹配成功: " + matcher4.group(1));
        } else {
            System.out.println("  匹配失败");
        }
    }
}
