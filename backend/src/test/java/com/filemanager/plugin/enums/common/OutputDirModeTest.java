package com.filemanager.plugin.enums.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OutputDirModeTest {

    @Test
    public void testFromCode() {
        OutputDirMode mode = OutputDirMode.fromCode("subdirectory");
        assertEquals(OutputDirMode.SUBDIRECTORY, mode);
    }

    @Test
    public void testFromCodeWithInvalidValue() {
        OutputDirMode mode = OutputDirMode.fromCode("invalid");
        assertEquals(OutputDirMode.SUBDIRECTORY, mode);
    }

    @Test
    public void testGetCode() {
        assertEquals("subdirectory", OutputDirMode.SUBDIRECTORY.getCode());
        assertEquals("specified_dir", OutputDirMode.SPECIFIED_DIR.getCode());
        assertEquals("root_dir", OutputDirMode.ROOT_DIR.getCode());
    }

    @Test
    public void testGetNameZh() {
        assertEquals("子目录", OutputDirMode.SUBDIRECTORY.getNameZh());
        assertEquals("指定目录", OutputDirMode.SPECIFIED_DIR.getNameZh());
        assertEquals("根目录", OutputDirMode.ROOT_DIR.getNameZh());
    }

    @Test
    public void testGetNameEn() {
        assertEquals("Subdirectory", OutputDirMode.SUBDIRECTORY.getNameEn());
        assertEquals("Specified Directory", OutputDirMode.SPECIFIED_DIR.getNameEn());
        assertEquals("Root Directory", OutputDirMode.ROOT_DIR.getNameEn());
    }
}