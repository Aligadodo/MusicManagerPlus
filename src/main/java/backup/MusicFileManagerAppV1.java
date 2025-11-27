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
import lombok.AllArgsConstructor;
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

public class MusicFileManagerAppV1 extends Application {

    private Stage primaryStage;
    
    // Data Models
    private ObservableList<FileItem> sourceFiles = FXCollections.observableArrayList();
    private ObservableList<ChangeRecord> changePreviewList = FXCollections.observableArrayList();

    // UI Controls - Global Config
    private TextField txtSourcePath;
    private CheckBox chkRecursive;
    private CheckComboBox<String> ccbFileTypes;
    private JFXTextArea logArea;
    
    // UI Controls - Panels
    private TreeView<File> dirTree; // 左侧目录树
    private TreeView<ChangeRecord> previewTree; // 变更预览树
    private JFXComboBox<AppStrategy> cbStrategy; // 策略选择
    private VBox strategyConfigContainer; // 策略专属配置面板容器

    // Strategies
    private List<AppStrategy> strategies = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Echo - 音乐文件管理专家 v2.0");

        initStrategies();
        Scene scene = new Scene(createMainLayout(), 1400, 900);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initStrategies() {
        strategies.add(new AdvancedRenameStrategy());
        strategies.add(new AlbumDirNormalizeStrategy());
        strategies.add(new TrackNumberStrategy());
        strategies.add(new FileMigrateStrategy()); // 原 ArtistDistributorStrategy 增强版
    }

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();
        
        // 1. Top: Header & Global Config
        VBox topContainer = new VBox(10);
        topContainer.setPadding(new Insets(15));
        topContainer.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        
        Label title = new Label("Echo 文件管理工作台");
        title.setFont(Font.font("Segoe UI", 22));
        title.setTextFill(Color.web("#2c3e50"));

        topContainer.getChildren().addAll(title, createGlobalConfigPanel());
        root.setTop(topContainer);

        // 2. Left: Directory Tree
        VBox leftPanel = createLeftPanel();
        
        // 3. Center: Strategy & Preview (SplitPane)
        SplitPane centerSplit = new SplitPane();
        
        // 3.1 Strategy Action Panel
        VBox actionPanel = createActionPanel();
        
        // 3.2 Change Preview Panel
        VBox previewPanel = createPreviewPanel();
        
        centerSplit.getItems().addAll(actionPanel, previewPanel);
        centerSplit.setDividerPositions(0.35); // 给操作面板多一点空间

        // Combine Left and Center
        SplitPane mainSplit = new SplitPane();
        mainSplit.getItems().addAll(leftPanel, centerSplit);
        mainSplit.setDividerPositions(0.2); // 左侧目录树占 20%

        root.setCenter(mainSplit);

        // 4. Bottom: Logs
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
        grid.setAlignment(Pos.CENTER_LEFT);

        txtSourcePath = new TextField();
        txtSourcePath.setPromptText("请选择要处理的根目录...");
        txtSourcePath.setPrefWidth(500);
        txtSourcePath.setEditable(false); // 禁止手动输入，强制使用选择器以确保安全
        
        JFXButton btnBrowse = new JFXButton("选择目录");
        btnBrowse.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
        btnBrowse.setOnAction(e -> chooseDirectory());

        chkRecursive = new CheckBox("递归包含子目录");
        chkRecursive.setSelected(true);

        ObservableList<String> extensions = FXCollections.observableArrayList(
                "mp3", "flac", "wav", "ape", "dsf", "m4a", "jpg", "png", "nfo", "lrc"
        );
        ccbFileTypes = new CheckComboBox<>(extensions);
        ccbFileTypes.getCheckModel().checkAll();
        ccbFileTypes.setTooltip(new Tooltip("勾选要处理的文件类型"));
        ccbFileTypes.setPrefWidth(150);

        grid.add(new Label("工作目录:"), 0, 0);
        grid.add(txtSourcePath, 1, 0);
        grid.add(btnBrowse, 2, 0);
        
        HBox filters = new HBox(20, chkRecursive, new Separator(javafx.geometry.Orientation.VERTICAL), new Label("文件类型:"), ccbFileTypes);
        filters.setAlignment(Pos.CENTER_LEFT);
        grid.add(filters, 1, 1, 2, 1);

        return grid;
    }

    private VBox createLeftPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #ffffff;");
        
        Label lbl = new Label("目录结构");
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
        
        dirTree = new TreeView<>();
        // 自定义 Cell 显示文件夹名称而非完整路径
        dirTree.setCellFactory(tv -> new TreeCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName().isEmpty() ? item.getAbsolutePath() : item.getName());
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

        // 策略选择
        Label lblStrategy = new Label("1. 选择处理功能:");
        cbStrategy = new JFXComboBox<>();
        cbStrategy.setItems(FXCollections.observableArrayList(strategies));
        cbStrategy.setPrefWidth(Double.MAX_VALUE);
        // 设置显示策略名称
        cbStrategy.setConverter(new javafx.util.StringConverter<AppStrategy>() {
            @Override public String toString(AppStrategy object) { return object.getName(); }
            @Override public AppStrategy fromString(String string) { return null; }
        });

        // 动态配置区域
        Label lblConfig = new Label("2. 功能参数设置:");
        strategyConfigContainer = new VBox();
        strategyConfigContainer.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 4; -fx-padding: 10; -fx-background-color: #fafafa;");
        strategyConfigContainer.setMinHeight(100);
        strategyConfigContainer.setAlignment(Pos.TOP_LEFT);
        strategyConfigContainer.getChildren().add(new Label("请选择一种策略以显示配置项..."));

        // 监听策略选择变更
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

        // 按钮区域
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
            lblHeader, 
            new Separator(),
            lblStrategy, cbStrategy, 
            lblConfig, strategyConfigContainer, 
            new Region(), // Spacer
            btnPreview, btnExecute
        );
        VBox.setVgrow(box.getChildren().get(6), Priority.ALWAYS); // Push buttons down
        
        return box;
    }

    private VBox createPreviewPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label lbl = new Label("变更预览");
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
                    
                    Label oldName = new Label(item.getOriginalName());
                    
                    if (item.isChanged()) {
                        Label arrow = new Label("➜");
                        arrow.setTextFill(Color.GRAY);
                        Label newName = new Label(item.getNewName());
                        newName.setTextFill(Color.web("#27ae60")); // Green
                        newName.setStyle("-fx-font-weight: bold;");
                        
                        oldName.setStyle("-fx-strikethrough: true; -fx-text-fill: #e74c3c;"); // Red strike
                        
                        // 如果是移动操作，显示完整路径提示
                        if (item.isMove()) {
                            Tooltip tt = new Tooltip("移动至: " + item.getNewPath());
                            Tooltip.install(node, tt);
                            Label moveTag = new Label("[移动]");
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

    private void chooseDirectory() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("选择音乐根目录");
        File f = dc.showDialog(primaryStage);
        if (f != null) {
            txtSourcePath.setText(f.getAbsolutePath());
            loadDirectoryTree(f); // 加载左侧树
        }
    }

    private void loadDirectoryTree(File rootFile) {
        // 简单的懒加载树实现
        TreeItem<File> rootItem = new TreeItem<>(rootFile);
        rootItem.setExpanded(true);
        populateTreeItem(rootItem); // 初始加载一层
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
                        // 可以在这里递归，但对于大文件夹建议监听 Expanded 事件进行懒加载
                    }
                }
            }
        }
    }

    private void runPreview() {
        String pathStr = txtSourcePath.getText();
        if (pathStr == null || pathStr.isEmpty()) {
            log("错误: 请先选择源路径。");
            return;
        }

        AppStrategy strategy = cbStrategy.getValue();
        if (strategy == null) {
            log("错误: 请先选择一个操作策略。");
            return;
        }
        
        Task<TreeItem<ChangeRecord>> task = new Task<TreeItem<ChangeRecord>>() {
            @Override
            protected TreeItem<ChangeRecord> call() throws Exception {
                updateMessage("正在扫描文件...");
                List<File> files = scanFiles(new File(pathStr));
                
                updateMessage("正在根据策略计算变更...");
                List<ChangeRecord> changes = strategy.analyze(files, new File(pathStr));
                
                // 简单的树结构展示结果
                TreeItem<ChangeRecord> root = new TreeItem<>(new ChangeRecord("本次操作汇总", "", null, false, "", false));
                root.setExpanded(true);
                
                // 将结果按文件夹分组展示
                Map<String, TreeItem<ChangeRecord>> folderNodes = new HashMap<>();
                folderNodes.put("ROOT", root);

                for (ChangeRecord rec : changes) {
                    String parentPath = rec.getFileHandle().getParent();
                    TreeItem<ChangeRecord> parentNode = folderNodes.computeIfAbsent(parentPath, k -> {
                        TreeItem<ChangeRecord> p = new TreeItem<>(new ChangeRecord("文件夹: " + new File(k).getName(), "", null, false, "", false));
                        root.getChildren().add(p);
                        p.setExpanded(true);
                        return p;
                    });
                    parentNode.getChildren().add(new TreeItem<>(rec));
                }
                
                changePreviewList.setAll(changes);
                return root;
            }
        };

        task.setOnSucceeded(e -> {
            previewTree.setRoot(task.getValue());
            long changedCount = changePreviewList.stream().filter(ChangeRecord::isChanged).count();
            log(String.format("预览完成。共扫描 %d 个文件，预计变更 %d 个。", changePreviewList.size(), changedCount));
        });
        
        task.setOnFailed(e -> {
            log("操作失败: " + e.getSource().getException().getMessage());
            e.getSource().getException().printStackTrace();
        });
        new Thread(task).start();
    }

    private void runExecute() {
        long changedCount = changePreviewList.stream().filter(ChangeRecord::isChanged).count();
        if (changedCount == 0) {
            log("没有有效的变更需要执行。");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定要执行 " + changedCount + " 个变更吗？\n注意：文件移动操作可能不可逆。", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                int success = 0;
                int fail = 0;
                for (ChangeRecord rec : changePreviewList) {
                    if (rec.isChanged()) {
                        try {
                            performFileOperation(rec);
                            success++;
                        } catch (Exception ex) {
                            log("执行失败 [" + rec.getOriginalName() + "]: " + ex.getMessage());
                            fail++;
                        }
                    }
                }
                log("批量操作结束。成功: " + success + ", 失败: " + fail);
                previewTree.setRoot(null);
                changePreviewList.clear();
                // 刷新左侧树
                loadDirectoryTree(new File(txtSourcePath.getText()));
            }
        });
    }

    private void performFileOperation(ChangeRecord rec) throws IOException {
        File source = rec.getFileHandle();
        File target = new File(rec.getNewPath());
        
        if (source.equals(target)) return;

        if (rec.isMove()) {
             if (!target.getParentFile().exists()) {
                 target.getParentFile().mkdirs();
             }
             Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            source.renameTo(target);
        }
    }

    private List<File> scanFiles(File root) {
        List<File> result = new ArrayList<>();
        if (!root.exists()) return result;
        
        boolean recursive = chkRecursive.isSelected();
        ObservableList<String> allowedTypes = ccbFileTypes.getCheckModel().getCheckedItems();

        try (Stream<Path> stream = recursive ? Files.walk(root.toPath()) : Files.list(root.toPath())) {
            result = stream.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> {
                        String name = f.getName().toLowerCase();
                        for (String ext : allowedTypes) {
                            if (name.endsWith("." + ext)) return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log("扫描错误: " + e.getMessage());
        }
        return result;
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    // --- Models ---

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class FileItem {
        private String name;
        private File fileHandle;
        public FileItem(File f) { this.name = f.getName(); this.fileHandle = f; }
    }

    @Data
    @AllArgsConstructor
    static class ChangeRecord {
        private String originalName;
        private String newName;
        private File fileHandle;
        private boolean changed;
        private String newPath; 
        private boolean isMove; 
        
        public String getOriginalPath() { return fileHandle != null ? fileHandle.getAbsolutePath() : ""; }
        @Override public String toString() { return originalName; }
    }

    // --- Abstract Strategy with UI Support ---

    abstract static class AppStrategy {
        public abstract String getName();
        public abstract Node getConfigNode(); // 子类提供自己的配置界面
        public abstract List<ChangeRecord> analyze(List<File> files, File rootDir);
    }

    // --- Strategy Implementations ---

    // 1. 高级重命名策略 (支持子模式)
    class AdvancedRenameStrategy extends AppStrategy {
        private final JFXComboBox<String> cbMode;
        private final TextField txtParam1;
        private final TextField txtParam2; // Optional, e.g. for replace
        
        public AdvancedRenameStrategy() {
            cbMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "添加前缀", "添加后缀", "字符替换", "扩展名转小写", "去除空格"
            ));
            cbMode.getSelectionModel().select(0);
            txtParam1 = new TextField();
            txtParam1.setPromptText("输入前缀/后缀/查找内容...");
            txtParam2 = new TextField();
            txtParam2.setPromptText("替换为...");
            
            // 简单的显隐控制
            cbMode.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
                txtParam1.setDisable(val.contains("小写") || val.contains("去除空格"));
                txtParam2.setVisible(val.contains("替换"));
            });
            txtParam2.setVisible(false); // default
        }

        @Override
        public String getName() { return "高级批量重命名"; }

        @Override
        public Node getConfigNode() {
            VBox box = new VBox(10);
            box.getChildren().addAll(
                new Label("重命名方式:"), cbMode,
                new Label("参数 1:"), txtParam1,
                txtParam2 // Might be hidden
            );
            return box;
        }

        @Override
        public List<ChangeRecord> analyze(List<File> files, File rootDir) {
            List<ChangeRecord> records = new ArrayList<>();
            String mode = cbMode.getValue();
            String p1 = txtParam1.getText();
            String p2 = txtParam2.getText();

            for (File f : files) {
                String oldName = f.getName();
                String newName = oldName;

                if ("添加前缀".equals(mode) && !p1.isEmpty()) newName = p1 + oldName;
                else if ("添加后缀".equals(mode) && !p1.isEmpty()) {
                    int dot = oldName.lastIndexOf(".");
                    if (dot > 0) {
                        newName = oldName.substring(0, dot) + p1 + oldName.substring(dot);
                    } else {
                        newName = oldName + p1;
                    }
                }
                else if ("字符替换".equals(mode) && !p1.isEmpty()) newName = oldName.replace(p1, p2 == null ? "" : p2);
                else if ("扩展名转小写".equals(mode)) {
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

    // 2. 文件迁移/归档策略 (支持目标路径)
    class FileMigrateStrategy extends AppStrategy {
        private final TextField txtTargetDir;
        private final JFXButton btnPickTarget;

        public FileMigrateStrategy() {
            txtTargetDir = new TextField();
            txtTargetDir.setPromptText("选择归档的目标文件夹...");
            txtTargetDir.setEditable(false);
            
            btnPickTarget = new JFXButton("浏览...");
            btnPickTarget.setStyle("-fx-background-color: #ddd;");
            btnPickTarget.setOnAction(e -> {
                DirectoryChooser dc = new DirectoryChooser();
                File f = dc.showDialog(null);
                if (f != null) txtTargetDir.setText(f.getAbsolutePath());
            });
        }

        @Override
        public String getName() { return "按歌手归档 (文件迁移)"; }

        @Override
        public Node getConfigNode() {
            VBox box = new VBox(10);
            box.getChildren().addAll(
                new Label("目标根目录 (若为空则在原处新建):"),
                new HBox(10, txtTargetDir, btnPickTarget),
                new Label("提示: 将自动创建 '歌手名' 文件夹")
            );
            return box;
        }

        @Override
        public List<ChangeRecord> analyze(List<File> files, File rootDir) {
            List<ChangeRecord> records = new ArrayList<>();
            String targetBase = txtTargetDir.getText();
            if (targetBase == null || targetBase.trim().isEmpty()) {
                targetBase = rootDir.getAbsolutePath(); // Default to source root
            }

            for (File f : files) {
                // Mock Metadata logic
                String artist = "其他歌手"; 
                if (f.getName().contains("陈粒")) artist = "陈粒";
                if (f.getName().contains("周杰伦")) artist = "周杰伦";
                if (f.getName().contains("Coldplay")) artist = "Coldplay";

                String newDirStr = targetBase + File.separator + artist;
                String newPath = newDirStr + File.separator + f.getName();
                
                // Logic: Check if moving is needed (diff path)
                boolean changed = !f.getParentFile().getAbsolutePath().equals(newDirStr);
                records.add(new ChangeRecord(f.getName(), f.getName(), f, changed, newPath, true));
            }
            return records;
        }
    }

    // 3. (原有) 专辑目录标准化
    class AlbumDirNormalizeStrategy extends AppStrategy {
        @Override public String getName() { return "专辑目录标准化"; }
        @Override public Node getConfigNode() { return null; } // No extra config needed
        
        @Override
        public List<ChangeRecord> analyze(List<File> files, File rootDir) {
            List<ChangeRecord> records = new ArrayList<>();
            Map<File, List<File>> folderGroups = files.stream().collect(Collectors.groupingBy(File::getParentFile));

            for (Map.Entry<File, List<File>> entry : folderGroups.entrySet()) {
                File folder = entry.getKey();
                if (folder.equals(rootDir)) continue; 

                // Mock Logic
                String artist = "MockArtist";
                String year = "2023";
                String album = folder.getName();
                String type = "MP3";
                if (entry.getValue().stream().anyMatch(f -> f.getName().endsWith(".flac"))) type = "FLAC";

                if (folder.getName().startsWith("Unknown")) { artist = "Eason"; album = "U87"; }

                String newFolderName = String.format("%s - %s - %s - %s", artist, year, album, type);
                if (!folder.getName().equals(newFolderName)) {
                    String newPath = folder.getParent() + File.separator + newFolderName;
                    records.add(new ChangeRecord(folder.getName(), newFolderName, folder, true, newPath, false));
                }
            }
            return records;
        }
    }

    // 4. (原有) 歌曲序号补全
    class TrackNumberStrategy extends AppStrategy {
        @Override public String getName() { return "歌曲序号补全"; }
        @Override public Node getConfigNode() { return null; }

        @Override
        public List<ChangeRecord> analyze(List<File> files, File rootDir) {
            List<ChangeRecord> records = new ArrayList<>();
            Map<File, List<File>> folderGroups = files.stream().collect(Collectors.groupingBy(File::getParentFile));

            for (List<File> folderFiles : folderGroups.values()) {
                folderFiles.sort(Comparator.comparing(File::getName));
                for (int i = 0; i < folderFiles.size(); i++) {
                    File f = folderFiles.get(i);
                    String oldName = f.getName();
                    String ext = oldName.contains(".") ? oldName.substring(oldName.lastIndexOf(".")) : "";
                    
                    // Mock Logic: clean old index
                    String title = oldName.replace(ext, "").trim().replaceAll("^\\d+[.\\s-]*", "");
                    String newName = String.format("%02d. %s%s", (i + 1), title, ext);
                    
                    String newPath = f.getParent() + File.separator + newName;
                    records.add(new ChangeRecord(oldName, newName, f, !oldName.equals(newName), newPath, false));
                }
            }
            return records;
        }
    }
}