package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.CollectionNamingStrategy;

/**
 * 测试用例生成器，用于为不同的命名策略生成测试用例
 */
public class TestCaseGenerator {
    
    /**
     * 生成简洁风格的测试用例
     */
    public static TestCase generateConciseStyleTestCase(String testName, String description) {
        TestCase testCase = new TestCase(testName);
        testCase.setDescription(description);
        testCase.setNamingStrategy(CollectionNamingStrategy.CONCISE);
        
        return testCase;
    }
    
    /**
     * 生成精确风格的测试用例
     */
    public static TestCase generatePreciseStyleTestCase(String testName, String description) {
        TestCase testCase = new TestCase(testName);
        testCase.setDescription(description);
        testCase.setNamingStrategy(CollectionNamingStrategy.PRECISE);
        
        return testCase;
    }
    
    /**
     * 生成古琴音乐测试用例 - 简洁风格
     */
    public static TestCase generateGuqinMusicConciseTestCase() {
        TestCase testCase = generateConciseStyleTestCase(
            "古琴音乐-简洁风格",
            "测试古琴音乐文件的简洁风格命名"
        );
        
        testCase.addFolder("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨].cd1");
        testCase.addFolder("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨]cd2");
        
        ExpectedCollection expected = new ExpectedCollection();
        expected.setCollectionName("【古琴音乐】吴景略《古琴艺术》");
        expected.addFolder("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨].cd1");
        expected.addFolder("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨]cd2");
        
        testCase.addExpectedCollection(expected);
        
        return testCase;
    }
    
    /**
     * 生成古琴音乐测试用例 - 精确风格
     */
    public static TestCase generateGuqinMusicPreciseTestCase() {
        TestCase testCase = generatePreciseStyleTestCase(
            "古琴音乐-精确风格",
            "测试古琴音乐文件的精确风格命名"
        );
        
        testCase.addFolder("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨].cd1");
        testCase.addFolder("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨]cd2");
        
        ExpectedCollection expected = new ExpectedCollection();
        expected.setCollectionName("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨]");
        expected.addFolder("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨].cd1");
        expected.addFolder("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨]cd2");
        
        testCase.addExpectedCollection(expected);
        
        return testCase;
    }
    
    /**
     * 生成龙音系列测试用例 - 简洁风格
     */
    public static TestCase generateLongyinSeriesConciseTestCase() {
        TestCase testCase = generateConciseStyleTestCase(
            "龙音系列-简洁风格",
            "测试龙音系列文件的简洁风格命名"
        );
        
        testCase.addFolder("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C].cd1");
        testCase.addFolder("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C].cd2");
        
        ExpectedCollection expected = new ExpectedCollection();
        expected.setCollectionName("龙音系列-梅庵琴谱-龚一");
        expected.addFolder("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C].cd1");
        expected.addFolder("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C].cd2");
        
        testCase.addExpectedCollection(expected);
        
        return testCase;
    }
    
    /**
     * 生成龙音系列测试用例 - 精确风格
     */
    public static TestCase generateLongyinSeriesPreciseTestCase() {
        TestCase testCase = generatePreciseStyleTestCase(
            "龙音系列-精确风格",
            "测试龙音系列文件的精确风格命名"
        );
        
        testCase.addFolder("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C].cd1");
        testCase.addFolder("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C].cd2");
        
        ExpectedCollection expected = new ExpectedCollection();
        expected.setCollectionName("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C]");
        expected.addFolder("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C].cd1");
        expected.addFolder("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C].cd2");
        
        testCase.addExpectedCollection(expected);
        
        return testCase;
    }
    
    /**
     * 生成滚石系列测试用例 - 简洁风格
     */
    public static TestCase generateRockRecordsConciseTestCase() {
        TestCase testCase = generateConciseStyleTestCase(
            "滚石系列-简洁风格",
            "测试滚石系列文件的简洁风格命名"
        );
        
        testCase.addFolder("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨].CD1");
        testCase.addFolder("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨].CD2");
        
        ExpectedCollection expected = new ExpectedCollection();
        expected.setCollectionName("滚石系列-张国荣《风继续吹》");
        expected.addFolder("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨].CD1");
        expected.addFolder("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨].CD2");
        
        testCase.addExpectedCollection(expected);
        
        return testCase;
    }
    
    /**
     * 生成滚石系列测试用例 - 精确风格
     */
    public static TestCase generateRockRecordsPreciseTestCase() {
        TestCase testCase = generatePreciseStyleTestCase(
            "滚石系列-精确风格",
            "测试滚石系列文件的精确风格命名"
        );
        
        testCase.addFolder("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨].CD1");
        testCase.addFolder("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨].CD2");
        
        ExpectedCollection expected = new ExpectedCollection();
        expected.setCollectionName("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨]");
        expected.addFolder("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨].CD1");
        expected.addFolder("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨].CD2");
        
        testCase.addExpectedCollection(expected);
        
        return testCase;
    }
}