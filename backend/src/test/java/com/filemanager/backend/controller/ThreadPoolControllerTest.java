package com.filemanager.backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadPoolControllerTest {

    @InjectMocks
    private ThreadPoolController threadPoolController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetThreadPoolConfig_Success() {
        ResponseEntity<Map<String, Object>> response = threadPoolController.getThreadPoolConfig();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().get("previewThreads"));
        assertEquals(8, response.getBody().get("executionThreads"));
    }

    @Test
    public void testSetPreviewThreads_Success() {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("threads", 8);

        ResponseEntity<Map<String, Object>> response = threadPoolController.setPreviewThreads(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("预览线程数设置成功", response.getBody().get("message"));
        
        ResponseEntity<Map<String, Object>> getResponse = threadPoolController.getThreadPoolConfig();
        assertEquals(8, getResponse.getBody().get("previewThreads"));
    }

    @Test
    public void testSetPreviewThreads_ZeroThreads() {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("threads", 0);

        ResponseEntity<Map<String, Object>> response = threadPoolController.setPreviewThreads(request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("线程数必须大于0", response.getBody().get("message"));
    }

    @Test
    public void testSetPreviewThreads_NegativeThreads() {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("threads", -1);

        ResponseEntity<Map<String, Object>> response = threadPoolController.setPreviewThreads(request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("线程数必须大于0", response.getBody().get("message"));
    }

    @Test
    public void testSetPreviewThreads_NullThreads() {
        Map<String, Object> request = new java.util.HashMap<>();

        ResponseEntity<Map<String, Object>> response = threadPoolController.setPreviewThreads(request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("线程数必须大于0", response.getBody().get("message"));
    }

    @Test
    public void testSetExecutionThreads_Success() {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("threads", 16);

        ResponseEntity<Map<String, Object>> response = threadPoolController.setExecutionThreads(request);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("执行线程数设置成功", response.getBody().get("message"));
        
        ResponseEntity<Map<String, Object>> getResponse = threadPoolController.getThreadPoolConfig();
        assertEquals(16, getResponse.getBody().get("executionThreads"));
    }

    @Test
    public void testSetExecutionThreads_ZeroThreads() {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("threads", 0);

        ResponseEntity<Map<String, Object>> response = threadPoolController.setExecutionThreads(request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("线程数必须大于0", response.getBody().get("message"));
    }

    @Test
    public void testSetExecutionThreads_NegativeThreads() {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("threads", -1);

        ResponseEntity<Map<String, Object>> response = threadPoolController.setExecutionThreads(request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("线程数必须大于0", response.getBody().get("message"));
    }

    @Test
    public void testSetExecutionThreads_NullThreads() {
        Map<String, Object> request = new java.util.HashMap<>();

        ResponseEntity<Map<String, Object>> response = threadPoolController.setExecutionThreads(request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("线程数必须大于0", response.getBody().get("message"));
    }

    @Test
    public void testGetThreadPoolConfig_AfterUpdate() {
        Map<String, Object> previewRequest = new java.util.HashMap<>();
        previewRequest.put("threads", 10);
        
        Map<String, Object> executionRequest = new java.util.HashMap<>();
        executionRequest.put("threads", 20);

        threadPoolController.setPreviewThreads(previewRequest);
        threadPoolController.setExecutionThreads(executionRequest);
        
        ResponseEntity<Map<String, Object>> response = threadPoolController.getThreadPoolConfig();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().get("previewThreads"));
        assertEquals(20, response.getBody().get("executionThreads"));
    }

    @Test
    public void testSetPreviewThreads_MultipleUpdates() {
        Map<String, Object> request1 = new java.util.HashMap<>();
        request1.put("threads", 6);
        
        Map<String, Object> request2 = new java.util.HashMap<>();
        request2.put("threads", 12);

        threadPoolController.setPreviewThreads(request1);
        threadPoolController.setPreviewThreads(request2);
        
        ResponseEntity<Map<String, Object>> getResponse = threadPoolController.getThreadPoolConfig();
        assertEquals(12, getResponse.getBody().get("previewThreads"));
    }

    @Test
    public void testSetExecutionThreads_MultipleUpdates() {
        Map<String, Object> request1 = new java.util.HashMap<>();
        request1.put("threads", 10);
        
        Map<String, Object> request2 = new java.util.HashMap<>();
        request2.put("threads", 24);

        threadPoolController.setExecutionThreads(request1);
        threadPoolController.setExecutionThreads(request2);
        
        ResponseEntity<Map<String, Object>> getResponse = threadPoolController.getThreadPoolConfig();
        assertEquals(24, getResponse.getBody().get("executionThreads"));
    }

    @Test
    public void testGetThreadPoolConfig_DefaultValues() {
        ResponseEntity<Map<String, Object>> response = threadPoolController.getThreadPoolConfig();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().get("previewThreads"));
        assertEquals(8, response.getBody().get("executionThreads"));
    }
}
