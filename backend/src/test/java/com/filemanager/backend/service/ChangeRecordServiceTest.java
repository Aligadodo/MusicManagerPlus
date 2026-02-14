package com.filemanager.backend.service;

import com.filemanager.backend.entity.ChangeRecordPO;
import com.filemanager.backend.mapper.ChangeRecordMapper;
import com.filemanager.backend.service.impl.ChangeRecordServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChangeRecordServiceTest {

    @Mock
    private ChangeRecordMapper changeRecordMapper;

    @InjectMocks
    private ChangeRecordServiceImpl changeRecordService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testCreateRecord() {
        ChangeRecordPO changeRecord = new ChangeRecordPO();
        changeRecord.setId(1L);
        changeRecord.setTaskId("task-001");
        changeRecord.setOriginalName("test.mp3");
        changeRecord.setNewPath("/new/path/test.mp3");

        when(changeRecordMapper.insert(any(ChangeRecordPO.class))).thenReturn(1);

        ChangeRecordPO result = changeRecordService.createRecord(changeRecord);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(changeRecordMapper, times(1)).insert(any(ChangeRecordPO.class));
    }

    @Test
    void testGetRecordById() {
        ChangeRecordPO changeRecord = new ChangeRecordPO();
        changeRecord.setId(1L);
        changeRecord.setTaskId("task-001");

        when(changeRecordMapper.selectById(1L)).thenReturn(changeRecord);

        ChangeRecordPO result = changeRecordService.getRecordById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("task-001", result.getTaskId());
        verify(changeRecordMapper, times(1)).selectById(1L);
    }

    @Test
    void testGetRecordsByTaskId() {
        ChangeRecordPO change1 = new ChangeRecordPO();
        change1.setId(1L);
        change1.setTaskId("task-001");

        ChangeRecordPO change2 = new ChangeRecordPO();
        change2.setId(2L);
        change2.setTaskId("task-001");

        List<ChangeRecordPO> changes = Arrays.asList(change1, change2);

        when(changeRecordMapper.selectByTaskId("task-001")).thenReturn(changes);

        List<ChangeRecordPO> result = changeRecordService.getRecordsByTaskId("task-001");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(changeRecordMapper, times(1)).selectByTaskId("task-001");
    }

    @Test
    void testGetRecordsByPage() {
        ChangeRecordPO change1 = new ChangeRecordPO();
        change1.setId(1L);
        change1.setTaskId("task-001");

        List<ChangeRecordPO> changes = Arrays.asList(change1);

        when(changeRecordMapper.selectByPage(
            "task-001", null, null, null, null, null, "created_at", "DESC", 0, 10
        )).thenReturn(changes);

        List<ChangeRecordPO> result = changeRecordService.getRecordsByPage(
            "task-001", null, null, null, null, null, "created_at", "DESC", 1, 10
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(changeRecordMapper, times(1)).selectByPage(
            "task-001", null, null, null, null, null, "created_at", "DESC", 0, 10
        );
    }

    @Test
    void testUpdateRecord() {
        ChangeRecordPO changeRecord = new ChangeRecordPO();
        changeRecord.setId(1L);
        changeRecord.setStatus("SUCCESS");

        when(changeRecordMapper.update(any(ChangeRecordPO.class))).thenReturn(1);

        ChangeRecordPO result = changeRecordService.updateRecord(changeRecord);

        assertNotNull(result);
        verify(changeRecordMapper, times(1)).update(any(ChangeRecordPO.class));
    }

    @Test
    void testDeleteRecord() {
        when(changeRecordMapper.deleteById(1L)).thenReturn(1);

        boolean result = changeRecordService.deleteRecord(1L);

        assertTrue(result);
        verify(changeRecordMapper, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteRecordsByTaskId() {
        when(changeRecordMapper.deleteByTaskId("task-001")).thenReturn(2);

        boolean result = changeRecordService.deleteRecordsByTaskId("task-001");

        assertTrue(result);
        verify(changeRecordMapper, times(1)).deleteByTaskId("task-001");
    }

    @Test
    void testGetTotalRecordCount() {
        when(changeRecordMapper.countByPage(null, null, null, null, null, null)).thenReturn(5);

        long result = changeRecordService.getTotalRecordCount();

        assertEquals(5L, result);
        verify(changeRecordMapper, times(1)).countByPage(null, null, null, null, null, null);
    }

    @Test
    void testGetRecordCountByTaskId() {
        when(changeRecordMapper.countByPage("task-001", null, null, null, null, null)).thenReturn(5);

        long result = changeRecordService.getRecordCountByTaskId("task-001");

        assertEquals(5L, result);
        verify(changeRecordMapper, times(1)).countByPage("task-001", null, null, null, null, null);
    }

    @Test
    void testGetRecordCountByStatus() {
        when(changeRecordMapper.countByPage(null, "SUCCESS", null, null, null, null)).thenReturn(3);

        long result = changeRecordService.getRecordCountByStatus("SUCCESS");

        assertEquals(3L, result);
        verify(changeRecordMapper, times(1)).countByPage(null, "SUCCESS", null, null, null, null);
    }

    @Test
    void testCountByPage() {
        when(changeRecordMapper.countByPage(
            "task-001", "SUCCESS", "RENAME", null, null, null
        )).thenReturn(3);

        long result = changeRecordService.countByPage(
            "task-001", "SUCCESS", "RENAME", null, null
        );

        assertEquals(3L, result);
        verify(changeRecordMapper, times(1)).countByPage(
            "task-001", "SUCCESS", "RENAME", null, null, null
        );
    }

    @Test
    void testSearchRecords() {
        ChangeRecordPO change1 = new ChangeRecordPO();
        change1.setId(1L);
        change1.setOriginalName("test_song.mp3");

        List<ChangeRecordPO> changes = Arrays.asList(change1);

        when(changeRecordMapper.selectByPage(
            null, null, null, null, null, "test", "created_at", "DESC", 0, 10
        )).thenReturn(changes);

        List<ChangeRecordPO> result = changeRecordService.searchRecords(
            "test", null, 1, 10
        );

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(changeRecordMapper, times(1)).selectByPage(
            null, null, null, null, null, "test", "created_at", "DESC", 0, 10
        );
    }

    @Test
    void testGetRecordsByStatus() {
        ChangeRecordPO change1 = new ChangeRecordPO();
        change1.setId(1L);
        change1.setStatus("SUCCESS");

        List<ChangeRecordPO> changes = Arrays.asList(change1);

        when(changeRecordMapper.selectByTaskIdAndStatus(null, "SUCCESS")).thenReturn(changes);

        List<ChangeRecordPO> result = changeRecordService.getRecordsByStatus("SUCCESS");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(changeRecordMapper, times(1)).selectByTaskIdAndStatus(null, "SUCCESS");
    }

    @Test
    void testGetRecordsByOperationType() {
        ChangeRecordPO change1 = new ChangeRecordPO();
        change1.setId(1L);
        change1.setOperationType("RENAME");

        List<ChangeRecordPO> changes = Arrays.asList(change1);

        when(changeRecordMapper.selectByTaskIdAndOperationType(null, "RENAME")).thenReturn(changes);

        List<ChangeRecordPO> result = changeRecordService.getRecordsByOperationType("RENAME");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(changeRecordMapper, times(1)).selectByTaskIdAndOperationType(null, "RENAME");
    }
}
