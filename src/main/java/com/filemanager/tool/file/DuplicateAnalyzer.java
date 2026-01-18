package com.filemanager.tool.file;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.CleanupParams;
import com.filemanager.strategy.FileCleanupStrategy;
import com.filemanager.type.OperationType;
import com.filemanager.util.LanguageUtil;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import java.text.DecimalFormat;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DuplicateAnalyzer {
    // 常见媒体类型定义，用于同类比较
    private static final Set<String> EXT_AUDIO = new HashSet<>(Arrays.asList("mp3", "flac", "wav", "aac", "m4a", "ogg", "wma", "ape", "alac", "aiff", "dsf", "dff"));
    private static final Set<String> EXT_VIDEO = new HashSet<>(Arrays.asList("mp4", "mkv", "avi", "mov", "wmv", "flv", "m4v", "mpg"));
    private static final Set<String> EXT_IMAGE = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "bmp", "gif", "webp", "tiff"));

    private final CleanupParams params;

    public DuplicateAnalyzer(CleanupParams params) {
        this.params = params;
    }

    public List<ChangeRecord> analyze(File file) {
        if (params.getMode() == FileCleanupStrategy.CleanupMode.REMOVE_EMPTY_DIRS) {
            if (isDirectoryEmpty(file)) {
                return Collections.singletonList(createDeleteRecord(file, "空文件夹 (无子文件)"));
            }
            return Collections.emptyList();
        } else if (params.getMode() == FileCleanupStrategy.CleanupMode.DEDUP_FOLDERS) {
            File[] files = file.listFiles();
            if (file.isFile() || files == null || files.length < 2) {
                return Collections.emptyList();
            }
            return analyzeDuplicateFolders(Arrays.asList(files));
        } else if (params.getMode() == FileCleanupStrategy.CleanupMode.DIRECT_CLEANUP) {
            // 直接清理模式：直接删除文件（应用大小范围过滤）
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files == null) {
                    return Collections.emptyList();
                }
                List<ChangeRecord> results = new ArrayList<>();
                for (File subFile : files) {
                    if (subFile.isFile() && params.getSizeRange().isInRange(subFile.length())) {
                        results.add(createDeleteRecord(subFile, "直接清理文件"));
                    }
                }
                return results;
            } else {
                // 如果是单个文件，直接检查是否符合大小范围并删除
                if (params.getSizeRange().isInRange(file.length())) {
                    return Collections.singletonList(createDeleteRecord(file, "直接清理文件"));
                }
                return Collections.emptyList();
            }
        } else if (params.getMode() == FileCleanupStrategy.CleanupMode.MERGE_SAME_NAME_PARENT_CHILD) {
            // 同名父子文件夹合并模式
            return analyzeMergeSameNameFolders(file);
        } else {
            // 默认是文件去重模式
            File[] files = file.listFiles();
            if (file.isFile() || files == null || files.length < 2) {
                return Collections.emptyList();
            }
            // 应用文件大小范围过滤
            List<File> filteredFiles = Arrays.stream(files)
                    .filter(subFile -> subFile.isFile() && params.getSizeRange().isInRange(subFile.length()))
                    .collect(Collectors.toList());
            if (filteredFiles.size() < 2) {
                return Collections.emptyList();
            }
            return analyzeDuplicateFiles(filteredFiles);
        }
    }

    /**
     * 智能文件去重
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
                String core = m.find() ? m.group(1).trim() : name;
                // 应用文件名预处理
                core = preprocessFilename(core);
                String ext = getExt(name);
                String typeTag = getMediaType(ext);
                return core + "::" + typeTag; // Key: "song::AUDIO"
            }));

            for (List<File> group : nameGroup.values()) {
                if (group.size() < 2) continue; // 无重复

                // 检查是否是音频文件组
                String ext = getExt(group.get(0).getName());
                boolean isAudioGroup = EXT_AUDIO.contains(ext);
                
                List<File> filesToProcess = new ArrayList<>(group);
                
                if (isAudioGroup && params.isAudioSpecial()) {
                    // 音频文件特殊处理：按持续时间二次分组，仅对时间一致的文件去重
                    Map<Long, List<File>> durationGroups = new HashMap<>();
                    
                    for (File f : group) {
                        Map<String, Long> metadata = getAudioMetadata(f);
                        if (metadata != null) {
                            long duration = metadata.get("duration");
                            // 精确匹配持续时间（毫秒级）
                            durationGroups.computeIfAbsent(duration, k -> new ArrayList<>()).add(f);
                        } else {
                            // 无法读取元数据的文件单独处理
                            durationGroups.computeIfAbsent(-1L, k -> new ArrayList<>()).add(f);
                        }
                    }
                    
                    // 对每个持续时间组进行处理
                    for (List<File> durationGroup : durationGroups.values()) {
                        if (durationGroup.size() < 2) continue;
                        
                        // 音频文件特殊选择逻辑：优先保留高质量文件
                        File keeper = Collections.max(durationGroup, (f1, f2) -> {
                            // 1. 优先后缀匹配
                            if (params.getKeepExt() != null && !params.getKeepExt().isEmpty()) {
                                boolean k1 = f1.getName().toLowerCase().endsWith("." + params.getKeepExt().toLowerCase());
                                boolean k2 = f2.getName().toLowerCase().endsWith("." + params.getKeepExt().toLowerCase());
                                if (k1 != k2) {
                                    return k1 ? 1 : -1;
                                }
                            }
                            
                            // 2. 优先比较码率（音频质量）
                            Map<String, Long> meta1 = getAudioMetadata(f1);
                            Map<String, Long> meta2 = getAudioMetadata(f2);
                            
                            if (meta1 != null && meta2 != null) {
                                long bitrate1 = meta1.getOrDefault("bitrate", 0L);
                                long bitrate2 = meta2.getOrDefault("bitrate", 0L);
                                if (bitrate1 != bitrate2) {
                                    return Long.compare(bitrate1, bitrate2);
                                }
                            }
                            
                            // 3. 体积优先
                            if (params.isKeepLargest()) {
                                int sizeCmp = Long.compare(f1.length(), f2.length());
                                if (sizeCmp != 0) {
                                    return sizeCmp;
                                }
                            }
                            
                            // 4. 变更时间优先
                            if (params.isKeepEarliest()) {
                                int timeCmp = Long.compare(f2.lastModified(), f1.lastModified());
                                if (timeCmp != 0) {
                                    return timeCmp;
                                }
                                
                                try {
                                    java.nio.file.attribute.BasicFileAttributes attributes = Files.readAttributes(Paths.get(f1.getPath()), java.nio.file.attribute.BasicFileAttributes.class);
                                    java.nio.file.attribute.BasicFileAttributes attributes2 = Files.readAttributes(Paths.get(f2.getPath()), java.nio.file.attribute.BasicFileAttributes.class);
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
                            
                            // 5. 默认：名字短的优先 (通常不带 (1) 的是原件)
                            int lenCmp = Integer.compare(f2.getName().length(), f1.getName().length());
                            if (lenCmp != 0) {
                                return lenCmp;
                            }
                            
                            // 6. 默认：名字排序靠前的优先 (通常是大写)
                            return StringUtils.compare(f2.getName(), f1.getName(), true);
                        });
                        
                        // 标记要删除的文件
                        for (File f : durationGroup) {
                            if (f == keeper) continue;
                            result.add(createDeleteRecord(f, "重复副本 (与 " + keeper.getName() + " 内容重复)"));
                        }
                    }
                } else {
                    // 非音频文件或未启用音频特殊处理：使用原有逻辑
                    File keeper = Collections.max(group, (f1, f2) -> {
                        // 1. 优先后缀匹配
                        if (params.getKeepExt() != null && !params.getKeepExt().isEmpty()) {
                            boolean k1 = f1.getName().toLowerCase().endsWith("." + params.getKeepExt().toLowerCase());
                            boolean k2 = f2.getName().toLowerCase().endsWith("." + params.getKeepExt().toLowerCase());
                            if (k1 != k2) {
                                return k1 ? 1 : -1;
                            }
                        }

                        // 2. 体积优先
                        if (params.isKeepLargest()) {
                            int sizeCmp = Long.compare(f1.length(), f2.length());
                            if (sizeCmp != 0) {
                                return sizeCmp;
                            }
                        }

                        // 3. 变更时间优先
                        if (params.isKeepEarliest()) {
                            int sizeCmp = Long.compare(f2.lastModified(), f1.lastModified());
                            if (sizeCmp != 0) {
                                return sizeCmp;
                            }

                            try {
                                java.nio.file.attribute.BasicFileAttributes attributes = Files.readAttributes(Paths.get(f1.getPath()), java.nio.file.attribute.BasicFileAttributes.class);
                                java.nio.file.attribute.BasicFileAttributes attributes2 = Files.readAttributes(Paths.get(f2.getPath()), java.nio.file.attribute.BasicFileAttributes.class);
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
        }
        return result;
    }

    /**
     * 同名父子文件夹合并
     */
    private List<ChangeRecord> analyzeMergeSameNameFolders(File file) {
        List<ChangeRecord> result = new ArrayList<>();
        
        // 确保当前文件是目录
        if (!file.isDirectory()) {
            return result;
        }
        
        // 获取当前目录的名称
        String currentDirName = file.getName();
        
        // 遍历当前目录的所有子文件/目录
        File[] files = file.listFiles();
        if (files == null) {
            return result;
        }
        
        // 检查是否存在与当前目录同名的子目录
        for (File child : files) {
            if (child.isDirectory() && child.getName().equals(currentDirName)) {
                // 找到同名子目录，需要合并
                
                // 首先处理同名子目录中的所有文件
                File[] subFiles = child.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        // 创建移动记录
                        String destPath = file.getPath() + File.separator + subFile.getName();
                        File destFile = new File(destPath);
                        
                        // 如果目标文件已存在，需要处理冲突
                        if (destFile.exists()) {
                            // 生成新的文件名
                            String fileName = subFile.getName();
                            String nameWithoutExt = fileName;
                            String ext = "";
                            int dotIndex = fileName.lastIndexOf('.');
                            if (dotIndex > 0) {
                                nameWithoutExt = fileName.substring(0, dotIndex);
                                ext = fileName.substring(dotIndex);
                            }
                            
                            // 生成唯一的文件名
                            int counter = 1;
                            while (destFile.exists()) {
                                destPath = file.getPath() + File.separator + nameWithoutExt + " (" + counter + ")" + ext;
                                destFile = new File(destPath);
                                counter++;
                            }
                        }
                        
                        // 创建移动记录 - 使用DELETE OperationType，但在extraParams中增加标识
                        Map<String, String> params = new HashMap<>();
                        params.put("operation", "merge_move"); // 标识这是合并操作中的移动
                        params.put("destPath", destPath); // 目标文件路径
                        
                        ChangeRecord record = new ChangeRecord(
                                subFile.getName(),
                                "移动文件 " + subFile.getName() + " 到父目录",
                                subFile,
                                true,
                                destPath, // newPath设置为实际的目标文件绝对路径
                                OperationType.DELETE, // 使用DELETE OperationType
                                params, // 使用extraParams传递额外信息
                                null
                        );
                        result.add(record);
                    }
                }
                
                // 添加删除同名子目录的记录
                result.add(createDeleteRecord(child, "删除空的同名子目录"));
                
                // 停止遍历，因为一个目录最多只有一个同名子目录
                break;
            }
        }
        
        return result;
    }
    
    /**
     * 文件夹去重 (内容一致性检查)
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

    private boolean isDirectoryEmpty(File directory) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory.toPath())) {
            return !dirStream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
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
     * 对文件名进行预处理
     * @param filename 原始文件名
     * @return 预处理后的文件名
     */
    private String preprocessFilename(String filename) {
        String processed = filename;
        
        // 繁简中文转换
        if (params.isPreprocessSimplified()) {
            try {
                // 使用LanguageUtil进行中文转换
                processed = LanguageUtil.toSimpleChinese(processed);
            } catch (Exception e) {
                // 如果转换失败，保持原样
                e.printStackTrace();
            }
        }
        
        // 大小写转换（优先级：大写优先于小写）
        if (params.isPreprocessUpper()) {
            processed = processed.toUpperCase();
        } else if (params.isPreprocessLower()) {
            processed = processed.toLowerCase();
        }
        
        return processed;
    }
    
    /**
     * 获取音频文件的元数据
     * @param file 音频文件
     * @return 包含持续时间（毫秒）和码率（kbps）的Map，无法读取时返回null
     */
    private Map<String, Long> getAudioMetadata(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader audioHeader = audioFile.getAudioHeader();
            
            Map<String, Long> metadata = new HashMap<>();
            // 获取持续时间（毫秒）
            metadata.put("duration", audioHeader.getTrackLength() * 1000L);
            // 获取码率（kbps）
            metadata.put("bitrate", audioHeader.getBitRateAsNumber());
            
            return metadata;
        } catch (Exception e) {
            // 忽略无法读取的音频文件
            return null;
        }
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

    private ChangeRecord createDeleteRecord(File f, String reason) {
        Map<String, String> params = new HashMap<>();
        return new ChangeRecord(f.getName(), "[删除] " + f.getName(), f, true, "PERMANENT_DELETE", OperationType.DELETE, params, null);
    }
}