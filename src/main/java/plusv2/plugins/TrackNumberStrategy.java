package plusv2.plugins;

import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import plusv2.AppStrategy;
import plusv2.model.ChangeRecord;
import plusv2.type.OperationType;
import plusv2.type.ScanTarget;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class TrackNumberStrategy extends AppStrategy {
    private final JFXComboBox<String> cbMode;
    private final CheckBox chkPadZero;
    private final TextField txtSeparator;

    // Captured params
    private String pMode;
    private boolean pPadZero;
    private String pSeparator;

    public TrackNumberStrategy() {
        cbMode = new JFXComboBox<>(FXCollections.observableArrayList(
                "默认排序 (按文件名/拼音 A-Z)",
                "文本列表匹配 (读取目录下的 .txt/.nfo)"
        ));
        cbMode.getSelectionModel().select(0);
        cbMode.setTooltip(new Tooltip("选择序号生成的依据源"));

        chkPadZero = new CheckBox("双位补零 (01, 02...)");
        chkPadZero.setSelected(true);

        txtSeparator = new TextField(". ");
        txtSeparator.setPrefWidth(60);
        txtSeparator.setPromptText("分隔符");
        txtSeparator.setTooltip(new Tooltip("序号和歌名之间的分隔符，例如 '. ' 生成 '01. 歌名'"));
    }

    @Override public String getName() { return "歌曲序号补全 (重命名)"; }
    @Override public ScanTarget getTargetType() { return ScanTarget.FILES_ONLY; }

    @Override
    public void captureParams() {
        pMode = cbMode.getValue();
        pPadZero = chkPadZero.isSelected();
        pSeparator = txtSeparator.getText();
        if (pSeparator == null) pSeparator = ". ";
    }

    @Override
    public Node getConfigNode() {
        VBox box = new VBox(10);
        box.getChildren().addAll(
                new Label("生成模式:"), cbMode,
                new Label("格式设置:"),
                new HBox(10, chkPadZero, new Label("分隔符:"), txtSeparator),
                new Label("说明：'文本列表匹配' 会读取同目录下的 txt 文件内容，\n按行顺序匹配歌名来确定序号。")
        );
        return box;
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("tns_mode", cbMode.getValue());
        props.setProperty("tns_pad", String.valueOf(chkPadZero.isSelected()));
        props.setProperty("tns_sep", txtSeparator.getText());
    }

    @Override
    public void loadConfig(Properties props) {
        if(props.containsKey("tns_mode")) cbMode.getSelectionModel().select(props.getProperty("tns_mode"));
        if(props.containsKey("tns_pad")) chkPadZero.setSelected(Boolean.parseBoolean(props.getProperty("tns_pad")));
        if(props.containsKey("tns_sep")) txtSeparator.setText(props.getProperty("tns_sep"));
    }

    @Override
    public List<ChangeRecord> analyze(List<File> files, List<File> rootDirs, BiConsumer<Double, String> progressReporter) {
        List<ChangeRecord> records = new ArrayList<>();
        boolean useTextFile = pMode.contains("文本列表");
        boolean pad = pPadZero;
        String sep = pSeparator;

        // 按父目录分组
        Map<File, List<File>> dirGroups = files.stream().collect(Collectors.groupingBy(File::getParentFile));

        int totalDirs = dirGroups.size();
        AtomicInteger processedDirs = new AtomicInteger(0);

        // 拼音排序器
        java.text.Collator collator = java.text.Collator.getInstance(java.util.Locale.CHINA);

        for (Map.Entry<File, List<File>> entry : dirGroups.entrySet()) {
            File dir = entry.getKey();
            List<File> audioFiles = entry.getValue();

            if (progressReporter != null) {
                int c = processedDirs.incrementAndGet();
                Platform.runLater(() -> progressReporter.accept((double)c/totalDirs, "分析目录: " + dir.getName()));
            }

            // 确定排序后的列表
            List<File> sortedFiles = new ArrayList<>(audioFiles);
            Map<File, Integer> trackMap = new HashMap<>();

            if (useTextFile) {
                // 尝试寻找列表文件
                File[] txtFiles = dir.listFiles((d, name) -> {
                    String low = name.toLowerCase();
                    return low.endsWith(".txt") || low.endsWith(".nfo") || low.endsWith(".log");
                });

                boolean foundList = false;
                if (txtFiles != null && txtFiles.length > 0) {
                    // 简单策略：取最大的那个文本文件
                    File listFile = Arrays.stream(txtFiles).max(Comparator.comparingLong(File::length)).orElse(null);
                    if (listFile != null) {
                        trackMap = parseTrackList(listFile, audioFiles);
                        if (!trackMap.isEmpty()) foundList = true;
                    }
                }

                // 如果没找到列表或解析失败，回退到默认排序
                if (!foundList) {
                    sortedFiles.sort((f1, f2) -> collator.compare(f1.getName(), f2.getName()));
                    for (int i = 0; i < sortedFiles.size(); i++) trackMap.put(sortedFiles.get(i), i + 1);
                }
            } else {
                // 默认模式：按文件名排序 (先去除旧序号再排序，避免旧序号影响)
                sortedFiles.sort((f1, f2) -> {
                    String n1 = cleanFileName(f1.getName());
                    String n2 = cleanFileName(f2.getName());
                    return collator.compare(n1, n2);
                });
                for (int i = 0; i < sortedFiles.size(); i++) trackMap.put(sortedFiles.get(i), i + 1);
            }

            // 生成变更记录
            for (File f : audioFiles) {
                Integer trackNum = trackMap.get(f);
                if (trackNum == null) continue; // 不在列表里的文件暂不处理

                String numStr = String.valueOf(trackNum);
                if (pad && trackNum < 10) numStr = "0" + numStr;

                String originalName = f.getName();
                String ext = "";
                int dot = originalName.lastIndexOf('.');
                if (dot > 0) ext = originalName.substring(dot);

                String cleanTitle = cleanFileName(originalName.substring(0, dot > 0 ? dot : originalName.length()));
                String newName = numStr + sep + cleanTitle + ext;

                if (!originalName.equals(newName)) {
                    File target = new File(f.getParent(), newName);
                    records.add(new ChangeRecord(originalName, newName, f, true, target.getAbsolutePath(), OperationType.RENAME));
                }
            }
        }
        return records;
    }

    @Override public void execute(ChangeRecord rec) throws Exception {
        File s = rec.getFileHandle(); File t = new File(rec.getNewPath());
        if(s.equals(t)) return;
        if(!t.getParentFile().exists()) t.getParentFile().mkdirs();
        Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // --- Helpers ---

    // 移除文件名开头的数字序号 (支持 "01. ", "1 - ", "02 ", "1.")
    private String cleanFileName(String name) {
        // Regex: Start with digits, followed by optional spaces/dots/dashes
        return name.replaceFirst("^\\d+[\\s.\\-_]*", "").trim();
    }

    // 解析列表文件，尝试将音频文件匹配到行号
    private Map<File, Integer> parseTrackList(File listFile, List<File> audioFiles) {
        Map<File, Integer> result = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(listFile.toPath(), java.nio.charset.Charset.defaultCharset());
            // 过滤太短的行
            List<String> validLines = lines.stream()
                    .map(String::trim)
                    .filter(l -> l.length() > 1)
                    .collect(Collectors.toList());

            // 匹配逻辑：对于每个音频文件，看它的核心名字是否出现在某一行中
            for (File audio : audioFiles) {
                String coreName = cleanFileName(audio.getName());
                if (coreName.lastIndexOf('.') > 0) coreName = coreName.substring(0, coreName.lastIndexOf('.'));
                coreName = coreName.toLowerCase();

                // 寻找最匹配的行
                for (int i = 0; i < validLines.size(); i++) {
                    String line = validLines.get(i).toLowerCase();
                    // 简单包含匹配，实际可能需要更复杂的模糊匹配算法
                    // 如果行里包含文件名(去后缀)，认为匹配
                    if (line.contains(coreName)) {
                        result.put(audio, i + 1); // 行号作为序号
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // ignore read errors
        }
        return result;
    }
}