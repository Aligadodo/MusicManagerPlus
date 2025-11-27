package plusv2.plugins;

import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import plusv2.AppStrategyV2;
import plusv2.model.AudioMeta;
import plusv2.model.ChangeRecord;
import plusv2.type.ExecStatus;
import plusv2.type.OperationType;
import plusv2.type.ScanTarget;
import plusv2.util.MetadataHelper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Demo 功能待实现
 */
public class FileMigrateStrategy extends AppStrategyV2 {
    private final TextField txtDestDir;
    private final TextField txtPathPattern;
    private final JFXButton btnPick;
    private final CheckBox chkCleanEmpty;
    private String pDestDir;
    private String pPattern;
    private boolean pClean;

    public FileMigrateStrategy() {
        txtDestDir = new TextField();
        txtDestDir.setPromptText("选择目标根目录...");
        btnPick = new JFXButton("浏览");
        btnPick.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) txtDestDir.setText(f.getAbsolutePath());
        });
        txtPathPattern = new TextField("%artist%/%year% %album%/%track% - %title%");
        chkCleanEmpty = new CheckBox("移动后清理源空文件夹");
        chkCleanEmpty.setSelected(true);
    }

    @Override
    public String getName() {
        return "文件归档/整理 (移动文件)";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public void captureParams() {
        pDestDir = txtDestDir.getText();
        pPattern = txtPathPattern.getText();
        pClean = chkCleanEmpty.isSelected();
    }

    @Override
    public Node getConfigNode() {
        return new VBox(10, new Label("目标根目录:"), new HBox(10, txtDestDir, btnPick), new Label("目录结构模板 (用 / 分隔):"), txtPathPattern, chkCleanEmpty, new Label("变量: %artist%, %album%, %year%, %track%, %title%"));
    }

    @Override
    public void saveConfig(Properties props) {
        if (!txtDestDir.getText().isEmpty()) props.setProperty("fms_dest", txtDestDir.getText());
        props.setProperty("fms_pattern", txtPathPattern.getText());
        props.setProperty("fms_clean", String.valueOf(chkCleanEmpty.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("fms_dest")) txtDestDir.setText(props.getProperty("fms_dest"));
        if (props.containsKey("fms_pattern")) txtPathPattern.setText(props.getProperty("fms_pattern"));
        if (props.containsKey("fms_clean"))
            chkCleanEmpty.setSelected(Boolean.parseBoolean(props.getProperty("fms_clean")));
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        List<ChangeRecord> records = new ArrayList<>();
        String destRoot = pDestDir;
        String pattern = pPattern;
        boolean clean = pClean;
        if (destRoot == null || destRoot.isEmpty()) return records; // Must have dest

        int total = files.size();
        AtomicInteger processed = new AtomicInteger(0);

        records.addAll(files.parallelStream().map(f -> {
            int curr = processed.incrementAndGet();
            if (progressReporter != null && curr % 100 == 0)
                Platform.runLater(() -> progressReporter.accept((double) curr / total, "规划路径: " + curr + "/" + total));

            AudioMeta meta = MetadataHelper.getSmartMetadata(f,false);
            String relPath = MetadataHelper.format(pattern, meta).replaceAll("[*?\"<>|]", "_"); // Keep / and \
            String ext = "";
            int dot = f.getName().lastIndexOf('.');
            if (dot > 0) ext = f.getName().substring(dot);

            // Ensure extension is present if template didn't include it
            if (!relPath.toLowerCase().endsWith(ext.toLowerCase())) relPath += ext;

            File target = new File(destRoot, relPath);
            if (!target.getAbsolutePath().equals(f.getAbsolutePath())) {
                Map<String, String> params = new HashMap<>();
                if (clean) params.put("cleanSource", "true");
                return new ChangeRecord(f.getName(), target.getName(), f, true, target.getAbsolutePath(), OperationType.MOVE, params, ExecStatus.PENDING);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList()));
        return records;
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File s = rec.getFileHandle();
        File t = new File(rec.getNewPath());
        if (s.equals(t)) return;
        if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
        Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Clean empty dir logic
        if ("true".equals(rec.getExtraParams().get("cleanSource"))) {
            File parent = s.getParentFile();
            if (parent != null && parent.isDirectory() && Objects.requireNonNull(parent.list()).length == 0) {
                parent.delete();
            }
        }
    }
}