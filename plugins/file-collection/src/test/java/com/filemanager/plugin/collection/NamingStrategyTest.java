package com.filemanager.plugin.collection;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NamingStrategyTest {

    @Test
    void testExactNamingStrategy() {
        NamingStrategy strategy = new ExactNamingStrategy();
        
        assertEquals("exact", strategy.getId());
        assertEquals("精确命名策略", strategy.getName());
        assertEquals("使用完整的最长公共前缀作为合集名称", strategy.getDescription());
    }

    @Test
    void testExactNamingStrategyGenerateName() {
        NamingStrategy strategy = new ExactNamingStrategy();
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/周杰伦 - 青花瓷.mp3");
        cluster.addFilePath("/path/周杰伦 - 青花瓷 (Remix).mp3");
        cluster.calculateCommonPrefix();
        
        Map<String, Object> context = new HashMap<>();
        String name = strategy.generateName(cluster, context);
        
        assertEquals("周杰伦 - 青花瓷", name);
    }

    @Test
    void testExactNamingStrategyGenerateNameWithEmptyPrefix() {
        NamingStrategy strategy = new ExactNamingStrategy();
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/song1.mp3");
        cluster.addFilePath("/path/other2.mp3");
        cluster.calculateCommonPrefix();
        
        Map<String, Object> context = new HashMap<>();
        String name = strategy.generateName(cluster, context);
        
        assertTrue(name.startsWith("合集_"));
    }

    @Test
    void testSimpleNamingStrategy() {
        NamingStrategy strategy = new SimpleNamingStrategy();
        
        assertEquals("simple", strategy.getId());
        assertEquals("简洁命名策略", strategy.getName());
        assertEquals("使用简化的公共前缀作为合集名称", strategy.getDescription());
    }

    @Test
    void testSimpleNamingStrategyGenerateName() {
        NamingStrategy strategy = new SimpleNamingStrategy();
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/周杰伦 - 青花瓷.mp3");
        cluster.addFilePath("/path/周杰伦 - 青花瓷 (Remix).mp3");
        cluster.calculateCommonPrefix();
        
        Map<String, Object> context = new HashMap<>();
        String name = strategy.generateName(cluster, context);
        
        assertTrue(name.contains("周杰伦"));
        assertTrue(name.contains("青花瓷"));
    }

    @Test
    void testSimpleNamingStrategyGenerateNameWithSpecialChars() {
        NamingStrategy strategy = new SimpleNamingStrategy();
        FileCluster cluster = new FileCluster();
        cluster.setCommonPrefix("周杰伦__青花瓷--测试");
        
        Map<String, Object> context = new HashMap<>();
        String name = strategy.generateName(cluster, context);
        
        assertFalse(name.contains("__"));
        assertFalse(name.contains("--"));
    }

    @Test
    void testSimpleNamingStrategyGenerateNameWithLongName() {
        NamingStrategy strategy = new SimpleNamingStrategy();
        String longName = "这是一个非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常非常长的文件名用于测试长度限制功能";
        FileCluster cluster = new FileCluster();
        cluster.setCommonPrefix(longName);
        
        Map<String, Object> context = new HashMap<>();
        String name = strategy.generateName(cluster, context);
        
        assertTrue(name.length() <= 50);
    }

    @Test
    void testTemplateNamingStrategy() {
        NamingStrategy strategy = new TemplateNamingStrategy();
        
        assertEquals("template", strategy.getId());
        assertEquals("模板命名策略", strategy.getName());
        assertEquals("使用自定义模板生成合集名称", strategy.getDescription());
    }

    @Test
    void testTemplateNamingStrategyGenerateName() {
        NamingStrategy strategy = new TemplateNamingStrategy();
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/周杰伦 - 青花瓷.mp3");
        cluster.addFilePath("/path/周杰伦 - 青花瓷 (Remix).mp3");
        cluster.calculateCommonPrefix();
        
        Map<String, Object> context = new HashMap<>();
        context.put("template", "{prefix}{name}{suffix}");
        context.put("prefix", "[");
        context.put("suffix", "]");
        
        String name = strategy.generateName(cluster, context);
        
        assertEquals("[周杰伦 - 青花瓷]", name);
    }

    @Test
    void testTemplateNamingStrategyGenerateNameWithSize() {
        NamingStrategy strategy = new TemplateNamingStrategy();
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/song1.mp3");
        cluster.addFilePath("/path/song2.mp3");
        cluster.addFilePath("/path/song3.mp3");
        cluster.calculateCommonPrefix();
        
        Map<String, Object> context = new HashMap<>();
        context.put("template", "{name} ({size} files)");
        
        String name = strategy.generateName(cluster, context);
        
        assertTrue(name.contains("3 files"));
    }

    @Test
    void testTemplateNamingStrategyGenerateNameWithSimilarity() {
        NamingStrategy strategy = new TemplateNamingStrategy();
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/song1.mp3");
        cluster.addFilePath("/path/song2.mp3");
        cluster.calculateAverageSimilarity();
        cluster.calculateCommonPrefix();
        
        Map<String, Object> context = new HashMap<>();
        context.put("template", "{name} [similarity: {similarity}]");
        
        String name = strategy.generateName(cluster, context);
        
        assertTrue(name.contains("similarity:"));
    }

    @Test
    void testTemplateNamingStrategyValidateContextWithoutTemplate() {
        NamingStrategy strategy = new TemplateNamingStrategy();
        Map<String, Object> context = new HashMap<>();
        
        assertThrows(IllegalArgumentException.class, () -> {
            strategy.validateContext(context);
        });
    }

    @Test
    void testTemplateNamingStrategyValidateContextWithTemplate() {
        NamingStrategy strategy = new TemplateNamingStrategy();
        Map<String, Object> context = new HashMap<>();
        context.put("template", "{name}");
        
        assertDoesNotThrow(() -> {
            strategy.validateContext(context);
        });
    }

    @Test
    void testUniversalNamingStrategy() {
        NamingStrategy strategy = new UniversalNamingStrategy();
        
        assertEquals("universal", strategy.getId());
        assertEquals("通用命名策略", strategy.getName());
        assertEquals("使用通用名称作为合集名称", strategy.getDescription());
    }

    @Test
    void testUniversalNamingStrategyGenerateName() {
        NamingStrategy strategy = new UniversalNamingStrategy();
        FileCluster cluster = new FileCluster();
        cluster.addFilePath("/path/song1.mp3");
        
        Map<String, Object> context = new HashMap<>();
        String name = strategy.generateName(cluster, context);
        
        assertTrue(name.startsWith("合集_"));
    }

    @Test
    void testNamingStrategyFactoryGetStrategy() {
        NamingStrategy exactStrategy = NamingStrategyFactory.getStrategy("exact");
        assertEquals("exact", exactStrategy.getId());
        
        NamingStrategy simpleStrategy = NamingStrategyFactory.getStrategy("simple");
        assertEquals("simple", simpleStrategy.getId());
        
        NamingStrategy templateStrategy = NamingStrategyFactory.getStrategy("template");
        assertEquals("template", templateStrategy.getId());
        
        NamingStrategy universalStrategy = NamingStrategyFactory.getStrategy("universal");
        assertEquals("universal", universalStrategy.getId());
    }

    @Test
    void testNamingStrategyFactoryGetStrategyWithNull() {
        NamingStrategy strategy = NamingStrategyFactory.getStrategy(null);
        assertEquals("exact", strategy.getId());
    }

    @Test
    void testNamingStrategyFactoryGetStrategyWithEmpty() {
        NamingStrategy strategy = NamingStrategyFactory.getStrategy("");
        assertEquals("exact", strategy.getId());
    }

    @Test
    void testNamingStrategyFactoryGetStrategyWithInvalidId() {
        NamingStrategy strategy = NamingStrategyFactory.getStrategy("invalid");
        assertEquals("exact", strategy.getId());
    }

    @Test
    void testNamingStrategyFactoryGetStrategyWithCaseInsensitive() {
        NamingStrategy strategy = NamingStrategyFactory.getStrategy("EXACT");
        assertEquals("exact", strategy.getId());
        
        strategy = NamingStrategyFactory.getStrategy("Simple");
        assertEquals("simple", strategy.getId());
    }

    @Test
    void testNamingStrategyFactoryGetAllStrategies() {
        List<NamingStrategy> strategies = NamingStrategyFactory.getAllStrategies();
        
        assertEquals(4, strategies.size());
        assertTrue(strategies.stream().anyMatch(s -> "exact".equals(s.getId())));
        assertTrue(strategies.stream().anyMatch(s -> "simple".equals(s.getId())));
        assertTrue(strategies.stream().anyMatch(s -> "template".equals(s.getId())));
        assertTrue(strategies.stream().anyMatch(s -> "universal".equals(s.getId())));
    }

    @Test
    void testNamingStrategyFactoryGetDefaultStrategy() {
        NamingStrategy strategy = NamingStrategyFactory.getDefaultStrategy();
        assertEquals("exact", strategy.getId());
    }
}
