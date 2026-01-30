package com.filemanager.backend.controller;

import com.filemanager.domain.dto.FileInfoDTO;
import com.filemanager.domain.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class FileControllerTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileController fileController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testScanDirectory_Success() {
        List<FileInfoDTO> mockFiles = new ArrayList<>();
        FileInfoDTO file = new FileInfoDTO();
        file.setPath("/test/file.txt");
        file.setName("file.txt");
        file.setDirectory(false);
        file.setSize(1024);
        mockFiles.add(file);

        when(fileService.scanDirectory(eq("/test/path"), eq(0), eq(3), isNull()))
            .thenReturn(mockFiles);

        ResponseEntity<List<FileInfoDTO>> response = fileController.scanDirectory("/test/path", 0, 3, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("file.txt", response.getBody().get(0).getName());
    }

    @Test
    public void testScanDirectory_WithPattern() {
        List<FileInfoDTO> mockFiles = new ArrayList<>();
        FileInfoDTO file = new FileInfoDTO();
        file.setPath("/test/file.txt");
        file.setName("file.txt");
        file.setDirectory(false);
        mockFiles.add(file);

        when(fileService.scanDirectory(eq("/test/path"), eq(0), eq(3), eq("*.txt")))
            .thenReturn(mockFiles);

        ResponseEntity<List<FileInfoDTO>> response = fileController.scanDirectory("/test/path", 0, 3, "*.txt");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    public void testGetFileInfo_Success() {
        FileInfoDTO mockFile = new FileInfoDTO();
        mockFile.setPath("/test/file.txt");
        mockFile.setName("file.txt");
        mockFile.setDirectory(false);
        mockFile.setSize(1024);

        when(fileService.getFileInfo("/test/file.txt")).thenReturn(mockFile);

        ResponseEntity<FileInfoDTO> response = fileController.getFileInfo("/test/file.txt");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("file.txt", response.getBody().getName());
    }

    @Test
    public void testGetFileInfo_NotFound() {
        when(fileService.getFileInfo("/nonexistent/file.txt"))
            .thenThrow(new RuntimeException("File not found"));

        ResponseEntity<FileInfoDTO> response = fileController.getFileInfo("/nonexistent/file.txt");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testCheckExists_Success() {
        Map<String, List<String>> request = new HashMap<>();
        List<String> paths = new ArrayList<>();
        paths.add("/test/file1.txt");
        paths.add("/test/file2.txt");
        request.put("paths", paths);

        Map<String, Boolean> mockResult = new HashMap<>();
        mockResult.put("/test/file1.txt", true);
        mockResult.put("/test/file2.txt", false);

        when(fileService.checkExists(paths)).thenReturn(mockResult);

        ResponseEntity<Map<String, Boolean>> response = fileController.checkExists(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("/test/file1.txt"));
        assertFalse(response.getBody().get("/test/file2.txt"));
    }

    @Test
    public void testFileOperation_Copy_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("operation", "copy");
        request.put("source", "/test/source.txt");
        request.put("target", "/test/target.txt");

        when(fileService.copy("/test/source.txt", "/test/target.txt")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = fileController.fileOperation(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("复制成功", response.getBody().get("message"));
    }

    @Test
    public void testFileOperation_Move_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("operation", "move");
        request.put("source", "/test/source.txt");
        request.put("target", "/test/target.txt");

        when(fileService.move("/test/source.txt", "/test/target.txt")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = fileController.fileOperation(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("移动成功", response.getBody().get("message"));
    }

    @Test
    public void testFileOperation_Delete_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("operation", "delete");
        request.put("source", "/test/file.txt");
        request.put("target", "");

        when(fileService.delete("/test/file.txt")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = fileController.fileOperation(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("删除成功", response.getBody().get("message"));
    }

    @Test
    public void testFileOperation_Rename_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("operation", "rename");
        request.put("source", "/test/old.txt");
        request.put("target", "/test/new.txt");

        when(fileService.rename("/test/old.txt", "/test/new.txt")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = fileController.fileOperation(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("重命名成功", response.getBody().get("message"));
    }
}
