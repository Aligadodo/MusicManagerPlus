package com.filemanager.backend.controller;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
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

public class StrategyControllerTest {

    @Mock
    private StrategyService strategyService;

    @InjectMocks
    private StrategyController strategyController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetStrategies_Success() {
        List<StrategyInfoDTO> mockStrategies = new ArrayList<>();
        StrategyInfoDTO strategy = new StrategyInfoDTO();
        strategy.setId("test-strategy");
        strategy.setName("Test Strategy");
        strategy.setDescription("A test strategy");
        strategy.setEnabled(true);
        mockStrategies.add(strategy);

        when(strategyService.getAvailableStrategies()).thenReturn(mockStrategies);

        ResponseEntity<List<StrategyInfoDTO>> response = strategyController.getStrategies();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("test-strategy", response.getBody().get(0).getId());
    }

    @Test
    public void testGetStrategyInfo_Success() {
        StrategyInfoDTO mockStrategy = new StrategyInfoDTO();
        mockStrategy.setId("test-strategy");
        mockStrategy.setName("Test Strategy");
        mockStrategy.setDescription("A test strategy");
        mockStrategy.setEnabled(true);

        when(strategyService.getStrategyInfo("test-strategy")).thenReturn(mockStrategy);

        ResponseEntity<StrategyInfoDTO> response = strategyController.getStrategyInfo("test-strategy");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test-strategy", response.getBody().getId());
    }

    @Test
    public void testGetStrategyInfo_NotFound() {
        when(strategyService.getStrategyInfo("nonexistent")).thenReturn(null);

        ResponseEntity<StrategyInfoDTO> response = strategyController.getStrategyInfo("nonexistent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testGetStrategyConfig_Success() {
        StrategyConfigDTO mockConfig = new StrategyConfigDTO();
        mockConfig.setConfigValues(new HashMap<>());

        when(strategyService.getStrategyConfig("test-strategy")).thenReturn(mockConfig);

        ResponseEntity<StrategyConfigDTO> response = strategyController.getStrategyConfig("test-strategy");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testUpdateStrategyConfig_Success() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());

        when(strategyService.updateStrategyConfig(eq("test-strategy"), any(StrategyConfigDTO.class)))
            .thenReturn(true);

        ResponseEntity<Map<String, Object>> response = strategyController.updateStrategyConfig("test-strategy", config);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("配置更新成功", response.getBody().get("message"));

        verify(strategyService, times(1)).updateStrategyConfig(eq("test-strategy"), any(StrategyConfigDTO.class));
    }

    @Test
    public void testAnalyzeFiles_Success() {
        Map<String, Object> request = new HashMap<>();
        List<String> files = new ArrayList<>();
        files.add("/test/file1.txt");
        files.add("/test/file2.txt");
        request.put("files", files);

        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> configValues = new HashMap<>();
        configMap.put("configValues", configValues);
        request.put("config", configMap);

        List<ChangeRecord> mockChanges = new ArrayList<>();
        ChangeRecord change = new ChangeRecord();
        change.setId("1");
        change.setOriginalName("file1.txt");
        change.setNewName("processed1.txt");
        change.setStatus("PENDING");
        mockChanges.add(change);

        when(strategyService.analyzeFiles(eq("test-strategy"), eq(files), any(StrategyConfigDTO.class)))
            .thenReturn(mockChanges);

        ResponseEntity<List<ChangeRecord>> response = strategyController.analyzeFiles("test-strategy", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("file1.txt", response.getBody().get(0).getOriginalName());
    }

    @Test
    public void testExecuteStrategy_Success() {
        Map<String, Object> request = new HashMap<>();
        List<String> files = new ArrayList<>();
        files.add("/test/file1.txt");
        request.put("files", files);

        Map<String, Object> configMap = new HashMap<>();
        Map<String, Object> configValues = new HashMap<>();
        configMap.put("configValues", configValues);
        request.put("config", configMap);

        List<ChangeRecord> mockChanges = new ArrayList<>();
        ChangeRecord change = new ChangeRecord();
        change.setId("1");
        change.setOriginalName("file1.txt");
        change.setNewName("processed1.txt");
        change.setStatus("SUCCESS");
        mockChanges.add(change);

        when(strategyService.executeStrategy(eq("test-strategy"), eq(files), any(StrategyConfigDTO.class)))
            .thenReturn(mockChanges);

        ResponseEntity<List<ChangeRecord>> response = strategyController.executeStrategy("test-strategy", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("SUCCESS", response.getBody().get(0).getStatus());
    }
}
