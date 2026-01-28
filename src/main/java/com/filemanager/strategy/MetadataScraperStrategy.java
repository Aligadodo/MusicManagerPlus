/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-28
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.scraper.cache.MetadataCacheManager;
import com.filemanager.strategy.scraper.config.*;
import com.filemanager.strategy.scraper.model.*;
import com.filemanager.strategy.scraper.processor.MetadataScraperProcessor;
import com.filemanager.strategy.scraper.source.MetadataSource;
import com.filemanager.strategy.scraper.source.impl.*;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
import com.filemanager.strategy.scraper.ui.ModuleConfigUI;
import com.filemanager.strategy.scraper.ui.AlbumInfoConfigUI;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import com.filemanager.app.tools.display.FloatingTooltip;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MetadataScraperStrategy extends IAppStrategy {

    private final JFXComboBox<String> cbSource;
    private final Spinner<Integer> spThreads;
    
    private final ModuleConfigUI lyricsConfigUI;
    private final ModuleConfigUI coverConfigUI;
    private final AlbumInfoConfigUI albumInfoConfigUI;
    
    private final Map<String, MetadataSource> sources;
    private final MetadataCacheManager cacheManager;
    private final MetadataScraperProcessor processor;
    
    protected String pSource;
    protected int pThreads;
    
    protected LyricsModuleConfig lyricsConfig;
    protected CoverModuleConfig coverConfig;
    protected AlbumInfoModuleConfig albumInfoConfig;

    public MetadataScraperStrategy() {
        sources = new LinkedHashMap<>();
        sources.put("本地推断 (仅生成清单)", new LocalInferenceSource());
        sources.put("网易云音乐 (中文歌曲)", new NeteaseMusicSource());
        sources.put("咪咕音乐 (版权歌曲)", new MiguMusicSource());
        
        cbSource = new JFXComboBox<>(FXCollections.observableArrayList(sources.keySet()));
        cbSource.getSelectionModel().select(0);
        
        updateSourceTooltip();
        
        cbSource.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateSourceTooltip();
        });

        spThreads = new Spinner<>(1, 8, 2);
        
        ArrayList<String> threadsTooltipLines = new ArrayList<>();
        threadsTooltipLines.add("参数名称：网络并发");
        threadsTooltipLines.add("参数用途：用于设置网络请求的并发线程数");
        threadsTooltipLines.add("线程说明：");
        threadsTooltipLines.add("- 1：单线程，速度较慢但稳定");
        threadsTooltipLines.add("- 2：双线程，平衡速度和稳定性");
        threadsTooltipLines.add("- 4：四线程，推荐配置");
        threadsTooltipLines.add("- 8：多线程，速度快但可能不稳定");
        FloatingTooltip.bindToNode(spThreads, "元数据刮削设置", threadsTooltipLines);

        cacheManager = new MetadataCacheManager(true);
        processor = new MetadataScraperProcessor(cacheManager);
        
        lyricsConfig = new LyricsModuleConfig();
        coverConfig = new CoverModuleConfig();
        albumInfoConfig = new AlbumInfoModuleConfig();
        
        lyricsConfigUI = new ModuleConfigUI(lyricsConfig);
        coverConfigUI = new ModuleConfigUI(coverConfig);
        albumInfoConfigUI = new AlbumInfoConfigUI(albumInfoConfig);
    }
    
    private void updateSourceTooltip() {
        String selectedSource = cbSource.getValue();
        MetadataSource source = sources.get(selectedSource);
        
        if (source != null) {
            ArrayList<String> tooltipLines = new ArrayList<>();
            tooltipLines.add("参数名称：数据源");
            tooltipLines.add("参数用途：用于设置元数据刮削的数据源");
            tooltipLines.add("数据源说明：");
            
            String description = source.getSourceDescription();
            String[] lines = description.split("\n");
            for (String line : lines) {
                tooltipLines.add(line);
            }
            
            FloatingTooltip.bindToNode(cbSource, "元数据刮削设置", tooltipLines);
        }
    }

    @Override
    public String getName() {
        return "音频元数据自动刮削";
    }

    @Override
    public String getDescription() {
        return "一站式补全：音频Tag、歌词、专辑封面图(jpg)及专辑简介文档(txt)。支持自动生成曲目列表。";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10, 0, 0, 0));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(StyleFactory.createParamLabel("数据源:"), 0, 0);
        grid.add(cbSource, 1, 0);
        grid.add(StyleFactory.createParamLabel("网络并发:"), 0, 1);
        grid.add(spThreads, 1, 1);

        TitledPane lyricsPane = new TitledPane("歌词匹配模块", lyricsConfigUI);
        TitledPane coverPane = new TitledPane("封面匹配模块", coverConfigUI);
        TitledPane albumInfoPane = new TitledPane("专辑信息模块", albumInfoConfigUI);
        
        lyricsPane.setCollapsible(true);
        coverPane.setCollapsible(true);
        albumInfoPane.setCollapsible(true);

        box.getChildren().addAll(grid, new Separator(), lyricsPane, coverPane, albumInfoPane);
        return box;
    }

    @Override
    public void captureParams() {
        pSource = cbSource.getValue();
        pThreads = spThreads.getValue();
        
        lyricsConfigUI.captureParams();
        coverConfigUI.captureParams();
        albumInfoConfigUI.captureParams();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("meta_source", cbSource.getValue());
        props.setProperty("meta_threads", String.valueOf(spThreads.getValue()));
        
        props.setProperty("meta_lyrics_enabled", String.valueOf(lyricsConfig.isEnabled()));
        props.setProperty("meta_lyrics_save_mode", lyricsConfig.getSaveMode().name());
        props.setProperty("meta_lyrics_duplicate", lyricsConfig.getDuplicateMode().name());
        props.setProperty("meta_lyrics_cache", String.valueOf(lyricsConfig.isUseCache()));
        
        props.setProperty("meta_cover_enabled", String.valueOf(coverConfig.isEnabled()));
        props.setProperty("meta_cover_save_mode", coverConfig.getSaveMode().name());
        props.setProperty("meta_cover_duplicate", coverConfig.getDuplicateMode().name());
        props.setProperty("meta_cover_cache", String.valueOf(coverConfig.isUseCache()));
        
        props.setProperty("meta_album_enabled", String.valueOf(albumInfoConfig.isEnabled()));
        props.setProperty("meta_album_save_mode", albumInfoConfig.getSaveMode().name());
        props.setProperty("meta_album_duplicate", albumInfoConfig.getDuplicateMode().name());
        props.setProperty("meta_album_cache", String.valueOf(albumInfoConfig.isUseCache()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("meta_source")) cbSource.getSelectionModel().select(props.getProperty("meta_source"));
        if (props.containsKey("meta_threads")) {
            try {
                spThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("meta_threads")));
            } catch (Exception e) {
            }
        }
        
        if (props.containsKey("meta_lyrics_enabled"))
            lyricsConfig.setEnabled(Boolean.parseBoolean(props.getProperty("meta_lyrics_enabled")));
        if (props.containsKey("meta_lyrics_save_mode"))
            lyricsConfig.setSaveMode(ModuleConfig.SaveMode.valueOf(props.getProperty("meta_lyrics_save_mode")));
        if (props.containsKey("meta_lyrics_duplicate"))
            lyricsConfig.setDuplicateMode(ModuleConfig.DuplicateMode.valueOf(props.getProperty("meta_lyrics_duplicate")));
        if (props.containsKey("meta_lyrics_cache"))
            lyricsConfig.setUseCache(Boolean.parseBoolean(props.getProperty("meta_lyrics_cache")));
        
        if (props.containsKey("meta_cover_enabled"))
            coverConfig.setEnabled(Boolean.parseBoolean(props.getProperty("meta_cover_enabled")));
        if (props.containsKey("meta_cover_save_mode"))
            coverConfig.setSaveMode(ModuleConfig.SaveMode.valueOf(props.getProperty("meta_cover_save_mode")));
        if (props.containsKey("meta_cover_duplicate"))
            coverConfig.setDuplicateMode(ModuleConfig.DuplicateMode.valueOf(props.getProperty("meta_cover_duplicate")));
        if (props.containsKey("meta_cover_cache"))
            coverConfig.setUseCache(Boolean.parseBoolean(props.getProperty("meta_cover_cache")));
        
        if (props.containsKey("meta_album_enabled"))
            albumInfoConfig.setEnabled(Boolean.parseBoolean(props.getProperty("meta_album_enabled")));
        if (props.containsKey("meta_album_save_mode"))
            albumInfoConfig.setSaveMode(ModuleConfig.SaveMode.valueOf(props.getProperty("meta_album_save_mode")));
        if (props.containsKey("meta_album_duplicate"))
            albumInfoConfig.setDuplicateMode(ModuleConfig.DuplicateMode.valueOf(props.getProperty("meta_album_duplicate")));
        if (props.containsKey("meta_album_cache"))
            albumInfoConfig.setUseCache(Boolean.parseBoolean(props.getProperty("meta_album_cache")));
        
        lyricsConfigUI.loadFromConfig(lyricsConfig);
        coverConfigUI.loadFromConfig(coverConfig);
        albumInfoConfigUI.loadFromConfig(albumInfoConfig);
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        Set<String> supportedExts = new HashSet<>(Arrays.asList("mp3", "flac", "m4a", "ogg", "wav", "dsf", "dff"));

        File file = rec.getFileHandle();
        File parentDir = file.getParentFile();
        String name = file.getName().toLowerCase();
        String ext = name.lastIndexOf(".") > 0 ? name.substring(name.lastIndexOf(".") + 1) : "";
        if (!supportedExts.contains(ext)) {
            return Collections.emptyList();
        }
        
        List<ChangeRecord> results = new ArrayList<>();
        MetadataHelper.AudioMeta guess = MetadataHelper.extractFromFileSystem(file);
        
        MetadataSource source = sources.get(pSource);
        if (source == null) {
            logError("未找到数据源: " + pSource);
            return Collections.emptyList();
        }
        
        processor.setSource(source);
        processor.setLyricsConfig(lyricsConfig);
        processor.setCoverConfig(coverConfig);
        processor.setAlbumInfoConfig(albumInfoConfig);

        try {
            AudioFile f = AudioFileIO.read(file);
            Tag tag = f.getTag();
            int duration = f.getAudioHeader().getTrackLength();

            if (lyricsConfig.isEnabled()) {
                boolean hasLyricsField = true;
                try {
                    tag.getFirst(FieldKey.LYRICS);
                } catch (Exception e) {
                    hasLyricsField = false;
                }
                
                boolean processLyrics = lyricsConfig.getDuplicateMode() == ModuleConfig.DuplicateMode.OVERWRITE 
                    || tag == null || (hasLyricsField && tag.getFirst(FieldKey.LYRICS).isEmpty());
                
                if (processLyrics) {
                    rec.addProcessInfo("开始处理歌词: " + guess.getArtist() + " - " + guess.getTitle());
                    rec.addProcessInfo("使用数据源: " + source.getSourceName());
                    
                    if (processor.processLyrics(guess.getArtist(), guess.getTitle(), duration, file)) {
                        rec.setChanged(true);
                        rec.setOpType(OperationType.SCRAPER);
                        rec.getExtraParams().put("scraper_active", "true");
                        rec.addProcessInfo("成功获取歌词");
                        rec.setNewName("[更新] " + file.getName());
                    } else {
                        rec.addProcessInfo("未找到歌词");
                    }
                }
            }

            if (coverConfig.isEnabled()) {
                File targetCover = new File(parentDir, "cover.jpg");
                boolean processCover = coverConfig.getDuplicateMode() == ModuleConfig.DuplicateMode.OVERWRITE 
                    || !targetCover.exists();
                
                if (processCover) {
                    rec.addProcessInfo("开始处理封面: " + guess.getArtist() + " - " + guess.getAlbum());
                    rec.addProcessInfo("使用数据源: " + source.getSourceName());
                    
                    if (processor.processCover(guess.getArtist(), guess.getAlbum(), parentDir)) {
                        Map<String, String> p = new HashMap<>();
                        p.put("task_type", "DOWNLOAD_COVER");
                        ChangeRecord coverRec = new ChangeRecord("下载: 专辑封面", "cover.jpg", parentDir,
                                true, targetCover.getAbsolutePath(), OperationType.SCRAPER, p, ExecStatus.PENDING);
                        coverRec.addProcessInfo("成功获取封面");
                        results.add(coverRec);
                    } else {
                        rec.addProcessInfo("未找到封面");
                    }
                }
            }

            if (albumInfoConfig.isEnabled()) {
                File targetInfo = new File(parentDir, "AlbumInfo.txt");
                boolean processInfo = albumInfoConfig.getDuplicateMode() == ModuleConfig.DuplicateMode.OVERWRITE 
                    || !targetInfo.exists();
                
                if (processInfo) {
                    rec.addProcessInfo("开始处理专辑信息: " + guess.getArtist() + " - " + guess.getAlbum());
                    rec.addProcessInfo("使用数据源: " + source.getSourceName());
                    
                    if (processor.processAlbumInfo(guess.getArtist(), guess.getAlbum(), parentDir)) {
                        Map<String, String> p = new HashMap<>();
                        p.put("task_type", "GENERATE_INFO");
                        ChangeRecord infoRec = new ChangeRecord("生成: 专辑资料", "AlbumInfo.txt", parentDir,
                                true, targetInfo.getAbsolutePath(), OperationType.SCRAPER, p, ExecStatus.PENDING);
                        infoRec.addProcessInfo("成功获取专辑信息");
                        results.add(infoRec);
                    } else {
                        rec.addProcessInfo("未找到专辑信息");
                    }
                }
            }
        } catch (Exception e) {
            rec.addProcessInfo("处理失败: " + e.getMessage());
            logError("处理文件失败: " + file.getName() + ", 错误: " + e.getMessage());
        }
        
        return results;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        String taskType = rec.getExtraParams().get("task_type");

        if ("DOWNLOAD_COVER".equals(taskType)) {
            downloadCoverFile(rec);
        } else if ("GENERATE_INFO".equals(taskType)) {
            generateAlbumInfo(rec);
        } else if ("true".equals(rec.getExtraParams().get("scraper_active"))) {
            updateTrackMeta(rec);
        }
    }

    private void downloadCoverFile(ChangeRecord rec) {
        try {
            String url = rec.getExtraParams().get("url");
            if (url == null) return;
            byte[] data = downloadBytes(url);
            if (data != null && data.length > 0) {
                Files.write(new File(rec.getNewPath()).toPath(), data);
                log("封面下载成功: " + rec.getNewPath());
            }
        } catch (Exception e) {
            logError("封面下载失败: " + e.getMessage());
        }
    }

    private void generateAlbumInfo(ChangeRecord rec) {
        File dir = rec.getFileHandle();
        File target = new File(rec.getNewPath());
        Map<String, String> p = rec.getExtraParams();

        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("专辑名称: " + p.getOrDefault("album", "Unknown") + "\n");
        sb.append("艺术家  : " + p.getOrDefault("artist", "Unknown") + "\n");
        if (p.containsKey("year")) sb.append("发行年份: " + p.get("year") + "\n");
        if (p.containsKey("genre")) sb.append("流派    : " + p.get("genre") + "\n");
        sb.append("==================================================\n\n");

        if (p.containsKey("intro") && !p.get("intro").isEmpty()) {
            sb.append("[ 专辑简介 ]\n");
            sb.append(p.get("intro") + "\n\n");
        }

        sb.append("[ 曲目列表 ]\n");

        File[] files = dir.listFiles();
        if (files != null) {
            List<File> audios = Arrays.stream(files)
                    .filter(f -> f.getName().matches(".*\\.(mp3|flac|wav|m4a|ape|dsf|dff)$"))
                    .sorted(Comparator.comparing(File::getName))
                    .collect(Collectors.toList());

            for (File f : audios) {
                String title = f.getName();
                String time = "";
                try {
                    AudioFile af = AudioFileIO.read(f);
                    int len = af.getAudioHeader().getTrackLength();
                    time = String.format("%02d:%02d", len / 60, len % 60);
                    Tag t = af.getTag();
                    if (t != null) {
                        String tt = t.getFirst(FieldKey.TITLE);
                        if (!tt.isEmpty()) title = tt;
                    }
                } catch (Exception e) {
                }
                sb.append(String.format("%-50s %s\n", title, time));
            }
        }

        sb.append("\nGenerated by Echo Music Manager at " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));

        try {
            Files.write(target.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            log("专辑资料生成成功: " + target.getAbsolutePath());
        } catch (IOException e) {
            logError("专辑资料生成失败: " + e.getMessage());
        }
    }

    private void updateTrackMeta(ChangeRecord rec) throws Exception {
        File targetFile = new File(rec.getNewPath());
        if (!targetFile.exists()) targetFile = rec.getFileHandle();

        Map<String, String> params = rec.getExtraParams();
        boolean overwrite = "true".equals(params.get("scraper_overwrite"));

        AudioFile audioFile = AudioFileIO.read(targetFile);
        Tag tag = audioFile.getTag();
        if (tag == null) tag = audioFile.createDefaultTag();

        setTag(tag, FieldKey.TITLE, params.get("meta_title"), overwrite);
        setTag(tag, FieldKey.ARTIST, params.get("meta_artist"), overwrite);
        setTag(tag, FieldKey.ALBUM, params.get("meta_album"), overwrite);
        setTag(tag, FieldKey.YEAR, params.get("meta_year"), overwrite);
        setTag(tag, FieldKey.GENRE, params.get("meta_genre"), overwrite);

        if (params.containsKey("meta_lyrics_b64")) {
            String lyric = new String(Base64.getDecoder().decode(params.get("meta_lyrics_b64")), StandardCharsets.UTF_8);
            if (overwrite || tag.getFirst(FieldKey.LYRICS).isEmpty()) tag.setField(FieldKey.LYRICS, lyric);
        }

        if (params.containsKey("meta_cover_url")) {
            if (overwrite || tag.getArtworkList().isEmpty()) {
                byte[] img = downloadBytes(params.get("meta_cover_url"));
                if (img != null) {
                    Artwork artwork = StandardArtwork.createArtworkFromFile(targetFile);
                    artwork.setBinaryData(img);
                    artwork.setMimeType("image/jpeg");
                    tag.deleteArtworkField();
                    tag.setField(artwork);
                }
            }
        }
        audioFile.commit();
        log("元数据更新成功: " + targetFile.getName());
    }

    private void setTag(Tag tag, FieldKey key, String val, boolean overwrite) throws Exception {
        if (val != null && !val.isEmpty()) {
            if (overwrite || tag.getFirst(key).isEmpty()) tag.setField(key, val);
        }
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() == 200) {
            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        }
        return null;
    }

    private byte[] downloadBytes(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                return out.toByteArray();
            }
        } catch (Exception e) {
            logError("下载失败: " + urlStr);
            return null;
        }
    }

    private String extractJsonValue(String json, String key) {
        Pattern p = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
}