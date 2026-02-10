package com.filemanager.plugin.enums.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CrossDriveModeTest {

    @Test
    public void testFromCode() {
        CrossDriveMode mode = CrossDriveMode.fromCode("move");
        assertEquals(CrossDriveMode.MOVE, mode);
    }

    @Test
    public void testFromCodeWithInvalidValue() {
        CrossDriveMode mode = CrossDriveMode.fromCode("invalid");
        assertEquals(CrossDriveMode.MOVE, mode);
    }

    @Test
    public void testGetCode() {
        assertEquals("move", CrossDriveMode.MOVE.getCode());
        assertEquals("copy", CrossDriveMode.COPY.getCode());
    }

    @Test
    public void testGetNameZh() {
        assertEquals("移动 (Move)", CrossDriveMode.MOVE.getNameZh());
        assertEquals("复制 (Copy)", CrossDriveMode.COPY.getNameZh());
    }

    @Test
    public void testGetNameEn() {
        assertEquals("Move", CrossDriveMode.MOVE.getNameEn());
        assertEquals("Copy", CrossDriveMode.COPY.getNameEn());
    }

    @Test
    public void testIsMove() {
        assertTrue(CrossDriveMode.MOVE.isMove());
        assertFalse(CrossDriveMode.COPY.isMove());
    }

    @Test
    public void testIsCopy() {
        assertTrue(CrossDriveMode.COPY.isCopy());
        assertFalse(CrossDriveMode.MOVE.isCopy());
    }

    @Test
    public void testGetActionType() {
        assertEquals("移动", CrossDriveMode.MOVE.getActionType());
        assertEquals("复制", CrossDriveMode.COPY.getActionType());
    }
}