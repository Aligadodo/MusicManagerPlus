/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-28
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.ChangeRecord;
import com.filemanager.tool.file.FileTypeUtil;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.filemanager.util.MetadataHelper;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import com.filemanager.app.tools.display.FloatingTooltip;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class AlbumDirNormalizeStrategy extends IAppStrategy {
    private final ComboBox<String> cbTemplate;
    private final TextField txtCustomTemplate;
    private final CheckBox chkCleanSpecialChars;
    private final CheckBox chkRemoveYearPrefix;
    private final CheckBox chkUseConsensusMetadata;
    private final CheckBox chkPreserveOriginalName;
    private final CheckBox chkValidateAlbumInfo;
    
    protected String pTemplate;
    protected String pCustomTemplate;
    protected boolean pCleanSpecialChars;
    protected boolean pRemoveYearPrefix;
    protected boolean pUseConsensusMetadata;
    protected boolean pPreserveOriginalName;
    protected boolean pValidateAlbumInfo;

    private static final String[] PRESET_TEMPLATES = {
        "%artist% - %year% - %album%",
        "[%year%] %artist% - %album%",
        "%artist%/%album% (%year%)",
        "%year% - %album% - %artist%",
        "%album% - %artist% [%year%]",
        "%artist% - %album%",
        "%album% (%year%)",
        "自定义模板"
    };

    public AlbumDirNormalizeStrategy() {
        txtCustomTemplate = new TextField();
        txtCustomTemplate.setPromptText("输入自定义模板，如: %year% %album%");
        txtCustomTemplate.setDisable(true);
        
        ArrayList<String> customTemplateTooltipLines = new ArrayList<>();
        customTemplateTooltipLines.add("参数名称：自定义模板");
        customTemplateTooltipLines.add("参数用途：当选择自定义模板时，在此输入自定义命名规则");
        customTemplateTooltipLines.add("示例：");
        customTemplateTooltipLines.add("- %year% %album%");
        customTemplateTooltipLines.add("- %artist%/[%year%] %album%");
        FloatingTooltip.bindToNode(txtCustomTemplate, "专辑目录标准化设置", customTemplateTooltipLines);
        
        cbTemplate = new ComboBox<>();
        cbTemplate.getItems().addAll(PRESET_TEMPLATES);
        cbTemplate.getSelectionModel().select(0);
        cbTemplate.setOnAction(e -> {
            if ("自定义模板".equals(cbTemplate.getValue())) {
                txtCustomTemplate.setDisable(false);
            } else {
                txtCustomTemplate.setDisable(true);
                txtCustomTemplate.setText(cbTemplate.getValue());
            }
        });
        
        ArrayList<String> templateTooltipLines = new ArrayList<>();
        templateTooltipLines.add("参数名称：目录命名模板");
        templateTooltipLines.add("参数用途：用于设置专辑目录的命名模板");
        templateTooltipLines.add("支持变量：");
        templateTooltipLines.add("- %artist%：艺术家名称");
        templateTooltipLines.add("- %album%：专辑名称");
        templateTooltipLines.add("- %year%：发行年份");
        templateTooltipLines.add("- %genre%：音乐流派");
        templateTooltipLines.add("示例：");
        templateTooltipLines.add("- %artist% - %year% - %album%");
        templateTooltipLines.add("- [%year%] %artist% - %album%");
        FloatingTooltip.bindToNode(cbTemplate, "专辑目录标准化设置", templateTooltipLines);

        chkCleanSpecialChars = new CheckBox("清理特殊字符");
        chkCleanSpecialChars.setSelected(true);
        
        ArrayList<String> cleanSpecialCharsTooltipLines = new ArrayList<>();
        cleanSpecialCharsTooltipLines.add("参数名称：清理特殊字符");
        cleanSpecialCharsTooltipLines.add("参数用途：移除目录名称中的特殊字符，如 /\\:*?\"<>|");
        cleanSpecialCharsTooltipLines.add("示例：");
        cleanSpecialCharsTooltipLines.add("- 启用：将 'Artist: Album' 转换为 'Artist - Album'");
        cleanSpecialCharsTooltipLines.add("- 禁用：保留原始特殊字符");
        FloatingTooltip.bindToNode(chkCleanSpecialChars, "专辑目录标准化设置", cleanSpecialCharsTooltipLines);

        chkRemoveYearPrefix = new CheckBox("移除年份前缀");
        chkRemoveYearPrefix.setSelected(true);
        
        ArrayList<String> removeYearPrefixTooltipLines = new ArrayList<>();
        removeYearPrefixTooltipLines.add("参数名称：移除年份前缀");
        removeYearPrefixTooltipLines.add("参数用途：移除目录名称开头的年份前缀（如 2024-）");
        removeYearPrefixTooltipLines.add("示例：");
        removeYearPrefixTooltipLines.add("- 启用：将 '2024-Album Name' 转换为 'Album Name'");
        removeYearPrefixTooltipLines.add("- 禁用：保留年份前缀");
        FloatingTooltip.bindToNode(chkRemoveYearPrefix, "专辑目录标准化设置", removeYearPrefixTooltipLines);

        chkUseConsensusMetadata = new CheckBox("使用共识元数据");
        chkUseConsensusMetadata.setSelected(true);
        
        ArrayList<String> useConsensusMetadataTooltipLines = new ArrayList<>();
        useConsensusMetadataTooltipLines.add("参数名称：使用共识元数据");
        useConsensusMetadataTooltipLines.add("参数用途：从目录内所有音频文件中提取元数据，使用出现频率最高的值");
        useConsensusMetadataTooltipLines.add("示例：");
        useConsensusMetadataTooltipLines.add("- 启用：统计所有文件的元数据，选择最一致的值");
        useConsensusMetadataTooltipLines.add("- 禁用：仅使用第一个文件的元数据");
        FloatingTooltip.bindToNode(chkUseConsensusMetadata, "专辑目录标准化设置", useConsensusMetadataTooltipLines);

        chkPreserveOriginalName = new CheckBox("保留原始目录名作为备份");
        chkPreserveOriginalName.setSelected(false);
        
        ArrayList<String> preserveOriginalNameTooltipLines = new ArrayList<>();
        preserveOriginalNameTooltipLines.add("参数名称：保留原始目录名");
        preserveOriginalNameTooltipLines.add("参数用途：在重命名前创建原始目录名的备份");
        preserveOriginalNameTooltipLines.add("示例：");
        preserveOriginalNameTooltipLines.add("- 启用：创建 'OriginalName.bak' 文件");
        preserveOriginalNameTooltipLines.add("- 禁用：不创建备份");
        FloatingTooltip.bindToNode(chkPreserveOriginalName, "专辑目录标准化设置", preserveOriginalNameTooltipLines);

        chkValidateAlbumInfo = new CheckBox("验证专辑信息完整性");
        chkValidateAlbumInfo.setSelected(true);
        
        ArrayList<String> validateAlbumInfoTooltipLines = new ArrayList<>();
        validateAlbumInfoTooltipLines.add("参数名称：验证专辑信息");
        validateAlbumInfoTooltipLines.add("参数用途：检查专辑信息的完整性，跳过信息不完整的目录");
        validateAlbumInfoTooltipLines.add("示例：");
        validateAlbumInfoTooltipLines.add("- 启用：跳过缺少艺术家或专辑名称的目录");
        validateAlbumInfoTooltipLines.add("- 禁用：处理所有目录，即使信息不完整");
        FloatingTooltip.bindToNode(chkValidateAlbumInfo, "专辑目录标准化设置", validateAlbumInfoTooltipLines);
    }

    @Override
    public String getName() {
        return "专辑目录标准化";
    }

    @Override
    public void captureParams() {
        pTemplate = cbTemplate.getValue();
        pCustomTemplate = txtCustomTemplate.getText();
        pCleanSpecialChars = chkCleanSpecialChars.isSelected();
        pRemoveYearPrefix = chkRemoveYearPrefix.isSelected();
        pUseConsensusMetadata = chkUseConsensusMetadata.isSelected();
        pPreserveOriginalName = chkPreserveOriginalName.isSelected();
        pValidateAlbumInfo = chkValidateAlbumInfo.isSelected();
    }

    @Override
    public String getDescription() {
        return "智能规范化专辑目录名称，支持多种命名模板、元数据提取、特殊字符清理等功能。";
    }

    @Override
    public void saveConfig(Properties props) {
        props.setProperty("adn_template", cbTemplate.getValue());
        props.setProperty("adn_custom_template", txtCustomTemplate.getText());
        props.setProperty("adn_clean_special", String.valueOf(chkCleanSpecialChars.isSelected()));
        props.setProperty("adn_remove_year", String.valueOf(chkRemoveYearPrefix.isSelected()));
        props.setProperty("adn_consensus", String.valueOf(chkUseConsensusMetadata.isSelected()));
        props.setProperty("adn_preserve", String.valueOf(chkPreserveOriginalName.isSelected()));
        props.setProperty("adn_validate", String.valueOf(chkValidateAlbumInfo.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        if (props.containsKey("adn_template")) {
            cbTemplate.getSelectionModel().select(props.getProperty("adn_template"));
        }
        if (props.containsKey("adn_custom_template")) {
            txtCustomTemplate.setText(props.getProperty("adn_custom_template"));
        }
        if (props.containsKey("adn_clean_special")) {
            chkCleanSpecialChars.setSelected(Boolean.parseBoolean(props.getProperty("adn_clean_special")));
        }
        if (props.containsKey("adn_remove_year")) {
            chkRemoveYearPrefix.setSelected(Boolean.parseBoolean(props.getProperty("adn_remove_year")));
        }
        if (props.containsKey("adn_consensus")) {
            chkUseConsensusMetadata.setSelected(Boolean.parseBoolean(props.getProperty("adn_consensus")));
        }
        if (props.containsKey("adn_preserve")) {
            chkPreserveOriginalName.setSelected(Boolean.parseBoolean(props.getProperty("adn_preserve")));
        }
        if (props.containsKey("adn_validate")) {
            chkValidateAlbumInfo.setSelected(Boolean.parseBoolean(props.getProperty("adn_validate")));
        }
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }

    @Override
    public Node getConfigNode() {
        return StyleFactory.createVBoxPanel(
                StyleFactory.createParamPairLine("命名模板:", cbTemplate),
                StyleFactory.createParamPairLine("自定义模板:", txtCustomTemplate),
                StyleFactory.createHBox(chkCleanSpecialChars, chkRemoveYearPrefix),
                StyleFactory.createHBox(chkUseConsensusMetadata, chkPreserveOriginalName),
                StyleFactory.createHBox(chkValidateAlbumInfo)
        );
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        if (rec.getOpType() == OperationType.ALBUM_RENAME) {
            File s = rec.getFileHandle();
            File t = new File(rec.getNewPath());
            if (s.equals(t)) return;
            
            if (pPreserveOriginalName) {
                File backupFile = new File(s.getParent(), s.getName() + ".bak");
                if (!backupFile.exists()) {
                    Files.write(backupFile.toPath(), s.getName().getBytes());
                }
            }
            
            if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
            Files.move(s.toPath(), t.toPath());
        }
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord changeRecord, List<ChangeRecord> inputRecords, List<File> rootDirs) {
        if (changeRecord.getFileHandle().isFile()) {
            return Collections.emptyList();
        }
        String parentPath = changeRecord.getFileHandle().getParentFile().getAbsolutePath();
        List<ChangeRecord> dirFiles = getFilesUnderDir(changeRecord.getFileHandle(), inputRecords).stream()
                .filter(file -> FileTypeUtil.isMusicFile(file.getFileHandle())).collect(Collectors.toList());
        File parentDir = new File(parentPath);

        if (dirFiles.isEmpty()) {
            return Collections.emptyList();
        }

        MetadataHelper.AudioMeta consensus;
        if (pUseConsensusMetadata) {
            consensus = extractConsensusMetadata(dirFiles);
        } else {
            consensus = MetadataHelper.getSmartMetadata(dirFiles.get(0).getFileHandle(), false);
        }

        if (pValidateAlbumInfo) {
            if (consensus.getArtist().isEmpty() || consensus.getArtist().equals("Unknown Artist") ||
                consensus.getAlbum().isEmpty() || consensus.getAlbum().equals("Unknown Album")) {
                log("跳过目录（元数据不完整）: " + parentDir.getName());
                return Collections.emptyList();
            }
        }

        String template = "自定义模板".equals(pTemplate) ? pCustomTemplate : pTemplate;
        if (template == null || template.trim().isEmpty()) {
            template = "%artist% - %year% - %album%";
        }

        String newDirName = MetadataHelper.format(template, consensus);
        
        if (pCleanSpecialChars) {
            newDirName = cleanDirectoryName(newDirName);
        }
        
        if (pRemoveYearPrefix) {
            newDirName = removeYearPrefix(newDirName);
        }
        
        newDirName = newDirName.trim();
        if (newDirName.endsWith(" - ")) {
            newDirName = newDirName.substring(0, newDirName.length() - 3);
        }

        if (!parentDir.getName().equals(newDirName)) {
            changeRecord.setChanged(true);
            changeRecord.setOpType(OperationType.ALBUM_RENAME);
            changeRecord.setNewPath(newDirName);
            changeRecord.setStatus(ExecStatus.PENDING);
        }
        return Collections.emptyList();
    }

    private MetadataHelper.AudioMeta extractConsensusMetadata(List<ChangeRecord> dirFiles) {
        Map<String, Integer> artists = new HashMap<>();
        Map<String, Integer> albums = new HashMap<>();
        Map<String, Integer> years = new HashMap<>();
        Map<String, Integer> genres = new HashMap<>();

        for (ChangeRecord rec : dirFiles) {
            MetadataHelper.AudioMeta meta = MetadataHelper.getSmartMetadata(rec.getFileHandle(), false);
            
            if (!meta.getArtist().isEmpty()) {
                artists.merge(normalizeArtistName(meta.getArtist()), 1, Integer::sum);
            }
            if (!meta.getAlbum().isEmpty()) {
                albums.merge(normalizeAlbumName(meta.getAlbum()), 1, Integer::sum);
            }
            if (!meta.getYear().isEmpty()) {
                years.merge(meta.getYear(), 1, Integer::sum);
            }
            if (!meta.getGenre().isEmpty()) {
                genres.merge(meta.getGenre(), 1, Integer::sum);
            }
        }

        String bestArtist = getTopKey(artists, "Unknown Artist");
        String bestAlbum = getTopKey(albums, "Unknown Album");
        String bestYear = getTopKey(years, "");
        String bestGenre = getTopKey(genres, "");

        MetadataHelper.AudioMeta consensus = new MetadataHelper.AudioMeta();
        consensus.setArtist(bestArtist);
        consensus.setAlbum(bestAlbum);
        consensus.setYear(bestYear);
        consensus.setGenre(bestGenre);

        return consensus;
    }

    private String normalizeArtistName(String artist) {
        if (artist == null || artist.isEmpty()) {
            return "";
        }
        String normalized = artist.trim();
        normalized = normalized.replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("、", ",");
        normalized = normalized.replaceAll("，", ",");
        normalized = normalized.replaceAll("&", ",");
        normalized = normalized.replaceAll("feat\\..*", "");
        normalized = normalized.replaceAll("ft\\..*", "");
        normalized = normalized.replaceAll("\\(.*\\)", "");
        normalized = normalized.replaceAll("\\[.*\\]", "");
        return normalized.trim();
    }

    private String normalizeAlbumName(String album) {
        if (album == null || album.isEmpty()) {
            return "";
        }
        String normalized = album.trim();
        normalized = normalized.replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("\\(.*\\)", "");
        normalized = normalized.replaceAll("\\[.*\\]", "");
        normalized = normalized.replaceAll("-\\s*CD\\s*\\d+", "");
        normalized = normalized.replaceAll("-\\s*Disc\\s*\\d+", "");
        normalized = normalized.replaceAll("-\\s*Vol\\.?\\s*\\d+", "");
        return normalized.trim();
    }

    private String cleanDirectoryName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String cleaned = name;
        cleaned = cleaned.replaceAll("[\\\\/:*?\"<>|]", "-");
        cleaned = cleaned.replaceAll("\\s+", " ");
        cleaned = cleaned.replaceAll("[-_]{2,}", "-");
        cleaned = cleaned.trim();
        return cleaned;
    }

    private String removeYearPrefix(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String cleaned = name;
        cleaned = cleaned.replaceAll("^\\d{4}[-\\s]+", "");
        cleaned = cleaned.replaceAll("^\\d{4}\\.\\s+", "");
        return cleaned.trim();
    }

    private String getTopKey(Map<String, Integer> map, String def) {
        return map.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(def);
    }
}
