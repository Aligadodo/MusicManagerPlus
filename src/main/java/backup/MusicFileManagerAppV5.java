package backup;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextArea;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.controlsfx.control.CheckComboBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 实际开发请确保引入: JFoenix, ControlsFX, Lombok, Commons-IO, JAudioTagger

public class MusicFileManagerAppV5 extends Application {

    private Stage primaryStage;
    
    // Data Models
    private ObservableList<ChangeRecord> changePreviewList = FXCollections.observableArrayList();
    // 支持多路径选择
    private ObservableList<String> sourcePathStrings = FXCollections.observableArrayList();
    private List<File> sourceRootDirs = new ArrayList<>();

    // UI Controls
    private ListView<String> lvSourcePaths; 
    // 替换原有的 CheckBox，升级为深度控制
    private JFXComboBox<String> cbRecursionMode;
    private Spinner<Integer> spRecursionDepth;
    
    private CheckComboBox<String> ccbFileTypes;
    private JFXTextArea logArea;
    
    private TreeView<File> dirTree; // 左侧目录树
    private TreeView<ChangeRecord> previewTree; // 变更预览树
    private JFXComboBox<AppStrategy> cbStrategy; // 策略选择
    private VBox strategyConfigContainer; // 策略专属配置面板容器

    private List<AppStrategy> strategies = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Echo - 音乐文件管理专家 v3.5 (深度控制版)");

        initStrategies();
        Scene scene = new Scene(createMainLayout(), 1400, 900);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initStrategies() {
        strategies.add(new AdvancedRenameStrategy());
        strategies.add(new AlbumDirNormalizeStrategy());
        strategies.add(new TrackNumberStrategy());
        strategies.add(new FileMigrateStrategy());
    }

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();
        
        // Top: Header & Global Config
        VBox topContainer = new VBox(10);
        topContainer.setPadding(new Insets(15));
        topContainer.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        
        Label title = new Label("Echo 文件管理工作台");
        title.setFont(Font.font("Segoe UI", 22));
        title.setTextFill(Color.web("#2c3e50"));

        topContainer.getChildren().addAll(title, createGlobalConfigPanel());
        root.setTop(topContainer);

        // Center: Split Pane
        SplitPane centerSplit = new SplitPane();
        
        // Left: Directory Tree
        VBox leftPanel = createLeftPanel();
        
        // Middle: Strategy Config
        VBox actionPanel = createActionPanel();
        
        // Right: Change Preview
        VBox previewPanel = createPreviewPanel();
        
        SplitPane mainSplit = new SplitPane();
        mainSplit.getItems().addAll(leftPanel, actionPanel, previewPanel);
        mainSplit.setDividerPositions(0.2, 0.55);

        root.setCenter(mainSplit);

        // Bottom: Logs
        logArea = new JFXTextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);
        logArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-text-fill: #333;");
        root.setBottom(logArea);

        return root;
    }

    // --- UI Construction Helpers ---

    private GridPane createGlobalConfigPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setAlignment(Pos.TOP_LEFT);

        // 1. Source Paths Section
        Label lblSources = new Label("工作目录:");
        GridPane.setValignment(lblSources, javafx.geometry.VPos.TOP);
        
        lvSourcePaths = new ListView<>(sourcePathStrings);
        lvSourcePaths.setPrefHeight(80); 
        lvSourcePaths.setPrefWidth(500);
        lvSourcePaths.setPlaceholder(new Label("请添加一个或多个目录进行处理..."));
        
        VBox btnBox = new VBox(5);
        JFXButton btnAdd = new JFXButton("添加目录");
        btnAdd.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
        btnAdd.setPrefWidth(100);
        btnAdd.setOnAction(e -> addDirectory());

        JFXButton btnRemove = new JFXButton("移除选中");
        btnRemove.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
        btnRemove.setPrefWidth(100);
        btnRemove.setOnAction(e -> removeDirectory());
        
        btnBox.getChildren().addAll(btnAdd, btnRemove);

        // 2. Dynamic Scope Controls (Recursion Depth)
        cbRecursionMode = new JFXComboBox<>(FXCollections.observableArrayList(
            "仅当前目录", 
            "递归所有子目录", 
            "指定目录深度"
        ));
        cbRecursionMode.getSelectionModel().select(1); // Default: All Recursive
        
        spRecursionDepth = new Spinner<>(1, 20, 2); // Min 1, Max 20, Initial 2
        spRecursionDepth.setEditable(true);
        spRecursionDepth.setPrefWidth(80);
        spRecursionDepth.setTooltip(new Tooltip("设置向下递归的层级数 (1代表包含当前目录的一级子项)"));
        
        // 动态禁用 Spinner
        spRecursionDepth.disableProperty().bind(
            cbRecursionMode.getSelectionModel().selectedItemProperty().isNotEqualTo("指定目录深度")
        );

        HBox scopeBox = new HBox(10);
        scopeBox.setAlignment(Pos.CENTER_LEFT);
        scopeBox.getChildren().addAll(
            new Label("扫描范围:"), 
            cbRecursionMode, 
            new Label("深度:"), 
            spRecursionDepth
        );

        // 3. File Type Filters
        ObservableList<String> extensions = FXCollections.observableArrayList(
                "mp3", "flac", "wav", "ape", "dsf", "m4a", "jpg", "png", "nfo", "lrc"
        );
        ccbFileTypes = new CheckComboBox<>(extensions);
        ccbFileTypes.getCheckModel().checkAll();
        ccbFileTypes.setTooltip(new Tooltip("勾选要处理的文件类型"));
        ccbFileTypes.setPrefWidth(150);

        // Layout
        grid.add(lblSources, 0, 0);
        grid.add(lvSourcePaths, 1, 0);
        grid.add(btnBox, 2, 0);
        
        HBox filters = new HBox(20, scopeBox, new Separator(javafx.geometry.Orientation.VERTICAL), new Label("文件类型:"), ccbFileTypes);
        filters.setAlignment(Pos.CENTER_LEFT);
        grid.add(filters, 1, 1, 2, 1);

        return grid;
    }

    private VBox createLeftPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #ffffff;");
        
        Label lbl = new Label("资源浏览");
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
        
        dirTree = new TreeView<>();
        dirTree.setShowRoot(true); 
        dirTree.setCellFactory(tv -> new TreeCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item.getPath().equals("ALL_ROOTS")) {
                        setText("已选目录汇总");
                    } else {
                        setText(item.getName().isEmpty() ? item.getAbsolutePath() : item.getName());
                    }
                }
            }
        });
        
        VBox.setVgrow(dirTree, Priority.ALWAYS);
        box.getChildren().addAll(lbl, dirTree);
        return box;
    }

    private VBox createActionPanel() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #ffffff;");

        Label lblHeader = new Label("操作配置");
        lblHeader.setFont(Font.font("Segoe UI", 16));
        lblHeader.setStyle("-fx-font-weight: bold;");

        Label lblStrategy = new Label("1. 选择处理功能:");
        cbStrategy = new JFXComboBox<>();
        cbStrategy.setItems(FXCollections.observableArrayList(strategies));
        cbStrategy.setPrefWidth(Double.MAX_VALUE);
        cbStrategy.setConverter(new javafx.util.StringConverter<AppStrategy>() {
            @Override public String toString(AppStrategy object) { return object.getName(); }
            @Override public AppStrategy fromString(String string) { return null; }
        });

        Label lblConfig = new Label("2. 功能参数设置:");
        strategyConfigContainer = new VBox();
        strategyConfigContainer.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 4; -fx-padding: 10; -fx-background-color: #fafafa;");
        strategyConfigContainer.setMinHeight(100);
        strategyConfigContainer.setAlignment(Pos.TOP_LEFT);
        strategyConfigContainer.getChildren().add(new Label("请选择一种策略以显示配置项..."));

        cbStrategy.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            strategyConfigContainer.getChildren().clear();
            if (newVal != null) {
                Node configNode = newVal.getConfigNode();
                if (configNode != null) {
                    strategyConfigContainer.getChildren().add(configNode);
                } else {
                    strategyConfigContainer.getChildren().add(new Label("该功能无需额外配置。"));
                }
            }
        });

        JFXButton btnPreview = new JFXButton("生成变更预览 (Dry Run)");
        btnPreview.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnPreview.setPrefWidth(Double.MAX_VALUE);
        btnPreview.setPrefHeight(40);
        btnPreview.setOnAction(e -> runPreview());

        JFXButton btnExecute = new JFXButton("立即执行变更");
        btnExecute.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnExecute.setPrefWidth(Double.MAX_VALUE);
        btnExecute.setPrefHeight(40);
        btnExecute.setOnAction(e -> runExecute());

        box.getChildren().addAll(
            lblHeader, new Separator(),
            lblStrategy, cbStrategy, 
            lblConfig, strategyConfigContainer, 
            new Region(),
            btnPreview, btnExecute
        );
        VBox.setVgrow(box.getChildren().get(6), Priority.ALWAYS);
        
        return box;
    }

    private VBox createPreviewPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label lbl = new Label("变更预览 (树形结构)");
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");

        previewTree = new TreeView<>();
        previewTree.setCellFactory(tv -> new TreeCell<ChangeRecord>() {
            @Override
            protected void updateItem(ChangeRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox node = new HBox(8);
                    node.setAlignment(Pos.CENTER_LEFT);
                    
                    if ("VIRTUAL_ROOT".equals(item.getNewPath())) {
                        setText("预览汇总根节点");
                        return;
                    }

                    boolean isDir = item.getFileHandle() != null && item.getFileHandle().isDirectory();
                    Label oldName = new Label(item.getOriginalName());
                    if (isDir) oldName.setStyle("-fx-font-weight: bold;");
                    
                    if (item.isChanged()) {
                        Label arrow = new Label("➜");
                        arrow.setTextFill(Color.GRAY);
                        Label newName = new Label(item.getNewName());
                        newName.setTextFill(Color.web("#27ae60"));
                        newName.setStyle(isDir ? "-fx-font-weight: bold;" : "-fx-font-weight: normal;");
                        
                        oldName.setStyle("-fx-strikethrough: true; -fx-text-fill: #e74c3c;" + (isDir ? "-fx-font-weight: bold;" : ""));
                        
                        if (item.isMove()) {
                            Tooltip tt = new Tooltip("移动至: " + item.getNewPath());
                            Tooltip.install(node, tt);
                            Label moveTag = new Label("[归档]");
                            moveTag.setTextFill(Color.BLUE);
                            node.getChildren().addAll(moveTag, oldName, arrow, newName);
                        } else {
                            node.getChildren().addAll(oldName, arrow, newName);
                        }
                    } else {
                        node.getChildren().add(oldName);
                    }
                    setGraphic(node);
                }
            }
        });
        
        VBox.setVgrow(previewTree, Priority.ALWAYS);
        box.getChildren().addAll(lbl, previewTree);
        return box;
    }

    // --- Actions ---

    private void addDirectory() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("选择音乐目录 (支持多次添加)");
        File f = dc.showDialog(primaryStage);
        if (f != null) {
            if (sourceRootDirs.contains(f)) {
                log("提示: 目录已存在列表中。");
                return;
            }
            sourceRootDirs.add(f);
            sourcePathStrings.add(f.getAbsolutePath());
            refreshLeftTree();
        }
    }

    private void removeDirectory() {
        int idx = lvSourcePaths.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            sourceRootDirs.remove(idx);
            sourcePathStrings.remove(idx);
            refreshLeftTree();
        }
    }

    private void refreshLeftTree() {
        File dummyRoot = new File("ALL_ROOTS");
        TreeItem<File> rootItem = new TreeItem<>(dummyRoot);
        rootItem.setExpanded(true);

        for (File rootDir : sourceRootDirs) {
            TreeItem<File> item = new TreeItem<>(rootDir);
            item.setExpanded(true);
            populateTreeItem(item);
            rootItem.getChildren().add(item);
        }
        dirTree.setRoot(rootItem);
    }

    private void populateTreeItem(TreeItem<File> item) {
        File file = item.getValue();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    if (child.isDirectory()) {
                        TreeItem<File> childItem = new TreeItem<>(child);
                        item.getChildren().add(childItem);
                    }
                }
            }
        }
    }

    private void runPreview() {
        if (sourceRootDirs.isEmpty()) {
            log("错误: 请至少添加一个源目录。");
            return;
        }

        AppStrategy strategy = cbStrategy.getValue();
        if (strategy == null) {
            log("错误: 请先选择一个操作策略。");
            return;
        }

        // 获取用户配置的递归深度
        int depthConfig = 1;
        String mode = cbRecursionMode.getValue();
        if ("递归所有子目录".equals(mode)) {
            depthConfig = Integer.MAX_VALUE;
        } else if ("指定目录深度".equals(mode)) {
            depthConfig = spRecursionDepth.getValue();
        }
        final int maxDepth = depthConfig;
        
        Task<TreeItem<ChangeRecord>> task = new Task<TreeItem<ChangeRecord>>() {
            @Override
            protected TreeItem<ChangeRecord> call() throws Exception {
                updateMessage("正在扫描文件系统 (深度: " + (maxDepth == Integer.MAX_VALUE ? "无限" : maxDepth) + ")...");
                
                // 1. 扫描所有根目录 (传入深度参数)
                List<File> allFiles = new ArrayList<>();
                for (File root : sourceRootDirs) {
                    allFiles.addAll(scanFiles(root, strategy.getTargetType(), maxDepth));
                }
                
                updateMessage("正在计算变更...");
                // 2. 分析
                List<ChangeRecord> changes = strategy.analyze(allFiles, sourceRootDirs);
                changePreviewList.setAll(changes);
                
                updateMessage("正在构建树形视图...");
                // 3. 视图
                return buildPreviewTree(changes, sourceRootDirs);
            }
        };

        task.setOnSucceeded(e -> {
            previewTree.setRoot(task.getValue());
            long changedCount = changePreviewList.stream().filter(ChangeRecord::isChanged).count();
            log(String.format("预览完成。共扫描 %d 个对象，预计变更 %d 个。", changePreviewList.size(), changedCount));
        });
        
        task.setOnFailed(e -> {
            log("预览失败: " + e.getSource().getException().getMessage());
            e.getSource().getException().printStackTrace();
        });
        new Thread(task).start();
    }

    private TreeItem<ChangeRecord> buildPreviewTree(List<ChangeRecord> records, List<File> rootDirs) {
        ChangeRecord virtualRootRec = new ChangeRecord("全部目录", "", null, false, "VIRTUAL_ROOT", false);
        TreeItem<ChangeRecord> virtualRoot = new TreeItem<>(virtualRootRec);
        virtualRoot.setExpanded(true);

        Map<String, TreeItem<ChangeRecord>> pathMap = new HashMap<>();
        pathMap.put("VIRTUAL_ROOT", virtualRoot);
        
        for (File root : rootDirs) {
             ChangeRecord rootRec = new ChangeRecord(root.getAbsolutePath(), "", root, false, root.getAbsolutePath(), false);
             TreeItem<ChangeRecord> rootItem = new TreeItem<>(rootRec);
             rootItem.setExpanded(true);
             virtualRoot.getChildren().add(rootItem);
             pathMap.put(root.getAbsolutePath(), rootItem);
        }

        records.sort(Comparator.comparing(ChangeRecord::getOriginalPath));

        for (ChangeRecord rec : records) {
            File currentFile = rec.getFileHandle();
            if (rootDirs.contains(currentFile)) continue; 

            TreeItem<ChangeRecord> parentItem = ensureParentNode(currentFile.getParentFile(), rootDirs, pathMap, virtualRoot);
            
            TreeItem<ChangeRecord> item = new TreeItem<>(rec);
            parentItem.getChildren().add(item);
            pathMap.put(currentFile.getAbsolutePath(), item);
        }

        return virtualRoot;
    }

    private TreeItem<ChangeRecord> ensureParentNode(File directory, List<File> rootDirs, Map<String, TreeItem<ChangeRecord>> pathMap, TreeItem<ChangeRecord> virtualRoot) {
        String path = directory.getAbsolutePath();
        if (pathMap.containsKey(path)) {
            return pathMap.get(path);
        }

        File matchedRoot = null;
        for (File r : rootDirs) {
            if (path.startsWith(r.getAbsolutePath())) {
                matchedRoot = r;
                break;
            }
        }

        if (matchedRoot != null && !directory.equals(matchedRoot)) {
            TreeItem<ChangeRecord> parentOfThis = ensureParentNode(directory.getParentFile(), rootDirs, pathMap, virtualRoot);
            
            ChangeRecord dirRec = new ChangeRecord(directory.getName(), "", directory, false, directory.getAbsolutePath(), false);
            TreeItem<ChangeRecord> thisItem = new TreeItem<>(dirRec);
            thisItem.setExpanded(true);
            parentOfThis.getChildren().add(thisItem);
            
            pathMap.put(path, thisItem);
            return thisItem;
        }
        
        return virtualRoot;
    }

    private void runExecute() {
        long changedCount = changePreviewList.stream().filter(ChangeRecord::isChanged).count();
        if (changedCount == 0) {
            log("没有有效的变更需要执行。");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定要执行 " + changedCount + " 个变更吗？\n注意：操作不可逆。", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                List<ChangeRecord> sortedChanges = changePreviewList.stream()
                        .filter(ChangeRecord::isChanged)
                        .sorted((r1, r2) -> Integer.compare(
                                r2.getOriginalPath().length(), 
                                r1.getOriginalPath().length()))
                        .collect(Collectors.toList());

                int success = 0;
                int fail = 0;
                for (ChangeRecord rec : sortedChanges) {
                    try {
                        performFileOperation(rec);
                        success++;
                    } catch (Exception ex) {
                        log("执行失败 [" + rec.getOriginalName() + "]: " + ex.getMessage());
                        fail++;
                    }
                }
                log("批量操作结束。成功: " + success + ", 失败: " + fail);
                previewTree.setRoot(null);
                changePreviewList.clear();
                refreshLeftTree();
            }
        });
    }

    private void performFileOperation(ChangeRecord rec) throws IOException {
        File source = rec.getFileHandle();
        File target = new File(rec.getNewPath());
        if (source.equals(target)) return;

        if (rec.isMove()) {
             if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
             Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            if (source.isDirectory()) {
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                source.renameTo(target);
            }
        }
    }

    /**
     * 升级后的扫描方法：支持 maxDepth 控制
     */
    private List<File> scanFiles(File root, ScanTarget targetType, int maxDepth) {
        List<File> result = new ArrayList<>();
        if (!root.exists()) return result;
        
        ObservableList<String> allowedTypes = ccbFileTypes.getCheckModel().getCheckedItems();

        // 使用 Files.walk(path, maxDepth) 控制递归深度
        try (Stream<Path> stream = Files.walk(root.toPath(), maxDepth)) {
            result = stream
                .map(Path::toFile)
                .filter(f -> {
                    // 保留根目录对象以便后续过滤或展示，但在操作时应小心处理
                    
                    boolean isDir = f.isDirectory();
                    if (targetType == ScanTarget.FILES_ONLY && isDir) return false;
                    if (targetType == ScanTarget.FOLDERS_ONLY && !isDir) return false;
                    
                    if (!isDir) {
                        String name = f.getName().toLowerCase();
                        boolean typeMatch = false;
                        for (String ext : allowedTypes) {
                            if (name.endsWith("." + ext)) { typeMatch = true; break; }
                        }
                        return typeMatch;
                    }
                    return true; 
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            log("扫描错误 [" + root.getName() + "]: " + e.getMessage());
        }
        return result;
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    // --- Models ---

    enum ScanTarget {
        FILES_ONLY, FOLDERS_ONLY, ALL
    }

    @Data
    @NoArgsConstructor
    static class ChangeRecord {
        private String originalName;
        private String newName;
        private File fileHandle;
        private boolean changed;
        private String newPath; 
        private boolean isMove;

        public ChangeRecord(String originalName, String newName, File fileHandle, boolean changed, String newPath, boolean isMove) {
            this.originalName = originalName;
            this.newName = newName;
            this.fileHandle = fileHandle;
            this.changed = changed;
            this.newPath = newPath;
            this.isMove = isMove;
        }

        public String getOriginalPath() { return fileHandle != null ? fileHandle.getAbsolutePath() : ""; }
        @Override public String toString() { return originalName; }
    }

    // --- Abstract Strategy ---

    abstract static class AppStrategy {
        public abstract String getName();
        public abstract Node getConfigNode();
        public abstract ScanTarget getTargetType();
        public abstract List<ChangeRecord> analyze(List<File> files, List<File> rootDirs);
    }

    // --- Strategy Implementations ---

    class AdvancedRenameStrategy extends AppStrategy {
        private final JFXComboBox<String> cbMode;
        private final JFXComboBox<String> cbTarget; 
        private final TextField txtParam1;
        private final TextField txtParam2;
        
        public AdvancedRenameStrategy() {
            cbMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "添加前缀", "添加后缀", "字符替换", "扩展名转小写", "去除空格"
            ));
            cbMode.getSelectionModel().select(0);

            cbTarget = new JFXComboBox<>(FXCollections.observableArrayList(
                "仅处理文件", "仅处理文件夹", "全部处理"
            ));
            cbTarget.getSelectionModel().select(0);

            txtParam1 = new TextField();
            txtParam1.setPromptText("输入内容...");
            txtParam2 = new TextField();
            txtParam2.setPromptText("替换为...");
            
            cbMode.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
                txtParam1.setDisable(val.contains("小写") || val.contains("去除空格"));
                txtParam2.setVisible(val.contains("替换"));
            });
            txtParam2.setVisible(false);
        }

        @Override public String getName() { return "高级批量重命名"; }
        
        @Override public ScanTarget getTargetType() {
            String t = cbTarget.getValue();
            if ("仅处理文件".equals(t)) return ScanTarget.FILES_ONLY;
            if ("仅处理文件夹".equals(t)) return ScanTarget.FOLDERS_ONLY;
            return ScanTarget.ALL;
        }

        @Override
        public Node getConfigNode() {
            VBox box = new VBox(10);
            box.getChildren().addAll(
                new Label("操作对象:"), cbTarget,
                new Label("方式:"), cbMode,
                new Label("参数:"), txtParam1, txtParam2
            );
            return box;
        }

        @Override
        public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
            List<ChangeRecord> records = new ArrayList<>();
            String mode = cbMode.getValue();
            String p1 = txtParam1.getText();
            String p2 = txtParam2.getText();
            ScanTarget target = getTargetType();

            for (File f : files) {
                if (rootDirs.contains(f)) continue;

                boolean isDir = f.isDirectory();
                if (target == ScanTarget.FILES_ONLY && isDir) continue;
                if (target == ScanTarget.FOLDERS_ONLY && !isDir) continue;

                String oldName = f.getName();
                String newName = oldName;

                if ("添加前缀".equals(mode) && !p1.isEmpty()) newName = p1 + oldName;
                else if ("添加后缀".equals(mode) && !p1.isEmpty()) {
                    if (isDir) {
                         newName = oldName + p1;
                    } else {
                        int dot = oldName.lastIndexOf(".");
                        if (dot > 0) newName = oldName.substring(0, dot) + p1 + oldName.substring(dot);
                        else newName = oldName + p1;
                    }
                }
                else if ("字符替换".equals(mode) && !p1.isEmpty()) newName = oldName.replace(p1, p2 == null ? "" : p2);
                else if ("扩展名转小写".equals(mode) && !isDir) {
                     int dot = oldName.lastIndexOf(".");
                     if (dot > 0) newName = oldName.substring(0, dot) + oldName.substring(dot).toLowerCase();
                }
                else if ("去除空格".equals(mode)) newName = oldName.replace(" ", "");

                String newPath = f.getParent() + File.separator + newName;
                records.add(new ChangeRecord(oldName, newName, f, !oldName.equals(newName), newPath, false));
            }
            return records;
        }
    }

    class FileMigrateStrategy extends AppStrategy {
        private final TextField txtTargetDir;
        private final JFXButton btnPickTarget;

        public FileMigrateStrategy() {
            txtTargetDir = new TextField();
            txtTargetDir.setPromptText("默认：原处创建子文件夹");
            txtTargetDir.setEditable(false);
            btnPickTarget = new JFXButton("...");
            btnPickTarget.setOnAction(e -> {
                DirectoryChooser dc = new DirectoryChooser();
                File f = dc.showDialog(null);
                if (f != null) txtTargetDir.setText(f.getAbsolutePath());
            });
        }

        @Override public String getName() { return "按歌手归档 (文件迁移)"; }
        @Override public ScanTarget getTargetType() { return ScanTarget.FILES_ONLY; }

        @Override
        public Node getConfigNode() {
            return new VBox(10, new Label("归档根目录:"), new HBox(5, txtTargetDir, btnPickTarget));
        }

        @Override
        public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
            List<ChangeRecord> records = new ArrayList<>();
            String targetBase = txtTargetDir.getText();
            if (targetBase == null || targetBase.trim().isEmpty()) {
                targetBase = rootDirs.isEmpty() ? "" : rootDirs.get(0).getAbsolutePath();
            }

            for (File f : files) {
                String artist = "其他"; 
                if (f.getName().contains("陈粒")) artist = "陈粒"; 
                if (f.getName().contains("周杰伦")) artist = "周杰伦";
                
                String newDirStr = targetBase + File.separator + artist;
                String newPath = newDirStr + File.separator + f.getName();
                boolean changed = !f.getParentFile().getAbsolutePath().equals(newDirStr);
                records.add(new ChangeRecord(f.getName(), f.getName(), f, changed, newPath, true));
            }
            return records;
        }
    }

    class AlbumDirNormalizeStrategy extends AppStrategy {
        @Override public String getName() { return "专辑目录标准化"; }
        @Override public Node getConfigNode() { return new Label("自动识别底层文件夹内的歌曲信息并重命名文件夹。"); }
        @Override public ScanTarget getTargetType() { return ScanTarget.FILES_ONLY; }
        
        @Override
        public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
            List<ChangeRecord> records = new ArrayList<>();
            Map<File, List<File>> folderGroups = files.stream().collect(Collectors.groupingBy(File::getParentFile));

            for (Map.Entry<File, List<File>> entry : folderGroups.entrySet()) {
                File folder = entry.getKey();
                if (rootDirs.contains(folder)) continue; 

                String artist = "Unknown";
                String album = folder.getName();
                String year = "2000";
                String type = "MP3";
                
                if (entry.getValue().stream().anyMatch(f -> f.getName().endsWith(".flac"))) type = "FLAC";
                if (folder.getName().contains("U87")) { artist = "陈奕迅"; year="2005"; album="U87"; }

                String newFolderName = String.format("%s - %s - %s - %s", artist, year, album, type);
                
                if (!folder.getName().equals(newFolderName)) {
                    String newPath = folder.getParent() + File.separator + newFolderName;
                    records.add(new ChangeRecord(folder.getName(), newFolderName, folder, true, newPath, false));
                }
            }
            return records;
        }
    }

    class TrackNumberStrategy extends AppStrategy {
        @Override public String getName() { return "歌曲序号补全"; }
        @Override public Node getConfigNode() { return null; }
        @Override public ScanTarget getTargetType() { return ScanTarget.FILES_ONLY; }

        @Override
        public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs) {
            List<ChangeRecord> records = new ArrayList<>();
            Map<File, List<File>> folderGroups = files.stream().collect(Collectors.groupingBy(File::getParentFile));

            for (List<File> folderFiles : folderGroups.values()) {
                folderFiles.sort(Comparator.comparing(File::getName));
                for (int i = 0; i < folderFiles.size(); i++) {
                    File f = folderFiles.get(i);
                    String oldName = f.getName();
                    String ext = oldName.contains(".") ? oldName.substring(oldName.lastIndexOf(".")) : "";
                    
                    String title = oldName.replace(ext, "").replaceAll("^\\d+[.\\s-]*", "").trim();
                    String newName = String.format("%02d. %s%s", (i + 1), title, ext);
                    
                    String newPath = f.getParent() + File.separator + newName;
                    records.add(new ChangeRecord(oldName, newName, f, !oldName.equals(newName), newPath, false));
                }
            }
            return records;
        }
    }
}