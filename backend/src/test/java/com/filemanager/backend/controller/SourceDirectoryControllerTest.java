package com.filemanager.backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SourceDirectoryControllerTest {

    @InjectMocks
    private SourceDirectoryController sourceDirectoryController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetSourceDirectories_Empty() {
        ResponseEntity<List<Map<String, Object>>> response = sourceDirectoryController.getSourceDirectories();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testAddSourceDirectory_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("path", "/test/path");
        request.put("threadCount", 4);

        ResponseEntity<Map<String, Object>> response = sourceDirectoryController.addSourceDirectory(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("源目录添加成功", response.getBody().get("message"));
    }

    @Test
    public void testAddSourceDirectory_DuplicatePath() {
        Map<String, Object> request = new HashMap<>();
        request.put("path", "/test/path");
        request.put("threadCount", 4);

        sourceDirectoryController.addSourceDirectory(request);
        ResponseEntity<Map<String, Object>> response = sourceDirectoryController.addSourceDirectory(request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("路径已存在", response.getBody().get("message"));
    }

    @Test
    public void testAddSourceDirectory_DefaultThreadCount() {
        Map<String, Object> request = new HashMap<>();
        request.put("path", "/test/path");

        ResponseEntity<Map<String, Object>> response = sourceDirectoryController.addSourceDirectory(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        
        ResponseEntity<List<Map<String, Object>>> getResponse = sourceDirectoryController.getSourceDirectories();
        assertEquals(4, getResponse.getBody().get(0).get("threadCount"));
    }

    @Test
    public void testRemoveSourceDirectory_Success() {
        Map<String, Object> request = new HashMap<>();
        request.put("path", "/test/path");
        request.put("threadCount", 4);

        sourceDirectoryController.addSourceDirectory(request);
        ResponseEntity<Map<String, Object>> response = sourceDirectoryController.removeSourceDirectory("/test/path");
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("源目录移除成功", response.getBody().get("message"));
    }

    @Test
    public void testRemoveSourceDirectory_NotFound() {
        ResponseEntity<Map<String, Object>> response = sourceDirectoryController.removeSourceDirectory("/nonexistent/path");
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("源目录不存在", response.getBody().get("message"));
    }

    @Test
    public void testClearSourceDirectories_Success() {
        Map<String, Object> request1 = new HashMap<>();
        request1.put("path", "/test/path1");
        request1.put("threadCount", 4);
        
        Map<String, Object> request2 = new HashMap<>();
        request2.put("path", "/test/path2");
        request2.put("threadCount", 8);

        sourceDirectoryController.addSourceDirectory(request1);
        sourceDirectoryController.addSourceDirectory(request2);
        
        ResponseEntity<Map<String, Object>> response = sourceDirectoryController.clearSourceDirectories();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("源目录清空成功", response.getBody().get("message"));
        
        ResponseEntity<List<Map<String, Object>>> getResponse = sourceDirectoryController.getSourceDirectories();
        assertTrue(getResponse.getBody().isEmpty());
    }

    @Test
    public void testClearSourceDirectories_Empty() {
        ResponseEntity<Map<String, Object>> response = sourceDirectoryController.clearSourceDirectories();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("源目录清空成功", response.getBody().get("message"));
    }

    @Test
    public void testUpdateThreadCount_Success() {
        Map<String, Object> addRequest = new HashMap<>();
        addRequest.put("path", "/test/path");
        addRequest.put("threadCount", 4);

        sourceDirectoryController.addSourceDirectory(addRequest);
        
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("threadCount", 8);

        ResponseEntity<Map<String, Object>> response = sourceDirectoryController.updateThreadCount("/test/path", updateRequest);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("线程数更新成功", response.getBody().get("message"));
        
        ResponseEntity<List<Map<String, Object>>> getResponse = sourceDirectoryController.getSourceDirectories();
        assertEquals(8, getResponse.getBody().get(0).get("threadCount"));
    }

    @Test
    public void testUpdateThreadCount_NotFound() {
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("threadCount", 8);

        ResponseEntity<Map<String, Object>> response = sourceDirectoryController.updateThreadCount("/nonexistent/path", updateRequest);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("源目录不存在", response.getBody().get("message"));
    }

    @Test
    public void testUpdateThreadCount_NullThreadCount() {
        Map<String, Object> addRequest = new HashMap<>();
        addRequest.put("path", "/test/path");
        addRequest.put("threadCount", 4);

        sourceDirectoryController.addSourceDirectory(addRequest);
        
        Map<String, Object> updateRequest = new HashMap<>();

        ResponseEntity<Map<String, Object>> response = sourceDirectoryController.updateThreadCount("/test/path", updateRequest);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("线程数不能为空", response.getBody().get("message"));
    }

    @Test
    public void testGetSourceDirectories_MultipleDirectories() {
        Map<String, Object> request1 = new HashMap<>();
        request1.put("path", "/test/path1");
        request1.put("threadCount", 4);
        
        Map<String, Object> request2 = new HashMap<>();
        request2.put("path", "/test/path2");
        request2.put("threadCount", 8);

        sourceDirectoryController.addSourceDirectory(request1);
        sourceDirectoryController.addSourceDirectory(request2);
        
        ResponseEntity<List<Map<String, Object>>> response = sourceDirectoryController.getSourceDirectories();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("/test/path1", response.getBody().get(0).get("path"));
        assertEquals(4, response.getBody().get(0).get("threadCount"));
        assertEquals("/test/path2", response.getBody().get(1).get("path"));
        assertEquals(8, response.getBody().get(1).get("threadCount"));
    }

    @Test
    public void testGetSourceDirectories_AfterRemove() {
        Map<String, Object> request1 = new HashMap<>();
        request1.put("path", "/test/path1");
        request1.put("threadCount", 4);
        
        Map<String, Object> request2 = new HashMap<>();
        request2.put("path", "/test/path2");
        request2.put("threadCount", 8);

        sourceDirectoryController.addSourceDirectory(request1);
        sourceDirectoryController.addSourceDirectory(request2);
        
        sourceDirectoryController.removeSourceDirectory("/test/path1");
        
        ResponseEntity<List<Map<String, Object>>> response = sourceDirectoryController.getSourceDirectories();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("/test/path2", response.getBody().get(0).get("path"));
    }
}
