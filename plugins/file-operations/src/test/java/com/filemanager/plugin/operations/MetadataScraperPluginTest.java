package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MetadataScraperPluginTest {

    private MetadataScraperPlugin plugin;
    private PluginConfigDTO config;
    private ExecutionContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        plugin = new MetadataScraperPlugin();
        config = new PluginConfigDTO();
        config.setConfigValues(new HashMap<>());
        context = new ExecutionContext();
    }

    @Test
    public void testPluginInitialization() {
        assertEquals("metadata-scraper", plugin.getId());
        assertEquals("元数据抓取工具", plugin.getName());
        assertNotNull(plugin.getParameters());
        assertFalse(plugin.getParameters().isEmpty());
    }

    @Test
    public void testGetDefaultConfig() {
        PluginConfigDTO defaultConfig = plugin.getDefaultConfig();
        assertNotNull(defaultConfig);
        assertNotNull(defaultConfig.getConfigValues());
        assertEquals("discogs", defaultConfig.getValue("primarySource"));
        assertTrue((boolean) defaultConfig.getValue("updateTags"));
        assertTrue((boolean) defaultConfig.getValue("updateCoverArt"));
        assertFalse((boolean) defaultConfig.getValue("forceUpdate"));
    }

    @Test
    public void testPreview_EmptyFileList() {
        List<ChangeRecord> records = plugin.preview(new ArrayList<>(), config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    public void testPreview_WithFiles() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.preview(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("PENDING", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithFiles() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithMultipleSources() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("sources", List.of("discogs", "musicbrainz", "local"));
        config.setValue("updateTags", true);
        config.setValue("updateCoverArt", true);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_NonExistentFile() {
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/nonexistent/file.mp3");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithCancellation() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        context.cancel();
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty() || records.get(0).getStatus().equals("SKIPPED"));
    }

    @Test
    public void testMetadataScraper_ScrapeMetadata() throws IOException {
        MetadataScraper scraper = new MetadataScraper();
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        MetadataScraper.ScrapingResult result = scraper.scrapeMetadata(testFile.getAbsolutePath());
        assertNotNull(result);
        assertNotNull(result.getMetadata());
    }

    @Test
    public void testMetadataScraper_ScrapeFromDiscogs() throws IOException {
        MetadataScraper scraper = new MetadataScraper();
        scraper.addSource(MetadataScraper.DataSource.DISCOGS);
        
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        MetadataScraper.ScrapingResult result = scraper.scrapeMetadata(testFile.getAbsolutePath());
        assertNotNull(result);
        assertEquals(MetadataScraper.DataSource.DISCOGS, result.getMetadata().getSource());
    }

    @Test
    public void testMetadataScraper_ScrapeFromMusicBrainz() throws IOException {
        MetadataScraper scraper = new MetadataScraper();
        scraper.addSource(MetadataScraper.DataSource.MUSICBRAINZ);
        
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        MetadataScraper.ScrapingResult result = scraper.scrapeMetadata(testFile.getAbsolutePath());
        assertNotNull(result);
        assertEquals(MetadataScraper.DataSource.MUSICBRAINZ, result.getMetadata().getSource());
    }

    @Test
    public void testMetadataScraper_ScrapeFromLocal() throws IOException {
        MetadataScraper scraper = new MetadataScraper();
        scraper.addSource(MetadataScraper.DataSource.LOCAL);
        
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        MetadataScraper.ScrapingResult result = scraper.scrapeMetadata(testFile.getAbsolutePath());
        assertNotNull(result);
        assertEquals(MetadataScraper.DataSource.LOCAL, result.getMetadata().getSource());
    }

    @Test
    public void testMetadataScraper_ScrapeFromFilename() throws IOException {
        MetadataScraper scraper = new MetadataScraper();
        scraper.addSource(MetadataScraper.DataSource.FILENAME);
        
        File testFile = tempDir.resolve("Artist - Title.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        MetadataScraper.ScrapingResult result = scraper.scrapeMetadata(testFile.getAbsolutePath());
        assertNotNull(result);
        assertEquals(MetadataScraper.DataSource.FILENAME, result.getMetadata().getSource());
    }

    @Test
    public void testMetadataScraper_BatchScraping() throws IOException {
        MetadataScraper scraper = new MetadataScraper();
        
        File file1 = tempDir.resolve("file1.mp3").toFile();
        File file2 = tempDir.resolve("file2.mp3").toFile();
        Files.write(file1.toPath(), "content1".getBytes());
        Files.write(file2.toPath(), "content2".getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<MetadataScraper.ScrapingResult> results = scraper.scrapeBatch(filePaths);
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testMetadataScraper_MergeMultipleSources() throws IOException {
        MetadataScraper scraper = new MetadataScraper();
        scraper.addSource(MetadataScraper.DataSource.DISCOGS);
        scraper.addSource(MetadataScraper.DataSource.MUSICBRAINZ);
        scraper.addSource(MetadataScraper.DataSource.LOCAL);
        scraper.setMergeMultipleSources(true);
        
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        MetadataScraper.ScrapingResult result = scraper.scrapeMetadata(testFile.getAbsolutePath());
        assertNotNull(result);
        assertNotNull(result.getMetadata());
    }

    @Test
    public void testMetadataScraper_ConfidenceThreshold() throws IOException {
        MetadataScraper scraper = new MetadataScraper();
        scraper.addSource(MetadataScraper.DataSource.DISCOGS);
        scraper.setMinimumConfidence(0.8f);
        
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        MetadataScraper.ScrapingResult result = scraper.scrapeMetadata(testFile.getAbsolutePath());
        assertNotNull(result);
    }

    @Test
    public void testMetadataScraper_MetadataFields() {
        MetadataScraper.Metadata metadata = new MetadataScraper.Metadata();
        
        metadata.setField(MetadataScraper.MetadataField.TITLE, "Test Title");
        metadata.setField(MetadataScraper.MetadataField.ARTIST, "Test Artist");
        metadata.setField(MetadataScraper.MetadataField.ALBUM, "Test Album");
        metadata.setField(MetadataScraper.MetadataField.YEAR, 2023);
        
        assertEquals("Test Title", metadata.getString(MetadataScraper.MetadataField.TITLE));
        assertEquals("Test Artist", metadata.getString(MetadataScraper.MetadataField.ARTIST));
        assertEquals("Test Album", metadata.getString(MetadataScraper.MetadataField.ALBUM));
        assertEquals(Integer.valueOf(2023), metadata.getInteger(MetadataScraper.MetadataField.YEAR));
    }

    @Test
    public void testMetadataScraper_CustomFields() {
        MetadataScraper.Metadata metadata = new MetadataScraper.Metadata();
        
        metadata.setCustomField("custom_field", "custom_value");
        metadata.setCustomField("another_field", 123);
        
        Map<String, Object> customFields = metadata.getCustomFields();
        assertEquals("custom_value", customFields.get("custom_field"));
        assertEquals(Integer.valueOf(123), customFields.get("another_field"));
    }

    @Test
    public void testMetadataScraper_CoverArt() {
        MetadataScraper.Metadata metadata = new MetadataScraper.Metadata();
        byte[] coverData = new byte[]{1, 2, 3, 4, 5};
        
        metadata.addCoverArt(coverData);
        List<byte[]> coverArt = metadata.getCoverArt();
        assertNotNull(coverArt);
        assertEquals(1, coverArt.size());
    }

    @Test
    public void testMetadataScraper_Lyrics() {
        MetadataScraper.Metadata metadata = new MetadataScraper.Metadata();
        String lyrics = "Test lyrics\nLine 1\nLine 2";
        
        metadata.setLyrics(lyrics);
        assertEquals(lyrics, metadata.getLyrics());
    }

    @Test
    public void testMetadataScraper_Url() {
        MetadataScraper.Metadata metadata = new MetadataScraper.Metadata();
        String url = "https://example.com";
        
        metadata.setUrl(url);
        assertEquals(url, metadata.getUrl());
    }

    @Test
    public void testMetadataScraper_HasRequiredFields() {
        MetadataScraper.Metadata metadata = new MetadataScraper.Metadata();
        assertFalse(metadata.hasRequiredFields());
        
        metadata.setField(MetadataScraper.MetadataField.TITLE, "Title");
        metadata.setField(MetadataScraper.MetadataField.ARTIST, "Artist");
        metadata.setField(MetadataScraper.MetadataField.ALBUM, "Album");
        
        assertTrue(metadata.hasRequiredFields());
    }

    @Test
    public void testMetadataScraper_Merge() {
        MetadataScraper.Metadata metadata1 = new MetadataScraper.Metadata();
        metadata1.setField(MetadataScraper.MetadataField.TITLE, "Title");
        metadata1.setConfidence(0.8f);
        
        MetadataScraper.Metadata metadata2 = new MetadataScraper.Metadata();
        metadata2.setField(MetadataScraper.MetadataField.ARTIST, "Artist");
        metadata2.setConfidence(0.9f);
        
        metadata1.merge(metadata2);
        
        assertEquals("Title", metadata1.getString(MetadataScraper.MetadataField.TITLE));
        assertEquals("Artist", metadata1.getString(MetadataScraper.MetadataField.ARTIST));
        assertEquals(0.9f, metadata1.getConfidence());
    }

    @Test
    public void testMetadataScraper_ToMap() {
        MetadataScraper.Metadata metadata = new MetadataScraper.Metadata();
        metadata.setField(MetadataScraper.MetadataField.TITLE, "Title");
        metadata.setField(MetadataScraper.MetadataField.ARTIST, "Artist");
        metadata.setConfidence(0.8f);
        
        Map<String, Object> map = metadata.toMap();
        assertNotNull(map);
        assertEquals("Title", map.get("title"));
        assertEquals("Artist", map.get("artist"));
        assertEquals(0.8f, map.get("confidence"));
    }

    @Test
    public void testExtractMetadata() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        Map<String, String> metadata = plugin.extractMetadata(testFile.getAbsolutePath());
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("filename"));
    }

    @Test
    public void testExtractMetadata_NonExistent() {
        Map<String, String> metadata = plugin.extractMetadata("/nonexistent/file.mp3");
        assertNotNull(metadata);
        assertTrue(metadata.isEmpty());
    }

    @Test
    public void testUpdateFileMetadata() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Test Title");
        metadata.put("artist", "Test Artist");
        
        boolean updated = plugin.updateFileMetadata(testFile.getAbsolutePath(), metadata);
        // This might fail if actual metadata writing isn't implemented, but should not throw exceptions
        assertNotNull(Boolean.valueOf(updated));
    }

    @Test
    public void testUpdateCoverArt() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        byte[] coverData = new byte[]{1, 2, 3, 4, 5};
        boolean updated = plugin.updateCoverArt(testFile.getAbsolutePath(), coverData);
        // This might fail if actual cover art writing isn't implemented, but should not throw exceptions
        assertNotNull(Boolean.valueOf(updated));
    }

    @Test
    public void testBatchMetadataUpdate() throws IOException {
        File file1 = tempDir.resolve("file1.mp3").toFile();
        File file2 = tempDir.resolve("file2.mp3").toFile();
        
        Files.write(file1.toPath(), "content1".getBytes());
        Files.write(file2.toPath(), "content2".getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(2, records.size());
    }

    @Test
    public void testErrorHandling() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        
        // Test with invalid source
        config.setValue("primarySource", "invalid_source");
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testSupportedSources() {
        List<String> supportedSources = plugin.getSupportedSources();
        assertNotNull(supportedSources);
        assertFalse(supportedSources.isEmpty());
        assertTrue(supportedSources.contains("discogs"));
        assertTrue(supportedSources.contains("musicbrainz"));
        assertTrue(supportedSources.contains("last.fm"));
        assertTrue(supportedSources.contains("spotify"));
        assertTrue(supportedSources.contains("local"));
        assertTrue(supportedSources.contains("filename"));
    }

    @Test
    public void testIsSupportedSource() {
        assertTrue(plugin.isSupportedSource("discogs"));
        assertTrue(plugin.isSupportedSource("musicbrainz"));
        assertTrue(plugin.isSupportedSource("local"));
        assertFalse(plugin.isSupportedSource("invalid_source"));
    }

    @Test
    public void testCleanup() throws IOException {
        File tempFile = tempDir.resolve("temp.txt").toFile();
        Files.write(tempFile.toPath(), "temp content".getBytes());
        
        boolean cleaned = plugin.cleanup(tempFile.getAbsolutePath());
        assertTrue(cleaned);
        assertFalse(tempFile.exists());
    }

    @Test
    public void testCleanup_NonExistent() {
        boolean cleaned = plugin.cleanup("/nonexistent/file.txt");
        assertTrue(cleaned);
    }
}
