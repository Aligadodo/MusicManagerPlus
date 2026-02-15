package com.filemanager.backend.service.impl;

import com.filemanager.backend.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FileFilterServiceImplTest {

    private FileFilterServiceImpl fileFilterService;
    
    @Mock
    private ConfigManager configManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 模拟配置管理器返回null，这样就会使用默认过滤规则
        when(configManager.getConfig(ConfigManager.KEY_SCAN_FILTER_LIST, Object.class)).thenReturn(null);
        fileFilterService = new FileFilterServiceImpl(configManager);
    }

    @Test
    void testIsFileIncluded_WithValidFile() throws IOException {
        File file = Files.createFile(tempDir.resolve("music.mp3")).toFile();
        assertTrue(fileFilterService.isFileIncluded(file));
    }

    @Test
    void testIsFileIncluded_WithNullFile() {
        assertFalse(fileFilterService.isFileIncluded(null));
    }

    @Test
    void testIsFileFiltered_WithMatchingFilter() throws IOException {
        Path convertDir = tempDir.resolve("Convert");
        Files.createDirectories(convertDir);
        File file = Files.createFile(convertDir.resolve("file.mp3")).toFile();
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testIsFileFiltered_WithNonMatchingFilter() throws IOException {
        Path musicDir = tempDir.resolve("music");
        Files.createDirectories(musicDir);
        File file = Files.createFile(musicDir.resolve("file.mp3")).toFile();
        assertFalse(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testIsFileFiltered_WithTempFile() throws IOException {
        Path tempPath = tempDir.resolve("Temp");
        Files.createDirectories(tempPath);
        File file = Files.createFile(tempPath.resolve("file.mp3")).toFile();
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testIsFileFiltered_WithCacheFile() throws IOException {
        Path cachePath = tempDir.resolve("Cache");
        Files.createDirectories(cachePath);
        File file = Files.createFile(cachePath.resolve("file.mp3")).toFile();
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testIsFileFiltered_WithHiddenFile() throws IOException {
        Path hiddenDir = tempDir.resolve(".hidden");
        Files.createDirectories(hiddenDir);
        File file = Files.createFile(hiddenDir.resolve("file.mp3")).toFile();
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testGetScanFilterList() {
        List<String> filters = fileFilterService.getScanFilterList();
        assertNotNull(filters);
        assertFalse(filters.isEmpty());
        assertTrue(filters.contains("*Convert*"));
    }

    @Test
    void testAddScanFilter() {
        int initialSize = fileFilterService.getScanFilterList().size();
        fileFilterService.addScanFilter("*Test*");
        assertEquals(initialSize + 1, fileFilterService.getScanFilterList().size());
        assertTrue(fileFilterService.getScanFilterList().contains("*Test*"));
    }

    @Test
    void testAddScanFilter_WithEmptyFilter() {
        int initialSize = fileFilterService.getScanFilterList().size();
        fileFilterService.addScanFilter("");
        assertEquals(initialSize, fileFilterService.getScanFilterList().size());
    }

    @Test
    void testAddScanFilter_WithExistingFilter() {
        int initialSize = fileFilterService.getScanFilterList().size();
        fileFilterService.addScanFilter("*Convert*");
        assertEquals(initialSize, fileFilterService.getScanFilterList().size());
    }

    @Test
    void testRemoveScanFilter() {
        fileFilterService.addScanFilter("*Test*");
        assertTrue(fileFilterService.getScanFilterList().contains("*Test*"));
        
        fileFilterService.removeScanFilter("*Test*");
        assertFalse(fileFilterService.getScanFilterList().contains("*Test*"));
    }

    @Test
    void testClearScanFilters() {
        fileFilterService.addScanFilter("*Test1*");
        fileFilterService.addScanFilter("*Test2*");
        assertTrue(fileFilterService.getScanFilterList().size() > 0);
        
        fileFilterService.clearScanFilters();
        assertTrue(fileFilterService.getScanFilterList().isEmpty());
    }

    @Test
    void testMatchesFilter_WithWildcard() throws IOException {
        Path convertDir = tempDir.resolve("Convert");
        Files.createDirectories(convertDir);
        File file = Files.createFile(convertDir.resolve("file.mp3")).toFile();
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testMatchesFilter_WithQuestionMark() throws IOException {
        Path tempPath = tempDir.resolve("Temp");
        Files.createDirectories(tempPath);
        File file = Files.createFile(tempPath.resolve("file.mp3")).toFile();
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testMatchesFilter_WithExactMatch() throws IOException {
        File file = Files.createFile(tempDir.resolve("Thumbs.db")).toFile();
        assertTrue(fileFilterService.isFileFiltered(file));
    }
}
