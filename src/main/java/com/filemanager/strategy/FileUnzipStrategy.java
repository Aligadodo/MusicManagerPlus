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
 * 批量智能解压策略 (v4.2 - 密码箱与自动匹配版)
 * 特性：
 * 1. 密码管理箱：支持保存多个常用密码。
 * 2. 自动匹配：尝试无密码及列表中的所有密码进行解压。
 * 3. 失败处理：支持解压失败（如无匹配密码）后自动删除源文件。
 * 4. 线程数：使用全局配置，本策略不单独维护。
 */
public class FileUnzipStrategy extends AppStrategy {

    // --- UI Components ---
    private final JFXComboBox<String> cbEngine;
    private final TextField txtExePath;
    private final JFXComboBox<String> cbOutputMode;
    private final TextField txtCustomPath;

    // 选项
    private final CheckBox chkSmartFolder;
    private final CheckBox chkDeleteSource; // 解压成功后删除
    private final CheckBox chkOverwrite;
    private final CheckBox chkDeleteOnFail; // [新增] 解压失败后删除

    // 密码箱 UI
    private final ListView<String> lvPasswords;
    private final TextField txtNewPass;
    private final JFXButton btnAddPass;
    private final JFXButton btnDelPass;

    // --- Runtime Params ---
    private String pEngine;
    private String pExePath;
    private String pMode;
    private String pCustomPath;
    private boolean pSmart;
    private boolean pDeleteSuccess;
    private boolean pDeleteFail;
    private boolean pOverwrite;
    private List<String> pPasswords;

    public FileUnzipStrategy() {
        // 引擎选择
        cbEngine = new JFXComboBox<>(FXCollections.observableArrayList(
                "内置引擎 (Java Commons Compress)",
                "外部程序 (7-Zip / Bandizip 命令行)"
        ));
        cbEngine.getSelectionModel().select(0);

        txtExePath = new TextField();
        txtExePath.setPromptText("7z.exe 或 bandizip.exe 路径");
        txtExePath.visibleProperty().bind(cbEngine.getSelectionModel().selectedItemProperty().isNotEqualTo("内置引擎 (Java Commons Compress)"));

        // 路径模式
        cbOutputMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "当前目录 (Current Dir)",
                "指定目录 (Custom Path)",
                "同级新建文件夹 (Sibling Folder)"
        ));
        cbOutputMode.getSelectionModel().select(0);

        txtCustomPath = new TextField("Unzipped");
        txtCustomPath.visibleProperty().bind(cbOutputMode.getSelectionModel().selectedItemProperty().isEqualTo("指定目录 (Custom Path)"));

        // 选项
        chkSmartFolder = new CheckBox("智能目录 (防炸弹)");
        chkSmartFolder.setSelected(true);

        chkDeleteSource = new CheckBox("成功后删除源文件");
        chkDeleteSource.setSelected(false);
        chkDeleteSource.setStyle("-fx-text-fill: #27ae60;"); // 绿色提示

        chkDeleteOnFail = new CheckBox("失败后删除源文件 (慎用)");
        chkDeleteOnFail.setSelected(false);
        chkDeleteOnFail.setStyle("-fx-text-fill: #e74c3c;"); // 红色警示

        chkOverwrite = new CheckBox("覆盖已存在");
        chkOverwrite.setSelected(false);

        // 密码箱初始化
        lvPasswords = new ListView<>();
        lvPasswords.setPrefHeight(80);
        lvPasswords.setPlaceholder(new Label("无密码 (默认尝试空密码)"));

        txtNewPass = new TextField();
        txtNewPass.setPromptText("输入常用密码...");
        HBox.setHgrow(txtNewPass, Priority.ALWAYS);

        btnAddPass = new JFXButton("添加");
        btnAddPass.setOnAction(e -> {
            String pwd = txtNewPass.getText();
            if (pwd != null && !pwd.isEmpty() && !lvPasswords.getItems().contains(pwd)) {
                lvPasswords.getItems().add(pwd);
                txtNewPass.clear();
            }
        });

        btnDelPass = new JFXButton("删除");
        btnDelPass.setOnAction(e -> {
            String sel = lvPasswords.getSelectionModel().getSelectedItem();
            if (sel != null) lvPasswords.getItems().remove(sel);
        });

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
        return "批量智能解压 (密码箱版)";
    }

    @Override
    public String getDescription() {
        return "支持多密码自动匹配、外部引擎调用及失败自动清理。";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(80);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        // 1. 引擎配置
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
        grid.add(new Label("解压引擎:"), 0, 0);
        grid.add(exeBox, 1, 0);

        // 2. 路径配置
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
        grid.add(new Label("解压位置:"), 0, 1);
        grid.add(pathBox, 1, 1);

        // 3. 密码箱配置
        VBox passBox = new VBox(5);
        HBox passInput = new HBox(5, txtNewPass, btnAddPass, btnDelPass);
        passBox.getChildren().addAll(lvPasswords, passInput);

        TitledPane tpPass = new TitledPane("密码管理箱 (自动匹配)", passBox);
        tpPass.setExpanded(false);

        // 4. 选项配置
        VBox opts = new VBox(5);
        opts.getChildren().addAll(chkSmartFolder, chkOverwrite, chkDeleteSource, chkDeleteOnFail);

        box.getChildren().addAll(grid, tpPass, new Separator(), new Label("操作选项:"), opts);
        return box;
    }

    @Override
    public void captureParams() {
        pEngine = cbEngine.getValue();
        pExePath = txtExePath.getText();
        pMode = cbOutputMode.getValue();
        pCustomPath = txtCustomPath.getText();
        pSmart = chkSmartFolder.isSelected();
        pDeleteSuccess = chkDeleteSource.isSelected();
        pDeleteFail = chkDeleteOnFail.isSelected();
        pOverwrite = chkOverwrite.isSelected();
        pPasswords = new ArrayList<>(lvPasswords.getItems());
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("zip_engine", pEngine);
        props.setProperty("zip_exe", pExePath);
        props.setProperty("zip_mode", pMode);
        props.setProperty("zip_path", pCustomPath);
        props.setProperty("zip_smart", String.valueOf(pSmart));
        props.setProperty("zip_del_ok", String.valueOf(pDeleteSuccess));
        props.setProperty("zip_del_fail", String.valueOf(pDeleteFail));
        props.setProperty("zip_over", String.valueOf(pOverwrite));

        // Save passwords list
        props.setProperty("zip_pwd_count", String.valueOf(lvPasswords.getItems().size()));
        for (int i = 0; i < lvPasswords.getItems().size(); i++) {
            props.setProperty("zip_pwd_" + i, lvPasswords.getItems().get(i));
        }
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("zip_engine")) cbEngine.getSelectionModel().select(props.getProperty("zip_engine"));
        if (props.containsKey("zip_exe")) txtExePath.setText(props.getProperty("zip_exe"));
        if (props.containsKey("zip_mode")) cbOutputMode.getSelectionModel().select(props.getProperty("zip_mode"));
        if (props.containsKey("zip_path")) txtCustomPath.setText(props.getProperty("zip_path"));
        if (props.containsKey("zip_smart"))
            chkSmartFolder.setSelected(Boolean.parseBoolean(props.getProperty("zip_smart")));
        if (props.containsKey("zip_del_ok"))
            chkDeleteSource.setSelected(Boolean.parseBoolean(props.getProperty("zip_del_ok")));
        if (props.containsKey("zip_del_fail"))
            chkDeleteOnFail.setSelected(Boolean.parseBoolean(props.getProperty("zip_del_fail")));
        if (props.containsKey("zip_over"))
            chkOverwrite.setSelected(Boolean.parseBoolean(props.getProperty("zip_over")));

        // Load passwords
        lvPasswords.getItems().clear();
        int count = Integer.parseInt(props.getProperty("zip_pwd_count", "0"));
        for (int i = 0; i < count; i++) {
            String pwd = props.getProperty("zip_pwd_" + i);
            if (pwd != null) lvPasswords.getItems().add(pwd);
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

            // 1. 计算目标路径
            File baseDestDir;
            if (pMode.startsWith("当前目录")) {
                baseDestDir = file.getParentFile();
            } else if (pMode.startsWith("指定目录")) {
                baseDestDir = new File(pCustomPath);
            } else {
                baseDestDir = new File(file.getParentFile(), "Extracted_" + file.getName());
            }

            File previewDest = pSmart ? new File(baseDestDir, getBaseName(file.getName())) : baseDestDir;

            String displayName = (pEngine.contains("外部") ? "[外部] " : "[内置] ") +
                    (pSmart ? "智能解压 -> " : "解压 -> ") + previewDest.getName();

            // 序列化参数
            Map<String, String> params = new HashMap<>();
            params.put("baseDest", baseDestDir.getAbsolutePath());
            params.put("engine", pEngine);
            params.put("exePath", pExePath);
            params.put("smart", String.valueOf(pSmart));
            params.put("overwrite", String.valueOf(pOverwrite));
            params.put("deleteSuccess", String.valueOf(pDeleteSuccess));
            params.put("deleteFail", String.valueOf(pDeleteFail));
            // 密码列表不在此处序列化，而是使用运行时捕获的 pPasswords 列表

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
        boolean deleteSuccess = Boolean.parseBoolean(rec.getExtraParams().get("deleteSuccess"));
        boolean deleteFail = Boolean.parseBoolean(rec.getExtraParams().get("deleteFail"));
        boolean overwrite = Boolean.parseBoolean(rec.getExtraParams().get("overwrite"));

        File baseDestDir = new File(baseDestPath);
        if (!baseDestDir.exists()) baseDestDir.mkdirs();

        // 1. 确定解压根目录
        File extractRoot;
        if (smart) {
            String wrapperName = getBaseName(archiveFile.getName());
            extractRoot = new File(baseDestDir, wrapperName);
            if (!extractRoot.exists()) extractRoot.mkdirs();
        } else {
            extractRoot = baseDestDir;
        }

        // 2. 构建尝试列表 (空密码 + 用户密码库)
        List<String> passwordsToTry = new ArrayList<>();
        passwordsToTry.add(null); // 首先尝试无密码
        if (pPasswords != null) passwordsToTry.addAll(pPasswords);

        boolean success = false;
        Exception lastError = null;

        // 3. 循环尝试密码
        for (String pwd : passwordsToTry) {
            try {
                if (engine.contains("外部")) {
                    extractWithExternalTool(archiveFile, extractRoot, rec.getExtraParams(), pwd);
                } else {
                    extractWithJava(archiveFile, extractRoot, overwrite, pwd);
                }
                success = true;
                break; // 成功则退出循环
            } catch (Exception e) {
                lastError = e;
                // 如果是密码错误，继续尝试；如果是文件IO错误可能就没必要重试了，但简单起见继续
            }
        }

        if (!success) {
            // 所有密码都失败
            if (deleteFail) {
                try {
                    Files.delete(archiveFile.toPath());
                } catch (Exception ignored) {
                }
                throw new IOException("解压失败(密码耗尽)，源文件已删除: " + (lastError != null ? lastError.getMessage() : "未知错误"));
            } else {
                throw new IOException("解压失败(密码耗尽): " + (lastError != null ? lastError.getMessage() : "未知错误"));
            }
        }

        // 4. 后置智能处理
        if (smart) {
            optimizeSmartFolder(extractRoot, baseDestDir);
        }

        // 5. 成功后删除源
        if (deleteSuccess) {
            try {
                Files.delete(archiveFile.toPath());
            } catch (IOException e) { /* log warn */ }
        }
    }

    // --- 解压引擎实现 ---

    private void extractWithExternalTool(File archive, File destDir, Map<String, String> params, String pwd) throws Exception {
        String exePath = params.get("exePath");
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
        cmd.add(overwrite ? "-aoa" : "-aos");
        cmd.add("-y");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // 不合并错误流，以便区分标准输出和错误
        // pb.redirectErrorStream(true); 
        Process p = pb.start();

        // 消耗流
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
                while (r.readLine() != null) {
                }
            } catch (Exception e) {
            }
        }).start();

        int exitCode = p.waitFor();
        if (exitCode != 0 && exitCode != 1) {
            throw new IOException("外部程序退出码: " + exitCode + " (可能密码错误)");
        }
    }

    private void extractWithJava(File archive, File destDir, boolean overwrite, String pwd) throws Exception {
        String lowerName = archive.getName().toLowerCase();

        // 7z (支持密码)
        if (lowerName.endsWith(".7z")) {
            byte[] pwdBytes = pwd == null ? null : pwd.getBytes(Charset.defaultCharset());
            try (SevenZFile sevenZFile = pwdBytes == null ? new SevenZFile(archive) : new SevenZFile(archive, pwdBytes)) {
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
            return;
        }

        // Rar (不支持)
        if (lowerName.endsWith(".rar")) {
            throw new IOException("内置引擎不支持 RAR，请切换外部引擎。");
        }

        // Zip 等流式 (不支持密码)
        try (InputStream fi = Files.newInputStream(archive.toPath());
             InputStream bi = new BufferedInputStream(fi);
             ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(bi)) {

            ArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!in.canReadEntryData(entry)) {
                    if (pwd != null) throw new IOException("内置引擎不支持此格式的加密流，请用外部引擎。");
                    continue;
                }

                File target = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!target.isDirectory() && !target.mkdirs()) throw new IOException("无法创建目录");
                } else {
                    File parent = target.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("无法创建父目录");
                    if (target.exists() && !overwrite) continue;
                    try (OutputStream o = Files.newOutputStream(target.toPath())) {
                        IOUtils.copy(in, o);
                    }
                }
            }
        }
    }

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

        if (validFiles.size() == 1 && validFiles.get(0).isDirectory()) {
            File singleInnerDir = validFiles.get(0);
            File targetDir = new File(parentDir, singleInnerDir.getName());

            try {
                if (targetDir.equals(wrapperDir)) {
                    // 同名嵌套：Archive/Archive/ -> Archive/
                    File[] innerFiles = singleInnerDir.listFiles();
                    if (innerFiles != null) {
                        for (File innerFile : innerFiles) {
                            File dest = new File(wrapperDir, innerFile.getName());
                            Files.move(innerFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    Files.delete(singleInnerDir.toPath());
                } else if (!targetDir.exists()) {
                    // 异名嵌套：Archive/Data/ -> Data/
                    Files.move(singleInnerDir.toPath(), targetDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
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

    private String getBaseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}