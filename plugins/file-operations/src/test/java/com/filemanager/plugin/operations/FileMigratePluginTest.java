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

public class FileMigratePluginTest {

    private FileMigratePlugin plugin;
    private PluginConfigDTO config;
    private ExecutionContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        plugin = new FileMigratePlugin();
        config = new PluginConfigDTO();
        config.setConfigValues(new HashMap<>());
        context = new ExecutionContext();
    }

    @Test
    public void testPluginInitialization() {
        assertEquals("file-migrate", plugin.getId());
        assertEquals("文件迁移策略", plugin.getName());
        assertNotNull(plugin.getParameters());
        assertFalse(plugin.getParameters().isEmpty());
    }

    @Test
    public void testGetDefaultConfig() {
        PluginConfigDTO defaultConfig = plugin.getDefaultConfig();
        assertNotNull(defaultConfig);
        assertNotNull(defaultConfig.getConfigValues());
    }

    @Test
    public void testPreview_EmptyFileList() {
        List<ChangeRecord> records = plugin.preview(new ArrayList<>(), config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    public void testPreview_WithFiles() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
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
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithTemplate() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("template", "artist-album");
        config.setValue("outputDir", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithMetadataValidation() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("validateMetadata", true);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithPlaylistGeneration() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("generatePlaylist", true);
        config.setValue("playlistFormat", "m3u");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_NonExistentFile() {
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/nonexistent/file.txt");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithCancellation() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        context.cancel();
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty() || records.get(0).getStatus().equals("SKIPPED"));
    }

    @Test
    public void testDirectoryTemplate_ArtistAlbum() {
        DirectoryTemplate template = new DirectoryTemplate();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("artist", "Artist");
        metadata.put("album", "Album");
        
        String path = template.generatePath("artist-album", metadata);
        assertNotNull(path);
        assertEquals("Artist/Album", path);
    }

    @Test
    public void testDirectoryTemplate_AlbumArtist() {
        DirectoryTemplate template = new DirectoryTemplate();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("artist", "Artist");
        metadata.put("album", "Album");
        
        String path = template.generatePath("album-artist", metadata);
        assertNotNull(path);
        assertEquals("Album/Artist", path);
    }

    @Test
    public void testDirectoryTemplate_YearAlbum() {
        DirectoryTemplate template = new DirectoryTemplate();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("year", "2023");
        metadata.put("album", "Album");
        
        String path = template.generatePath("year-album", metadata);
        assertNotNull(path);
        assertEquals("2023/Album", path);
    }

    @Test
    public void testDirectoryTemplate_Custom() {
        DirectoryTemplate template = new DirectoryTemplate();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("artist", "Artist");
        metadata.put("album", "Album");
        metadata.put("year", "2023");
        
        String path = template.generatePath("{artist}/{year} - {album}", metadata);
        assertNotNull(path);
        assertEquals("Artist/2023 - Album", path);
    }

    @Test
    public void testMetadataValidator_RequiredFields() {
        MetadataValidator validator = new MetadataValidator();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", "Title");
        metadata.put("artist", "Artist");
        metadata.put("album", "Album");
        
        boolean isValid = validator.validate(metadata);
        assertTrue(isValid);
    }

    @Test
    public void testMetadataValidator_MissingRequiredFields() {
        MetadataValidator validator = new MetadataValidator();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", "Title");
        
        boolean isValid = validator.validate(metadata);
        assertFalse(isValid);
    }

    @Test
    public void testMetadataValidator_FormatValidation() {
        MetadataValidator validator = new MetadataValidator();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", "Title");
        metadata.put("artist", "Artist");
        metadata.put("album", "Album");
        metadata.put("year", "2023");
        
        boolean isValid = validator.validate(metadata);
        assertTrue(isValid);
    }

    @Test
    public void testPlaylistGenerator_M3U() throws IOException {
        PlaylistGenerator generator = new PlaylistGenerator(PlaylistGenerator.Format.M3U);
        generator.addEntry("file1.mp3", "Song 1", 180);
        generator.addEntry("file2.mp3", "Song 2", 200);
        
        File playlistFile = tempDir.resolve("playlist.m3u").toFile();
        String path = generator.generate(playlistFile.getAbsolutePath(), false);
        
        assertNotNull(path);
        assertTrue(new File(path).exists());
    }

    @Test
    public void testPlaylistGenerator_M3U8() throws IOException {
        PlaylistGenerator generator = new PlaylistGenerator(PlaylistGenerator.Format.M3U8);
        generator.addEntry("file1.mp3", "Song 1", 180);
        
        File playlistFile = tempDir.resolve("playlist.m3u8").toFile();
        String path = generator.generate(playlistFile.getAbsolutePath(), false);
        
        assertNotNull(path);
        assertTrue(new File(path).exists());
    }

    @Test
    public void testPlaylistGenerator_PLS() throws IOException {
        PlaylistGenerator generator = new PlaylistGenerator(PlaylistGenerator.Format.PLS);
        generator.addEntry("file1.mp3", "Song 1", 180);
        
        File playlistFile = tempDir.resolve("playlist.pls").toFile();
        String path = generator.generate(playlistFile.getAbsolutePath(), false);
        
        assertNotNull(path);
        assertTrue(new File(path).exists());
    }

    @Test
    public void testPathPattern_Absolute() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        
        config.setValue("pathPattern", "absolute");
        config.setValue("outputDir", tempDir.resolve("output").toString());
        
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testPathPattern_Relative() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        
        config.setValue("pathPattern", "relative");
        config.setValue("outputDir", tempDir.resolve("output").toString());
        
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testPathPattern_Flat() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        
        config.setValue("pathPattern", "flat");
        config.setValue("outputDir", tempDir.resolve("output").toString());
        
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testPathPattern_Template() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        
        config.setValue("pathPattern", "template");
        config.setValue("template", "artist-album");
        config.setValue("outputDir", tempDir.resolve("output").toString());
        
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testMetadataExtraction() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        Map<String, String> metadata = plugin.extractMetadata(testFile.getAbsolutePath());
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("filename"));
    }

    @Test
    public void testMetadataExtraction_NonExistentFile() {
        Map<String, String> metadata = plugin.extractMetadata("/nonexistent/file.txt");
        assertNotNull(metadata);
        assertTrue(metadata.isEmpty());
    }

    @Test
    public void testGenerateOutputPath() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        String outputDir = tempDir.resolve("output").toString();
        String outputPath = plugin.generateOutputPath(testFile.getAbsolutePath(), outputDir, "flat");
        
        assertNotNull(outputPath);
        assertTrue(outputPath.startsWith(outputDir));
    }

    @Test
    public void testGenerateOutputPath_WithTemplate() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        String outputDir = tempDir.resolve("output").toString();
        String outputPath = plugin.generateOutputPath(testFile.getAbsolutePath(), outputDir, "template");
        
        assertNotNull(outputPath);
        assertTrue(outputPath.startsWith(outputDir));
    }

    @Test
    public void testValidateFile() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        boolean isValid = plugin.validateFile(testFile.getAbsolutePath());
        assertTrue(isValid);
    }

    @Test
    public void testValidateFile_NonExistent() {
        boolean isValid = plugin.validateFile("/nonexistent/file.txt");
        assertFalse(isValid);
    }

    @Test
    public void testCreateDirectoryStructure() throws IOException {
        String dirPath = tempDir.resolve("test/dir/structure").toString();
        boolean created = plugin.createDirectoryStructure(dirPath);
        
        assertTrue(created);
        assertTrue(new File(dirPath).exists());
        assertTrue(new File(dirPath).isDirectory());
    }

    @Test
    public void testCreateDirectoryStructure_Existing() throws IOException {
        String dirPath = tempDir.resolve("existing").toString();
        new File(dirPath).mkdirs();
        
        boolean created = plugin.createDirectoryStructure(dirPath);
        assertTrue(created);
        assertTrue(new File(dirPath).exists());
    }
}
