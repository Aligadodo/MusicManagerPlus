package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.FileClusteringAlgorithm;
import com.filemanager.strategy.collection.test.TestCase;
import com.filemanager.strategy.collection.test.TestCasePersister;
import org.junit.Test;

import java.util.Map;

public class TestClusterNames {
    
    @Test
    public void testClassicalMusicClusterNames() {
        TestCasePersister persister = new TestCasePersister();
        TestCase testCase = persister.loadTestCase("test-data/classical_music_test");
        
        if (testCase == null) {
            System.out.println("测试用例加载失败");
            return;
        }
        
        FileClusteringAlgorithm algorithm = new FileClusteringAlgorithm();
        Map<String, java.util.List<String>> clusters = algorithm.clusterFilenames(testCase.getAllFolders());
        
        System.out.println("\n=== 生成的合集名称 ===");
        for (Map.Entry<String, java.util.List<String>> entry : clusters.entrySet()) {
            System.out.println("合集名称: " + entry.getKey());
            System.out.println("文件夹数量: " + entry.getValue().size());
            System.out.println("第一个文件夹: " + entry.getValue().get(0));
            System.out.println();
        }
    }
}
