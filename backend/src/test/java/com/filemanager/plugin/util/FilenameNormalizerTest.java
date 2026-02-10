package com.filemanager.plugin.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FilenameNormalizerTest {

    @Test
    public void testNormalizeSimple() {
        String input = "Test [FLAC] Song - Artist.mp3";
        String result = FilenameNormalizer.normalizeSimple(input);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Normalized: " + result);
    }

    @Test
    public void testNormalizePrecise() {
        String input = "[CD1] 01. Song Name (2023) [FLAC].mp3";
        String result = FilenameNormalizer.normalizePrecise(input);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Normalized: " + result);
    }

    @Test
    public void testNormalizeTemplate() {
        String input = "[CD1] 01. Song Name (2023) [FLAC].mp3";
        String result = FilenameNormalizer.normalizeTemplate(input);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Normalized: " + result);
    }

    @Test
    public void testNormalize() {
        String input = "Test [FLAC] Song - Artist.mp3";
        String result = FilenameNormalizer.normalize(input);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("Normalized: " + result);
    }
}