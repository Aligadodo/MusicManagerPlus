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

public class LogControllerTest {

    @InjectMocks
    private LogController logController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetLogs_Empty() {
        ResponseEntity<List<Map<String, Object>>> response = logController.getLogs(null, null, 1, 50);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testGetLogs_WithLogs() {
        Map<String, Object> logEntry1 = new HashMap<>();
        logEntry1.put("level", "INFO");
        logEntry1.put("message", "Test message 1");
        logEntry1.put("source", "api");

        logController.addLog(logEntry1);

        Map<String, Object> logEntry2 = new HashMap<>();
        logEntry2.put("level", "ERROR");
        logEntry2.put("message", "Test error");
        logEntry2.put("source", "system");

        logController.addLog(logEntry2);

        ResponseEntity<List<Map<String, Object>>> response = logController.getLogs(null, null, 1, 50);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    public void testGetLogs_WithLevelFilter() {
        Map<String, Object> logEntry1 = new HashMap<>();
        logEntry1.put("level", "INFO");
        logEntry1.put("message", "Test message 1");
        logEntry1.put("source", "api");

        logController.addLog(logEntry1);

        Map<String, Object> logEntry2 = new HashMap<>();
        logEntry2.put("level", "ERROR");
        logEntry2.put("message", "Test error");
        logEntry2.put("source", "system");

        logController.addLog(logEntry2);

        ResponseEntity<List<Map<String, Object>>> response = logController.getLogs("ERROR", null, 1, 50);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("ERROR", response.getBody().get(0).get("level"));
    }

    @Test
    public void testGetLogs_WithPagination() {
        for (int i = 0; i < 10; i++) {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("level", "INFO");
            logEntry.put("message", "Test message " + i);
            logEntry.put("source", "api");
            logController.addLog(logEntry);
        }

        ResponseEntity<List<Map<String, Object>>> response = logController.getLogs(null, null, 2, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().size());
    }

    @Test
    public void testAddLog_Success() {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("level", "INFO");
        logEntry.put("message", "Test message");
        logEntry.put("source", "api");

        ResponseEntity<Map<String, Object>> response = logController.addLog(logEntry);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("日志添加成功", response.getBody().get("message"));
    }

    @Test
    public void testAddLog_WithoutMessage() {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("level", "INFO");
        logEntry.put("source", "api");

        ResponseEntity<Map<String, Object>> response = logController.addLog(logEntry);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testClearLogs_Success() {
        Map<String, Object> logEntry1 = new HashMap<>();
        logEntry1.put("level", "INFO");
        logEntry1.put("message", "Test message 1");
        logEntry1.put("source", "api");
        logController.addLog(logEntry1);

        Map<String, Object> logEntry2 = new HashMap<>();
        logEntry2.put("level", "ERROR");
        logEntry2.put("message", "Test error");
        logEntry2.put("source", "system");
        logController.addLog(logEntry2);

        ResponseEntity<Map<String, Object>> response = logController.clearLogs();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("日志已清空", response.getBody().get("message"));

        ResponseEntity<List<Map<String, Object>>> getResponse = logController.getLogs(null, null, 1, 50);
        assertTrue(getResponse.getBody().isEmpty());
    }

    @Test
    public void testGetLogCount_Success() {
        Map<String, Object> logEntry1 = new HashMap<>();
        logEntry1.put("level", "INFO");
        logEntry1.put("message", "Test message 1");
        logEntry1.put("source", "api");
        logController.addLog(logEntry1);

        Map<String, Object> logEntry2 = new HashMap<>();
        logEntry2.put("level", "ERROR");
        logEntry2.put("message", "Test error");
        logEntry2.put("source", "system");
        logController.addLog(logEntry2);

        ResponseEntity<Map<String, Object>> response = logController.getLogCount();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().get("count"));
        assertEquals(1000, response.getBody().get("maxCount"));
    }
}
