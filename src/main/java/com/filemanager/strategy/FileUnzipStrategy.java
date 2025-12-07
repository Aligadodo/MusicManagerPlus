package com.filemanager.strategy;

import com.filemanager.model.*;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 批量智能解压策略 (v3.1 - 修复同名文件夹上浮问题)
 * 优化：移除耗时的解压前探测，改为“先解压到隔离目录，再智能判断是否移出”
 */
public class FileUnzipStrategy extends AppStrategy {

    // --- UI Components ---
    private final JFXComboBox<String> cbEngine;
    private final TextField txtExePath;
    private final JFXComboBox<String> cbOutputMode;
    private final TextField txtCustomPath;
    private final CheckBox chkSmartFolder;
    private final CheckBox chkDeleteSource;
    private final CheckBox chkOverwrite;
    private final Spinner<Integer> spThreads;
    private final TextField txtPassword;

    // --- Runtime Params ---
    private String pEngine;
    private String pExePath;
    private String pMode;
    private String pCustomPath;
    private boolean pSmart;
    private boolean pDelete;
    private boolean pOverwrite;
    private int pThreads;
    private String pPassword;

    public FileUnzipStrategy() {
        cbEngine = new JFXComboBox<>(FXCollections.observableArrayList(
                "内置引擎 (Java Commons Compress)",
                "外部程序 (7-Zip / Bandizip 命令行)"
        ));
        cbEngine.getSelectionModel().select(0);
        cbEngine.setTooltip(new Tooltip("内置引擎兼容性有限(不支持新版RAR/加密7Z)。\n推荐安装 7-Zip 并选择外部程序模式以获得最佳兼容性。"));

        txtExePath = new TextField();
        txtExePath.setPromptText("7z.exe 或 bandizip.exe 的路径");
        txtExePath.visibleProperty().bind(cbEngine.getSelectionModel().selectedItemProperty().isNotEqualTo("内置引擎 (Java Commons Compress)"));

        cbOutputMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "当前目录 (Current Dir)",
                "指定目录 (Custom Path)",
                "同级新建文件夹 (Sibling Folder)"
        ));
        cbOutputMode.getSelectionModel().select(0);

        txtCustomPath = new TextField("Unzipped");
        txtCustomPath.setPromptText("目标文件夹路径");
        txtCustomPath.visibleProperty().bind(cbOutputMode.getSelectionModel().selectedItemProperty().isEqualTo("指定目录 (Custom Path)"));

        chkSmartFolder = new CheckBox("智能目录 (Smart Folder)");
        chkSmartFolder.setSelected(true);
        chkSmartFolder.setTooltip(new Tooltip("始终先在独立文件夹解压，若解压后发现只有单目录则自动移出。\n防止“解压炸弹”弄乱目录。"));

        chkDeleteSource = new CheckBox("解压成功后删除源文件");
        chkDeleteSource.setSelected(false);
        chkDeleteSource.setStyle("-fx-text-fill: #e74c3c;");

        chkOverwrite = new CheckBox("覆盖已存在文件");
        chkOverwrite.setSelected(false);

        txtPassword = new TextField();
        txtPassword.setPromptText("密码 (如需要)");

        spThreads = new Spinner<>(1, 16, 2);
        spThreads.setEditable(true);

        autoDetect7Zip();
    }

    private void autoDetect7Zip() {
        String[] paths = {
                "C:\\Program Files\\7-Zip\\7z.exe",
                "C:\\Program Files (x86)\\7-Zip\\7z.exe"
        };
        for (String p : paths) {
            if (new File(p).exists()) {
                txtExePath.setText(p);
                break;
            }
        }
    }

    @Override
    public String getName() {
        return "批量智能解压 (Unzip/Unrar)";
    }

    @Override
    public String getDescription() {
        return "支持多种压缩格式。采用后置智能目录优化，解压速度更快且安全。";
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
        VBox box = new VBox(10);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        col1.setHgrow(Priority.NEVER);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        grid.add(new Label("解压引擎:"), 0, 0);
        HBox exeBox = new HBox(5, cbEngine, txtExePath);
        HBox.setHgrow(cbEngine, Priority.ALWAYS);
        HBox.setHgrow(txtExePath, Priority.ALWAYS);
        JFXButton btnExePick = new JFXButton("浏览");
        btnExePick.visibleProperty().bind(txtExePath.visibleProperty());
        btnExePick.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("选择解压程序");
            File f = fc.showOpenDialog(null);
            if (f != null) txtExePath.setText(f.getAbsolutePath());
        });
        exeBox.getChildren().add(btnExePick);
        grid.add(exeBox, 1, 0);

        grid.add(new Label("解压位置:"), 0, 1);
        HBox pathBox = new HBox(5, cbOutputMode, txtCustomPath);
        HBox.setHgrow(cbOutputMode, Priority.ALWAYS);
        HBox.setHgrow(txtCustomPath, Priority.ALWAYS);
        JFXButton btnPathPick = new JFXButton("...");
        btnPathPick.visibleProperty().bind(txtCustomPath.visibleProperty());
        btnPathPick.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtCustomPath.setText(f.getAbsolutePath());
        });
        pathBox.getChildren().add(btnPathPick);
        grid.add(pathBox, 1, 1);

        grid.add(new Label("并发线程:"), 0, 2);
        grid.add(spThreads, 1, 2);
        grid.add(new Label("解压密码:"), 0, 3);
        grid.add(txtPassword, 1, 3);

        box.getChildren().addAll(grid, new Separator(), chkSmartFolder, chkOverwrite, new Separator(), chkDeleteSource);
        return box;
    }

    @Override
    public void captureParams() {
        pEngine = cbEngine.getValue();
        pExePath = txtExePath.getText();
        pMode = cbOutputMode.getValue();
        pCustomPath = txtCustomPath.getText();
        pSmart = chkSmartFolder.isSelected();
        pDelete = chkDeleteSource.isSelected();
        pOverwrite = chkOverwrite.isSelected();
        pThreads = spThreads.getValue();
        pPassword = txtPassword.getText();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("zip_engine", pEngine);
        props.setProperty("zip_exe", pExePath);
        props.setProperty("zip_mode", pMode);
        props.setProperty("zip_path", pCustomPath);
        props.setProperty("zip_smart", String.valueOf(pSmart));
        props.setProperty("zip_del", String.valueOf(pDelete));
        props.setProperty("zip_over", String.valueOf(pOverwrite));
        props.setProperty("zip_threads", String.valueOf(pThreads));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("zip_engine")) cbEngine.getSelectionModel().select(props.getProperty("zip_engine"));
        if (props.containsKey("zip_exe")) txtExePath.setText(props.getProperty("zip_exe"));
        if (props.containsKey("zip_mode")) cbOutputMode.getSelectionModel().select(props.getProperty("zip_mode"));
        if (props.containsKey("zip_path")) txtCustomPath.setText(props.getProperty("zip_path"));
        if (props.containsKey("zip_smart"))
            chkSmartFolder.setSelected(Boolean.parseBoolean(props.getProperty("zip_smart")));
        if (props.containsKey("zip_del"))
            chkDeleteSource.setSelected(Boolean.parseBoolean(props.getProperty("zip_del")));
        if (props.containsKey("zip_over"))
            chkOverwrite.setSelected(Boolean.parseBoolean(props.getProperty("zip_over")));
        if (props.containsKey("zip_threads")) {
            try {
                spThreads.getValueFactory().setValue(Integer.parseInt(props.getProperty("zip_threads")));
            } catch (Exception e) {
            }
        }
    }

    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        List<ChangeRecord> results = new ArrayList<>();
        Set<String> archiveExts = new HashSet<>(Arrays.asList("zip", "7z", "rar", "tar", "gz", "jar", "xz", "bz2", "iso"));

        int total = inputRecords.size();
        AtomicInteger processed = new AtomicInteger(0);

        return inputRecords.parallelStream().map(rec -> {
            int curr = processed.incrementAndGet();
            if (progressReporter != null && curr % 20 == 0) {
                Platform.runLater(() -> progressReporter.accept((double) curr / total, "分析压缩包: " + curr + "/" + total));
            }

            File file = rec.getFileHandle();
            String name = file.getName().toLowerCase();
            int dot = name.lastIndexOf('.');
            if (dot == -1) return rec;
            String ext = name.substring(dot + 1);

            if (!archiveExts.contains(ext)) return rec;

            // 1. 计算基础输出目录 (Base Dest)
            File baseDestDir;
            if (pMode.startsWith("当前目录")) {
                baseDestDir = file.getParentFile();
            } else if (pMode.startsWith("指定目录")) {
                baseDestDir = new File(pCustomPath);
            } else {
                baseDestDir = new File(file.getParentFile(), "Extracted_" + file.getName());
            }

            // 2. 预览展示逻辑
            // 如果开启智能模式，我们会先解压到 wrapper，然后可能移出。
            // 预览阶段无法预知解压后结构，因此展示最保守的路径（Wrapper路径），并在日志中标注"智能"
            File previewDest = pSmart ? new File(baseDestDir, getBaseName(file.getName())) : baseDestDir;

            String displayName = (pEngine.contains("外部") ? "[外部] " : "[内置] ") +
                    (pSmart ? "智能解压 -> " : "解压 -> ") + previewDest.getName();

            Map<String, String> params = new HashMap<>();
            params.put("baseDest", baseDestDir.getAbsolutePath()); // 基础输出路径
            params.put("engine", pEngine);
            params.put("exePath", pExePath);
            params.put("smart", String.valueOf(pSmart));
            params.put("overwrite", String.valueOf(pOverwrite));
            params.put("delete", String.valueOf(pDelete));
            params.put("password", pPassword);

            return new ChangeRecord(rec.getOriginalName(), displayName, file, true, previewDest.getAbsolutePath(), OperationType.UNZIP, params, ExecStatus.PENDING);

        }).collect(Collectors.toList());
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (!rec.getExtraParams().containsKey("baseDest")) return;

        File archiveFile = rec.getFileHandle();
        String baseDestPath = rec.getExtraParams().get("baseDest");
        String engine = rec.getExtraParams().get("engine");
        boolean smart = Boolean.parseBoolean(rec.getExtraParams().get("smart"));
        boolean deleteSource = Boolean.parseBoolean(rec.getExtraParams().get("delete"));
        boolean overwrite = Boolean.parseBoolean(rec.getExtraParams().get("overwrite"));

        File baseDestDir = new File(baseDestPath);
        if (!baseDestDir.exists()) baseDestDir.mkdirs();

        // 1. 确定实际解压路径
        // 如果开启智能模式，强制解压到一个以压缩包名命名的临时目录 (Wrapper Dir)
        File extractRoot;
        if (smart) {
            String wrapperName = getBaseName(archiveFile.getName());
            extractRoot = new File(baseDestDir, wrapperName);
            // 防止重名冲突 (如果是 overwrite 模式，可能合并不清，所以 smart 模式下建议先清空或使用唯一名)
            if (!extractRoot.exists()) extractRoot.mkdirs();
        } else {
            extractRoot = baseDestDir;
        }

        // 2. 执行解压
        if (engine.contains("外部")) {
            extractWithExternalTool(archiveFile, extractRoot, rec.getExtraParams());
        } else {
            extractWithJava(archiveFile, extractRoot, overwrite);
        }

        // 3. 后置智能处理 (Smart Folder Optimization)
        if (smart) {
            optimizeSmartFolder(extractRoot, baseDestDir);
        }

        // 4. 删除源文件
        if (deleteSource) {
            try {
                Files.delete(archiveFile.toPath());
            } catch (IOException e) { /* log warn */ }
        }
    }

    /**
     * 智能目录优化核心逻辑：
     * 检查解压后的 wrapper 目录，如果里面仅包含**一个**文件夹（且没有其他文件），
     * 则将该文件夹移动到上一层，并删除空的 wrapper。
     * [修复] 增加对同名嵌套文件夹 (Archive/Archive/) 的处理
     */
    private void optimizeSmartFolder(File wrapperDir, File parentDir) {
        if (wrapperDir == null || !wrapperDir.exists() || !wrapperDir.isDirectory()) return;

        File[] files = wrapperDir.listFiles();
        List<File> validFiles = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.getName().equals(".DS_Store") || f.getName().equalsIgnoreCase("Thumbs.db")) continue;
                validFiles.add(f);
            }
        }

        // 判定条件：只有一个条目，且该条目是文件夹
        if (validFiles.size() == 1 && validFiles.get(0).isDirectory()) {
            File singleInnerDir = validFiles.get(0);
            File targetDir = new File(parentDir, singleInnerDir.getName());

            try {
                // 情况 1: 压缩包名和内部文件夹名一致 (例如 Archive/Archive/)
                // 此时 targetDir (Parent/Archive) == wrapperDir (Parent/Archive)
                // 我们需要把 Archive/Archive/* 的内容上移一层到 Archive/
                if (targetDir.equals(wrapperDir)) {
                    File[] innerFiles = singleInnerDir.listFiles();
                    if (innerFiles != null) {
                        for (File innerFile : innerFiles) {
                            File dest = new File(wrapperDir, innerFile.getName());
                            // 移动子文件到 wrapper 层，覆盖模式
                            Files.move(innerFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    // 删除空的内部 Archive 文件夹
                    Files.delete(singleInnerDir.toPath());
                }
                // 情况 2: 名字不同 (例如 Archive/Data/ -> Data/)
                // 仅当目标位置没有同名文件夹时移动，避免合并冲突
                else if (!targetDir.exists()) {
                    Files.move(singleInnerDir.toPath(), targetDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    // 移动成功后，Wrapper 应该是空的，删除 Wrapper
                    deleteDirectoryRecursively(wrapperDir);
                }
            } catch (IOException e) {
                System.err.println("Smart move failed: " + e.getMessage());
            }
        }
    }

    private void deleteDirectoryRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) for (File entry : entries) deleteDirectoryRecursively(entry);
        }
        Files.delete(file.toPath());
    }

    // --- 解压引擎 ---

    private void extractWithExternalTool(File archive, File destDir, Map<String, String> params) throws Exception {
        String exePath = params.get("exePath");
        String pwd = params.get("password");
        boolean overwrite = Boolean.parseBoolean(params.get("overwrite"));

        if (exePath == null || exePath.isEmpty() || !new File(exePath).exists()) {
            throw new IOException("未找到解压程序: " + exePath);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(exePath);
        cmd.add("x");
        cmd.add(archive.getAbsolutePath());
        cmd.add("-o" + destDir.getAbsolutePath());

        if (pwd != null && !pwd.isEmpty()) cmd.add("-p" + pwd);
        cmd.add(overwrite ? "-aoa" : "-aos"); // Overwrite All / Skip
        cmd.add("-y"); // Yes

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // 消耗输出流防止阻塞
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
            while (reader.readLine() != null) {
            }
        }

        int exitCode = p.waitFor();
        if (exitCode != 0 && exitCode != 1) { // 7z exit code 1 is warning (non-fatal)
            throw new IOException("外部程序异常退出码: " + exitCode);
        }
    }

    private void extractWithJava(File archive, File destDir, boolean overwrite) throws Exception {
        String lowerName = archive.getName().toLowerCase();

        if (lowerName.endsWith(".7z")) {
            extract7z(archive, destDir, overwrite);
            return;
        }

        if (lowerName.endsWith(".rar")) {
            try (InputStream fi = Files.newInputStream(archive.toPath());
                 ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("rar", fi)) {
            } catch (Exception e) {
                throw new IOException("内置引擎不支持此 RAR (可能是 RAR5)，请切换到 '外部程序' 模式。");
            }
        }

        try (InputStream fi = Files.newInputStream(archive.toPath());
             InputStream bi = new BufferedInputStream(fi);
             ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(bi)) {

            ArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!in.canReadEntryData(entry)) continue;
                File target = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!target.isDirectory() && !target.mkdirs()) throw new IOException("无法创建目录: " + target);
                } else {
                    File parent = target.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("无法创建父目录: " + parent);
                    if (target.exists() && !overwrite) continue;
                    try (OutputStream o = Files.newOutputStream(target.toPath())) {
                        IOUtils.copy(in, o);
                    }
                }
            }
        }
    }

    private void extract7z(File archive, File destDir, boolean overwrite) throws Exception {
        try (SevenZFile sevenZFile = new SevenZFile(archive)) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                File target = new File(destDir, entry.getName());
                File parent = target.getParentFile();
                if (!parent.exists()) parent.mkdirs();
                if (target.exists() && !overwrite) continue;
                try (FileOutputStream out = new FileOutputStream(target)) {
                    byte[] content = new byte[(int) entry.getSize()];
                    sevenZFile.read(content, 0, content.length);
                    out.write(content);
                }
            }
        }
    }

    private String getBaseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}