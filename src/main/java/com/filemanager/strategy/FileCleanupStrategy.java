package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文件清理策略
 * 支持：文件去重、文件夹去重、空目录清理
 * 删除方式：直接删除、伪删除（归档到垃圾箱）
 */
public class FileCleanupStrategy extends IAppStrategy {
    // 常见媒体类型定义，用于同类比较
    private static final Set<String> EXT_AUDIO = new HashSet<>(Arrays.asList("mp3", "flac", "wav", "aac", "m4a", "ogg", "wma", "ape", "alac", "aiff", "dsf", "dff"));
    private static final Set<String> EXT_VIDEO = new HashSet<>(Arrays.asList("mp4", "mkv", "avi", "mov", "wmv", "flv", "m4v", "mpg"));
    private static final Set<String> EXT_IMAGE = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "bmp", "gif", "webp", "tiff"));
    // --- UI Components ---
    private final JFXComboBox<CleanupMode> cbMode;
    private final JFXComboBox<DeleteMethod> cbMethod;
    private final TextField txtTrashPath; // 回收站路径（支持相对或绝对）
    private final CheckBox chkKeepLargest;
    private final CheckBox chkKeepEarliest;
    private final TextField txtKeepExt;
    // --- Runtime Params ---
    private CleanupMode pMode;
    private DeleteMethod pMethod;
    private String pTrashPath;
    private boolean pKeepLargest;
    private boolean pKeepEarliest;
    private String pKeepExt;

    public FileCleanupStrategy() {
        cbMode = new JFXComboBox<>(FXCollections.observableArrayList(CleanupMode.values()));
        cbMode.getSelectionModel().select(CleanupMode.DEDUP_FILES);
        cbMode.setTooltip(new Tooltip("选择清理的逻辑规则"));

        cbMethod = new JFXComboBox<>(FXCollections.observableArrayList(DeleteMethod.values()));
        cbMethod.getSelectionModel().select(DeleteMethod.PSEUDO_DELETE);
        cbMethod.setTooltip(new Tooltip("选择删除的方式"));

        txtTrashPath = new TextField(".EchoTrash");
        txtTrashPath.setPromptText("回收站位置");
        txtTrashPath.setTooltip(new Tooltip("输入相对名称（如 .del）将在各盘根目录创建；输入绝对路径（如 D:\\Trash）则统一移动到该处。"));
        txtTrashPath.visibleProperty().bind(cbMethod.getSelectionModel().selectedItemProperty().isEqualTo(DeleteMethod.PSEUDO_DELETE));

        chkKeepLargest = new CheckBox("保留体积/质量最佳的副本");
        chkKeepLargest.setSelected(true);
        chkKeepLargest.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        chkKeepLargest.setTooltip(new Tooltip("勾选：保留最大的文件；不勾选：保留名字最短（通常是原件）的文件"));

        chkKeepEarliest = new CheckBox("保留日期最早/最晚的副本");
        chkKeepEarliest.setSelected(true);
        chkKeepEarliest.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isNotEqualTo(CleanupMode.REMOVE_EMPTY_DIRS));
        chkKeepEarliest.setTooltip(new Tooltip("勾选：保留日期最早的文件(夹)；不勾选：保留最新的文件(夹)"));

        txtKeepExt = new TextField("wav");
        txtKeepExt.setPromptText("优先保留后缀");
        txtKeepExt.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));

    }

    @Override
    public String getName() {
        return "文件清理与去重";
    }

    @Override
    public String getDescription() {
        return "智能识别重复文件/文件夹、清理空目录。支持按盘符结构伪删除。";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.ALL;
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("清理模式:"), 0, 0);
        grid.add(cbMode, 1, 0);
        grid.add(new Label("删除方式:"), 0, 1);
        grid.add(cbMethod, 1, 1);

        // 动态配置区
        VBox dynamicArea = new VBox(8);
        dynamicArea.setStyle("-fx-background-color: rgba(0,0,0,0.03); -fx-padding: 10; -fx-background-radius: 5;");

        // 回收站配置
        HBox trashBox = new HBox(10);
        trashBox.setAlignment(Pos.CENTER_LEFT);
        JFXButton btnPickTrash = new JFXButton("浏览...");
        btnPickTrash.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtTrashPath.setText(f.getAbsolutePath());
        });
        trashBox.getChildren().addAll(new Label("回收站路径:"), txtTrashPath, btnPickTrash);
        trashBox.visibleProperty().bind(cbMethod.getSelectionModel().selectedItemProperty().isEqualTo(DeleteMethod.PSEUDO_DELETE));
        trashBox.managedProperty().bind(trashBox.visibleProperty());

        // 去重配置
        VBox dedupBox = new VBox(5);
        HBox keepRow1 = new HBox(10, new Label("优先后缀:"), txtKeepExt);
        keepRow1.setAlignment(Pos.CENTER_LEFT);
        HBox keepRow2 = new HBox(10, chkKeepLargest);
        keepRow2.setAlignment(Pos.CENTER_LEFT);
        HBox keepRow3 = new HBox(10, chkKeepEarliest);
        keepRow3.setAlignment(Pos.CENTER_LEFT);
        Label lblHint = new Label("提示：去重仅在同类型文件（如音频vs音频）间进行，会自动忽略 '(1)', 'Copy' 等后缀。");
        lblHint.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        dedupBox.getChildren().addAll(keepRow1, keepRow2, keepRow3, lblHint);
        dedupBox.visibleProperty().bind(cbMode.getSelectionModel().selectedItemProperty().isEqualTo(CleanupMode.DEDUP_FILES));
        dedupBox.managedProperty().bind(dedupBox.visibleProperty());

        dynamicArea.getChildren().addAll(trashBox, dedupBox);

        box.getChildren().addAll(grid, dynamicArea);
        return box;
    }

    @Override
    public void captureParams() {
        pMode = cbMode.getValue();
        pMethod = cbMethod.getValue();
        pTrashPath = txtTrashPath.getText();
        if (pTrashPath == null || pTrashPath.trim().isEmpty()) pTrashPath = ".EchoTrash";
        pKeepLargest = chkKeepLargest.isSelected();
        pKeepEarliest = chkKeepEarliest.isSelected();
        pKeepExt = txtKeepExt.getText();
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("clean_mode", pMode.name());
        props.setProperty("clean_method", pMethod.name());
        props.setProperty("clean_trash", pTrashPath);
        props.setProperty("clean_keepLarge", String.valueOf(pKeepLargest));
        props.setProperty("clean_keepEarly", String.valueOf(pKeepEarliest));
        props.setProperty("clean_keepExt", pKeepExt);
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("clean_mode"))
            cbMode.getSelectionModel().select(CleanupMode.valueOf(props.getProperty("clean_mode")));
        if (props.containsKey("clean_method"))
            cbMethod.getSelectionModel().select(DeleteMethod.valueOf(props.getProperty("clean_method")));
        if (props.containsKey("clean_trash")) txtTrashPath.setText(props.getProperty("clean_trash"));
        if (props.containsKey("clean_keepLarge"))
            chkKeepLargest.setSelected(Boolean.parseBoolean(props.getProperty("clean_keepLarge")));
        if (props.containsKey("clean_keepEarly"))
            chkKeepEarliest.setSelected(Boolean.parseBoolean(props.getProperty("clean_keepEarly")));
        if (props.containsKey("clean_keepExt")) txtKeepExt.setText(props.getProperty("clean_keepExt"));
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord rec, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        // 1. 根据模式选择分析器
        File f = rec.getFileHandle();

        if (pMode == CleanupMode.REMOVE_EMPTY_DIRS) {
            if (isDirectoryEmpty(f)) {
                return Collections.singletonList(createDeleteRecord(f, "空文件夹 (无子文件)"));
            }
            return Collections.emptyList();
        } else if (pMode == CleanupMode.DEDUP_FOLDERS) {
            File[] files = f.listFiles();
            if (f.isFile() || files == null || files.length < 2) {
                return Collections.emptyList();
            }
            return analyzeDuplicateFolders(Arrays.asList(files));
        } else {
            File[] files = f.listFiles();
            if (f.isFile() || files == null || files.length < 2) {
                return Collections.emptyList();
            }
            return analyzeDuplicateFiles(Arrays.asList(files));
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
                // 尝试移动
                try {
                    Files.move(file.toPath(), trashFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    // 跨盘移动 fallback
                    if (file.isDirectory()) copyDirectory(file, trashFile);
                    else Files.copy(file.toPath(), trashFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    deleteDirectoryRecursively(file); // 移完后删源
                }
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
     * 1. 智能文件去重
     */
    private List<ChangeRecord> analyzeDuplicateFiles(List<File> files) {
        List<ChangeRecord> result = new ArrayList<>();
        Map<File, List<File>> dirMap = files.stream().filter(File::isFile).collect(Collectors.groupingBy(File::getParentFile));

        // 正则：提取文件名核心 (忽略 (1), - Copy 等)
        Pattern normPattern = Pattern.compile("^(.+?)(\\s*[\\(\\[（].*?[\\)\\]）])?(\\s*-\\s*(副本|Copy))?(\\s*\\(\\d+\\))?(\\.[^.]+)?$");

        for (Map.Entry<File, List<File>> entry : dirMap.entrySet()) {
            // 二级分组：CoreName -> List<File>
            Map<String, List<File>> nameGroup = entry.getValue().stream().collect(Collectors.groupingBy(f -> {
                String name = f.getName();
                Matcher m = normPattern.matcher(name);
                String core = m.find() ? m.group(1).trim().toLowerCase() : name.toLowerCase();
                String ext = getExt(name);
                String typeTag = getMediaType(ext);
                return core + "::" + typeTag; // Key: "song::AUDIO"
            }));

            for (List<File> group : nameGroup.values()) {
                if (group.size() < 2) continue; // 无重复

                // 决策：保留哪一个？
                File keeper = Collections.max(group, (f1, f2) -> {
                    // 1. 优先后缀匹配
                    if (pKeepExt != null && !pKeepExt.isEmpty()) {
                        boolean k1 = f1.getName().toLowerCase().endsWith("." + pKeepExt.toLowerCase());
                        boolean k2 = f2.getName().toLowerCase().endsWith("." + pKeepExt.toLowerCase());
                        if (k1 != k2) {
                            return k1 ? 1 : -1;
                        }
                    }

                    // 2. 体积优先
                    if (pKeepLargest) {
                        int sizeCmp = Long.compare(f1.length(), f2.length());
                        if (sizeCmp != 0) {
                            return sizeCmp;
                        }
                    }

                    // 3. 变更时间优先
                    if (pKeepEarliest) {
                        int sizeCmp = Long.compare(f2.lastModified(), f1.lastModified());
                        if (sizeCmp != 0) {
                            return sizeCmp;
                        }

                        try {
                            BasicFileAttributes attributes = Files.readAttributes(Paths.get(f1.getPath()), BasicFileAttributes.class);
                            BasicFileAttributes attributes2 = Files.readAttributes(Paths.get(f2.getPath()), BasicFileAttributes.class);
                            if (attributes2.lastModifiedTime().compareTo(attributes.lastModifiedTime()) != 0) {
                                return attributes2.lastModifiedTime().compareTo(attributes.lastModifiedTime());
                            }
                            if (attributes2.creationTime().compareTo(attributes.creationTime()) != 0) {
                                return attributes2.creationTime().compareTo(attributes.creationTime());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    // 4. 默认：名字短的优先 (通常不带 (1) 的是原件)
                    int compLen = Integer.compare(f2.getName().length(), f1.getName().length());
                    if (compLen != 0) {
                        return compLen;
                    }

                    // 5. 默认：名字排序靠前的优先 (通常是大写)
                    return StringUtils.compare(f2.getName(), f1.getName(), true);
                });

                // 严格检查：必须确保 keeper 存在于 group 中，且不被删除
                for (File f : group) {
                    if (f == keeper) {
                        continue; // 保留
                    }
                    result.add(createDeleteRecord(f, "重复副本 (与 " + keeper.getName() + " 内容重复)"));
                }
            }
        }
        return result;
    }

    private String getExt(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(i + 1).toLowerCase() : "";
    }

    private String getMediaType(String ext) {
        if (EXT_AUDIO.contains(ext)) return "AUDIO";
        if (EXT_VIDEO.contains(ext)) return "VIDEO";
        if (EXT_IMAGE.contains(ext)) return "IMAGE";
        return "OTHER_" + ext;
    }

    /**
     * 2. 文件夹去重 (内容一致性检查)
     */
    private List<ChangeRecord> analyzeDuplicateFolders(List<File> files) {
        List<ChangeRecord> result = new ArrayList<>();
        List<File> dirs = files.stream().filter(File::isDirectory).collect(Collectors.toList());
        Map<File, List<File>> parentMap = dirs.stream().filter(f -> f.getParentFile() != null).collect(Collectors.groupingBy(File::getParentFile));
        for (List<File> siblings : parentMap.values()) {
            // 计算指纹（包含递归内容）
            Map<File, String> fingerprints = new HashMap<>();
            for (File dir : siblings) {
                fingerprints.put(dir, calculateRecursiveDirFingerprint(dir));
            }

            // 按指纹分组
            Map<String, List<File>> dupeGroups = siblings.stream().collect(Collectors.groupingBy(fingerprints::get));

            for (Map.Entry<String, List<File>> entry : dupeGroups.entrySet()) {
                if (entry.getKey().isEmpty()) {
                    continue; // 忽略空指纹或无法读取的
                }
                List<File> group = entry.getValue();
                if (group.size() < 2) {
                    continue;
                }

                // 保留名字最短的
                group.sort(Comparator.comparingInt((File f) -> f.getName().length()));
                File keeper = group.get(0);

                for (int i = 1; i < group.size(); i++) {
                    File toDelete = group.get(i);
                    String sizeStr = formatSize(getDirSize(toDelete));
                    result.add(createDeleteRecord(toDelete, "文件夹内容重复 (同: " + keeper.getName() + ", 大小: " + sizeStr + ")"));
                }
            }
        }
        return result;
    }

    /**
     * 递归计算文件夹指纹：相对路径 + 文件大小
     * 只有目录结构和文件大小完全一致才视为重复
     */
    private String calculateRecursiveDirFingerprint(File dir) {
        try {
            StringBuilder sb = new StringBuilder();
            Files.walk(dir.toPath()).sorted() // 确保顺序一致
                    .forEach(path -> {
                        File f = path.toFile();
                        String relPath = dir.toPath().relativize(path).toString();
                        sb.append(relPath).append(":");
                        if (f.isFile()) sb.append(f.length());
                        else sb.append("D");
                        sb.append("|");
                    });
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 递归计算文件夹大小
     */
    private long getDirSize(File dir) {
        try {
            return Files.walk(dir.toPath()).filter(p -> p.toFile().isFile()).mapToLong(p -> p.toFile().length()).sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private boolean isDirectoryEmpty(File directory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory.toPath())) {
            return !dirStream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    // --- 路径计算核心 ---

    private ChangeRecord createDeleteRecord(File f, String reason) {
        String newPath;
        String newName;
        Map<String, String> params = new HashMap<>();

        if (pMethod == DeleteMethod.DIRECT_DELETE) {
            newPath = "PERMANENT_DELETE";
            newName = "[删除] " + f.getName();
            params.put("method", "DIRECT");
        } else {
            // 伪删除路径计算
            File trashRoot;
            Path sourcePath = f.toPath();
            Path root = sourcePath.getRoot();

            if (Paths.get(pTrashPath).isAbsolute()) {
                // 模式 B: 固定绝对路径
                File fixedTrash = new File(pTrashPath);
                String driveTag = root.toString().replace(":", "").replace(File.separator, "") + "_Drive";
                Path relativeToRoot = root.relativize(sourcePath).getParent();
                File targetDir = new File(fixedTrash, driveTag);
                if (relativeToRoot != null) targetDir = new File(targetDir, relativeToRoot.toString());
                trashRoot = targetDir;
            } else {
                // 模式 A: 相对路径
                File driveRoot = root.toFile();
                trashRoot = new File(driveRoot, pTrashPath);
                Path relativeToRoot = root.relativize(sourcePath).getParent();
                if (relativeToRoot != null) trashRoot = new File(trashRoot, relativeToRoot.toString());
            }

            File trashFile = new File(trashRoot, f.getName());
            newPath = trashFile.getAbsolutePath();
            newName = "[回收] " + f.getName();
            params.put("method", "PSEUDO");
        }
        params.put("reason", reason);

        // 在 ChangeRecord 中复用 RENAME 只是为了显示，OpType 必须是 DELETE
        return new ChangeRecord(f.getName(), newName, f, true, newPath, OperationType.DELETE, params, ExecStatus.PENDING);
    }

    private void copyDirectory(File source, File target) throws IOException {
        Files.walk(source.toPath()).forEach(sourcePath -> {
            Path targetPath = target.toPath().resolve(source.toPath().relativize(sourcePath));
            try {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public enum CleanupMode {
        DEDUP_FILES("同名/近似文件去重"), DEDUP_FOLDERS("重复内容文件夹去重"), REMOVE_EMPTY_DIRS("删除空文件夹");
        private final String label;

        CleanupMode(String l) {
            label = l;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum DeleteMethod {
        PSEUDO_DELETE("伪删除 (保留目录结构到回收站)"), DIRECT_DELETE("直接物理删除 (不可恢复)");
        private final String label;

        DeleteMethod(String l) {
            label = l;
        }

        @Override
        public String toString() {
            return label;
        }
    }

}