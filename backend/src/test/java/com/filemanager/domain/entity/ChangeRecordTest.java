package com.filemanager.domain.entity;

import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChangeRecordTest {

    @Test
    public void testDefaultConstructor() {
        ChangeRecord record = new ChangeRecord();
        
        assertNotNull(record);
        assertNull(record.getId());
        assertNull(record.getOriginalName());
        assertNull(record.getNewName());
        assertNull(record.getFileHandle());
        assertFalse(record.isChanged());
        assertNull(record.getNewPath());
        assertNull(record.getOperationType());
        assertNotNull(record.getExtraParams());
        assertTrue(record.getExtraParams().isEmpty());
        assertNull(record.getStatus());
    }

    @Test
    public void testSixParameterConstructor() {
        File file = new File("/path/to/file.mp3");
        ChangeRecord record = new ChangeRecord(
            "file.mp3",
            "file_renamed.mp3",
            file,
            true,
            "/path/to/file_renamed.mp3",
            OperationType.RENAME
        );
        
        assertEquals("file.mp3", record.getOriginalName());
        assertEquals("file_renamed.mp3", record.getNewName());
        assertEquals(file, record.getFileHandle());
        assertTrue(record.isChanged());
        assertEquals("/path/to/file_renamed.mp3", record.getNewPath());
        assertEquals("RENAME", record.getOperationType());
        assertEquals(OperationType.RENAME, record.getOperationTypeEnum());
    }

    @Test
    public void testEightParameterConstructor() {
        File file = new File("/path/to/file.mp3");
        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("key1", "value1");
        extraParams.put("key2", "value2");
        
        ChangeRecord record = new ChangeRecord(
            "file.mp3",
            "file_converted.flac",
            file,
            true,
            "/path/to/file_converted.flac",
            OperationType.CONVERT,
            extraParams,
            ExecStatus.PENDING
        );
        
        assertEquals("file.mp3", record.getOriginalName());
        assertEquals("file_converted.flac", record.getNewName());
        assertEquals(file, record.getFileHandle());
        assertTrue(record.isChanged());
        assertEquals("/path/to/file_converted.flac", record.getNewPath());
        assertEquals("CONVERT", record.getOperationType());
        assertEquals(OperationType.CONVERT, record.getOperationTypeEnum());
        assertEquals(extraParams, record.getExtraParams());
        assertEquals("PENDING", record.getStatus());
        assertEquals(ExecStatus.PENDING, record.getStatusEnum());
    }

    @Test
    public void testOperationTypeEnumGetter() {
        ChangeRecord record = new ChangeRecord();
        record.setOperationType("MOVE");
        
        assertEquals("MOVE", record.getOperationType());
        assertEquals(OperationType.MOVE, record.getOperationTypeEnum());
    }

    @Test
    public void testOperationTypeEnumSetter() {
        ChangeRecord record = new ChangeRecord();
        record.setOperationType(OperationType.DELETE);
        
        assertEquals("DELETE", record.getOperationType());
        assertEquals(OperationType.DELETE, record.getOperationTypeEnum());
    }

    @Test
    public void testOperationTypeEnumGetterWithInvalidValue() {
        ChangeRecord record = new ChangeRecord();
        record.setOperationType("INVALID_OPERATION");
        
        assertEquals("INVALID_OPERATION", record.getOperationType());
        assertEquals(OperationType.NONE, record.getOperationTypeEnum());
    }

    @Test
    public void testOperationTypeEnumSetterWithNull() {
        ChangeRecord record = new ChangeRecord();
        record.setOperationType((OperationType) null);
        
        assertEquals("NONE", record.getOperationType());
        assertEquals(OperationType.NONE, record.getOperationTypeEnum());
    }

    @Test
    public void testStatusEnumGetter() {
        ChangeRecord record = new ChangeRecord();
        record.setStatus("SUCCESS");
        
        assertEquals("SUCCESS", record.getStatus());
        assertEquals(ExecStatus.SUCCESS, record.getStatusEnum());
    }

    @Test
    public void testStatusEnumSetter() {
        ChangeRecord record = new ChangeRecord();
        record.setStatus(ExecStatus.FAILED);
        
        assertEquals("FAILED", record.getStatus());
        assertEquals(ExecStatus.FAILED, record.getStatusEnum());
    }

    @Test
    public void testStatusEnumGetterWithInvalidValue() {
        ChangeRecord record = new ChangeRecord();
        record.setStatus("INVALID_STATUS");
        
        assertEquals("INVALID_STATUS", record.getStatus());
        assertEquals(ExecStatus.PENDING, record.getStatusEnum());
    }

    @Test
    public void testStatusEnumSetterWithNull() {
        ChangeRecord record = new ChangeRecord();
        record.setStatus((ExecStatus) null);
        
        assertEquals("PENDING", record.getStatus());
        assertEquals(ExecStatus.PENDING, record.getStatusEnum());
    }

    @Test
    public void testAddProcessInfo() {
        ChangeRecord record = new ChangeRecord();
        record.addProcessInfo("开始处理文件");
        record.addProcessInfo("处理完成");
        
        assertEquals(2, record.getProcessInfo().size());
        assertEquals("开始处理文件", record.getProcessInfo().get(0));
        assertEquals("处理完成", record.getProcessInfo().get(1));
    }

    @Test
    public void testAddProcessInfoWithKeyValue() {
        ChangeRecord record = new ChangeRecord();
        record.addProcessInfo("开始时间", "2026-02-11 10:00:00");
        record.addProcessInfo("结束时间", "2026-02-11 10:00:05");
        
        assertEquals(2, record.getProcessInfo().size());
        assertEquals("开始时间: 2026-02-11 10:00:00", record.getProcessInfo().get(0));
        assertEquals("结束时间: 2026-02-11 10:00:05", record.getProcessInfo().get(1));
    }

    @Test
    public void testGetCurrentSourceWithIntermediateFile() {
        File originalFile = new File("/path/to/original.mp3");
        File intermediateFile = new File("/path/to/intermediate.mp3");
        
        ChangeRecord record = new ChangeRecord();
        record.setFileHandle(originalFile);
        record.setIntermediateFile(intermediateFile);
        
        assertEquals(intermediateFile, record.getCurrentSource());
    }

    @Test
    public void testGetCurrentSourceWithoutIntermediateFile() {
        File originalFile = new File("/path/to/original.mp3");
        
        ChangeRecord record = new ChangeRecord();
        record.setFileHandle(originalFile);
        
        assertEquals(originalFile, record.getCurrentSource());
    }

    @Test
    public void testSixParameterConstructorWithNoneOperation() {
        File file = new File("/path/to/file.mp3");
        ChangeRecord record = new ChangeRecord(
            "file.mp3",
            "file.mp3",
            file,
            true,
            "/path/to/file.mp3",
            OperationType.NONE
        );
        
        assertFalse(record.isChanged());
    }

    @Test
    public void testSixParameterConstructorWithNullOperation() {
        File file = new File("/path/to/file.mp3");
        ChangeRecord record = new ChangeRecord(
            "file.mp3",
            "file.mp3",
            file,
            true,
            "/path/to/file.mp3",
            "NONE"
        );
        
        assertEquals("NONE", record.getOperationType());
        assertFalse(record.isChanged());
    }

    @Test
    public void testAllOperationTypes() {
        OperationType[] operationTypes = OperationType.values();
        assertEquals(13, operationTypes.length);
        
        for (OperationType type : operationTypes) {
            ChangeRecord record = new ChangeRecord();
            record.setOperationType(type);
            assertEquals(type, record.getOperationTypeEnum());
        }
    }

    @Test
    public void testAllStatusTypes() {
        ExecStatus[] statusTypes = ExecStatus.values();
        assertEquals(6, statusTypes.length);
        
        for (ExecStatus status : statusTypes) {
            ChangeRecord record = new ChangeRecord();
            record.setStatus(status);
            assertEquals(status, record.getStatusEnum());
        }
    }
}
