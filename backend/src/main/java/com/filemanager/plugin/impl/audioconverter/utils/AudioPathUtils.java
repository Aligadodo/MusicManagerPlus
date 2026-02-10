package com.filemanager.plugin.impl.audioconverter.utils;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.plugin.impl.audioconverter.enums.AudioFormat;
import com.filemanager.plugin.enums.common.OutputDirMode;

import java.io.File;

public class AudioPathUtils {
    
    public static String getTargetPath(String filePath, PluginConfigDTO config) {
        Object outputDirModeObj = config.getValue("outputDirMode");
        String outputDirModeCode = outputDirModeObj != null ? outputDirModeObj.toString() : OutputDirMode.SUBDIRECTORY.getCode();
        OutputDirMode outputDirMode = OutputDirMode.fromCode(outputDirModeCode);
        
        Object outputPathObj = config.getValue("outputPath");
        String outputPath = outputPathObj != null ? outputPathObj.toString() : "Convert - WAV";
        
        Object targetFormatObj = config.getValue("targetFormat");
        String targetFormatCode = targetFormatObj != null ? targetFormatObj.toString() : AudioFormat.WAV_CD_STANDARD.getCode();
        AudioFormat targetFormat = AudioFormat.fromCode(targetFormatCode);
        
        File sourceFile = new File(filePath);
        String fileName = sourceFile.getName();
        String extension = targetFormat.getExtension();
        String targetFileName = AudioFileUtils.changeExtension(fileName, extension);
        
        switch (outputDirMode) {
            case SUBDIRECTORY:
                File sourceDir = sourceFile.getParentFile();
                if (sourceDir != null) {
                    return sourceDir.getPath() + File.separator + outputPath + File.separator + targetFileName;
                }
                return outputPath + File.separator + targetFileName;
            case SPECIFIED_DIR:
                return outputPath + File.separator + targetFileName;
            case ROOT_DIR:
                File rootPath = sourceFile;
                while (rootPath.getParent() != null) {
                    rootPath = rootPath.getParentFile();
                }
                return rootPath.getPath() + File.separator + outputPath + File.separator + targetFileName;
            default:
                return outputPath + File.separator + targetFileName;
        }
    }
}