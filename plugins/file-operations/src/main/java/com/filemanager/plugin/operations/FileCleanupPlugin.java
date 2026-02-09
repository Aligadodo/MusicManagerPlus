package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileCleanupPlugin implements IPlugin {
    private static final String MODE_DEDUP_FILES = "dedup_files";
    private static final String MODE_DEDUP_FOLDERS = "dedup_folders";
    private static final String MODE_REMOVE_EMPTY_DIRS = "remove_empty_dirs";
    private static final String MODE_DIRECT_CLEANUP = "direct_cleanup";
    private static final String MODE_MERGE_SAME_NAME = "merge_same_name";
    private static final String MODE_MERGE_NESTED = "merge_nested";
    
    private static final String METHOD_DIRECT_DELETE = "direct_delete";
    private static final String METHOD_PSEUDO_DELETE = "pseudo_delete";
    
    private static final String COMPARISON_METHOD_NAME = "name";
    private static final String COMPARISON_METHOD_SIZE = "size";
    private static final String COMPARISON_METHOD_MD5 = "md5";
    private static final String COMPARISON_METHOD_SHA1 = "sha1";
    private static final String COMPARISON_METHOD_SHA256 = "sha256";
    
    private static final Set<String> EXT_AUDIO = new HashSet<>(Arrays.asList("mp3", "flac", "wav", "aac", "m4a", "ogg", "wma", "ape", "alac", "aiff", "dsf", "dff"));
    private static final Set<String> EXT_VIDEO = new HashSet<>(Arrays.asList("mp4", "mkv", "avi", "mov", "wmv", "flv", "m4v", "mpg"));
    private static final Set<String> EXT_IMAGE = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "bmp", "gif", "webp", "tiff"));
    
    private static final Pattern NORM_PATTERN = Pattern.compile("^(.+?)(\\s*[\\(\\[（].*?[\\)\\]）])?(\\s*-\\s*(副本|Copy))?(\\s*\\(\\d+\\))?(\\.[^.]+)?$");

    @Override
    public String getId() {
        return "file-cleanup";
    }

    @Override
    public String getName() {
        return "文件清理插件";
    }

    @Override
    public String getDescription() {
        return "支持文件去重、文件夹去重、空目录清理、文件夹合并等多种清理模式";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("cleanupMode", MODE_DEDUP_FILES);
        config.setValue("deleteMethod", METHOD_PSEUDO_DELETE);
        config.setValue("trashPath", ".EchoTrash");
        config.setValue("keepLargest", true);
        config.setValue("keepEarliest", true);
        config.setValue("keepExt", "wav");
        config.setValue("comparisonMethod", COMPARISON_METHOD_MD5);
        config.setValue("preprocessLower", true);
        config.setValue("preprocessUpper", false);
        config.setValue("preprocessSimplified", false);
        config.setValue("audioSpecial", true);
        config.setValue("minFileSizeKB", 0);
        config.setValue("maxFileSizeKB", 10240);
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO modeParam = new PluginParameterDTO.Builder()
            .name("cleanupMode")
            .label("清理模式")
            .description("选择清理的逻辑规则")
            .type("select")
            .defaultValue(MODE_DEDUP_FILES)
            .required(true)
            .options(new String[]{
                MODE_DEDUP_FILES,
                MODE_DEDUP_FOLDERS,
                MODE_REMOVE_EMPTY_DIRS,
                MODE_DIRECT_CLEANUP,
                MODE_MERGE_SAME_NAME,
                MODE_MERGE_NESTED
            })
            .build();
        parameters.add(modeParam);
        
        PluginParameterDTO methodParam = new PluginParameterDTO.Builder()
            .name("deleteMethod")
            .label("删除方法")
            .description("选择删除的方式")
            .type("select")
            .defaultValue(METHOD_PSEUDO_DELETE)
            .required(true)
            .options(new String[]{
                METHOD_PSEUDO_DELETE,
                METHOD_DIRECT_DELETE
            })
            .build();
        parameters.add(methodParam);
        
        PluginParameterDTO trashPathParam = new PluginParameterDTO.Builder()
            .name("trashPath")
            .label("回收站路径")
            .description("输入相对名称（如 .del）将在各盘根目录创建；输入绝对路径（如 D:/Trash）则统一移动到该处。")
            .type("text")
            .defaultValue(".EchoTrash")
            .required(false)
            .build();
        parameters.add(trashPathParam);
        
        PluginParameterDTO keepLargestParam = new PluginParameterDTO.Builder()
            .name("keepLargest")
            .label("保留体积/质量最佳的副本")
            .description("勾选：保留最大的文件；不勾选：保留名字最短（通常是原件）的文件")
            .type("boolean")
            .defaultValue(true)
            .required(false)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FILES)
            .build();
        parameters.add(keepLargestParam);
        
        PluginParameterDTO keepEarliestParam = new PluginParameterDTO.Builder()
            .name("keepEarliest")
            .label("保留日期最早/最晚的副本")
            .description("勾选：保留日期最早的文件(夹)；不勾选：保留最新的文件(夹)")
            .type("boolean")
            .defaultValue(true)
            .required(false)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FILES)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FOLDERS)
            .addVisibilityCondition("cleanupMode", MODE_MERGE_SAME_NAME)
            .addVisibilityCondition("cleanupMode", MODE_MERGE_NESTED)
            .build();
        parameters.add(keepEarliestParam);
        
        PluginParameterDTO keepExtParam = new PluginParameterDTO.Builder()
            .name("keepExt")
            .label("优先后缀")
            .description("用于设置去重时优先保留的文件后缀")
            .type("text")
            .defaultValue("wav")
            .required(false)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FILES)
            .build();
        parameters.add(keepExtParam);
        
        PluginParameterDTO comparisonMethodParam = new PluginParameterDTO.Builder()
            .name("comparisonMethod")
            .label("比较方法")
            .description("选择文件比较方法")
            .type("select")
            .defaultValue(COMPARISON_METHOD_MD5)
            .required(false)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FILES)
            .options(new String[]{
                COMPARISON_METHOD_NAME,
                COMPARISON_METHOD_SIZE,
                COMPARISON_METHOD_MD5,
                COMPARISON_METHOD_SHA1,
                COMPARISON_METHOD_SHA256
            })
            .build();
        parameters.add(comparisonMethodParam);
        
        PluginParameterDTO preprocessLowerParam = new PluginParameterDTO.Builder()
            .name("preprocessLower")
            .label("文件名转小写")
            .description("将文件名转换为小写后进行比较")
            .type("boolean")
            .defaultValue(true)
            .required(false)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FILES)
            .addExclusiveParam("preprocessUpper", "when_true")
            .build();
        parameters.add(preprocessLowerParam);
        
        PluginParameterDTO preprocessUpperParam = new PluginParameterDTO.Builder()
            .name("preprocessUpper")
            .label("文件名转大写")
            .description("将文件名转换为大写后进行比较")
            .type("boolean")
            .defaultValue(false)
            .required(false)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FILES)
            .addExclusiveParam("preprocessLower", "when_true")
            .build();
        parameters.add(preprocessUpperParam);
        
        PluginParameterDTO preprocessSimplifiedParam = new PluginParameterDTO.Builder()
            .name("preprocessSimplified")
            .label("文件名转简体中文")
            .description("将文件名中的繁体中文转换为简体中文后进行比较")
            .type("boolean")
            .defaultValue(false)
            .required(false)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FILES)
            .build();
        parameters.add(preprocessSimplifiedParam);
        
        PluginParameterDTO audioSpecialParam = new PluginParameterDTO.Builder()
            .name("audioSpecial")
            .label("音频文件特殊处理")
            .description("对音频文件进行特殊处理")
            .type("boolean")
            .defaultValue(true)
            .required(false)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FILES)
            .build();
        parameters.add(audioSpecialParam);
        
        PluginParameterDTO minFileSizeParam = new PluginParameterDTO.Builder()
            .name("minFileSizeKB")
            .label("最小文件大小(KB)")
            .description("小于此大小的文件将被清理")
            .type("number")
            .defaultValue(0)
            .required(false)
            .minValue(0)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FILES)
            .addVisibilityCondition("cleanupMode", MODE_DIRECT_CLEANUP)
            .build();
        parameters.add(minFileSizeParam);
        
        PluginParameterDTO maxFileSizeParam = new PluginParameterDTO.Builder()
            .name("maxFileSizeKB")
            .label("最大文件大小(KB)")
            .description("大于此大小的文件将被清理")
            .type("number")
            .defaultValue(10240)
            .required(false)
            .minValue(0)
            .addVisibilityCondition("cleanupMode", MODE_DEDUP_FILES)
            .addVisibilityCondition("cleanupMode", MODE_DIRECT_CLEANUP)
            .build();
        parameters.add(maxFileSizeParam);
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        String cleanupMode = (String) config.getValue("cleanupMode", MODE_DEDUP_FILES);
        String deleteMethod = (String) config.getValue("deleteMethod", METHOD_PSEUDO_DELETE);
        String trashPath = (String) config.getValue("trashPath", ".EchoTrash");
        boolean keepLargest = (Boolean) config.getValue("keepLargest", true);
        boolean keepEarliest = (Boolean) config.getValue("keepEarliest", true);
        String keepExt = (String) config.getValue("keepExt", "wav");
        String comparisonMethod = (String) config.getValue("comparisonMethod", COMPARISON_METHOD_MD5);
        boolean preprocessLower = (Boolean) config.getValue("preprocessLower", true);
        boolean preprocessUpper = (Boolean) config.getValue("preprocessUpper", false);
        boolean preprocessSimplified = (Boolean) config.getValue("preprocessSimplified", false);
        boolean audioSpecial = (Boolean) config.getValue("audioSpecial", true);
        int minFileSizeKB = (Integer) config.getValue("minFileSizeKB", 0);
        int maxFileSizeKB = (Integer) config.getValue("maxFileSizeKB", 10240);
        
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (!file.exists()) {
                continue;
            }
            
            List<ChangeRecord> fileChanges = analyzeFile(file, cleanupMode, deleteMethod, trashPath, 
                keepLargest, keepEarliest, keepExt, comparisonMethod, preprocessLower, preprocessUpper, 
                preprocessSimplified, audioSpecial, minFileSizeKB, maxFileSizeKB);
            
            changes.addAll(fileChanges);
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        return execute(filePaths, config, context);
    }

    private List<ChangeRecord> analyzeFile(File file, String cleanupMode, String deleteMethod, 
            String trashPath, boolean keepLargest, boolean keepEarliest, String keepExt,
            String comparisonMethod, boolean preprocessLower, boolean preprocessUpper, boolean preprocessSimplified,
            boolean audioSpecial, int minFileSizeKB, int maxFileSizeKB) {
        
        List<ChangeRecord> result = new ArrayList<>();
        
        if (MODE_REMOVE_EMPTY_DIRS.equals(cleanupMode)) {
            if (isDirectoryEmpty(file)) {
                result.add(createDeleteRecord(file, "空文件夹 (无子文件)", deleteMethod, trashPath));
            }
        } else if (MODE_DEDUP_FOLDERS.equals(cleanupMode)) {
            File[] files = file.listFiles();
            if (file.isFile() || files == null || files.length < 2) {
                return result;
            }
            result.addAll(analyzeDuplicateFolders(Arrays.asList(files), deleteMethod, trashPath, 
                keepEarliest));
        } else if (MODE_DIRECT_CLEANUP.equals(cleanupMode)) {
            result.addAll(analyzeDirectCleanup(file, deleteMethod, trashPath, minFileSizeKB, maxFileSizeKB));
        } else if (MODE_MERGE_SAME_NAME.equals(cleanupMode)) {
            result.addAll(analyzeMergeSameNameFolders(file, deleteMethod, trashPath, keepEarliest));
        } else if (MODE_MERGE_NESTED.equals(cleanupMode)) {
            result.addAll(analyzeMergeNestedFolders(file, deleteMethod, trashPath));
        } else {
            result.addAll(analyzeDuplicateFiles(file, deleteMethod, trashPath, keepLargest, 
                keepEarliest, keepExt, comparisonMethod, preprocessLower, preprocessUpper, preprocessSimplified, 
                audioSpecial, minFileSizeKB, maxFileSizeKB));
        }
        
        return result;
    }

    private List<ChangeRecord> analyzeDuplicateFiles(File file, String deleteMethod, String trashPath,
            boolean keepLargest, boolean keepEarliest, String keepExt, String comparisonMethod,
            boolean preprocessLower, boolean preprocessUpper, boolean preprocessSimplified,
            boolean audioSpecial, int minFileSizeKB, int maxFileSizeKB) {
        
        List<ChangeRecord> result = new ArrayList<>();
        File[] files = file.listFiles();
        
        if (file.isFile() || files == null || files.length < 2) {
            return result;
        }
        
        List<File> filteredFiles = Arrays.stream(files)
            .filter(subFile -> subFile.isFile() && isSizeInRange(subFile.length(), minFileSizeKB, maxFileSizeKB))
            .collect(Collectors.toList());
        
        if (filteredFiles.size() < 2) {
            return result;
        }
        
        Map<String, List<File>> nameGroups = new HashMap<>();
        
        if (COMPARISON_METHOD_MD5.equals(comparisonMethod) || 
            COMPARISON_METHOD_SHA1.equals(comparisonMethod) || 
            COMPARISON_METHOD_SHA256.equals(comparisonMethod)) {
            
            Map<String, List<File>> hashGroups = new HashMap<>();
            
            for (File f : filteredFiles) {
                String hash;
                try {
                    if (COMPARISON_METHOD_MD5.equals(comparisonMethod)) {
                        hash = MD5Calculator.calculateMD5(f);
                    } else if (COMPARISON_METHOD_SHA1.equals(comparisonMethod)) {
                        hash = HashCalculator.calculateSHA1(f);
                    } else {
                        hash = HashCalculator.calculateSHA256(f);
                    }
                } catch (Exception e) {
                    continue;
                }
                
                hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(f);
            }
            
            for (List<File> group : hashGroups.values()) {
                if (group.size() < 2) {
                    continue;
                }
                
                File keeper = selectFileToKeep(group, keepLargest, keepEarliest, keepExt, audioSpecial);
                
                for (File f : group) {
                    if (!f.equals(keeper)) {
                        result.add(createDeleteRecord(f, "重复文件 (哈希相同): " + f.getName(), deleteMethod, trashPath));
                    }
                }
            }
            
            return result;
        }
        
        for (File f : filteredFiles) {
            String name = f.getName();
            String coreName = extractCoreName(name);
            coreName = preprocessFilename(coreName, preprocessLower, preprocessUpper, preprocessSimplified);
            String ext = getExt(name);
            String typeTag = getMediaType(ext);
            
            String key;
            if (COMPARISON_METHOD_SIZE.equals(comparisonMethod)) {
                key = coreName + "::" + typeTag + "::" + f.length();
            } else {
                key = coreName + "::" + typeTag;
            }
            
            nameGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(f);
        }
        
        for (List<File> group : nameGroups.values()) {
            if (group.size() < 2) {
                continue;
            }
            
            File keeper = selectFileToKeep(group, keepLargest, keepEarliest, keepExt, audioSpecial);
            
            for (File f : group) {
                if (!f.equals(keeper)) {
                    result.add(createDeleteRecord(f, "重复文件: " + f.getName(), deleteMethod, trashPath));
                }
            }
        }
        
        return result;
    }

    private List<ChangeRecord> analyzeDuplicateFolders(List<File> folders, String deleteMethod, 
            String trashPath, boolean keepEarliest) {
        List<ChangeRecord> result = new ArrayList<>();
        
        Map<String, List<File>> nameGroups = new HashMap<>();
        
        for (File folder : folders) {
            if (!folder.isDirectory()) {
                continue;
            }
            String name = folder.getName();
            String normalizedName = normalizeFolderName(name);
            nameGroups.computeIfAbsent(normalizedName, k -> new ArrayList<>()).add(folder);
        }
        
        for (List<File> group : nameGroups.values()) {
            if (group.size() < 2) {
                continue;
            }
            
            File keeper = selectFolderToKeep(group, keepEarliest);
            
            for (File f : group) {
                if (!f.equals(keeper)) {
                    result.add(createDeleteRecord(f, "重复文件夹: " + f.getName(), deleteMethod, trashPath));
                }
            }
        }
        
        return result;
    }

    private List<ChangeRecord> analyzeDirectCleanup(File file, String deleteMethod, 
            String trashPath, int minFileSizeKB, int maxFileSizeKB) {
        List<ChangeRecord> result = new ArrayList<>();
        
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return result;
            }
            
            for (File subFile : files) {
                if (subFile.isFile() && isSizeInRange(subFile.length(), minFileSizeKB, maxFileSizeKB)) {
                    result.add(createDeleteRecord(subFile, "直接清理文件", deleteMethod, trashPath));
                }
            }
        } else {
            if (isSizeInRange(file.length(), minFileSizeKB, maxFileSizeKB)) {
                result.add(createDeleteRecord(file, "直接清理文件", deleteMethod, trashPath));
            }
        }
        
        return result;
    }

    private List<ChangeRecord> analyzeMergeSameNameFolders(File file, String deleteMethod, 
            String trashPath, boolean keepEarliest) {
        List<ChangeRecord> result = new ArrayList<>();
        
        if (!file.isDirectory()) {
            return result;
        }
        
        File[] files = file.listFiles();
        if (files == null) {
            return result;
        }
        
        for (File f : files) {
            if (!f.isDirectory()) {
                continue;
            }
            
            File parent = f.getParentFile();
            if (parent == null) {
                continue;
            }
            
            String childName = f.getName();
            String parentName = parent.getName();
            
            if (childName.equals(parentName)) {
                File keeper = selectFolderToKeep(Arrays.asList(f, parent), keepEarliest);
                File toDelete = f.equals(keeper) ? parent : f;
                result.add(createDeleteRecord(toDelete, "同名父子文件夹合并", deleteMethod, trashPath));
            }
        }
        
        return result;
    }

    private List<ChangeRecord> analyzeMergeNestedFolders(File file, String deleteMethod, String trashPath) {
        List<ChangeRecord> result = new ArrayList<>();
        
        if (!file.isDirectory()) {
            return result;
        }
        
        File[] files = file.listFiles();
        if (files == null) {
            return result;
        }
        
        for (File f : files) {
            if (f.isDirectory()) {
                File[] subFiles = f.listFiles();
                if (subFiles != null && subFiles.length == 1 && subFiles[0].isDirectory()) {
                    File nestedDir = subFiles[0];
                    result.add(createDeleteRecord(f, "嵌套文件夹合并", deleteMethod, trashPath));
                }
            }
        }
        
        return result;
    }

    private ChangeRecord createDeleteRecord(File file, String reason, String deleteMethod, String trashPath) {
        ChangeRecord record = new ChangeRecord();
        record.setId("change-" + System.currentTimeMillis() + "-" + file.hashCode());
        record.setOriginalName(file.getAbsolutePath());
        record.setNewName(getTrashPath(file.getAbsolutePath(), deleteMethod, trashPath));
        record.setFilePath(file.getAbsolutePath());
        record.setChanged(true);
        record.setOperationType(ChangeRecord.OperationType.DELETE);
        record.setStatus(ChangeRecord.ExecStatus.PENDING);
        return record;
    }

    private String getTrashPath(String filePath, String deleteMethod, String trashPath) {
        if (METHOD_PSEUDO_DELETE.equals(deleteMethod)) {
            String dir = filePath.substring(0, filePath.lastIndexOf('/') + 1);
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            return dir + trashPath + "/" + fileName;
        } else {
            return "PERMANENT_DELETE";
        }
    }

    private boolean isDirectoryEmpty(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return true;
        }
        
        for (File f : files) {
            if (f.isDirectory()) {
                if (!isDirectoryEmpty(f)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        
        return true;
    }

    private boolean isSizeInRange(long fileSize, int minKB, int maxKB) {
        long minBytes = minKB * 1024L;
        long maxBytes = maxKB * 1024L;
        return fileSize >= minBytes && fileSize <= maxBytes;
    }

    private String extractCoreName(String fileName) {
        Matcher m = NORM_PATTERN.matcher(fileName);
        if (m.find()) {
            return m.group(1).trim();
        }
        return fileName;
    }

    private String preprocessFilename(String name, boolean toLower, boolean toUpper, boolean toSimplified) {
        if (toLower) {
            name = name.toLowerCase();
        } else if (toUpper) {
            name = name.toUpperCase();
        }
        
        if (toSimplified) {
            name = simplifyChinese(name);
        }
        
        return name;
    }

    private String normalizeFolderName(String name) {
        return name.toLowerCase().replaceAll("[\\s-_]+", "");
    }

    private String getExt(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    }

    private String getMediaType(String ext) {
        if (EXT_AUDIO.contains(ext)) {
            return "AUDIO";
        } else if (EXT_VIDEO.contains(ext)) {
            return "VIDEO";
        } else if (EXT_IMAGE.contains(ext)) {
            return "IMAGE";
        }
        return "OTHER";
    }

    private File selectFileToKeep(List<File> files, boolean keepLargest, boolean keepEarliest, 
            String keepExt, boolean audioSpecial) {
        File keeper = files.get(0);
        
        if (keepLargest) {
            for (File f : files) {
                if (f.length() > keeper.length()) {
                    keeper = f;
                }
            }
        }
        
        if (keepEarliest) {
            for (File f : files) {
                if (f.lastModified() < keeper.lastModified()) {
                    keeper = f;
                }
            }
        }
        
        if (keepExt != null && !keepExt.isEmpty()) {
            for (File f : files) {
                if (f.getName().toLowerCase().endsWith("." + keepExt.toLowerCase())) {
                    keeper = f;
                    break;
                }
            }
        }
        
        return keeper;
    }

    private File selectFolderToKeep(List<File> folders, boolean keepEarliest) {
        File keeper = folders.get(0);
        
        if (keepEarliest) {
            for (File f : folders) {
                if (f.lastModified() < keeper.lastModified()) {
                    keeper = f;
                }
            }
        }
        
        return keeper;
    }

    private String simplifyChinese(String text) {
        return text;
    }
}