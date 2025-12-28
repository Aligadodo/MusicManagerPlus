package com.filemanager.strategy;

import com.filemanager.base.IAppStrategy;
import com.filemanager.model.*;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.google.common.collect.Lists;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 批量智能解压策略 (v5.1 - SSD 极速优化版)
 * 优化点：
 * 1. [核心] 移除全局 IO 信号量限制，允许 SSD 全速并发解压。
 * 2. [核心] 增大 Java IO 缓冲区至 64KB，提升吞吐量。
 * 3. 保持了密码箱、智能目录、失败保护等所有特性。
 */
public class FileUnzipStrategy extends IAppStrategy {

    // --- UI Components ---
    private final JFXComboBox<String> cbEngine;
    private final TextField txtExePath;
    private final JFXComboBox<String> cbOutputMode;
    private final TextField txtCustomPath;

    // 选项
    private final CheckBox chkSmartFolder;
    private final CheckBox chkDeleteSource; // 解压成功后删除
    private final CheckBox chkOverwrite;
    private final CheckBox chkDeleteOnFail; // 解压失败后删除

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
        txtExePath.setPromptText("7z.exe 或 bz.exe 路径");
        txtExePath.visibleProperty().bind(cbEngine.getSelectionModel().selectedItemProperty().isNotEqualTo("内置引擎 (Java Commons Compress)"));

        // 路径模式
        cbOutputMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "当前目录 (Current Dir)",
                "指定目录 (Custom Path)",
                "同级新建文件夹 (Sibling Folder)"
        ));
        cbOutputMode.getSelectionModel().select(0);

        txtCustomPath = new TextField("Unzipped");
        txtCustomPath.setPromptText("目标文件夹路径");
        txtCustomPath.visibleProperty().bind(cbOutputMode.getSelectionModel().selectedItemProperty().isEqualTo("指定目录 (Custom Path)"));

        // 选项
        chkSmartFolder = new CheckBox("智能目录 (防炸弹)");
        chkSmartFolder.setSelected(true);
        chkSmartFolder.setTooltip(new Tooltip("始终先在独立文件夹解压，若解压后发现只有单目录则自动移出。\n防止“解压炸弹”弄乱目录。"));

        chkDeleteSource = new CheckBox("解压成功并校验后删除源文件");
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

        autoDetectExternalTools();
    }

    private void autoDetectExternalTools() {
        String[] paths = {
                "C:\\Program Files\\7-Zip\\7z.exe",
                "C:\\Program Files (x86)\\7-Zip\\7z.exe",
                "C:\\Program Files\\Bandizip\\bz.exe",
                "C:\\Program Files\\Bandizip\\Bandizip.exe"
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
        return "批量智能解压工具";
    }

    @Override
    public String getDescription() {
        return "支持对压缩文件自动解压，并支持内置算法和7zip程序功能等解压方式，使得功能更稳定和扩展性更强。" +
                "【内置的Java解压引擎兼容性一般，建议自行安装7zip，" +
                "请按照默认路径将7zip安装到C盘下，以支持自动识别路径】";
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
            fc.setTitle("选择解压程序 (7z.exe / bz.exe)");
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

        // 移除了内部线程配置 UI，由主程序统一控制

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
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        Set<String> archiveExts = new HashSet<>(Arrays.asList("zip", "7z", "rar", "tar", "gz", "jar", "xz", "bz2", "iso"));
        File file = rec.getFileHandle();
        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot == -1) return Collections.emptyList();
        String ext = name.substring(dot + 1);

        if (!archiveExts.contains(ext)) return Collections.emptyList();

        // 1. 计算目标路径
        File baseDestDir;
        if (pMode.startsWith("当前目录")) {
            baseDestDir = file.getParentFile();
        } else if (pMode.startsWith("指定目录")) {
            baseDestDir = new File(pCustomPath);
        } else {
            baseDestDir = new File(file.getParentFile(), "Extracted_" + file.getName());
        }

        // 预览路径（如果是智能模式，实际路径在执行时才确定，这里显示基础路径）
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

        return Lists.newArrayList(new ChangeRecord(rec.getOriginalName(), displayName, file, true,
                previewDest.getAbsolutePath(), OperationType.UNZIP, params, ExecStatus.PENDING));

    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (!rec.getExtraParams().containsKey("baseDest")) return;

        // [优化] 直接调用执行，不加锁，允许全速并发
        // 并发度由主程序的 executorService 控制
        executeInternal(rec);
    }

    private void executeInternal(ChangeRecord rec) throws Exception {
        File archiveFile = rec.getFileHandle();
        String baseDestPath = rec.getExtraParams().get("baseDest");
        String engine = rec.getExtraParams().get("engine");
        boolean smart = Boolean.parseBoolean(rec.getExtraParams().get("smart"));
        boolean deleteSuccess = Boolean.parseBoolean(rec.getExtraParams().get("deleteSuccess"));
        boolean deleteFail = Boolean.parseBoolean(rec.getExtraParams().get("deleteFail"));
        boolean overwrite = Boolean.parseBoolean(rec.getExtraParams().get("overwrite"));

        File baseDestDir = new File(baseDestPath);
        if (!baseDestDir.exists()) baseDestDir.mkdirs();

        // 1. 确定解压根目录 (Wrapper)
        File extractRoot;
        if (smart) {
            String wrapperName = getBaseName(archiveFile.getName());
            extractRoot = new File(baseDestDir, wrapperName);
            if (!extractRoot.exists()) extractRoot.mkdirs();
        } else {
            extractRoot = baseDestDir;
        }

        // 2. 准备尝试列表
        List<String> passwordsToTry = new ArrayList<>();
        passwordsToTry.add(null);
        if (pPasswords != null) passwordsToTry.addAll(pPasswords);

        boolean success = false;
        Exception lastError = null;

        // 3. 循环尝试解压
        for (String pwd : passwordsToTry) {
            try {
                if (engine.contains("外部")) {
                    extractWithExternalTool(archiveFile, extractRoot, rec.getExtraParams(), pwd);
                } else {
                    extractWithJava(archiveFile, extractRoot, overwrite, pwd);
                }

                // 校验阶段：确保有文件产出
                String[] files = extractRoot.list();
                if (files == null || files.length == 0) {
                    throw new IOException("解压程序返回成功，但目标目录为空 (可能是密码错误或程序假死)");
                }

                success = true;
                break;
            } catch (Exception e) {
                lastError = e;
            }
        }

        if (!success) {
            if (deleteFail) {
                try {
                    Files.delete(archiveFile.toPath());
                } catch (Exception ignored) {
                }
                throw new IOException("解压失败(源已删): " + (lastError != null ? lastError.getMessage() : "未知"));
            } else {
                throw new IOException("解压失败: " + (lastError != null ? lastError.getMessage() : "未知"));
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

        // 适配参数
        String lowerExe = exePath.toLowerCase();
        boolean isBandizip = lowerExe.contains("bandizip") || lowerExe.contains("bz.exe");

        // Bandizip 智能修复
        if (isBandizip && !lowerExe.endsWith("bz.exe") && !lowerExe.endsWith("bc.exe")) {
            File bz = new File(new File(exePath).getParent(), "bz.exe");
            if (bz.exists()) {
                cmd.set(0, bz.getAbsolutePath());
                isBandizip = true;
            }
        }

        cmd.add("x");
        cmd.add(archive.getAbsolutePath());

        // 路径参数适配
        if (isBandizip) {
            cmd.add("-o:" + destDir.getAbsolutePath());
        } else {
            cmd.add("-o" + destDir.getAbsolutePath()); // 7z
        }

        if (pwd != null && !pwd.isEmpty()) cmd.add("-p" + pwd);
        cmd.add(overwrite ? "-aoa" : "-aos");
        cmd.add("-y");

        ProcessBuilder pb = new ProcessBuilder(cmd);
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

        if (lowerName.endsWith(".rar")) {
            throw new IOException("内置引擎不支持 RAR，请切换外部引擎。");
        }

        try (InputStream fi = Files.newInputStream(archive.toPath());
             InputStream bi = new BufferedInputStream(fi);
             ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(bi)) {

            ArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!in.canReadEntryData(entry)) {
                    if (pwd != null) throw new IOException("内置引擎不支持加密流，请用外部引擎。");
                    continue;
                }

                File target = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!target.isDirectory() && !target.mkdirs()) throw new IOException("无法创建目录: " + target);
                } else {
                    File parent = target.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("无法创建父目录: " + parent);
                    if (target.exists() && !overwrite) continue;

                    // [优化] 使用 64KB 缓冲区
                    try (OutputStream o = Files.newOutputStream(target.toPath())) {
                        byte[] buffer = new byte[64 * 1024];
                        int n;
                        while (-1 != (n = in.read(buffer))) {
                            o.write(buffer, 0, n);
                        }
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
                    File[] innerFiles = singleInnerDir.listFiles();
                    if (innerFiles != null) {
                        for (File innerFile : innerFiles) {
                            File dest = new File(wrapperDir, innerFile.getName());
                            Files.move(innerFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    Files.delete(singleInnerDir.toPath());
                } else if (!targetDir.exists()) {
                    Files.move(singleInnerDir.toPath(), targetDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    deleteDirectoryRecursively(wrapperDir);
                }
            } catch (IOException e) {
                logError("Smart move failed: " + e.getMessage());
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