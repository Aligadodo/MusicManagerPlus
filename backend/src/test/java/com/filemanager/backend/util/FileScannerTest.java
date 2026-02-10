package com.filemanager.backend.util;

import com.filemanager.backend.service.FileFilterService;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FileScannerTest {

    @Mock
    private FileFilterService fileFilterService;

    private FileScanner fileScanner;

    private AtomicBoolean isTaskRunning;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        isTaskRunning = new AtomicBoolean(true);
        fileScanner = new FileScanner(fileFilterService, isTaskRunning, 2);
    }

    @Test
    void testScanFilesRobust_WithEmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectory(emptyDir);

        when(fileFilterService.isFileIncluded(any(File.class))).thenReturn(true);

        List<File> files = fileScanner.scanFilesRobust(
            emptyDir.toFile(), 0, 5, 
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            msg -> {}
        );

        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    void testScanFilesRobust_WithFiles() throws IOException {
        Path testDir = tempDir.resolve("test");
        Files.createDirectory(testDir);
        
        Files.createFile(testDir.resolve("file1.txt"));
        Files.createFile(testDir.resolve("file2.txt"));
        Files.createFile(testDir.resolve("file3.txt"));

        when(fileFilterService.isFileIncluded(any(File.class))).thenReturn(true);

        List<File> files = fileScanner.scanFilesRobust(
            testDir.toFile(), 0, 5,
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            msg -> {}
        );

        assertNotNull(files);
        assertEquals(3, files.size());
    }

    @Test
    void testScanFilesRobust_WithNestedDirectories() throws IOException {
        Path rootDir = tempDir.resolve("root");
        Files.createDirectory(rootDir);
        
        Path subDir1 = rootDir.resolve("sub1");
        Files.createDirectory(subDir1);
        Files.createFile(subDir1.resolve("file1.txt"));
        
        Path subDir2 = rootDir.resolve("sub2");
        Files.createDirectory(subDir2);
        Files.createFile(subDir2.resolve("file2.txt"));
        
        Files.createFile(rootDir.resolve("file3.txt"));

        when(fileFilterService.isFileIncluded(any(File.class))).thenReturn(true);

        List<File> files = fileScanner.scanFilesRobust(
            rootDir.toFile(), 0, 5,
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            msg -> {}
        );

        assertNotNull(files);
        assertEquals(3, files.size());
    }

    @Test
    void testScanFilesRobust_WithDepthLimit() throws IOException {
        Path rootDir = tempDir.resolve("root");
        Files.createDirectory(rootDir);
        
        Path subDir = rootDir.resolve("sub");
        Files.createDirectory(subDir);
        Files.createFile(subDir.resolve("file1.txt"));
        
        Files.createFile(rootDir.resolve("file2.txt"));

        when(fileFilterService.isFileIncluded(any(File.class))).thenReturn(true);

        List<File> files = fileScanner.scanFilesRobust(
            rootDir.toFile(), 0, 1,
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            msg -> {}
        );

        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("file2.txt", files.get(0).getName());
    }

    @Test
    void testScanFilesRobust_WithGlobalLimit() throws IOException {
        Path testDir = tempDir.resolve("test");
        Files.createDirectory(testDir);
        
        for (int i = 0; i < 10; i++) {
            Files.createFile(testDir.resolve("file" + i + ".txt"));
        }

        when(fileFilterService.isFileIncluded(any(File.class))).thenReturn(true);

        List<File> files = fileScanner.scanFilesRobust(
            testDir.toFile(), 0, 5,
            new java.util.concurrent.atomic.AtomicInteger(5),
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            msg -> {}
        );

        assertNotNull(files);
        assertTrue(files.size() <= 5);
    }

    @Test
    void testScanFilesRobust_WithFilteredFiles() throws IOException {
        Path testDir = tempDir.resolve("test");
        Files.createDirectory(testDir);
        
        Files.createFile(testDir.resolve("file1.txt"));
        Files.createFile(testDir.resolve("Convert_file2.txt"));
        Files.createFile(testDir.resolve("file3.txt"));

        when(fileFilterService.isFileIncluded(any(File.class)))
            .thenAnswer(invocation -> {
                File file = invocation.getArgument(0);
                return !file.getName().contains("Convert");
            });

        List<File> files = fileScanner.scanFilesRobust(
            testDir.toFile(), 0, 5,
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            msg -> {}
        );

        assertNotNull(files);
        assertEquals(2, files.size());
    }

    @Test
    void testScanFilesRobust_WithNonExistentDirectory() {
        File nonExistentDir = new File("/non/existent/directory");

        when(fileFilterService.isFileIncluded(any(File.class))).thenReturn(true);

        List<File> files = fileScanner.scanFilesRobust(
            nonExistentDir, 0, 5,
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            msg -> {}
        );

        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    void testScanFilesRobust_WithTaskCancelled() throws IOException {
        Path testDir = tempDir.resolve("test");
        Files.createDirectory(testDir);
        
        for (int i = 0; i < 10; i++) {
            Files.createFile(testDir.resolve("file" + i + ".txt"));
        }

        when(fileFilterService.isFileIncluded(any(File.class))).thenReturn(true);

        isTaskRunning.set(false);

        List<File> files = fileScanner.scanFilesRobust(
            testDir.toFile(), 0, 5,
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            msg -> {}
        );

        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    void testScanFilesRobust_WithProgressCallback() throws IOException {
        Path testDir = tempDir.resolve("test");
        Files.createDirectory(testDir);
        
        for (int i = 0; i < 3; i++) {
            Files.createFile(testDir.resolve("file" + i + ".txt"));
        }

        when(fileFilterService.isFileIncluded(any(File.class))).thenReturn(true);

        final boolean[] callbackCalled = {false};
        List<File> files = fileScanner.scanFilesRobust(
            testDir.toFile(), 0, 5,
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            new java.util.concurrent.atomic.AtomicInteger(Integer.MAX_VALUE),
            msg -> {
                callbackCalled[0] = true;
                assertTrue(msg.contains("已扫描"));
            }
        );

        assertNotNull(files);
        assertTrue(callbackCalled[0]);
    }
}
