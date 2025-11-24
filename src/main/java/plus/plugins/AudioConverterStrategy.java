package plus.plugins; // --- Strategies ---

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
import plus.AppStrategy;
import plus.model.ChangeRecord;
import plus.type.ExecStatus;
import plus.type.OperationType;
import plus.type.ScanTarget;

import java.io.File;
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

    // SSD Cache Controls
    private final CheckBox chkEnableCache;
    private final TextField txtCacheDir;


    // 缓存的参数 (用于 analyze)
    private String pFormat;
    private String pMode;
    private String pRelPath;
    private boolean pSkip;
    private String pFFmpeg;
    private boolean pUseCache;
    private String pCacheDir;
    private int pThreads;

    public AudioConverterStrategy() {
        cbTargetFormat = new JFXComboBox<>(FXCollections.observableArrayList("FLAC", "WAV", "WAV (CD标准)", "MP3"));
        cbTargetFormat.getSelectionModel().select("FLAC");

        cbOutputDirMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "原目录 (Source)",
                "子目录 (Sub-folder)",
                "同级目录 (Sibling folder)",
                "自定义相对路径"
        ));
        cbOutputDirMode.getSelectionModel().select(0);

        txtRelativePath = new TextField("Converted - WAV");
        txtRelativePath.setPromptText("例如: converted 或 ../wav");
        txtRelativePath.visibleProperty().bind(cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录 (Source)"));

        chkSkipExisting = new CheckBox("如果目标文件存在则跳过 (Skip Existing)");
        chkSkipExisting.setSelected(true);

        int cores = Runtime.getRuntime().availableProcessors();
        spThreads = new Spinner<>(1, 64, Math.max(1, cores / 2));
        spThreads.setTooltip(new Tooltip("并行转换的线程数，建议不超过CPU核心数"));

        txtFFmpegPath = new TextField("ffmpeg");

        // SSD Cache init
        chkEnableCache = new CheckBox("启用 SSD 缓存暂存 (解决机械硬盘并发瓶颈)");
        txtCacheDir = new TextField();
        txtCacheDir.setPromptText("选择 SSD 上的临时目录...");
        txtCacheDir.disableProperty().bind(chkEnableCache.selectedProperty().not());
    }

    // 新增：在 UI 线程捕获参数
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
    }

    @Override
    public String getName() {
        return "音频格式转换 (高并发/CD修复/SSD加速)";
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
    public Node getConfigNode() {
        // ... existing config node creation ...
        // (确保这里返回的内容被外层的 ScrollPane 包裹，逻辑在 createActionPanel 已处理)
        VBox box = new VBox(10);
        // ... (UI 构建代码保持不变) ...
        HBox ffmpegBox = new HBox(10, new Label("FFmpeg:"), txtFFmpegPath);
        ffmpegBox.setAlignment(Pos.CENTER_LEFT);
        JFXButton btnPick = new JFXButton("浏览");
        btnPick.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(null);
            if (f != null) txtFFmpegPath.setText(f.getAbsolutePath());
        });
        ffmpegBox.getChildren().add(btnPick);

        HBox cacheBox = new HBox(10, txtCacheDir);
        HBox.setHgrow(txtCacheDir, Priority.ALWAYS);
        JFXButton btnPickCache = new JFXButton("选择SSD目录");
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
                new Label("性能优化:"),
                new HBox(15, new Label("并发线程数:"), spThreads),
                chkEnableCache,
                cacheBox,
                new Separator(),
                new Label("其他:"),
                chkSkipExisting,
                ffmpegBox
        );
        return box;
    }

    // --- 核心优化：analyze 使用 parallelStream 并支持前置筛选 ---
    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        String selectedFormat = pFormat;
        final String[] mode = {pMode};
        String relPath = pRelPath;
        boolean skipExisting = pSkip;
        String ffmpeg = pFFmpeg;
        boolean useCache;
        String cacheDir = pCacheDir;

        if (pUseCache && (cacheDir != null && !cacheDir.trim().isEmpty())) {
            useCache = true;
        } else {
            useCache = false;
        }

        String extension;
        boolean isCdMode;
        if ("WAV (CD标准)".equals(selectedFormat)) {
            extension = "wav";
            isCdMode = true;
        } else {
            isCdMode = false;
            extension = selectedFormat != null ? selectedFormat.toLowerCase() : "flac";
        }

        Set<String> sourceExts = new HashSet<>(Arrays.asList("dsf", "dff", "dts", "ape", "wav", "flac", "m4a", "iso", "dfd"));

        int total = files.size();
        AtomicInteger processed = new AtomicInteger(0);

        // 1. 使用 parallelStream 极速并发处理检查逻辑
        return files.parallelStream().filter(f -> {
            // 2. 前置筛选：只处理支持的扩展名，减少无效的 IO 检查
            String name = f.getName().toLowerCase();
            int dotIndex = name.lastIndexOf(".");
            if (dotIndex == -1) return false;
            String fileExt = name.substring(dotIndex + 1);
            if (!sourceExts.contains(fileExt)) return false;

            // 如果源格式==目标格式 且 输出到原目录，无事可做，直接过滤
            return !fileExt.equals(extension) || mode[0] == null || !mode[0].startsWith("原目录") || isCdMode;
        }).map(f -> {
            // 3. 进度汇报：每 100 个更新一次，避免 UI 洪流
            int curr = processed.incrementAndGet();
            if (progressReporter != null && curr % 100 == 0) {
                double p = (double) curr / total;
                Platform.runLater(() -> progressReporter.accept(p, "正在分析: " + curr + "/" + total));
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
            else {
                assert relPath != null;
                targetFile = new File(new File(parent, relPath), newName);
            }

            // 4. 耗时点：File.exists() IO 操作，现在是并行的
            ExecStatus status = ExecStatus.PENDING;
            if (skipExisting && targetFile.exists()) status = ExecStatus.SKIPPED;

            Map<String, String> params = new HashMap<>();
            params.put("format", extension);
            params.put("ffmpegPath", ffmpeg);
            if (useCache && status != ExecStatus.SKIPPED) {
                String tempFileName = UUID.randomUUID() + "_" + newName;
                File stagingFile = new File(cacheDir, tempFileName);
                params.put("stagingPath", stagingFile.getAbsolutePath());
            }
            if (isCdMode) {
                params.put("codec", "pcm_s16le");
                params.put("sample_rate", "44100");
                params.put("channels", "2");
            } else {
                switch (extension) {
                    case "mp3":
                        params.put("codec", "libmp3lame");
                        break;
                    case "flac":
                        params.put("codec", "flac");
                        break;
                    case "wav":
                        params.put("codec", "pcm_s24le");
                        break;
                }
            }
            return new ChangeRecord(f.getName(), newName, f, true, targetFile.getAbsolutePath(), OperationType.CONVERT, params, status);
        }).collect(Collectors.toList());
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
        return Collections.emptyList();
    }

    // 修改：接受 Properties 而不是 Preferences
    public void savePrefs(Properties props) {
        if (cbTargetFormat.getValue() != null) props.setProperty("ac_targetFormat", cbTargetFormat.getValue());
        if (cbOutputDirMode.getValue() != null) props.setProperty("ac_outputMode", cbOutputDirMode.getValue());
        if (txtFFmpegPath.getText() != null) props.setProperty("ac_ffmpegPath", txtFFmpegPath.getText());
        props.setProperty("ac_useCache", String.valueOf(chkEnableCache.isSelected()));
        if (txtCacheDir.getText() != null) props.setProperty("ac_cacheDir", txtCacheDir.getText());
        props.setProperty("ac_threads", String.valueOf(spThreads.getValue()));
    }

    // 修改：接受 Properties 而不是 Preferences
    public void loadPrefs(Properties props) {
        String fmt = props.getProperty("ac_targetFormat");
        if (fmt != null) cbTargetFormat.getSelectionModel().select(fmt);

        String outMode = props.getProperty("ac_outputMode");
        if (outMode != null) cbOutputDirMode.getSelectionModel().select(outMode);

        txtFFmpegPath.setText(props.getProperty("ac_ffmpegPath", "ffmpeg"));
        chkEnableCache.setSelected(Boolean.parseBoolean(props.getProperty("ac_useCache", "false")));
        txtCacheDir.setText(props.getProperty("ac_cacheDir", ""));

        String threads = props.getProperty("ac_threads");
        if (threads != null) {
            try {
                spThreads.getValueFactory().setValue(Integer.parseInt(threads));
            } catch (NumberFormatException ignore) {
            }
        }
    }

}
