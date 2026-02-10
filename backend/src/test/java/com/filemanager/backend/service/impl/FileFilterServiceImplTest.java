package com.filemanager.backend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileFilterServiceImplTest {

    private FileFilterServiceImpl fileFilterService;

    @BeforeEach
    void setUp() {
        fileFilterService = new FileFilterServiceImpl();
    }

    @Test
    void testIsFileIncluded_WithValidFile() {
        File file = new File("/path/to/music.mp3");
        assertTrue(fileFilterService.isFileIncluded(file));
    }

    @Test
    void testIsFileIncluded_WithNullFile() {
        assertFalse(fileFilterService.isFileIncluded(null));
    }

    @Test
    void testIsFileFiltered_WithMatchingFilter() {
        File file = new File("/path/to/Convert/file.mp3");
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testIsFileFiltered_WithNonMatchingFilter() {
        File file = new File("/path/to/music/file.mp3");
        assertFalse(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testIsFileFiltered_WithTempFile() {
        File file = new File("/path/to/Temp/file.mp3");
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testIsFileFiltered_WithCacheFile() {
        File file = new File("/path/to/Cache/file.mp3");
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testIsFileFiltered_WithHiddenFile() {
        File file = new File("/path/to/.hidden/file.mp3");
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
    void testMatchesFilter_WithWildcard() {
        File file = new File("/path/to/Convert/file.mp3");
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testMatchesFilter_WithQuestionMark() {
        File file = new File("/path/to/Temp/file.mp3");
        assertTrue(fileFilterService.isFileFiltered(file));
    }

    @Test
    void testMatchesFilter_WithExactMatch() {
        File file = new File("/path/to/Thumbs.db");
        assertTrue(fileFilterService.isFileFiltered(file));
    }
}
