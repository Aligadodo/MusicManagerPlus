package com.filemanager.strategy;

import com.filemanager.tool.display.StyleFactory;
import com.filemanager.model.*;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 自动刮削器策略 (v3.0 - 专辑增强版)
 * 功能：
 * 1. 基础元数据刮削 (Tag)
 * 2. 歌词下载
 * 3. 专辑层级处理：下载封面文件(cover.jpg)、生成专辑简介(Info.txt)、生成播放清单
 */
public class MetadataScraperStrategy extends AppStrategy {

    // --- UI Components ---
    private final JFXComboBox<String> cbSource;

    // Track Level
    private final CheckBox chkUpdateBasicMeta;
    private final CheckBox chkFetchLyrics;

    // Album Level (New)
    private final CheckBox chkSaveCoverFile; // 保存 cover.jpg
    private final CheckBox chkSaveAlbumInfo; // 保存 AlbumInfo.txt (含简介+曲目)
    private final CheckBox chkScrapeIntro;   // 是否尝试从网络刮削简介文本

    private final CheckBox chkOverwrite;
    private final Spinner<Integer> spThreads;
    private final TextArea txtPreviewLog;
    // --- Services ---
    private final LyricsManager lyricsManager;
    // 用于在 analyze 阶段记录已处理的专辑目录，防止重复生成专辑级任务
    private final Set<String> processedAlbumDirs = Collections.synchronizedSet(new HashSet<>());
    // --- Runtime Params ---
    private String pSource;
    private boolean pUpdateBasic;
    private boolean pFetchLyrics;
    private boolean pSaveCoverFile;
    private boolean pSaveAlbumInfo;
    private boolean pScrapeIntro;
    private boolean pOverwrite;
    private int pThreads;

    public MetadataScraperStrategy() {
        cbSource = new JFXComboBox<>(FXCollections.observableArrayList(
                "iTunes Music API (稳定推荐)",
                "本地推断 (仅生成清单)"
        ));
        cbSource.getSelectionModel().select(0);

        chkUpdateBasicMeta = new CheckBox("更新单曲元数据 (标题/歌手/专辑/内嵌封面)");
        chkUpdateBasicMeta.setSelected(true);

        chkFetchLyrics = new CheckBox("下载歌词 (内嵌到音频文件)");
        chkFetchLyrics.setSelected(true);

        // 新增专辑级选项
        chkSaveCoverFile = new CheckBox("保存专辑封面文件 (cover.jpg 到目录)");
        chkSaveCoverFile.setSelected(true);

        chkSaveAlbumInfo = new CheckBox("生成专辑资料 (AlbumInfo.txt - 简介+曲目)");
        chkSaveAlbumInfo.setSelected(true);

        chkScrapeIntro = new CheckBox("尝试刮削网络简介/版权信息");
        chkScrapeIntro.setSelected(true);
        chkScrapeIntro.disableProperty().bind(chkSaveAlbumInfo.selectedProperty().not());

        chkOverwrite = new CheckBox("强制覆盖已有信息/文件");
        chkOverwrite.setSelected(false);

        spThreads = new Spinner<>(1, 8, 2);

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
        return "音频元数据自动刮削（未完成）";
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

        checks.getChildren().addAll(
                l1, chkUpdateBasicMeta, chkFetchLyrics,
                new Separator(),
                l2, chkSaveCoverFile, chkSaveAlbumInfo, chkScrapeIntro,
                new Separator(),
                chkOverwrite
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
        if (props.containsKey("meta_threads")) {
            try {
                spThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("meta_threads")));
            } catch (Exception e) {
            }
        }
    }

    // --- 核心逻辑：分析阶段 ---
    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        processedAlbumDirs.clear(); // 清除目录缓存
        AtomicInteger matched = new AtomicInteger(0);
        Set<String> supportedExts = new HashSet<>(Arrays.asList("mp3", "flac", "m4a", "ogg", "wav", "dsf", "dff", "ape"));

        // 我们需要收集每个目录下的所有音频文件，以便后续生成 AlbumInfo
        // 但 analyze 是并行流，无法简单聚合。
        // 策略：先进行单曲处理，同时利用 Set 锁判定目录是否是第一次遇到。如果是第一次，生成专辑任务。

        File file = rec.getFileHandle();
        File parentDir = file.getParentFile();
        String name = file.getName().toLowerCase();
        String ext = name.lastIndexOf(".") > 0 ? name.substring(name.lastIndexOf(".") + 1) : "";
        if (!supportedExts.contains(ext)) {
            return Collections.emptyList();
        }
        List<ChangeRecord> results = new ArrayList<>();
        // === 1. 单曲元数据处理 ===
        MetadataHelper.AudioMeta guess = MetadataHelper.extractFromFileSystem(file);
        ScrapedResult scraperRes = null;
        boolean metaChanged = false;

        if (pUpdateBasic || pFetchLyrics) {
            // 尝试读取现有
            try {
                AudioFile f = AudioFileIO.read(file);
                Tag tag = f.getTag();

                // 搜索逻辑
                if (pSource.contains("iTunes")) {
                    // 仅当需要更新Tag或封面时搜索
                    if (pUpdateBasic && (pOverwrite || tag == null || tag.getFirst(FieldKey.ALBUM).isEmpty())) {
                        scraperRes = searchITunes(guess.getArtist(), guess.getTitle(), false);
                    }
                }

                // 构建变更参数
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

                // 歌词
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
                    matched.incrementAndGet();
                }
            } catch (Exception e) {
            }
        }

        // === 2. 专辑层级处理 (封面文件 & Info.txt) ===
        // 利用 synchronizedSet 原子性地检查目录是否已处理
        String dirPath = parentDir.getAbsolutePath();
        boolean isFirstVisit = processedAlbumDirs.add(dirPath);

        if (isFirstVisit && (pSaveCoverFile || pSaveAlbumInfo)) {
            // 为了生成 Info.txt，我们需要该目录下所有歌曲的信息。
            // 由于当前是并行流处理单个文件，我们无法拿到"同目录其他文件"的 Record。
            // 变通：在 Execute 阶段再去扫描该目录生成内容。Analyze 阶段只生成一个“任务标记”。

            // 搜索专辑信息 (针对整个专辑)
            // 注意：这里为了性能，只搜一次专辑信息，而不是每首歌都搜
            ScrapedResult albumRes = null;
            if (pSource.contains("iTunes") && (pSaveCoverFile || (pSaveAlbumInfo && pScrapeIntro))) {
                // 用目录名或当前文件的专辑名搜
                String searchAlbum = guess.getAlbum();
                if (searchAlbum != null && !searchAlbum.isEmpty() && !searchAlbum.equals("Unknown Album")) {
                    albumRes = searchITunes(guess.getArtist(), searchAlbum, true); // true = search album entity
                }
            }

            if (pSaveCoverFile) {
                String coverUrl = (albumRes != null) ? albumRes.coverUrl : (scraperRes != null ? scraperRes.coverUrl : null);
                // 只有找到了 URL 才生成任务，或者我们生成一个"检查本地提取"的任务？
                // 简化：只处理网络下载
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
                        // 即使没搜到，也可以生成基于文件列表的 Info
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

    // --- 执行子任务 ---

    private void downloadCoverFile(ChangeRecord rec) {
        try {
            String url = rec.getExtraParams().get("url");
            if (url == null) return;
            byte[] data = downloadBytes(url);
            if (data != null && data.length > 0) {
                Files.write(new File(rec.getNewPath()).toPath(), data);
            }
        } catch (Exception e) {
            // log
        }
    }

    private void generateAlbumInfo(ChangeRecord rec) {
        File dir = rec.getFileHandle(); // This is the parent dir
        File target = new File(rec.getNewPath());
        Map<String, String> p = rec.getExtraParams();

        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("专辑名称: ").append(p.getOrDefault("album", "Unknown")).append("\n");
        sb.append("艺术家  : ").append(p.getOrDefault("artist", "Unknown")).append("\n");
        if (p.containsKey("year")) sb.append("发行年份: ").append(p.get("year")).append("\n");
        if (p.containsKey("genre")) sb.append("流派    : ").append(p.get("genre")).append("\n");
        sb.append("==================================================\n\n");

        if (p.containsKey("intro") && !p.get("intro").isEmpty()) {
            sb.append("[ 专辑简介 ]\n");
            sb.append(p.get("intro")).append("\n\n");
        }

        sb.append("[ 曲目列表 ]\n");

        // 扫描目录下的音频文件并排序
        File[] files = dir.listFiles();
        if (files != null) {
            List<File> audios = Arrays.stream(files)
                    .filter(f -> f.getName().matches(".*\\.(mp3|flac|wav|m4a|ape|dsf|dff)$"))
                    .sorted(Comparator.comparing(File::getName))
                    .collect(Collectors.toList());

            for (File f : audios) {
                // 尝试读取时长和标题
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
                        // 如果文件名有序号，title没有，可以尝试组合
                    }
                } catch (Exception e) {

                }
                sb.append(String.format("%-50s %s\n", title, time));
            }
        }

        sb.append("\nGenerated by Echo Music Manager at ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));

        try {
            Files.write(target.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logError("音频信息刮削失败，源文件："+rec.getFileHandle().getAbsolutePath());
        }
    }

    private void updateTrackMeta(ChangeRecord rec) throws Exception {
        // 同之前的实现：写入 ID3/Vorbis 标签
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
    }

    private void setTag(Tag tag, FieldKey key, String val, boolean overwrite) throws Exception {
        if (val != null && !val.isEmpty()) {
            if (overwrite || tag.getFirst(key).isEmpty()) tag.setField(key, val);
        }
    }

    // --- 网络与辅助 ---

    private ScrapedResult searchITunes(String artist, String titleOrAlbum, boolean isAlbumSearch) {
        String term = artist + " " + titleOrAlbum;
        String entity = isAlbumSearch ? "album" : "song";
        String urlStr = null;
        try {
            String json = httpGet(urlStr);
            if (json == null || !json.contains("resultCount")) return null;
            urlStr = "https://itunes.apple.com/search?term=" + URLEncoder.encode(term, "UTF-8") + "&media=music&entity=" + entity + "&limit=1";
            ScrapedResult res = new ScrapedResult();
            res.artist = extractJsonValue(json, "artistName");
            res.album = extractJsonValue(json, "collectionName"); // Album Name
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
            logError("音频信息刮削失败，地址："+urlStr);
            return null;
        }
    }

    // ... (httpGet, downloadBytes, extractJsonValue, Lyrics Providers 同前，保持不变) ...
    private String httpGet(String urlStr) throws Exception{
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
            logError("音频信息刮削失败，地址："+urlStr);
            return null;
        }
    }

    private String extractJsonValue(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    interface LyricsProvider {
        String search(String a, String t, int d);
    }

    private static class ScrapedResult {
        String title, artist, album, year, genre, coverUrl, intro;
    }

    class LyricsManager {
        List<LyricsProvider> ps = new ArrayList<>();

        void register(LyricsProvider p) {
            ps.add(p);
        }

        String searchLyrics(String a, String t, int d) {
            for (LyricsProvider p : ps) {
                String r = p.search(a, t, d);
                if (r != null) return r;
            }
            return null;
        }
    }

    class NeteaseLyricsProvider implements LyricsProvider {
        public String search(String a, String t, int d) {
            return null; /* Mock for brevity */
        }
    }

    class MiguLyricsProvider implements LyricsProvider {
        public String search(String a, String t, int d) {
            return null; /* Mock for brevity */
        }
    }
}