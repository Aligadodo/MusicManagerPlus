package com.filemanager.strategy;

import com.filemanager.model.*;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文件清理策略
 * 支持：文件去重、文件夹去重、空目录清理
 * 删除方式：直接删除、伪删除（归档到垃圾箱）
 */
public class FileCleanupStrategy extends AppStrategy {

    // --- UI Components ---
    private final JFXComboBox<CleanupMode> cbMode;
    private final JFXComboBox<DeleteMethod> cbMethod;
    private final TextField txtTrashDirName; // 伪删除的根目录名
    private final CheckBox chkKeepLargest;   // 去重时保留最大的
    private final TextField txtKeepExt;      // 去重时优先保留的后缀
    private final Spinner<Integer> spThreads;

    // --- Runtime Params ---
    private CleanupMode pMode;
    private DeleteMethod pMethod;
    private String pTrashName;
    private boolean pKeepLargest;
    private String pKeepExt;
    private int pThreads;

    public FileCleanupStrategy() {
        cbMode = new JFXComboBox<>(FXCollections.observableArrayList(CleanupMode.values()));
        cbMode.getSelectionModel().select(CleanupMode.DEDUP_FILES);
        cbMode.setTooltip(new Tooltip("选择清理的逻辑规则"));

        cbMethod = new JFXComboBox<>(FXCollections.observableArrayList(DeleteMethod.values()));
        cbMethod.getSelectionModel().select(DeleteMethod.PSEUDO_DELETE);
        cbMethod.setTooltip(new Tooltip("选择删除的方式"));

        txtTrashDirName = new TextField(".EchoTrash");
        txtTrashDirName.setPromptText("回收站目录名");
        txtTrashDirName.visibleProperty().bind(cbMethod.getSelectionModel().selectedItemProperty().isEqualTo(DeleteMethod.PSEUDO_DELETE));

        chkKeepLargest = new CheckBox("保留文件体积最大的副本");
        chkKeepLargest.setSelected(true);
        chkKeepLargest.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));

        txtKeepExt = new TextField("flac");
        txtKeepExt.setPromptText("优先保留的后缀 (如 flac)");
        txtKeepExt.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));

        spThreads = new Spinner<>(1, 32, 4);
    }

    @Override
    public String getName() { return "文件清理与去重"; }

    @Override
    public String getDescription() { return "支持同名文件去重、重复文件夹清理及空目录删除。支持伪删除（移动到回收站）。"; }

    @Override
    public ScanTarget getTargetType() { return ScanTarget.ALL; } // 需要同时处理文件和文件夹

    @Override
    public int getPreferredThreadCount() { return spThreads.getValue(); }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        
        grid.add(new Label("清理模式:"), 0, 0); grid.add(cbMode, 1, 0);
        grid.add(new Label("删除方式:"), 0, 1); grid.add(cbMethod, 1, 1);
        
        // 动态配置区
        VBox dynamicArea = new VBox(5);
        dynamicArea.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-padding: 8; -fx-background-radius: 4;");
        
        Label lblTrash = new Label("回收站目录名:");
        lblTrash.visibleProperty().bind(txtTrashDirName.visibleProperty());
        HBox trashBox = new HBox(5, lblTrash, txtTrashDirName);
        trashBox.setAlignment(Pos.CENTER_LEFT);

        Label lblKeep = new Label("去重规则:");
        lblKeep.visibleProperty().bind(chkKeepLargest.visibleProperty());
        HBox dedupBox = new HBox(10, chkKeepLargest, new Label("优先后缀:"), txtKeepExt);
        dedupBox.setAlignment(Pos.CENTER_LEFT);
        dedupBox.visibleProperty().bind(chkKeepLargest.visibleProperty());

        dynamicArea.getChildren().addAll(trashBox, lblKeep, dedupBox);
        
        box.getChildren().addAll(grid, dynamicArea, new Separator(), new HBox(10, new Label("并发线程:"), spThreads));
        return box;
    }

    @Override
    public void captureParams() {
        pMode = cbMode.getValue();
        pMethod = cbMethod.getValue();
        pTrashName = txtTrashDirName.getText();
        if(pTrashName == null || pTrashName.isEmpty()) pTrashName = ".EchoTrash";
        pKeepLargest = chkKeepLargest.isSelected();
        pKeepExt = txtKeepExt.getText();
        pThreads = spThreads.getValue();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("clean_mode", pMode.name());
        props.setProperty("clean_method", pMethod.name());
        props.setProperty("clean_trash", pTrashName);
        props.setProperty("clean_keepLarge", String.valueOf(pKeepLargest));
        props.setProperty("clean_keepExt", pKeepExt);
    }

    @Override
    public void loadConfig(Properties props) {
        if(props.containsKey("clean_mode")) cbMode.getSelectionModel().select(CleanupMode.valueOf(props.getProperty("clean_mode")));
        if(props.containsKey("clean_method")) cbMethod.getSelectionModel().select(DeleteMethod.valueOf(props.getProperty("clean_method")));
        if(props.containsKey("clean_trash")) txtTrashDirName.setText(props.getProperty("clean_trash"));
        if(props.containsKey("clean_keepLarge")) chkKeepLargest.setSelected(Boolean.parseBoolean(props.getProperty("clean_keepLarge")));
        if(props.containsKey("clean_keepExt")) txtKeepExt.setText(props.getProperty("clean_keepExt"));
    }

    @Override
    public List<ChangeRecord> analyze(List<ChangeRecord> inputRecords, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        // 1. 根据模式选择分析器
        List<File> inputFiles = inputRecords.stream().map(ChangeRecord::getFileHandle).collect(Collectors.toList());
        
        if (pMode == CleanupMode.REMOVE_EMPTY_DIRS) {
            return analyzeEmptyDirs(inputFiles, rootDirs);
        } else if (pMode == CleanupMode.DEDUP_FOLDERS) {
            return analyzeDuplicateFolders(inputFiles, progressReporter);
        } else {
            return analyzeDuplicateFiles(inputFiles, progressReporter);
        }
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() != OperationType.DELETE) return;
        
        File file = rec.getFileHandle();
        if (!file.exists()) return; // 可能已经被处理了
        
        String method = rec.getExtraParams().get("method");
        
        if ("DIRECT".equals(method)) {
            // 递归删除（如果是文件夹）
            if (file.isDirectory()) deleteDirectoryRecursively(file);
            else Files.delete(file.toPath());
        } else {
            // 伪删除：移动到 Trash
            String trashPath = rec.getNewPath();
            if (trashPath != null && !trashPath.isEmpty()) {
                File trashFile = new File(trashPath);
                if (!trashFile.getParentFile().exists()) trashFile.getParentFile().mkdirs();
                Files.move(file.toPath(), trashFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    
    private void deleteDirectoryRecursively(File file) throws IOException {
        Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // --- 具体分析逻辑 ---

    /**
     * 1. 文件去重分析
     * 逻辑：同目录下，核心文件名相同（去除 (1) 等后缀），判定为重复组。
     */
    private List<ChangeRecord> analyzeDuplicateFiles(List<File> files, BiConsumer<Double, String> reporter) {
        List<ChangeRecord> result = new ArrayList<>();
        // 按父目录分组
        Map<File, List<File>> dirMap = files.stream()
            .filter(File::isFile)
            .collect(Collectors.groupingBy(File::getParentFile));

        int total = dirMap.size();
        AtomicInteger curr = new AtomicInteger(0);

        // 定义正则：匹配文件名核心部分，去除 " (1)", " - 副本", ".mp3" 等
        // 示例: "Song (1).mp3" -> "Song"
        Pattern normPattern = Pattern.compile("^(.+?)(\\s*[\\(\\[（].*?[\\)\\]）])?(\\s*-\\s*副本)?(\\.[^.]+)?$");

        for (Map.Entry<File, List<File>> entry : dirMap.entrySet()) {
            if (reporter != null && curr.incrementAndGet() % 10 == 0) 
                Platform.runLater(() -> reporter.accept((double)curr.get()/total, "分析目录: " + entry.getKey().getName()));

            // 在同目录下，按 CoreName 分组
            Map<String, List<File>> nameGroup = entry.getValue().stream()
                .collect(Collectors.groupingBy(f -> {
                    Matcher m = normPattern.matcher(f.getName());
                    if (m.find()) return m.group(1).trim().toLowerCase();
                    return f.getName();
                }));

            for (List<File> group : nameGroup.values()) {
                if (group.size() < 2) continue; // 无重复

                // 选出保留者 (Keeper)
                File keeper = Collections.max(group, (f1, f2) -> {
                    // 1. 优先匹配后缀
                    if (pKeepExt != null && !pKeepExt.isEmpty()) {
                        boolean k1 = f1.getName().toLowerCase().endsWith("." + pKeepExt.toLowerCase());
                        boolean k2 = f2.getName().toLowerCase().endsWith("." + pKeepExt.toLowerCase());
                        if (k1 != k2) return k1 ? 1 : -1;
                    }
                    // 2. 大小优先
                    if (pKeepLargest) {
                        return Long.compare(f1.length(), f2.length());
                    }
                    // 3. 默认：名字短的优先 (通常不带 (1))
                    return Integer.compare(f2.getName().length(), f1.getName().length());
                });

                // 生成删除记录
                for (File f : group) {
                    if (f.equals(keeper)) continue;
                    result.add(createDeleteRecord(f, "重复文件 (保留: " + keeper.getName() + ")"));
                }
            }
        }
        return result;
    }

    /**
     * 2. 文件夹去重分析
     * 逻辑：同父目录下，如果两个文件夹内的文件列表（名称+大小）完全一致，视为重复。
     */
    private List<ChangeRecord> analyzeDuplicateFolders(List<File> files, BiConsumer<Double, String> reporter) {
        List<ChangeRecord> result = new ArrayList<>();
        
        // 仅处理目录
        List<File> dirs = files.stream().filter(File::isDirectory).collect(Collectors.toList());
        // 按父目录分组
        Map<File, List<File>> parentMap = dirs.stream()
            .filter(f -> f.getParentFile() != null)
            .collect(Collectors.groupingBy(File::getParentFile));

        int total = parentMap.size();
        AtomicInteger curr = new AtomicInteger(0);

        for (List<File> siblings : parentMap.values()) {
            if (reporter != null) Platform.runLater(() -> reporter.accept((double)curr.incrementAndGet()/total, "分析目录结构..."));
            
            // 计算每个文件夹的指纹 (文件名+大小 的 Hash)
            Map<File, String> fingerPrints = new HashMap<>();
            for (File dir : siblings) {
                fingerPrints.put(dir, calculateDirFingerprint(dir));
            }

            // 按指纹分组
            Map<String, List<File>> dupeGroups = siblings.stream()
                .collect(Collectors.groupingBy(fingerPrints::get));

            for (List<File> group : dupeGroups.values()) {
                if (group.size() < 2) continue;
                if (group.get(0) == null) continue; // 无法读取的目录

                // 排序：保留名字最短的，或修改日期最早的
                group.sort(Comparator.comparingInt((File f) -> f.getName().length()).thenComparing(File::lastModified));
                
                File keeper = group.get(0);
                for (int i = 1; i < group.size(); i++) {
                    result.add(createDeleteRecord(group.get(i), "重复文件夹 (内容与 " + keeper.getName() + " 一致)"));
                }
            }
        }
        return result;
    }

    /**
     * 3. 空文件夹清理
     * 逻辑：递归到底层，如果是空目录则删除。
     */
    private List<ChangeRecord> analyzeEmptyDirs(List<File> files, List<File> rootDirs) {
        List<ChangeRecord> result = new ArrayList<>();
        // 简单实现：只检查传入列表中的目录是否为空
        // 注意：为了安全，必须再次确认目录真实为空
        for (File f : files) {
            if (f.isDirectory() && !rootDirs.contains(f)) {
                String[] list = f.list();
                if (list != null && list.length == 0) {
                    result.add(createDeleteRecord(f, "空文件夹"));
                }
            }
        }
        return result;
    }

    // --- 辅助方法 ---

    private ChangeRecord createDeleteRecord(File f, String reason) {
        String newPath;
        String newName;
        Map<String, String> params = new HashMap<>();
        
        if (pMethod == DeleteMethod.DIRECT_DELETE) {
            newPath = "PERMANENT_DELETE";
            newName = "[删除] " + f.getName();
            params.put("method", "DIRECT");
        } else {
            // 计算伪删除路径： DriveRoot/.EchoTrash/OriginalRelativePath
            Path root = f.toPath().getRoot();
            if (root == null) root = f.toPath().getParent(); // Fallback
            
            // 构建相对路径结构，避免文件名冲突
            String relativeStruct = f.getParentFile().getAbsolutePath().replace(":", ""); 
            // Windows: D:\Music -> D\Music. Linux: /home/user -> home/user
            if (relativeStruct.startsWith(File.separator)) relativeStruct = relativeStruct.substring(1);
            
            File trashRoot = new File(root.toFile(), pTrashName);
            File trashDir = new File(trashRoot, relativeStruct);
            File trashFile = new File(trashDir, f.getName());
            
            newPath = trashFile.getAbsolutePath();
            newName = "[回收] " + f.getName();
            params.put("method", "PSEUDO");
        }
        params.put("reason", reason); // 记录原因供展示
        
        // 借用 RENAME 类型来显示，或者在 MusicFileManagerApp 中支持 DELETE 类型
        return new ChangeRecord(f.getName(), newName, f, true, newPath, OperationType.DELETE, params, ExecStatus.PENDING);
    }

    private String calculateDirFingerprint(File dir) {
        try {
            // 仅计算一级子文件，或者深度递归？这里做一级子文件指纹
            File[] files = dir.listFiles();
            if (files == null) return null;
            
            // 排序保证顺序一致
            Arrays.sort(files, Comparator.comparing(File::getName));
            
            StringBuilder sb = new StringBuilder();
            for (File f : files) {
                sb.append(f.getName()).append(":");
                if (f.isFile()) sb.append(f.length());
                else sb.append("DIR");
                sb.append("|");
            }
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    // --- Enums ---
    
    public enum CleanupMode {
        DEDUP_FILES("同名/近似文件去重"),
        DEDUP_FOLDERS("重复内容文件夹去重"),
        REMOVE_EMPTY_DIRS("删除空文件夹");
        
        private String label;
        CleanupMode(String l) { label = l; }
        @Override public String toString() { return label; }
    }
    
    public enum DeleteMethod {
        PSEUDO_DELETE("伪删除 (保留路径结构到回收站)"),
        DIRECT_DELETE("直接物理删除 (不可恢复)");
        
        private String label;
        DeleteMethod(String l) { label = l; }
        @Override public String toString() { return label; }
    }
}