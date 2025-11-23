package plus.plugins; // --- Strategies ---

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import plus.*;
import plus.model.ChangeRecord;
import plus.type.ExecStatus;
import plus.type.OperationType;
import plus.type.ScanTarget;

import java.io.File;
import java.util.*;
import java.util.prefs.Preferences;

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

        txtRelativePath = new TextField("converted");
        txtRelativePath.setPromptText("例如: converted 或 ../wav");
        txtRelativePath.visibleProperty().bind(cbOutputDirMode.getSelectionModel().selectedItemProperty().isNotEqualTo("原目录 (Source)"));

        chkSkipExisting = new CheckBox("如果目标文件存在则跳过 (Skip Existing)");
        chkSkipExisting.setSelected(true);

        int cores = Runtime.getRuntime().availableProcessors();
        spThreads = new Spinner<>(1, 32, Math.max(1, cores / 2));
        spThreads.setTooltip(new Tooltip("并行转换的线程数，建议不超过CPU核心数"));

        txtFFmpegPath = new TextField("ffmpeg");

        // SSD Cache init
        chkEnableCache = new CheckBox("启用 SSD 缓存暂存 (解决机械硬盘并发瓶颈)");
        txtCacheDir = new TextField();
        txtCacheDir.setPromptText("选择 SSD 上的临时目录...");
        txtCacheDir.disableProperty().bind(chkEnableCache.selectedProperty().not());
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


    @Override public Node getConfigNode() {
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
            if(f!=null) txtFFmpegPath.setText(f.getAbsolutePath());
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

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
        List<ChangeRecord> records = new ArrayList<>();
        String selectedFormat = cbTargetFormat.getValue();
        String mode = cbOutputDirMode.getValue();
        String relPath = txtRelativePath.getText();
        boolean skipExisting = chkSkipExisting.isSelected();
        String ffmpeg = txtFFmpegPath.getText();

        boolean useCache = chkEnableCache.isSelected();
        String cacheDir = txtCacheDir.getText();

        // 简单校验缓存设置
        if (useCache && (cacheDir == null || cacheDir.trim().isEmpty())) {
            useCache = false; // 回退
        }

        String extension;
        boolean isCdMode = false;

        if ("WAV (CD标准)".equals(selectedFormat)) {
            extension = "wav";
            isCdMode = true;
        } else {
            extension = selectedFormat.toLowerCase();
        }

        Set<String> sourceExts = new HashSet<>(Arrays.asList("dsf", "dff", "dts", "ape", "wav", "flac", "m4a"));

        for (File f : files) {
            String name = f.getName().toLowerCase();
            String fileExt = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : "";

            if (!sourceExts.contains(fileExt)) continue;
            if (fileExt.equals(extension) && mode.startsWith("原目录") && !isCdMode) continue;

            String newName = f.getName().substring(0, f.getName().lastIndexOf(".")) + "." + extension;

            File parent = f.getParentFile();
            File targetFile = null;

            if (mode.startsWith("原目录")) {
                targetFile = new File(parent, newName);
            } else if (mode.startsWith("子目录")) {
                targetFile = new File(new File(parent, relPath.isEmpty() ? "converted" : relPath), newName);
            } else if (mode.startsWith("同级目录")) {
                targetFile = new File(new File(parent.getParentFile(), relPath.isEmpty() ? parent.getName() + "_" + extension : relPath), newName);
            } else {
                targetFile = new File(new File(parent, relPath), newName);
            }

            ExecStatus status = ExecStatus.PENDING;
            if (skipExisting && targetFile.exists()) {
                status = ExecStatus.SKIPPED;
            }

            Map<String, String> params = new HashMap<>();
            params.put("format", extension);
            params.put("ffmpegPath", ffmpeg);

            // 设置暂存路径
            if (useCache && status != ExecStatus.SKIPPED) {
                // 使用 UUID 避免文件名冲突，保持平铺结构以最大化写入性能
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
            }

            ChangeRecord rec = new ChangeRecord(f.getName(), newName, f, true, targetFile.getAbsolutePath(), OperationType.CONVERT, params);
            rec.setStatus(status);
            records.add(rec);
        }
        return records;
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
            } catch (NumberFormatException ignore) {}
        }
    }

}
