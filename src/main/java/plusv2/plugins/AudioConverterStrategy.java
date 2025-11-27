package plusv2.plugins; // --- Strategies ---

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import plusv2.AppStrategy;
import plusv2.model.AudioMeta;
import plusv2.model.ChangeRecord;
import plusv2.type.ExecStatus;
import plusv2.type.OperationType;
import plusv2.type.ScanTarget;
import plusv2.util.MetadataHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class AudioConverterStrategy extends AppStrategy {
    private final JFXComboBox<String> cbTargetFormat;
    private final JFXComboBox<String> cbOutputDirMode;
    private final TextField txtRelativePath;
    private final CheckBox chkSkipExisting;
    private final Spinner<Integer> spThreads;
    private final TextField txtFFmpegPath;
    private final CheckBox chkEnableCache;
    private final TextField txtCacheDir;
    private final CheckBox chkForceFilenameMeta;

    private String pFormat;
    private String pMode;
    private String pRelPath;
    private boolean pSkip;
    private String pFFmpeg;
    private boolean pUseCache;
    private String pCacheDir;
    private int pThreads;
    private boolean pForceMeta;

    public AudioConverterStrategy() {
        cbTargetFormat = new JFXComboBox<>(FXCollections.observableArrayList("FLAC", "WAV", "WAV (CD标准)", "MP3", "ALAC", "AAC"));
        cbTargetFormat.getSelectionModel().select("FLAC");
        cbTargetFormat.getSelectionModel().selectedItemProperty().addListener((o, old, v) -> invalidatePreview());

        cbOutputDirMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "原目录 (Source)",
                "子目录 (Sub-folder)",
                "同级目录 (Sibling folder)",
                "自定义相对路径"
        ));
        cbOutputDirMode.getSelectionModel().select(0);
        cbOutputDirMode.getSelectionModel().selectedItemProperty().addListener((o, old, v) -> invalidatePreview());

        txtRelativePath = new TextField("converted");
        txtRelativePath.setPromptText("路径名");
        txtRelativePath.textProperty().addListener((o, old, v) -> invalidatePreview());
        txtRelativePath.visibleProperty().bind(cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录 (Source)"));

        chkSkipExisting = new CheckBox("如果目标文件存在则跳过 (Skip Existing)");
        chkSkipExisting.setSelected(true);
        chkSkipExisting.selectedProperty().addListener((o, old, v) -> invalidatePreview());

        chkForceFilenameMeta = new CheckBox("忽略原文件 Tag，强制使用文件名重构元数据");
        chkForceFilenameMeta.setTooltip(new Tooltip("默认情况下程序会自动检测并修复乱码。\n勾选此项将完全丢弃源文件内的信息，仅依据文件名和目录名生成标题、歌手等信息。"));
        chkForceFilenameMeta.setSelected(false);
        chkForceFilenameMeta.selectedProperty().addListener((o, old, v) -> invalidatePreview());

        spThreads = new Spinner<>(1, 64, 4);
        spThreads.setEditable(true);

        txtFFmpegPath = new TextField("ffmpeg");
        txtFFmpegPath.setPromptText("Path to ffmpeg executable");

        chkEnableCache = new CheckBox("启用 SSD 缓存暂存");
        txtCacheDir = new TextField();
        txtCacheDir.setPromptText("SSD 缓存目录路径");
        chkEnableCache.selectedProperty().addListener((o, old, v) -> invalidatePreview());
        txtCacheDir.textProperty().addListener((o, old, v) -> invalidatePreview());
    }

    @Override
    public String getName() {
        return "音频格式转换 (智能修复乱码)";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public int getPreferredThreadCount() {
        return spThreads.getValue();
    }

    @Override
    public void captureParams() {
        pFormat = cbTargetFormat.getValue();
        pMode = cbOutputDirMode.getValue();
        pRelPath = txtRelativePath.getText();
        pSkip = chkSkipExisting.isSelected();
        pFFmpeg = txtFFmpegPath.getText();
        pUseCache = chkEnableCache.isSelected();
        pCacheDir = txtCacheDir.getText();
        pThreads = spThreads.getValue();
        pForceMeta = chkForceFilenameMeta.isSelected();
    }

    @Override
    public void saveConfig(Properties props) {
        if (cbTargetFormat.getValue() != null) props.setProperty("ac_format", cbTargetFormat.getValue());
        if (cbOutputDirMode.getValue() != null) props.setProperty("ac_outMode", cbOutputDirMode.getValue());
        if (txtFFmpegPath.getText() != null) props.setProperty("ac_ffmpeg", txtFFmpegPath.getText());
        props.setProperty("ac_useCache", String.valueOf(chkEnableCache.isSelected()));
        if (txtCacheDir.getText() != null) props.setProperty("ac_cacheDir", txtCacheDir.getText());
        props.setProperty("ac_threads", String.valueOf(spThreads.getValue()));
        props.setProperty("ac_forceMeta", String.valueOf(chkForceFilenameMeta.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("ac_format")) cbTargetFormat.getSelectionModel().select(props.getProperty("ac_format"));
        if (props.containsKey("ac_outMode"))
            cbOutputDirMode.getSelectionModel().select(props.getProperty("ac_outMode"));
        if (props.containsKey("ac_ffmpeg")) txtFFmpegPath.setText(props.getProperty("ac_ffmpeg"));
        if (props.containsKey("ac_useCache"))
            chkEnableCache.setSelected(Boolean.parseBoolean(props.getProperty("ac_useCache")));
        if (props.containsKey("ac_cacheDir")) txtCacheDir.setText(props.getProperty("ac_cacheDir"));
        if (props.containsKey("ac_threads")) {
            try {
                spThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("ac_threads")));
            } catch (Exception e) {
            }
        }
        if (props.containsKey("ac_forceMeta"))
            chkForceFilenameMeta.setSelected(Boolean.parseBoolean(props.getProperty("ac_forceMeta")));
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        HBox ffmpegBox = new HBox(10, new Label("FFmpeg路径:"), txtFFmpegPath);
        ffmpegBox.setAlignment(Pos.CENTER_LEFT);
        JFXButton btnPick = new JFXButton("浏览");
        btnPick.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) txtFFmpegPath.setText(f.getAbsolutePath());
        });
        ffmpegBox.getChildren().add(btnPick);
        HBox.setHgrow(txtFFmpegPath, Priority.ALWAYS);

        HBox cacheBox = new HBox(10, txtCacheDir);
        HBox.setHgrow(txtCacheDir, Priority.ALWAYS);
        JFXButton btnPickCache = new JFXButton("选择目录");
        btnPickCache.disableProperty().bind(chkEnableCache.selectedProperty().not());
        btnPickCache.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtCacheDir.setText(f.getAbsolutePath());
        });
        cacheBox.getChildren().add(btnPickCache);

        box.getChildren().addAll(
                new Label("目标格式:"), cbTargetFormat,
                new Label("输出位置:"), new HBox(10, cbOutputDirMode, txtRelativePath),
                new Separator(),
                new Label("元数据处理:"), chkForceFilenameMeta,
                new Separator(),
                new Label("性能与IO优化:"),
                new HBox(15, new Label("并发线程:"), spThreads),
                chkEnableCache, cacheBox,
                new Separator(),
                new Label("其他设置:"), chkSkipExisting, ffmpegBox
        );
        return box;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.CONVERT) return;

        File source = rec.getFileHandle();
        File target = new File(rec.getNewPath());
        File finalTarget = target;
        File stagingFile = null;

        if (rec.getExtraParams().containsKey("stagingPath")) {
            stagingFile = new File(rec.getExtraParams().get("stagingPath"));
            if (!stagingFile.getParentFile().exists()) stagingFile.getParentFile().mkdirs();
            target = stagingFile;
        } else {
            if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
        }

        convertAudioFile(source, target, rec.getExtraParams());

        if (stagingFile != null && stagingFile.exists()) {
            if (!finalTarget.getParentFile().exists()) finalTarget.getParentFile().mkdirs();
            Files.move(stagingFile.toPath(), finalTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void convertAudioFile(File source, File target, Map<String, String> params) throws IOException {
        String ffmpegPath = params.getOrDefault("ffmpegPath", "ffmpeg");
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        boolean forceMeta = "true".equals(params.get("forceMeta"));

        AudioMeta smartMeta = MetadataHelper.getSmartMetadata(source, forceMeta);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .overrideOutputFiles(true);

        FFmpegOutputBuilder outputBuilder = builder.addOutput(target.getAbsolutePath())
                .setFormat(params.getOrDefault("format", "flac"))
                .setAudioCodec(params.getOrDefault("codec", "flac"));

        // [修复] 将 -map 0:a:0 移至 outputBuilder 的 extraArgs
        // 这确保了它出现在 output file 参数之前，修复 "applied to input url" 错误
        outputBuilder.addExtraArgs("-map", "0:a:0");

        outputBuilder.addExtraArgs("-map_metadata", "-1");

        if (!smartMeta.title.isEmpty()) outputBuilder.addMetaTag("title", smartMeta.title);
        if (!smartMeta.artist.isEmpty()) outputBuilder.addMetaTag("artist", smartMeta.artist);
        if (!smartMeta.album.isEmpty()) outputBuilder.addMetaTag("album", smartMeta.album);
        if (!smartMeta.year.isEmpty()) outputBuilder.addMetaTag("date", smartMeta.year);
        if (!smartMeta.track.isEmpty()) outputBuilder.addMetaTag("track", smartMeta.track);
        outputBuilder.addMetaTag("comment", "Processed by Echo Music Manager");

        if (params.containsKey("sample_rate")) {
            outputBuilder.setAudioSampleRate(Integer.parseInt(params.get("sample_rate")));
        }
        if (params.containsKey("channels")) {
            outputBuilder.setAudioChannels(Integer.parseInt(params.get("channels")));
        }

        new FFmpegExecutor(ffmpeg).createJob(builder).run();
    }

    // 重试机制使用的旧方法，也需要修复
    private void runFFmpegJob(FFmpeg ffmpeg, File source, File target, Map<String, String> params, boolean mapMetadata) throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .overrideOutputFiles(true);

        FFmpegOutputBuilder outputBuilder = builder.addOutput(target.getAbsolutePath())
                .setFormat(params.getOrDefault("format", "flac"))
                .setAudioCodec(params.getOrDefault("codec", "flac"));

        // [修复] 移至 outputBuilder
        outputBuilder.addExtraArgs("-map", "0:a:0");

        if (mapMetadata) {
            outputBuilder.addExtraArgs("-map_metadata", "0");
            if (target.getName().toLowerCase().endsWith(".mp3")) {
                outputBuilder.addExtraArgs("-id3v2_version", "3");
                outputBuilder.addExtraArgs("-write_id3v1", "1");
            }
        } else {
            outputBuilder.addExtraArgs("-map_metadata", "-1");
            AudioMeta meta = MetadataHelper.extractFromFileSystem(source);
            if (!"Unknown Title".equals(meta.title)) outputBuilder.addMetaTag("title", meta.title);
            if (!"Unknown Artist".equals(meta.artist)) outputBuilder.addMetaTag("artist", meta.artist);
            // ... (simplified for brevity as this is fallback)
        }

        if (params.containsKey("sample_rate")) {
            outputBuilder.setAudioSampleRate(Integer.parseInt(params.get("sample_rate")));
        }
        if (params.containsKey("channels")) {
            outputBuilder.setAudioChannels(Integer.parseInt(params.get("channels")));
        }

        new FFmpegExecutor(ffmpeg).createJob(builder).run();
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        // ... analyze implementation (unchanged from previous optimal version) ...
        List<ChangeRecord> records = new ArrayList<>();
        String selectedFormat = pFormat;
        final String[] mode = {pMode};
        String relPath = pRelPath;
        boolean skipExisting = pSkip;
        String ffmpeg = pFFmpeg;
        boolean useCache = pUseCache;
        String cacheDir = pCacheDir;
        boolean forceMeta = pForceMeta;
        if (useCache && (cacheDir == null || cacheDir.trim().isEmpty() || !new File(cacheDir).isDirectory()))
            useCache = false;
        String extension;
        boolean isCdMode;
        if ("WAV (CD标准)".equals(selectedFormat)) {
            extension = "wav";
            isCdMode = true;
        } else {
            isCdMode = false;
            extension = selectedFormat != null ? selectedFormat.toLowerCase() : "flac";
        }
        Set<String> sourceExts = new HashSet<>(Arrays.asList("dsf", "dff", "dts", "ape", "wav", "flac", "m4a", "iso", "dfd", "tak", "tta", "wv"));
        int total = files.size();
        AtomicInteger processed = new AtomicInteger(0);
        boolean finalUseCache = useCache;
        return files.parallelStream().filter(f -> {
            String name = f.getName().toLowerCase();
            int dotIndex = name.lastIndexOf(".");
            if (dotIndex == -1) return false;
            String fileExt = name.substring(dotIndex + 1);
            if (!sourceExts.contains(fileExt)) return false;
            return !fileExt.equals(extension) || mode[0] == null || !mode[0].startsWith("原目录") || isCdMode || forceMeta;
        }).map(f -> {
            int curr = processed.incrementAndGet();
            if (progressReporter != null && curr % 50 == 0) {
                double p = (double) curr / total;
                Platform.runLater(() -> progressReporter.accept(p, "分析文件: " + curr + "/" + total));
            }
            String newName = f.getName().substring(0, f.getName().lastIndexOf(".")) + "." + extension;
            File parent = f.getParentFile();
            File targetFile = null;
            if (mode[0] == null) mode[0] = "原目录";
            if (mode[0].startsWith("原目录")) targetFile = new File(parent, newName);
            else if (mode[0].startsWith("子目录"))
                targetFile = new File(new File(parent, relPath == null || relPath.isEmpty() ? "converted" : relPath), newName);
            else if (mode[0].startsWith("同级目录"))
                targetFile = new File(new File(parent.getParentFile(), relPath == null || relPath.isEmpty() ? parent.getName() + "_" + extension : relPath), newName);
            else targetFile = new File(new File(parent, relPath), newName);
            ExecStatus status = ExecStatus.PENDING;
            if (skipExisting && targetFile.exists()) status = ExecStatus.SKIPPED;
            Map<String, String> params = new HashMap<>();
            params.put("format", extension);
            params.put("ffmpegPath", ffmpeg);
            params.put("forceMeta", String.valueOf(forceMeta));
            if (finalUseCache && status != ExecStatus.SKIPPED) {
                String tempFileName = UUID.randomUUID() + "_" + newName;
                File stagingFile = new File(cacheDir, tempFileName);
                params.put("stagingPath", stagingFile.getAbsolutePath());
            }
            if (isCdMode) {
                params.put("codec", "pcm_s16le");
                params.put("sample_rate", "44100");
                params.put("channels", "2");
            } else {
                if ("mp3".equals(extension)) params.put("codec", "libmp3lame");
                else if ("flac".equals(extension)) params.put("codec", "flac");
                else if ("wav".equals(extension)) params.put("codec", "pcm_s24le");
                else if ("alac".equals(extension)) params.put("codec", "alac");
                else if ("aac".equals(extension)) params.put("codec", "aac");
            }
            return new ChangeRecord(f.getName(), newName, f, true, targetFile.getAbsolutePath(), OperationType.CONVERT, params, status);
        }).collect(Collectors.toList());
    }
}
