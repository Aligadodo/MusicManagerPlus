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
import com.filemanager.strategy.scraper.LyricsManager;
import com.filemanager.strategy.scraper.LyricsProvider;
import com.filemanager.strategy.scraper.MiguLyricsProvider;
import com.filemanager.strategy.scraper.NeteaseLyricsProvider;
import com.filemanager.strategy.scraper.ScrapedResult;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
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
    private final CheckBox chkUpdateBasicMeta;
    private final CheckBox chkFetchLyrics;
    private final CheckBox chkSaveCoverFile;
    private final CheckBox chkSaveAlbumInfo;
    private final CheckBox chkScrapeIntro;
    private final CheckBox chkOverwrite;
    private final CheckBox chkUseCache;
    private final Spinner<Integer> spThreads;
    private final TextArea txtPreviewLog;
    
    private final LyricsManager lyricsManager;
    private final Set<String> processedAlbumDirs = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, ScrapedResult> metadataCache = new ConcurrentHashMap<>();
    
    protected String pSource;
    protected boolean pUpdateBasic;
    protected boolean pFetchLyrics;
    protected boolean pSaveCoverFile;
    protected boolean pSaveAlbumInfo;
    protected boolean pScrapeIntro;
    protected boolean pOverwrite;
    protected boolean pUseCache;
    protected int pThreads;

    public MetadataScraperStrategy() {
        cbSource = new JFXComboBox<>(FXCollections.observableArrayList(
                "iTunes Music API (稳定推荐)",
                "MusicBrainz (专业数据库)",
                "Last.fm (社区驱动)",
                "网易云音乐 (中文歌曲)",
                "咪咕音乐 (版权歌曲)",
                "本地推断 (仅生成清单)"
        ));
        cbSource.getSelectionModel().select(0);
        
        ArrayList<String> sourceTooltipLines = new ArrayList<>();
        sourceTooltipLines.add("参数名称：数据源");
        sourceTooltipLines.add("参数用途：用于设置元数据刮削的数据源");
        sourceTooltipLines.add("数据源说明：");
        sourceTooltipLines.add("- iTunes Music API：稳定推荐，数据准确");
        sourceTooltipLines.add("- MusicBrainz：专业音乐数据库，数据全面");
        sourceTooltipLines.add("- Last.fm：社区驱动，数据丰富");
        sourceTooltipLines.add("- 网易云音乐：中文歌曲覆盖广");
        sourceTooltipLines.add("- 咪咕音乐：版权歌曲多，音质好");
        sourceTooltipLines.add("- 本地推断：仅基于文件名生成清单");
        FloatingTooltip.bindToNode(cbSource, "元数据刮削设置", sourceTooltipLines);

        chkUpdateBasicMeta = new CheckBox("更新单曲元数据 (标题/歌手/专辑/内嵌封面)");
        chkUpdateBasicMeta.setSelected(true);
        
        ArrayList<String> basicMetaTooltipLines = new ArrayList<>();
        basicMetaTooltipLines.add("参数名称：更新单曲元数据");
        basicMetaTooltipLines.add("参数用途：用于更新单曲的基本元数据信息");
        basicMetaTooltipLines.add("包含字段：");
        basicMetaTooltipLines.add("- 标题（Title）：歌曲名称");
        basicMetaTooltipLines.add("- 歌手（Artist）：艺术家名称");
        basicMetaTooltipLines.add("- 专辑（Album）：专辑名称");
        basicMetaTooltipLines.add("- 年份（Year）：发行年份");
        basicMetaTooltipLines.add("- 流派（Genre）：音乐流派");
        basicMetaTooltipLines.add("- 内嵌封面：将封面图片嵌入音频文件");
        FloatingTooltip.bindToNode(chkUpdateBasicMeta, "元数据刮削设置", basicMetaTooltipLines);

        chkFetchLyrics = new CheckBox("下载歌词 (内嵌到音频文件)");
        chkFetchLyrics.setSelected(true);
        
        ArrayList<String> lyricsTooltipLines = new ArrayList<>();
        lyricsTooltipLines.add("参数名称：下载歌词");
        lyricsTooltipLines.add("参数用途：用于下载歌词并内嵌到音频文件中");
        lyricsTooltipLines.add("支持来源：");
        lyricsTooltipLines.add("- 网易云音乐");
        lyricsTooltipLines.add("- 咪咕音乐");
        lyricsTooltipLines.add("歌词格式：LRC格式，包含时间轴");
        FloatingTooltip.bindToNode(chkFetchLyrics, "元数据刮削设置", lyricsTooltipLines);

        chkSaveCoverFile = new CheckBox("保存专辑封面文件 (cover.jpg 到目录)");
        chkSaveCoverFile.setSelected(true);
        
        ArrayList<String> coverFileTooltipLines = new ArrayList<>();
        coverFileTooltipLines.add("参数名称：保存专辑封面文件");
        coverFileTooltipLines.add("参数用途：用于保存专辑封面文件到目录中");
        coverFileTooltipLines.add("文件规格：");
        coverFileTooltipLines.add("- 格式：JPEG");
        coverFileTooltipLines.add("- 尺寸：600x600（推荐）");
        coverFileTooltipLines.add("- 文件名：cover.jpg");
        FloatingTooltip.bindToNode(chkSaveCoverFile, "元数据刮削设置", coverFileTooltipLines);

        chkSaveAlbumInfo = new CheckBox("生成专辑资料 (AlbumInfo.txt - 简介+曲目)");
        chkSaveAlbumInfo.setSelected(true);
        
        ArrayList<String> albumInfoTooltipLines = new ArrayList<>();
        albumInfoTooltipLines.add("参数名称：生成专辑资料");
        albumInfoTooltipLines.add("参数用途：用于生成专辑资料文件");
        albumInfoTooltipLines.add("文件内容：");
        albumInfoTooltipLines.add("- 专辑基本信息（名称、艺术家、年份、流派）");
        albumInfoTooltipLines.add("- 专辑简介（如果可用）");
        albumInfoTooltipLines.add("- 曲目列表（包含时长）");
        albumInfoTooltipLines.add("- 文件名：AlbumInfo.txt");
        FloatingTooltip.bindToNode(chkSaveAlbumInfo, "元数据刮削设置", albumInfoTooltipLines);

        chkScrapeIntro = new CheckBox("尝试刮削网络简介/版权信息");
        chkScrapeIntro.setSelected(true);
        chkScrapeIntro.disableProperty().bind(chkSaveAlbumInfo.selectedProperty().not());
        
        ArrayList<String> introTooltipLines = new ArrayList<>();
        introTooltipLines.add("参数名称：尝试刮削网络简介");
        introTooltipLines.add("参数用途：用于尝试从网络刮削专辑简介和版权信息");
        introTooltipLines.add("数据来源：");
        introTooltipLines.add("- iTunes：版权信息");
        introTooltipLines.add("- MusicBrainz：专辑简介");
        introTooltipLines.add("- Last.fm：用户评论和简介");
        FloatingTooltip.bindToNode(chkScrapeIntro, "元数据刮削设置", introTooltipLines);

        chkOverwrite = new CheckBox("强制覆盖已有信息/文件");
        chkOverwrite.setSelected(false);
        
        ArrayList<String> overwriteTooltipLines = new ArrayList<>();
        overwriteTooltipLines.add("参数名称：强制覆盖");
        overwriteTooltipLines.add("参数用途：用于设置是否强制覆盖已有信息和文件");
        overwriteTooltipLines.add("覆盖策略：");
        overwriteTooltipLines.add("- 启用：覆盖已有元数据和文件");
        overwriteTooltipLines.add("- 禁用：仅在无现有信息时添加");
        overwriteTooltipLines.add("注意：建议先备份重要文件");
        FloatingTooltip.bindToNode(chkOverwrite, "元数据刮削设置", overwriteTooltipLines);

        chkUseCache = new CheckBox("使用元数据缓存");
        chkUseCache.setSelected(true);
        
        ArrayList<String> useCacheTooltipLines = new ArrayList<>();
        useCacheTooltipLines.add("参数名称：使用元数据缓存");
        useCacheTooltipLines.add("参数用途：缓存已刮削的元数据，避免重复请求");
        useCacheTooltipLines.add("缓存策略：");
        useCacheTooltipLines.add("- 基于艺术家+标题/专辑名称");
        useCacheTooltipLines.add("- 提高处理速度");
        useCacheTooltipLines.add("- 减少网络请求");
        FloatingTooltip.bindToNode(chkUseCache, "元数据刮削设置", useCacheTooltipLines);

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

        txtPreviewLog = new TextArea();
        txtPreviewLog.setPromptText("预览日志区域...");
        txtPreviewLog.setPrefHeight(100);
        txtPreviewLog.setEditable(false);

        lyricsManager = new LyricsManager();
        lyricsManager.register(new NeteaseLyricsProvider());
        lyricsManager.register(new MiguLyricsProvider());
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

        VBox checks = new VBox(8);
        Label l1 = StyleFactory.createDescLabel("单曲处理:");
        Label l2 = StyleFactory.createDescLabel("专辑处理 (每个目录一份):");
        Label l3 = StyleFactory.createDescLabel("高级选项:");

        checks.getChildren().addAll(
                l1, chkUpdateBasicMeta, chkFetchLyrics,
                new Separator(),
                l2, chkSaveCoverFile, chkSaveAlbumInfo, chkScrapeIntro,
                new Separator(),
                l3, chkOverwrite, chkUseCache
        );

        box.getChildren().addAll(grid, new Separator(), checks, StyleFactory.createParamLabel("实时日志:"), txtPreviewLog);
        return box;
    }

    @Override
    public void captureParams() {
        pSource = cbSource.getValue();
        pUpdateBasic = chkUpdateBasicMeta.isSelected();
        pFetchLyrics = chkFetchLyrics.isSelected();
        pSaveCoverFile = chkSaveCoverFile.isSelected();
        pSaveAlbumInfo = chkSaveAlbumInfo.isSelected();
        pScrapeIntro = chkScrapeIntro.isSelected();
        pOverwrite = chkOverwrite.isSelected();
        pUseCache = chkUseCache.isSelected();
        pThreads = spThreads.getValue();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("meta_source", cbSource.getValue());
        props.setProperty("meta_basic", String.valueOf(chkUpdateBasicMeta.isSelected()));
        props.setProperty("meta_lyrics", String.valueOf(chkFetchLyrics.isSelected()));
        props.setProperty("meta_cover_file", String.valueOf(chkSaveCoverFile.isSelected()));
        props.setProperty("meta_info_txt", String.valueOf(chkSaveAlbumInfo.isSelected()));
        props.setProperty("meta_intro", String.valueOf(chkScrapeIntro.isSelected()));
        props.setProperty("meta_overwrite", String.valueOf(chkOverwrite.isSelected()));
        props.setProperty("meta_cache", String.valueOf(chkUseCache.isSelected()));
        props.setProperty("meta_threads", String.valueOf(spThreads.getValue()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("meta_source")) cbSource.getSelectionModel().select(props.getProperty("meta_source"));
        if (props.containsKey("meta_basic"))
            chkUpdateBasicMeta.setSelected(Boolean.parseBoolean(props.getProperty("meta_basic")));
        if (props.containsKey("meta_lyrics"))
            chkFetchLyrics.setSelected(Boolean.parseBoolean(props.getProperty("meta_lyrics")));
        if (props.containsKey("meta_cover_file"))
            chkSaveCoverFile.setSelected(Boolean.parseBoolean(props.getProperty("meta_cover_file")));
        if (props.containsKey("meta_info_txt"))
            chkSaveAlbumInfo.setSelected(Boolean.parseBoolean(props.getProperty("meta_info_txt")));
        if (props.containsKey("meta_intro"))
            chkScrapeIntro.setSelected(Boolean.parseBoolean(props.getProperty("meta_intro")));
        if (props.containsKey("meta_overwrite"))
            chkOverwrite.setSelected(Boolean.parseBoolean(props.getProperty("meta_overwrite")));
        if (props.containsKey("meta_cache"))
            chkUseCache.setSelected(Boolean.parseBoolean(props.getProperty("meta_cache")));
        if (props.containsKey("meta_threads")) {
            try {
                spThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("meta_threads")));
            } catch (Exception e) {
            }
        }
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        processedAlbumDirs.clear();
        metadataCache.clear();
        Set<String> supportedExts = new HashSet<>(Arrays.asList("mp3", "flac", "m4a", "ogg", "wav", "dsf", "dff", "ape"));

        File file = rec.getFileHandle();
        File parentDir = file.getParentFile();
        String name = file.getName().toLowerCase();
        String ext = name.lastIndexOf(".") > 0 ? name.substring(name.lastIndexOf(".") + 1) : "";
        if (!supportedExts.contains(ext)) {
            return Collections.emptyList();
        }
        List<ChangeRecord> results = new ArrayList<>();
        
        MetadataHelper.AudioMeta guess = MetadataHelper.extractFromFileSystem(file);
        ScrapedResult scraperRes = null;
        boolean metaChanged = false;

        if (pUpdateBasic || pFetchLyrics) {
            try {
                AudioFile f = AudioFileIO.read(file);
                Tag tag = f.getTag();

                if (pUpdateBasic && (pOverwrite || tag == null || tag.getFirst(FieldKey.ALBUM).isEmpty())) {
                    String cacheKey = guess.getArtist() + "|" + guess.getTitle();
                    if (pUseCache && metadataCache.containsKey(cacheKey)) {
                        scraperRes = metadataCache.get(cacheKey);
                        log("使用缓存数据: " + cacheKey);
                    } else {
                        scraperRes = searchMetadata(guess.getArtist(), guess.getTitle(), false);
                        if (pUseCache && scraperRes != null) {
                            metadataCache.put(cacheKey, scraperRes);
                        }
                    }
                }

                Map<String, String> params = new HashMap<>(rec.getExtraParams());

                if (scraperRes != null && pUpdateBasic) {
                    params.put("meta_title", scraperRes.title);
                    params.put("meta_artist", scraperRes.artist);
                    params.put("meta_album", scraperRes.album);
                    if (scraperRes.year != null) params.put("meta_year", scraperRes.year);
                    if (scraperRes.genre != null) params.put("meta_genre", scraperRes.genre);
                    if (scraperRes.coverUrl != null) params.put("meta_cover_url", scraperRes.coverUrl);
                    metaChanged = true;
                }

                if (pFetchLyrics && (pOverwrite || tag == null || tag.getFirst(FieldKey.LYRICS).isEmpty())) {
                    int duration = f.getAudioHeader().getTrackLength();
                    String lrc = lyricsManager.searchLyrics(guess.getArtist(), guess.getTitle(), duration);
                    if (lrc != null) {
                        params.put("meta_lyrics_b64", Base64.getEncoder().encodeToString(lrc.getBytes(StandardCharsets.UTF_8)));
                        metaChanged = true;
                    }
                }

                if (metaChanged) {
                    rec.setChanged(true);
                    rec.setOpType(OperationType.SCRAPER);
                    rec.getExtraParams().putAll(params);
                    rec.getExtraParams().put("scraper_active", "true");
                    if (pOverwrite) rec.getExtraParams().put("scraper_overwrite", "true");
                    rec.setNewName("[更新] " + file.getName());
                }
            } catch (Exception e) {
                logError("处理文件失败: " + file.getName() + ", 错误: " + e.getMessage());
            }
        }

        String dirPath = parentDir.getAbsolutePath();
        boolean isFirstVisit = processedAlbumDirs.add(dirPath);

        if (isFirstVisit && (pSaveCoverFile || pSaveAlbumInfo)) {
            ScrapedResult albumRes = null;
            if (pSource.contains("iTunes") || pSource.contains("MusicBrainz") || pSource.contains("Last.fm")) {
                String searchAlbum = guess.getAlbum();
                if (searchAlbum != null && !searchAlbum.isEmpty() && !searchAlbum.equals("Unknown Album")) {
                    String cacheKey = guess.getArtist() + "|" + searchAlbum + "|album";
                    if (pUseCache && metadataCache.containsKey(cacheKey)) {
                        albumRes = metadataCache.get(cacheKey);
                        log("使用缓存专辑数据: " + cacheKey);
                    } else {
                        albumRes = searchMetadata(guess.getArtist(), searchAlbum, true);
                        if (pUseCache && albumRes != null) {
                            metadataCache.put(cacheKey, albumRes);
                        }
                    }
                }
            }

            if (pSaveCoverFile) {
                String coverUrl = (albumRes != null) ? albumRes.coverUrl : (scraperRes != null ? scraperRes.coverUrl : null);
                if (coverUrl != null) {
                    File targetCover = new File(parentDir, "cover.jpg");
                    if (pOverwrite || !targetCover.exists()) {
                        Map<String, String> p = new HashMap<>();
                        p.put("url", coverUrl);
                        ChangeRecord coverRec = new ChangeRecord("下载: 专辑封面", "cover.jpg", parentDir,
                                true, targetCover.getAbsolutePath(), OperationType.SCRAPER, p, ExecStatus.PENDING);
                        coverRec.getExtraParams().put("task_type", "DOWNLOAD_COVER");
                        results.add(coverRec);
                    }
                }
            }

            if (pSaveAlbumInfo) {
                File targetInfo = new File(parentDir, "AlbumInfo.txt");
                if (pOverwrite || !targetInfo.exists()) {
                    Map<String, String> p = new HashMap<>();
                    if (albumRes != null) {
                        p.put("intro", albumRes.intro != null ? albumRes.intro : "");
                        p.put("album", albumRes.album);
                        p.put("artist", albumRes.artist);
                        p.put("year", albumRes.year);
                        p.put("genre", albumRes.genre);
                    } else {
                        p.put("album", guess.getAlbum());
                        p.put("artist", guess.getArtist());
                    }

                    ChangeRecord infoRec = new ChangeRecord("生成: 专辑资料", "AlbumInfo.txt", parentDir,
                            true, targetInfo.getAbsolutePath(), OperationType.SCRAPER, p, ExecStatus.PENDING);
                    infoRec.getExtraParams().put("task_type", "GENERATE_INFO");
                    results.add(infoRec);
                }
            }
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

    private ScrapedResult searchMetadata(String artist, String titleOrAlbum, boolean isAlbumSearch) {
        String term = artist + " " + titleOrAlbum;
        String entity = isAlbumSearch ? "album" : "song";
        String urlStr;
        try {
            urlStr = "https://itunes.apple.com/search?term=" + URLEncoder.encode(term, "UTF-8") + "&media=music&entity=" + entity + "&limit=1";
        } catch (Exception e) {
            logError("URL编码失败: " + e.getMessage());
            return null;
        }
        
        try {
            String json = httpGet(urlStr);
            if (json == null || !json.contains("resultCount")) return null;
            
            ScrapedResult res = new ScrapedResult();
            res.artist = extractJsonValue(json, "artistName");
            res.album = extractJsonValue(json, "collectionName");
            res.genre = extractJsonValue(json, "primaryGenreName");
            res.title = isAlbumSearch ? null : extractJsonValue(json, "trackName");

            String date = extractJsonValue(json, "releaseDate");
            if (date != null && date.length() >= 4) res.year = date.substring(0, 4);

            String artwork = extractJsonValue(json, "artworkUrl100");
            if (artwork != null) res.coverUrl = artwork.replace("100x100", "600x600");

            if (isAlbumSearch) {
                String copyright = extractJsonValue(json, "copyright");
                if (copyright != null) res.intro = "Copyright: " + copyright;
            }
            return res;
        } catch (Exception e) {
            logError("元数据搜索失败: " + e.getMessage());
            return null;
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
