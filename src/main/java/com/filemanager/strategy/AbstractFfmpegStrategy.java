package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.display.StyleFactory;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public abstract class AbstractFfmpegStrategy extends AppStrategy {
    // --- UI 组件 ---
    protected final JFXComboBox<String> cbTargetFormat;
    protected final JFXComboBox<String> cbOutputDirMode;
    protected final TextField txtRelativePath;
    // 覆盖控制
    protected final CheckBox chkOverwrite;
    // FFmpeg 参数控制
    protected final Spinner<Integer> spFfmpegThreads;
    protected final JFXComboBox<String> cbSampleRate;
    protected final JFXComboBox<String> cbChannels;
    protected final TextField txtFFmpegPath;
    protected final CheckBox chkEnableCache;
    protected final CheckBox chkEnableTempSuffix;
    protected final TextField txtCacheDir;
    protected final CheckBox chkForceFilenameMeta;


    // --- 运行时参数 ---
    protected String pFormat;
    protected String pMode;
    protected String pRelPath;
    protected boolean pOverwrite;
    protected String pFFmpeg;
    protected boolean pUseCache;
    protected boolean pUseTempSuffix;
    protected String pCacheDir;
    protected boolean pForceMeta;
    protected int pInnerThreads;
    protected String pSampleRate;
    protected String pChannels;

    public AbstractFfmpegStrategy() {
        cbTargetFormat = new JFXComboBox<>(FXCollections.observableArrayList("WAV (CD标准)", "FLAC", "WAV", "MP3", "ALAC", "AAC", "OGG"));
        cbTargetFormat.setTooltip(new Tooltip("WAV CD标准会按照16bit转录音频文件，反之则按照24bit转录，对CD刻录场景的播放会有负面影响。"));
        cbTargetFormat.getSelectionModel().select(0);

        cbOutputDirMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "原目录 (Source)",
                "子目录 (Sub-folder)",
                "相对路径"
        ));
        cbOutputDirMode.getSelectionModel().select(1);

        txtRelativePath = new TextField("");
        updateDefaultPathPrompt("WAV");

        cbSampleRate = new JFXComboBox<>(FXCollections.observableArrayList("保持原样 (Original)", "44100", "48000", "88200", "96000", "192000"));
        cbSampleRate.getSelectionModel().select(1);

        cbChannels = new JFXComboBox<>(FXCollections.observableArrayList("保持原样 (Original)", "1 (Mono)", "2 (Stereo)", "6 (5.1)"));
        cbChannels.getSelectionModel().select(2);

        cbTargetFormat.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateDefaultPathPrompt(newVal);
            invalidatePreview();
            // 如果选中 CD 标准，自动禁用采样率和声道选择，避免误导
            boolean isCD = "WAV (CD标准)".equals(newVal);
            cbSampleRate.setDisable(isCD);
            cbChannels.setDisable(isCD);
        });

        txtRelativePath.visibleProperty().bind(cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录 (Source)"));

        chkOverwrite = new CheckBox("强制覆盖已存在的目标文件");
        chkOverwrite.setSelected(false);
        chkOverwrite.setTooltip(new Tooltip("如果不勾选，遇到已存在的目标文件将跳过处理。"));

        chkForceFilenameMeta = new CheckBox("忽略原始文件标签（强制用文件名重构元数据）");
        chkForceFilenameMeta.setTooltip(new Tooltip("勾选此项可解决严重的乱码问题，完全丢弃源文件信息，仅依据文件名和目录名生成Tag。"));
        chkForceFilenameMeta.setSelected(false);

        spFfmpegThreads = new Spinner<>(1, 16, 4);
        spFfmpegThreads.setEditable(true);

        txtFFmpegPath = new TextField("ffmpeg");
        txtFFmpegPath.setPromptText("Path to ffmpeg executable");

        chkEnableCache = new CheckBox("启用 SSD 缓存暂存(解决IO瓶颈)");
        chkEnableTempSuffix = new CheckBox("启用.temp文件后缀(SSD缓存启用时不生效)");
        chkEnableTempSuffix.disableProperty().bind(chkEnableCache.selectedProperty());
        chkEnableTempSuffix.setSelected(true);

        txtCacheDir = new TextField();
        txtCacheDir.setPromptText("SSD 缓存目录路径");
    }

    public abstract String getDefaultDirPrefix();

    protected void updateDefaultPathPrompt(String format) {
        if (format == null) return;
        String cleanFormat = format.split(" ")[0].toUpperCase();
        txtRelativePath.setPromptText("默认: " + getDefaultDirPrefix() + " - " + cleanFormat);
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Node getConfigNode() {


        // 1. FFmpeg路径
        JFXButton btnPickFFmpeg = StyleFactory.createIconButton("浏览","",()-> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) txtFFmpegPath.setText(f.getAbsolutePath());
        });

        // 2. 缓存设置
        Node lblCache = StyleFactory.createParamLabel("缓存目录:");
        lblCache.disableProperty().bind(chkEnableCache.selectedProperty().not());
        txtCacheDir.disableProperty().bind(chkEnableCache.selectedProperty().not());
        JFXButton btnPickCache = StyleFactory.createActionButton("选择","",()-> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtCacheDir.setText(f.getAbsolutePath());
        });

        btnPickCache.disableProperty().bind(chkEnableCache.selectedProperty().not());
        return StyleFactory.createVBoxPanel(
                StyleFactory.createChapter("输出格式设置"),
                StyleFactory.createParamPairLine("目标格式:", cbTargetFormat),
                StyleFactory.createParamPairLine("输出模式:", StyleFactory.createHBox(cbOutputDirMode,txtRelativePath)),
                StyleFactory.createSeparator(),
                StyleFactory.createChapter("转换参数设置"),
                StyleFactory.createParamPairLine("FFmpeg路径:",txtFFmpegPath,btnPickFFmpeg),
                StyleFactory.createParamPairLine("FFmpeg线程:", spFfmpegThreads),
                StyleFactory.createParamPairLine("采样率(Hz):", cbSampleRate),
                StyleFactory.createParamPairLine("声道数:", cbChannels),
                StyleFactory.createSeparator(),
                StyleFactory.createChapter("文件处理选项"),
                StyleFactory.createVBoxPanel(chkOverwrite, chkForceFilenameMeta, chkEnableCache, chkEnableTempSuffix),
                StyleFactory.createHBox(lblCache, txtCacheDir,btnPickCache)
        );
    }

    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        return Collections.emptyList();
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File source = rec.getFileHandle();
        File target = new File(rec.getNewPath());
        File finalTarget = target;
        File stagingFile = null;

        if (!Boolean.parseBoolean(rec.getExtraParams().get("overwrite")) && target.exists()) {
            return;
        }

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

    @Override
    public void captureParams() {
        pFormat = cbTargetFormat.getValue();
        pMode = cbOutputDirMode.getValue();
        pRelPath = txtRelativePath.getText();
        pOverwrite = chkOverwrite.isSelected();
        pFFmpeg = txtFFmpegPath.getText();
        pUseCache = chkEnableCache.isSelected();
        pUseTempSuffix = chkEnableTempSuffix.isSelected();
        pCacheDir = txtCacheDir.getText();
        pForceMeta = chkForceFilenameMeta.isSelected();
        pInnerThreads = spFfmpegThreads.getValue();
        pSampleRate = cbSampleRate.getValue();
        pChannels = cbChannels.getValue();
    }

    @Override
    public void saveConfig(Properties props) {
        if (cbTargetFormat.getValue() != null) {
            props.setProperty("ac_format", cbTargetFormat.getValue());
        }
        if (cbOutputDirMode.getValue() != null) {
            props.setProperty("ac_outMode", cbOutputDirMode.getValue());
        }
        if (txtRelativePath.getText() != null) {
            props.setProperty("ac_pRelPath ", txtRelativePath.getText());
        }
        if (txtFFmpegPath.getText() != null) {
            props.setProperty("ac_ffmpeg", txtFFmpegPath.getText());
        }
        props.setProperty("ac_useCache", String.valueOf(chkEnableCache.isSelected()));
        props.setProperty("ac_useTempSuffix", String.valueOf(chkEnableTempSuffix.isSelected()));
        if (txtCacheDir.getText() != null) {
            props.setProperty("ac_cacheDir", txtCacheDir.getText());
        }
        props.setProperty("ac_forceMeta", String.valueOf(chkForceFilenameMeta.isSelected()));
        props.setProperty("ac_overwrite", String.valueOf(chkOverwrite.isSelected()));
        props.setProperty("ac_innerThreads", String.valueOf(spFfmpegThreads.getValue()));
        props.setProperty("ac_sampleRate", cbSampleRate.getValue());
        props.setProperty("ac_channels", cbChannels.getValue());
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("ac_format")) {
            cbTargetFormat.getSelectionModel().select(props.getProperty("ac_format"));
        }
        if (props.containsKey("ac_outMode")) {
            cbOutputDirMode.getSelectionModel().select(props.getProperty("ac_outMode"));
        }
        if (props.containsKey("ac_pRelPath ")) {
            txtRelativePath.setText(props.getProperty("ac_pRelPath "));
        }
        if (props.containsKey("ac_ffmpeg")) {
            txtFFmpegPath.setText(props.getProperty("ac_ffmpeg"));
        }
        if (props.containsKey("ac_useCache")) {
            chkEnableCache.setSelected(Boolean.parseBoolean(props.getProperty("ac_useCache")));
        }
        if (props.containsKey("ac_useTempSuffix")) {
            chkEnableTempSuffix.setSelected(Boolean.parseBoolean(props.getProperty("ac_useTempSuffix")));
        }
        if (props.containsKey("ac_cacheDir")) {
            txtCacheDir.setText(props.getProperty("ac_cacheDir"));
        }
        if (props.containsKey("ac_forceMeta")) {
            chkForceFilenameMeta.setSelected(Boolean.parseBoolean(props.getProperty("ac_forceMeta")));
        }
        if (props.containsKey("ac_overwrite")) {
            chkOverwrite.setSelected(Boolean.parseBoolean(props.getProperty("ac_overwrite")));
        }
        if (props.containsKey("ac_innerThreads")) {
            try {
                spFfmpegThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("ac_innerThreads")));
            } catch (Exception e) {
            }
        }
        if (props.containsKey("ac_sampleRate")) {
            cbSampleRate.getSelectionModel().select(props.getProperty("ac_sampleRate"));
        }
        if (props.containsKey("ac_channels")) {
            cbChannels.getSelectionModel().select(props.getProperty("ac_channels"));
        }
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public String getDescription() {
        return "";
    }

    protected void convertAudioFile(File source, File target, Map<String, String> params) throws IOException {
        String ffmpegPath = params.getOrDefault("ffmpegPath", "ffmpeg");
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
        boolean forceMeta = "true".equals(params.get("forceMeta"));

        try {
            runFFmpegJob(ffmpeg, source, target, params, !forceMeta);
        } catch (IOException e) {
            if (!forceMeta) {
                try {
                    if (target.exists()) target.delete();
                    runFFmpegJob(ffmpeg, source, target, params, false);
                } catch (IOException retryEx) {
                    throw new IOException("重试失败: " + retryEx.getMessage(), retryEx);
                }
            } else {
                throw e;
            }
        }
    }

    protected void runFFmpegJob(FFmpeg ffmpeg, File source, File target, Map<String, String> params, boolean mapMetadata) throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .overrideOutputFiles(true);

        if (params.containsKey("innerThreads")) {
            builder = builder.addExtraArgs("-threads", params.get("innerThreads"));
        }

        FFmpegOutputBuilder outputBuilder = builder.addOutput(target.getAbsolutePath())
                .setFormat(params.getOrDefault("format", "flac"))
                .setAudioCodec(params.getOrDefault("codec", "flac"));
        if (params.containsKey("start")) {
            long startMillis = Long.parseLong(params.get("start"));
            outputBuilder = outputBuilder.setStartOffset(startMillis, TimeUnit.MILLISECONDS);
        }
        if (params.containsKey("duration")) {
            long durationMillis = Long.parseLong(params.get("duration"));
            outputBuilder = outputBuilder.setDuration(durationMillis, TimeUnit.MILLISECONDS);
        }
        outputBuilder = outputBuilder.addExtraArgs("-map", "0:a:0");

        if (mapMetadata) {
            outputBuilder = outputBuilder.addExtraArgs("-map_metadata", "0");
            if (target.getName().toLowerCase().endsWith(".mp3")) {
                outputBuilder = outputBuilder.addExtraArgs("-id3v2_version", "3");
            }
        } else {
            outputBuilder = outputBuilder.addExtraArgs("-map_metadata", "-1");
            MetadataHelper.AudioMeta meta = MetadataHelper.getSmartMetadata(source, true);
            if (!meta.getTitle().isEmpty()) outputBuilder = outputBuilder.addMetaTag("title", meta.getTitle());
            if (!meta.getArtist().isEmpty()) outputBuilder = outputBuilder.addMetaTag("artist", meta.getArtist());
            if (!meta.getAlbum().isEmpty()) outputBuilder = outputBuilder.addMetaTag("album", meta.getAlbum());
            if (!meta.getYear().isEmpty()) outputBuilder = outputBuilder.addMetaTag("date", meta.getYear());
            if (!meta.getTrack().isEmpty()) outputBuilder = outputBuilder.addMetaTag("track", meta.getTrack());
            if (params.containsKey("meta_title"))
                outputBuilder = outputBuilder.addMetaTag("title", params.get("meta_title"));
            if (params.containsKey("meta_artist"))
                outputBuilder = outputBuilder.addMetaTag("artist", params.get("meta_artist"));
            if (params.containsKey("meta_album"))
                outputBuilder = outputBuilder.addMetaTag("album", params.get("meta_album"));
            if (params.containsKey("meta_track"))
                outputBuilder = outputBuilder.addMetaTag("track", params.get("meta_track"));
            if (params.containsKey("meta_genre"))
                outputBuilder = outputBuilder.addMetaTag("genre", params.get("meta_genre"));
            if (params.containsKey("meta_date"))
                outputBuilder = outputBuilder.addMetaTag("date", params.get("meta_date"));
            outputBuilder = outputBuilder.addMetaTag("comment", "Processed by Echo Music Manager");
        }

        // 统一参数读取：params 中的 key 已经是 sampleRate 和 channels
        outputBuilder = outputBuilder.setAudioSampleRate(44100);
        if (params.containsKey("sampleRate")) {
            try {
                outputBuilder = outputBuilder.setAudioSampleRate(Integer.parseInt(params.get("sampleRate")));
            } catch (NumberFormatException ignored) {
            }
        }
        outputBuilder = outputBuilder.setAudioChannels(2);
        if (params.containsKey("channels")) {
            try {
                outputBuilder = outputBuilder.setAudioChannels(Integer.parseInt(params.get("channels")));
            } catch (NumberFormatException ignored) {
            }
        }
        log("▶ 执行ffmpeg命令： " + StringUtils.join(outputBuilder.done().build(), " "));
        new FFmpegExecutor(ffmpeg).createJob(outputBuilder.done()).run();
    }


    protected Map<String, String> getParams(File parentDir, String tempName) {
        Map<String, String> params = new HashMap<>();
        // [核心优化] 默认目录名逻辑
        if (pRelPath == null || pRelPath.trim().isEmpty()) {
            String cleanFormat = pFormat != null ? pFormat.split(" ")[0].toUpperCase() : "UNK";
            pRelPath = getDefaultDirPrefix() + " - " + cleanFormat;
        }
        if (pUseCache && (pCacheDir == null || pCacheDir.trim().isEmpty() || !new File(pCacheDir).isDirectory())) {
            pUseCache = false;
        }
        if (pMode == null) pMode = "原目录";
        String parentPath = null;
        if (pMode.startsWith("原目录")) {
            parentPath = parentDir.getAbsolutePath();
        } else if (pMode.startsWith("子目录")) {
            parentPath = new File(parentDir, pRelPath).getAbsolutePath();
        } else if (pMode.startsWith("相对路径")) {
            parentPath = parentDir.toPath().resolve(new File(pRelPath).toPath()).toString();
        } else {
            parentPath = parentDir.getAbsolutePath();
        }
        params.put("parentPath", parentPath);

        // [核心优化] 确定最终参数：如果是 CD 模式，强制覆盖 UI 选项
        final boolean isCdMode = "WAV (CD标准)".equals(pFormat);
        final String extension = isCdMode ? "wav" : (pFormat != null ? pFormat.toLowerCase() : "flac");

        // 预处理音频参数字符串
        final String targetSampleRate;
        final String targetChannels;

        if (isCdMode) {
            targetSampleRate = "44100";
            targetChannels = "2";
        } else {
            // 非 CD 模式：从 UI 读取
            targetSampleRate = (pSampleRate != null && !pSampleRate.contains("Original")) ? pSampleRate.split(" ")[0] : null;
            String ch = null;
            if (pChannels != null && !pChannels.contains("Original")) {
                if (pChannels.startsWith("1")) ch = "1";
                else if (pChannels.startsWith("2")) ch = "2";
                else if (pChannels.startsWith("6")) ch = "6";
            }
            targetChannels = ch;
        }

        params.put("format", extension);
        params.put("ffmpegPath", pFFmpeg);
        params.put("forceMeta", String.valueOf(pForceMeta));
        params.put("overwrite", String.valueOf(pOverwrite));
        params.put("innerThreads", String.valueOf(pInnerThreads));

        // 放入计算好的最终音频参数
        if (targetSampleRate != null) params.put("sampleRate", targetSampleRate);
        if (targetChannels != null) params.put("channels", targetChannels);

        // 注意：wav 格式下 codec 默认值问题。
        // 如果是 CD 模式，runFFmpegJob 会被 isCdMode 逻辑控制吗？
        // 现在的 runFFmpegJob 逻辑是通用的，它只看 params。
        // 我们需要在 analyze 阶段把 CD 模式的特殊 codec 也写死。
        if (isCdMode) {
            params.put("codec", "pcm_s16le");
        } else if ("wav".equals(extension)) {
            params.put("codec", "pcm_s24le");
        } else if ("flac".equals(extension)) {
            params.put("codec", "flac");
        } else if ("aac".equals(extension)) {
            params.put("codec", "aac");
        } else if ("mp3".equals(extension)) {
            params.put("codec", "libmp3lame");
        }

        if (pUseCache) {
            String tempFileName = UUID.randomUUID().toString();
            File stagingFile = new File(pCacheDir, tempFileName);
            params.put("stagingPath", stagingFile.getAbsolutePath());
        } else if (pUseTempSuffix) {
            params.put("stagingPath", new File(parentPath, tempName + ".temp").getAbsolutePath());
        }
        return params;
    }

}
