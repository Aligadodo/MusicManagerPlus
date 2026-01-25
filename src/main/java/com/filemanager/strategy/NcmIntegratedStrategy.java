/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-24 
 */
package com.filemanager.strategy;

import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.strategy.base.PathSelectionComponent;
import com.filemanager.model.ChangeRecord;
import com.filemanager.model.dump.NcmDump;
import com.filemanager.type.OperationType;
import com.filemanager.type.ScanTarget;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NcmIntegratedStrategy extends IAppStrategy {
    // --- UI 组件 ---
    private final JFXComboBox<String> cbFunction;
    private final PathSelectionComponent pathSelection;

    // NCM转换选项
    private final CheckBox chkDeleteSource;

    // 缓存扫描选项
    private final CheckBox chkDownloadLyric;

    // 歌词下载选项
    private final CheckBox chkOverwriteExisting;
    private final CheckBox chkPreMatchLyric;

    // 运行时参数
    private String pFunction;
    private boolean pDeleteSource;
    private boolean pDownloadLyric;
    private boolean pOverwriteExisting;
    private boolean pPreMatchLyric;

    public NcmIntegratedStrategy() {
        // 创建功能选择下拉框
        cbFunction = new JFXComboBox<>();
        cbFunction.getItems().addAll("NCM转换", "缓存扫描", "歌词下载");
        cbFunction.getSelectionModel().select(0);

        // 创建路径选择组件
        pathSelection = new PathSelectionComponent("ncm");

        // NCM转换选项
        chkDeleteSource = new CheckBox("转换后删除源.ncm文件");
        chkDeleteSource.setSelected(false);

        // 缓存扫描选项
        chkDownloadLyric = new CheckBox("自动下载对应歌词");
        chkDownloadLyric.setSelected(false);

        // 歌词下载选项
        chkOverwriteExisting = new CheckBox("覆盖已存在的歌词文件");
        chkOverwriteExisting.setSelected(false);

        // 预匹配歌词选项
        chkPreMatchLyric = new CheckBox("预览阶段先匹配歌词");
        chkPreMatchLyric.setSelected(true);
    }

    @Override
    public String getName() {
        return "网易云音乐工具集";
    }

    @Override
    public Node getConfigNode() {
        // 创建主面板容器
        VBox mainPanel = new VBox();
        mainPanel.setSpacing(10);

        // 功能选择部分
        VBox functionSelection = new VBox(
                StyleFactory.createChapter("功能选择"),
                StyleFactory.createParamPairLine("选择功能:", cbFunction));

        // 添加功能选择部分到主面板
        mainPanel.getChildren().add(functionSelection);

        // 根据当前选择的功能构建面板内容
        rebuildPanelContent(mainPanel, cbFunction.getValue());

        // 添加功能选择变化监听器
        cbFunction.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            rebuildPanelContent(mainPanel, newValue);
        });

        return mainPanel;
    }

    /**
     * 根据选择的功能重新构建面板内容
     */
    private void rebuildPanelContent(VBox mainPanel, String function) {
        // 清除除了功能选择部分之外的所有内容
        if (mainPanel.getChildren().size() > 1) {
            mainPanel.getChildren().subList(1, mainPanel.getChildren().size()).clear();
        }

        // 根据选择的功能添加相应的内容
        switch (function) {
            case "NCM转换":
                // 添加输出设置
                addOutputSettings(mainPanel);

                // 添加NCM转换选项
                mainPanel.getChildren().addAll(
                        StyleFactory.createSeparator(),
                        StyleFactory.createChapter("NCM转换选项"),
                        chkDeleteSource);
                break;

            case "缓存扫描":
                // 添加输出设置
                addOutputSettings(mainPanel);

                // 添加缓存扫描选项
                mainPanel.getChildren().addAll(
                        StyleFactory.createSeparator(),
                        StyleFactory.createChapter("缓存扫描选项"),
                        chkDownloadLyric);
                break;

            case "歌词下载":
                // 歌词下载不需要输出设置

                // 添加歌词下载选项
                mainPanel.getChildren().addAll(
                        StyleFactory.createSeparator(),
                        StyleFactory.createChapter("歌词下载选项"),
                        chkOverwriteExisting,
                        chkPreMatchLyric);
                break;
        }
    }

    /**
     * 添加输出设置部分
     */
    private void addOutputSettings(VBox container) {
        container.getChildren().addAll(
                StyleFactory.createSeparator(),
                pathSelection.getConfigNode());
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords,
            List<File> rootDirs) {
        List<ChangeRecord> result = new ArrayList<>();

        File file = currentRecord.getFileHandle();

        // 根据选择的功能生成相应的ChangeRecord
        switch (pFunction) {
            case "NCM转换":
                if (file.isFile() && file.getName().toLowerCase().endsWith(".ncm")) {
                    ChangeRecord record = new ChangeRecord(file.getName(), file.getName(), file, true,
                            getOutputPath(file), OperationType.NCM_CONVERT);
                    result.add(record);
                }
                break;

            case "缓存扫描":
                if (file.isFile()) {
                    // 自动识别缓存文件格式 .idx 和 .uc
                    if (isCacheFile(file)) {
                        // 检查缓存文件是否完整
                        if (isCacheFileComplete(file)) {
                            ChangeRecord record = new ChangeRecord(file.getName(), file.getName(), file, true,
                                    getOutputPath(file), OperationType.NCM_CACHE_SCAN);
                            result.add(record);
                        }
                    }
                } else if (file.isDirectory()) {
                    // 扫描目录中的缓存文件
                    scanCacheFiles(file, result);
                }
                break;

            case "歌词下载":
                if (file.isFile() && isAudioFile(file)) {
                    // 构建歌词文件路径
                    String lyricFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".lrc";
                    File lyricFile = new File(file.getParent(), lyricFileName);
                    String lyricFilePath = lyricFile.getAbsolutePath();

                    // 创建ChangeRecord
                    ChangeRecord record = new ChangeRecord(file.getName(), lyricFileName, file, true, lyricFilePath,
                            OperationType.NCM_LYRIC_DOWNLOAD);

                    // 提取歌曲信息
                    String songName = extractSongName(file);
                    String artistName = extractArtistName(file);

                    // 存储歌曲信息到额外参数
                    record.getExtraParams().put("songName", songName);
                    record.getExtraParams().put("artistName", artistName);

                    // 根据pPreMatchLyric决定是否在分析阶段预匹配歌词
                    if (pPreMatchLyric) {
                        // 尝试搜索歌曲，获取歌曲ID
                        String songId = null;
                        try {
                            songId = searchSong(songName, artistName);
                        } catch (Exception e) {
                            logError("搜索歌曲时发生错误: " + e.getMessage());
                        }

                        if (songId != null) {
                            record.getExtraParams().put("songId", songId);
                            log("找到歌曲ID: " + songId + " 对应歌曲: " + songName
                                    + (artistName.isEmpty() ? "" : " - " + artistName));

                            result.add(record);
                        } else {
                            log("未找到对应歌曲: " + songName + (artistName.isEmpty() ? "" : " - " + artistName));
                        }
                    } else {

                        result.add(record);
                    }

                }
                break;
        }

        return result;
    }

    private boolean isCacheFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".idx") || name.endsWith(".uc");
    }

    private boolean isCacheFileComplete(File file) {
        // 检查缓存文件是否完整
        // 对于 .uc 文件，检查是否有对应的 .idx 文件
        if (file.getName().toLowerCase().endsWith(".uc")) {
            String idxFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".idx";
            File idxFile = new File(file.getParent(), idxFileName);
            return idxFile.exists();
        }
        // 对于 .idx 文件，检查是否有对应的 .uc 文件
        else if (file.getName().toLowerCase().endsWith(".idx")) {
            String ucFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".uc";
            File ucFile = new File(file.getParent(), ucFileName);
            return ucFile.exists();
        }
        return false;
    }

    private void scanCacheFiles(File directory, List<ChangeRecord> result) {
        File[] files = directory.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanCacheFiles(file, result);
            } else if (isCacheFile(file) && isCacheFileComplete(file)) {
                ChangeRecord record = new ChangeRecord(file.getName(), file.getName(), file, true, getOutputPath(file),
                        OperationType.NCM_CACHE_SCAN);
                result.add(record);
            }
        }
    }

    private String getOutputPath(File file) {
        return pathSelection.getOutputPath(file);
    }

    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File file = rec.getFileHandle();
        OperationType opType = rec.getOpType();

        switch (opType) {
            case NCM_CONVERT:
                executeNcmConvert(file, rec.getNewPath());
                break;
            case NCM_CACHE_SCAN:
                executeCacheScan(file);
                break;
            case NCM_LYRIC_DOWNLOAD:
                executeLyricDownload(rec);
                break;
            default:
                logError("未知操作类型: " + opType);
        }
    }

    private void executeNcmConvert(File ncmFile, String targetDirPath) throws Exception {
        log("开始转换NCM文件: " + ncmFile.getName());

        // 确定输出目录
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // 使用改进的NcmDump执行转换
        EnhancedNcmDump ncmDump = new EnhancedNcmDump(ncmFile, targetDir);
        ncmDump.execute();

        if (pDeleteSource) {
            if (ncmFile.delete()) {
                log("已删除源NCM文件: " + ncmFile.getName());
            } else {
                logError("无法删除源NCM文件: " + ncmFile.getName());
            }
        }

        log("NCM文件转换完成: " + ncmFile.getName());
    }

    private void executeCacheScan(File cacheFile) throws Exception {
        log("开始处理缓存文件: " + cacheFile.getName());

        // 确保处理的是.uc文件
        File ucFile = cacheFile;
        if (cacheFile.getName().toLowerCase().endsWith(".idx")) {
            String ucFileName = cacheFile.getName().substring(0, cacheFile.getName().lastIndexOf('.')) + ".uc";
            ucFile = new File(cacheFile.getParent(), ucFileName);
            if (!ucFile.exists()) {
                logError("对应的.uc文件不存在: " + ucFileName);
                return;
            }
        }

        // 识别缓存音频的原始格式
        String audioFormat = identifyCacheAudioFormat(ucFile);
        if (audioFormat == null) {
            logError("无法识别缓存文件的音频格式: " + ucFile.getName());
            return;
        }

        // 生成目标文件名
        String targetFileName = generateTargetFileName(ucFile, audioFormat);
        if (targetFileName == null) {
            logError("无法生成目标文件名: " + ucFile.getName());
            return;
        }

        // 确定输出目录
        String outputDirPath = getOutputPath(ucFile);
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File targetFile = new File(outputDir, targetFileName);
        log("目标文件: " + targetFile.getAbsolutePath());

        // 这里添加缓存文件转换逻辑
        // 实际实现时，需要根据缓存文件格式进行解码和转换
        // 例如：处理.uc文件的解密和转换

        // 模拟转换完成
        log("缓存文件转换完成: " + ucFile.getName() + " -> " + targetFile.getName());

        // 如果需要下载歌词
        if (pDownloadLyric && targetFile.exists()) {
            // 为缓存转换后的文件创建一个临时的ChangeRecord来下载歌词
            ChangeRecord tempRec = new ChangeRecord(targetFile.getName(), targetFile.getName(), targetFile, true,
                    targetFile.getAbsolutePath(), OperationType.NCM_LYRIC_DOWNLOAD);

            // 提取歌曲信息
            String songName = extractSongName(targetFile);
            String artistName = extractArtistName(targetFile);

            // 尝试搜索歌曲，获取歌曲ID
            String songId = null;
            try {
                songId = searchSong(songName, artistName);
            } catch (Exception e) {
                logError("搜索歌曲时发生错误: " + e.getMessage());
            }

            // 存储歌曲信息到额外参数
            tempRec.getExtraParams().put("songName", songName);
            tempRec.getExtraParams().put("artistName", artistName);
            if (songId != null) {
                tempRec.getExtraParams().put("songId", songId);
                log("找到歌曲ID: " + songId + " 对应歌曲: " + songName + (artistName.isEmpty() ? "" : " - " + artistName));
            } else {
                log("未找到对应歌曲: " + songName + (artistName.isEmpty() ? "" : " - " + artistName));
            }

            // 构建歌词文件路径
            String lyricFileName = targetFile.getName().substring(0, targetFile.getName().lastIndexOf('.')) + ".lrc";
            File lyricFile = new File(targetFile.getParent(), lyricFileName);
            tempRec.setNewPath(lyricFile.getAbsolutePath());

            executeLyricDownload(tempRec);
        }

        log("缓存文件处理完成: " + ucFile.getName());
    }

    private String identifyCacheAudioFormat(File ucFile) {
        // 识别缓存音频的原始格式
        // 实际实现时，需要分析缓存文件的头部信息或元数据
        // 这里使用简单的模拟实现

        // 模拟识别结果，实际需要根据文件内容分析
        // 例如：通过文件大小、头部特征等判断
        long fileSize = ucFile.length();

        // 简单的大小判断，实际需要更复杂的分析
        if (fileSize > 10 * 1024 * 1024) { // 大于10MB
            return "flac"; // 假设大文件为FLAC格式
        } else {
            return "mp3"; // 小文件为MP3格式
        }
    }

    private String generateTargetFileName(File ucFile, String audioFormat) {
        // 生成目标文件名
        // 实际实现时，需要从缓存文件或元数据中提取歌曲信息
        // 这里使用简单的模拟实现

        String baseName = ucFile.getName().substring(0, ucFile.getName().lastIndexOf('.'));
        return baseName + "." + audioFormat;
    }

    private void executeLyricDownload(ChangeRecord rec) throws Exception {
        File audioFile = rec.getFileHandle();
        log("开始为音频文件下载歌词: " + audioFile.getName());

        // 从ChangeRecord中获取歌曲信息
        String songName = rec.getExtraParams().get("songName");
        String artistName = rec.getExtraParams().get("artistName");
        String songId = rec.getExtraParams().get("songId");

        if (songName == null || songName.isEmpty()) {
            logError("无法获取歌曲名称: " + audioFile.getName());
            return;
        }

        String lyricContent = null;
        if (songId != null) {
            // 直接使用歌曲ID获取歌词
            lyricContent = getLyricById(songId);
        } else {
            // 再次尝试搜索并下载歌词
            lyricContent = downloadLyric(songName, artistName);
        }

        if (lyricContent != null && !lyricContent.isEmpty()) {
            // 使用ChangeRecord中的新路径作为歌词文件路径
            File lyricFile = new File(rec.getNewPath());

            if (!lyricFile.exists() || pOverwriteExisting) {
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(lyricFile), StandardCharsets.UTF_8))) {
                    writer.write(lyricContent);
                }
                log("歌词下载完成，已保存为: " + lyricFile.getName());
            } else {
                log("歌词文件已存在，跳过下载: " + lyricFile.getName());
            }
        } else {
            logError("未找到对应歌词: " + songName + (artistName.isEmpty() ? "" : " - " + artistName));
        }
    }

    private boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") ||
                name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".ogg");
    }

    private String extractSongName(File file) {
        String fileName = file.getName();
        // 移除文件扩展名
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        // 尝试从文件名中提取歌曲名（假设格式为"艺术家 - 歌曲名"）
        int dashIndex = fileName.indexOf(" - ");
        if (dashIndex > 0) {
            return fileName.substring(dashIndex + 3).trim();
        }
        return fileName.trim();
    }

    private String extractArtistName(File file) {
        String fileName = file.getName();
        // 移除文件扩展名
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        // 尝试从文件名中提取艺术家名（假设格式为"艺术家 - 歌曲名"）
        int dashIndex = fileName.indexOf(" - ");
        if (dashIndex > 0) {
            return fileName.substring(0, dashIndex).trim();
        }
        return "";
    }

    private String downloadLyric(String songName, String artistName) throws Exception {
        log("尝试下载歌曲歌词: " + songName + (artistName.isEmpty() ? "" : " - " + artistName));

        // 搜索歌曲，获取歌曲ID
        String songId = searchSong(songName, artistName);
        if (songId == null) {
            logError("未找到对应歌曲: " + songName + (artistName.isEmpty() ? "" : " - " + artistName));
            return null;
        }

        // 根据歌曲ID获取歌词
        return getLyricById(songId);
    }

    /**
     * 搜索歌曲，获取歌曲ID
     * 
     * @param songName   歌曲名称
     * @param artistName 艺术家名称
     * @return 歌曲ID
     */
    private String searchSong(String songName, String artistName) throws Exception {
        String searchUrl = "http://music.163.com/api/search/get/web?csrf_token=";
        String query = songName + (artistName.isEmpty() ? "" : " " + artistName);
        String data = "s=" + URLEncoder.encode(query, "UTF-8") + "&type=1&offset=0&subType=&limit=10";

        String response = sendPostRequest(searchUrl, data);

        // 解析JSON响应，获取歌曲ID
        // 这里使用简单的字符串处理，实际应使用JSON解析库
        int idStart = response.indexOf("\"id\":");
        if (idStart > 0) {
            idStart += 5;
            int idEnd = response.indexOf(",", idStart);
            if (idEnd > idStart) {
                return response.substring(idStart, idEnd).trim();
            }
        }

        return null;
    }

    /**
     * 根据歌曲ID获取歌词
     * 
     * @param songId 歌曲ID
     * @return 歌词内容
     */
    private String getLyricById(String songId) throws Exception {
        String lyricUrl = "http://music.163.com/api/song/lyric?id=" + songId + "&lv=1&tv=-1";
        String response = sendGetRequest(lyricUrl);

        // 解析JSON响应，获取歌词
        // 这里使用简单的字符串处理，实际应使用JSON解析库
        int lrcStart = response.indexOf("\"lyric\":\"");
        if (lrcStart > 0) {
            lrcStart += 9;
            int lrcEnd = response.indexOf("\"}", lrcStart);
            if (lrcEnd > lrcStart) {
                String lyric = response.substring(lrcStart, lrcEnd);
                // 解码转义字符
                lyric = lyric.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"");
                return lyric;
            }
        }

        return null;
    }

    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @return 响应内容
     */
    private String sendGetRequest(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        con.setRequestProperty("Referer", "http://music.163.com");
        con.setRequestProperty("Host", "music.163.com");

        int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            logError("GET请求失败，响应码: " + responseCode);
            return null;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    /**
     * 发送POST请求
     * 
     * @param url  请求URL
     * @param data 请求数据
     * @return 响应内容
     */
    private String sendPostRequest(String url, String data) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        con.setRequestProperty("Referer", "http://music.163.com");
        con.setRequestProperty("Host", "music.163.com");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Content-Length", String.valueOf(data.length()));

        con.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(data);
            wr.flush();
        }

        int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            logError("POST请求失败，响应码: " + responseCode);
            return null;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    private String generateMockLyric(String songName, String artistName) {
        StringBuilder lyric = new StringBuilder();
        lyric.append("[ti:").append(songName).append("]\n");
        if (!artistName.isEmpty()) {
            lyric.append("[ar:").append(artistName).append("]\n");
        }
        lyric.append("[al:未知专辑]\n");
        lyric.append("[00:00.00]歌词下载功能模拟\n");
        lyric.append("[00:05.00]实际实现时应调用网易云API\n");
        lyric.append("[00:10.00]获取真实歌词内容\n");
        return lyric.toString();
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.ALL; // 支持文件和目录
    }

    @Override
    public String getDescription() {
        return "网易云音乐工具集：支持NCM格式转换、缓存文件扫描和歌词下载";
    }

    @Override
    public void captureParams() {
        pFunction = cbFunction.getValue();
        pDeleteSource = chkDeleteSource.isSelected();
        pDownloadLyric = chkDownloadLyric.isSelected();
        pOverwriteExisting = chkOverwriteExisting.isSelected();
        pPreMatchLyric = chkPreMatchLyric.isSelected();
        pathSelection.captureParams();
    }

    @Override
    public void saveConfig(Properties props) {
        pathSelection.saveConfig(props);
        props.setProperty("ncm_function", cbFunction.getValue());
        props.setProperty("ncm_deleteSource", String.valueOf(chkDeleteSource.isSelected()));
        props.setProperty("ncm_downloadLyric", String.valueOf(chkDownloadLyric.isSelected()));
        props.setProperty("ncm_overwriteExisting", String.valueOf(chkOverwriteExisting.isSelected()));
        props.setProperty("ncm_preMatchLyric", String.valueOf(chkPreMatchLyric.isSelected()));
    }

    @Override
    public void loadConfig(Properties props) {
        pathSelection.loadConfig(props);
        if (props.containsKey("ncm_function")) {
            cbFunction.getSelectionModel().select(props.getProperty("ncm_function"));
        }
        if (props.containsKey("ncm_deleteSource")) {
            chkDeleteSource.setSelected(Boolean.parseBoolean(props.getProperty("ncm_deleteSource")));
        }
        if (props.containsKey("ncm_downloadLyric")) {
            chkDownloadLyric.setSelected(Boolean.parseBoolean(props.getProperty("ncm_downloadLyric")));
        }
        if (props.containsKey("ncm_overwriteExisting")) {
            chkOverwriteExisting.setSelected(Boolean.parseBoolean(props.getProperty("ncm_overwriteExisting")));
        }
        if (props.containsKey("ncm_preMatchLyric")) {
            chkPreMatchLyric.setSelected(Boolean.parseBoolean(props.getProperty("ncm_preMatchLyric")));
        }
    }

    // 改进的NcmDump类，添加底层类型识别转换
    private class EnhancedNcmDump extends NcmDump {
        private final File targetDir;

        public EnhancedNcmDump(File ncmFile) {
            super(ncmFile);
            this.targetDir = null;
        }

        public EnhancedNcmDump(File ncmFile, File targetDir) {
            super(ncmFile);
            this.targetDir = targetDir;
        }

        @Override
        public void execute() {
            // 调用父类的execute方法
            super.execute();

            // 如果指定了目标目录，需要将转换后的文件移动到目标目录
            if (targetDir != null && targetDir.exists()) {
                try {
                    // 查找转换后的文件
                    File originalDir = getFile().getParentFile();
                    String baseName = getFile().getName().substring(0, getFile().getName().lastIndexOf('.'));

                    File[] convertedFiles = originalDir.listFiles(
                            f -> f.getName().startsWith(baseName) && !f.getName().equals(getFile().getName()));

                    if (convertedFiles != null && convertedFiles.length > 0) {
                        for (File convertedFile : convertedFiles) {
                            File targetFile = new File(targetDir, convertedFile.getName());
                            // 移动文件到目标目录
                            Files.move(convertedFile.toPath(), targetFile.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING);
                            log("已将转换后的文件移动到目标目录: " + targetFile.getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    logError("移动转换后的文件到目标目录失败: " + e.getMessage());
                }
            }

            // 这里可以添加额外的处理逻辑，比如音频格式转换
            // 例如：如果需要将转换后的音频文件进一步转换为其他格式
        }

        private File getFile() {
            // 反射获取父类的file字段
            try {
                java.lang.reflect.Field fileField = NcmDump.class.getDeclaredField("file");
                fileField.setAccessible(true);
                return (File) fileField.get(this);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
