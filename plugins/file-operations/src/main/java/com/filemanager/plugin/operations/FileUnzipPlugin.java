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
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUnzipPlugin implements IPlugin {
    private static final String ENGINE_JAVA = "java";
    private static final String ENGINE_7ZIP = "7zip";
    private static final String ENGINE_BANDIZIP = "bandizip";
    
    private static final String OUTPUT_SAME_DIR = "same_dir";
    private static final String OUTPUT_CUSTOM_DIR = "custom_dir";
    private static final String OUTPUT_PARENT_DIR = "parent_dir";
    
    private static final Set<String> ARCHIVE_EXTENSIONS = new HashSet<>(Arrays.asList(
        "zip", "7z", "rar", "tar", "gz", "jar", "xz", "bz2", "iso"
    ));

    @Override
    public String getId() {
        return "file-unzip";
    }

    @Override
    public String getName() {
        return "文件解压插件";
    }

    @Override
    public String getDescription() {
        return "批量智能解压文件，支持多种压缩格式、密码管理、智能目录等功能。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("engine", ENGINE_JAVA);
        config.setValue("exePath", "");
        config.setValue("outputMode", OUTPUT_SAME_DIR);
        config.setValue("customPath", "");
        config.setValue("smartFolder", true);
        config.setValue("mergeSameName", false);
        config.setValue("deleteSource", false);
        config.setValue("overwrite", false);
        config.setValue("deleteOnFail", false);
        config.setValue("nestedFolderMerge", false);
        config.setValue("passwords", new ArrayList<String>());
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO engineParam = new PluginParameterDTO.Builder()
            .name("engine")
            .label("解压引擎")
            .description("选择解压引擎")
            .type("select")
            .defaultValue(ENGINE_JAVA)
            .required(true)
            .options(new String[]{ENGINE_JAVA, ENGINE_7ZIP, ENGINE_BANDIZIP})
            .build();
        parameters.add(engineParam);
        
        PluginParameterDTO exePathParam = new PluginParameterDTO.Builder()
            .name("exePath")
            .label("可执行文件路径")
            .description("外部解压工具的可执行文件路径")
            .type("file")
            .defaultValue("")
            .required(false)
            .addVisibilityCondition("engine", ENGINE_7ZIP)
            .addVisibilityCondition("engine", ENGINE_BANDIZIP)
            .autoDetectParams(
                Arrays.asList(ENGINE_7ZIP, ENGINE_BANDIZIP),
                Arrays.asList(
                    "/usr/local/bin/7z",
                    "/opt/homebrew/bin/7z",
                    "/usr/bin/7z",
                    "C:\\Program Files\\7-Zip\\7z.exe",
                    "C:\\Program Files (x86)\\7-Zip\\7z.exe",
                    "/usr/local/bin/bz",
                    "/opt/homebrew/bin/bz",
                    "/usr/bin/bz",
                    "C:\\Program Files\\Bandizip\\bz.exe",
                    "C:\\Program Files\\Bandizip\\bc.exe"
                ),
                "exePath"
            )
            .build();
        parameters.add(exePathParam);
        
        PluginParameterDTO outputModeParam = new PluginParameterDTO.Builder()
            .name("outputMode")
            .label("输出模式")
            .description("选择解压输出目录")
            .type("select")
            .defaultValue(OUTPUT_SAME_DIR)
            .required(true)
            .options(new String[]{OUTPUT_SAME_DIR, OUTPUT_CUSTOM_DIR, OUTPUT_PARENT_DIR})
            .build();
        parameters.add(outputModeParam);
        
        PluginParameterDTO customPathParam = new PluginParameterDTO.Builder()
            .name("customPath")
            .label("自定义路径")
            .description("自定义解压输出目录")
            .type("directory")
            .defaultValue("")
            .required(false)
            .addVisibilityCondition("outputMode", OUTPUT_CUSTOM_DIR)
            .build();
        parameters.add(customPathParam);
        
        PluginParameterDTO smartFolderParam = new PluginParameterDTO.Builder()
            .name("smartFolder")
            .label("智能文件夹")
            .description("自动识别并创建合适的文件夹结构")
            .type("boolean")
            .defaultValue(true)
            .required(false)
            .build();
        parameters.add(smartFolderParam);
        
        PluginParameterDTO mergeSameNameParam = new PluginParameterDTO.Builder()
            .name("mergeSameName")
            .label("合并同名文件夹")
            .description("解压时合并同名文件夹")
            .type("boolean")
            .defaultValue(false)
            .required(false)
            .build();
        parameters.add(mergeSameNameParam);
        
        PluginParameterDTO deleteSourceParam = new PluginParameterDTO.Builder()
            .name("deleteSource")
            .label("解压后删除源文件")
            .description("解压成功后删除原始压缩文件")
            .type("boolean")
            .defaultValue(false)
            .required(false)
            .build();
        parameters.add(deleteSourceParam);
        
        PluginParameterDTO overwriteParam = new PluginParameterDTO.Builder()
            .name("overwrite")
            .label("覆盖已存在文件")
            .description("解压时覆盖已存在的文件")
            .type("boolean")
            .defaultValue(false)
            .required(false)
            .build();
        parameters.add(overwriteParam);
        
        PluginParameterDTO deleteOnFailParam = new PluginParameterDTO.Builder()
            .name("deleteOnFail")
            .label("解压失败后删除")
            .description("解压失败后删除原始压缩文件")
            .type("boolean")
            .defaultValue(false)
            .required(false)
            .build();
        parameters.add(deleteOnFailParam);
        
        PluginParameterDTO nestedFolderMergeParam = new PluginParameterDTO.Builder()
            .name("nestedFolderMerge")
            .label("嵌套文件夹合并")
            .description("合并嵌套的文件夹结构")
            .type("boolean")
            .defaultValue(false)
            .required(false)
            .build();
        parameters.add(nestedFolderMergeParam);
        
        PluginParameterDTO passwordsParam = new PluginParameterDTO.Builder()
            .name("passwords")
            .label("密码列表")
            .description("解压密码列表")
            .type("list")
            .defaultValue(new ArrayList<String>())
            .required(false)
            .build();
        parameters.add(passwordsParam);
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        List<PreconditionGroupDTO> groups = new ArrayList<>();
        
        PreconditionGroupDTO group = new PreconditionGroupDTO();
        group.setId("default");
        group.setName("默认条件组");
        group.setDescription("文件解压的默认前置条件");
        group.setLogicType(PreconditionGroupDTO.LogicType.AND);
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
        PreconditionDTO typeCondition = new PreconditionDTO();
        typeCondition.setId("type-condition");
        typeCondition.setField("fileExtension");
        typeCondition.setOperator(PreconditionDTO.OperatorType.IN);
        typeCondition.setValue(Arrays.asList("zip", "rar", "7z", "tar", "gz", "bz2"));
        typeCondition.setDescription("文件是压缩文件");
        preconditions.add(typeCondition);
        
        group.setPreconditions(preconditions);
        groups.add(group);
        
        return groups;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        String engine = (String) config.getValue("engine", ENGINE_JAVA);
        String exePath = (String) config.getValue("exePath", "");
        String outputMode = (String) config.getValue("outputMode", OUTPUT_SAME_DIR);
        String customPath = (String) config.getValue("customPath", "");
        boolean smartFolder = (Boolean) config.getValue("smartFolder", true);
        boolean mergeSameName = (Boolean) config.getValue("mergeSameName", false);
        boolean deleteSource = (Boolean) config.getValue("deleteSource", false);
        boolean overwrite = (Boolean) config.getValue("overwrite", false);
        boolean deleteOnFail = (Boolean) config.getValue("deleteOnFail", false);
        boolean nestedFolderMerge = (Boolean) config.getValue("nestedFolderMerge", false);
        @SuppressWarnings("unchecked")
        List<String> passwords = (List<String>) config.getValue("passwords", new ArrayList<String>());
        
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (!file.exists()) {
                continue;
            }
            
            if (!isArchiveFile(file)) {
                continue;
            }
            
            try {
                ChangeRecord record = extractArchive(file, engine, exePath, outputMode, customPath,
                    smartFolder, mergeSameName, deleteSource, overwrite, deleteOnFail,
                    nestedFolderMerge, passwords);
                if (record != null) {
                    changes.add(record);
                }
            } catch (Exception e) {
                ChangeRecord errorRecord = new ChangeRecord();
                errorRecord.setId("error-" + System.currentTimeMillis() + "-" + file.hashCode());
                errorRecord.setOriginalName(file.getAbsolutePath());
                errorRecord.setNewName("解压失败: " + e.getMessage());
                errorRecord.setFilePath(file.getAbsolutePath());
                errorRecord.setChanged(false);
                errorRecord.setOperationType(ChangeRecord.OperationType.UNZIP);
                errorRecord.setStatus(ChangeRecord.ExecStatus.FAILED);
                changes.add(errorRecord);
            }
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        String engine = (String) config.getValue("engine", ENGINE_JAVA);
        String outputMode = (String) config.getValue("outputMode", OUTPUT_SAME_DIR);
        String customPath = (String) config.getValue("customPath", "");
        boolean smartFolder = (Boolean) config.getValue("smartFolder", true);
        
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (!file.exists() || !isArchiveFile(file)) {
                continue;
            }
            
            ChangeRecord record = new ChangeRecord();
            record.setId("preview-" + System.currentTimeMillis() + "-" + file.hashCode());
            record.setOriginalName(file.getAbsolutePath());
            record.setNewName(getPreviewPath(file, outputMode, customPath, smartFolder));
            record.setFilePath(file.getAbsolutePath());
            record.setChanged(true);
            record.setOperationType(ChangeRecord.OperationType.UNZIP);
            record.setStatus(ChangeRecord.ExecStatus.PENDING);
            changes.add(record);
        }
        
        return changes;
    }

    private boolean isArchiveFile(File file) {
        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot == -1) {
            return false;
        }
        String ext = name.substring(dot + 1);
        return ARCHIVE_EXTENSIONS.contains(ext);
    }

    private ChangeRecord extractArchive(File archiveFile, String engine, String exePath,
            String outputMode, String customPath, boolean smartFolder, boolean mergeSameName,
            boolean deleteSource, boolean overwrite, boolean deleteOnFail,
            boolean nestedFolderMerge, List<String> passwords) throws Exception {
        
        File baseDestDir = getBaseDestDir(archiveFile, outputMode, customPath);
        if (!baseDestDir.exists()) {
            baseDestDir.mkdirs();
        }
        
        File extractRoot = smartFolder ? new File(baseDestDir, getBaseName(archiveFile.getName())) : baseDestDir;
        if (!extractRoot.exists()) {
            extractRoot.mkdirs();
        }
        
        List<String> passwordsToTry = new ArrayList<>();
        passwordsToTry.add(null);
        if (passwords != null) {
            passwordsToTry.addAll(passwords);
        }
        
        boolean success = false;
        Exception lastError = null;
        
        for (String password : passwordsToTry) {
            try {
                if (ENGINE_JAVA.equals(engine)) {
                    extractWithJava(archiveFile, extractRoot, overwrite, password);
                } else if (ENGINE_7ZIP.equals(engine) || ENGINE_BANDIZIP.equals(engine)) {
                    extractWithExternalTool(archiveFile, extractRoot, exePath, overwrite, password);
                } else {
                    extractWithJava(archiveFile, extractRoot, overwrite, password);
                }
                
                String[] files = extractRoot.list();
                if (files == null || files.length == 0) {
                    throw new IOException("解压成功但目标目录为空");
                }
                
                success = true;
                break;
            } catch (Exception e) {
                lastError = e;
            }
        }
        
        if (!success) {
            if (deleteOnFail) {
                try {
                    Files.delete(archiveFile.toPath());
                } catch (Exception ignored) {
                }
            }
            throw new IOException("解压失败: " + (lastError != null ? lastError.getMessage() : "未知错误"));
        }
        
        if (smartFolder) {
            optimizeSmartFolder(extractRoot, baseDestDir);
        }
        
        if (nestedFolderMerge) {
            mergeNestedFolders(extractRoot, overwrite);
        }
        
        if (mergeSameName) {
            mergeSameNameFolders(extractRoot, overwrite);
        }
        
        if (deleteSource) {
            try {
                Files.delete(archiveFile.toPath());
            } catch (Exception ignored) {
            }
        }
        
        ChangeRecord record = new ChangeRecord();
        record.setId("change-" + System.currentTimeMillis() + "-" + archiveFile.hashCode());
        record.setOriginalName(archiveFile.getAbsolutePath());
        record.setNewName(extractRoot.getAbsolutePath());
        record.setFilePath(archiveFile.getAbsolutePath());
        record.setChanged(true);
        record.setOperationType(ChangeRecord.OperationType.UNZIP);
        record.setStatus(ChangeRecord.ExecStatus.SUCCESS);
        return record;
    }

    private void extractWithJava(File archiveFile, File destDir, boolean overwrite, String password) throws IOException {
        if (archiveFile.getName().toLowerCase().endsWith(".zip")) {
            extractZipFile(archiveFile, destDir, overwrite);
        } else {
            throw new IOException("Java引擎暂不支持此格式: " + archiveFile.getName());
        }
    }

    private void extractZipFile(File zipFile, File destDir, boolean overwrite) throws IOException {
        byte[] buffer = new byte[1024];
        
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                
                if (entry.isDirectory()) {
                    if (!newFile.exists()) {
                        newFile.mkdirs();
                    }
                    continue;
                }
                
                File parent = newFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                
                if (newFile.exists() && !overwrite) {
                    continue;
                }
                
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                
                zis.closeEntry();
            }
        }
    }

    private void extractWithExternalTool(File archiveFile, File destDir, String exePath, 
            boolean overwrite, String password) throws IOException {
        
        File exeFile = new File(exePath);
        if (!exeFile.exists() || !exeFile.isFile()) {
            throw new IOException("外部工具路径无效: " + exePath);
        }
        
        List<String> command = new ArrayList<>();
        command.add(exePath);
        command.add("x");
        command.add("-y");
        command.add(archiveFile.getAbsolutePath());
        command.add("-o" + destDir.getAbsolutePath());
        
        if (password != null && !password.isEmpty()) {
            command.add("-p" + password);
        }
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("外部工具执行失败，退出码: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("解压过程被中断");
        }
    }

    private void optimizeSmartFolder(File wrapperDir, File parentDir) throws IOException {
        if (wrapperDir == null || !wrapperDir.exists() || !wrapperDir.isDirectory()) {
            return;
        }
        
        File[] files = wrapperDir.listFiles();
        if (files == null) {
            return;
        }
        
        List<File> validFiles = Arrays.stream(files)
            .filter(f -> !f.getName().equals(".DS_Store") && !f.getName().equalsIgnoreCase("Thumbs.db"))
            .collect(Collectors.toList());
        
        if (validFiles.size() == 1 && validFiles.get(0).isDirectory()) {
            File singleInnerDir = validFiles.get(0);
            File targetDir = new File(parentDir, singleInnerDir.getName());
            
            if (!targetDir.exists()) {
                moveDirectory(singleInnerDir, targetDir);
                deleteDirectoryRecursively(wrapperDir);
            }
        }
    }

    private void mergeNestedFolders(File rootDir, boolean overwrite) throws IOException {
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            return;
        }
        
        int mergedCount = 0;
        File[] files = rootDir.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    if (subFiles != null && subFiles.length == 1 && subFiles[0].isDirectory()) {
                        File nestedDir = subFiles[0];
                        File targetDir = new File(rootDir, nestedDir.getName());
                        
                        if (!targetDir.exists()) {
                            moveDirectory(nestedDir, targetDir);
                            deleteDirectoryRecursively(file);
                            mergedCount++;
                        }
                    }
                }
            }
        }
    }

    private void mergeSameNameFolders(File rootDir, boolean overwrite) throws IOException {
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            return;
        }
        
        File[] files = rootDir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory() && file.getName().equals(rootDir.getName())) {
                mergeDirectories(file, rootDir, overwrite);
                deleteDirectoryRecursively(file);
                break;
            }
        }
    }

    private void mergeDirectories(File sourceDir, File targetDir, boolean overwrite) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            File targetFile = new File(targetDir, file.getName());
            
            if (file.isDirectory()) {
                if (targetFile.exists() && targetFile.isDirectory()) {
                    mergeDirectories(file, targetFile, overwrite);
                } else {
                    moveDirectory(file, targetFile);
                }
            } else {
                if (targetFile.exists() && !overwrite) {
                    continue;
                }
                Files.move(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void moveDirectory(File sourceDir, File targetDir) throws IOException {
        try {
            Files.move(sourceDir.toPath(), targetDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            copyDirectory(sourceDir, targetDir);
            deleteDirectoryRecursively(sourceDir);
        }
    }

    private void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        File[] files = sourceDir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            File targetFile = new File(targetDir, file.getName());
            
            if (file.isDirectory()) {
                copyDirectory(file, targetFile);
            } else {
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void deleteDirectoryRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectoryRecursively(entry);
                }
            }
        }
        Files.delete(file.toPath());
    }

    private File getBaseDestDir(File archiveFile, String outputMode, String customPath) {
        if (OUTPUT_CUSTOM_DIR.equals(outputMode) && customPath != null && !customPath.isEmpty()) {
            return new File(customPath);
        } else if (OUTPUT_PARENT_DIR.equals(outputMode)) {
            File parent = archiveFile.getParentFile();
            if (parent != null) {
                File grandParent = parent.getParentFile();
                if (grandParent != null) {
                    return grandParent;
                }
            }
            return archiveFile.getParentFile();
        } else {
            return archiveFile.getParentFile();
        }
    }

    private String getPreviewPath(File archiveFile, String outputMode, String customPath, boolean smartFolder) {
        File baseDestDir = getBaseDestDir(archiveFile, outputMode, customPath);
        if (smartFolder) {
            return new File(baseDestDir, getBaseName(archiveFile.getName())).getAbsolutePath();
        } else {
            return baseDestDir.getAbsolutePath();
        }
    }

    private String getBaseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}