package com.filemanager.domain.service;

import com.filemanager.domain.dto.FileInfoDTO;

import java.util.List;
import java.util.Map;

public interface FileService {
    List<FileInfoDTO> scanDirectory(String path, int minDepth, int maxDepth, String pattern);
    FileInfoDTO getFileInfo(String path);
    Map<String, Boolean> checkExists(List<String> paths);
    boolean copy(String source, String target);
    boolean move(String source, String target);
    boolean delete(String path);
    boolean rename(String source, String target);
    byte[] getFileContent(String path);
    boolean writeFileContent(String path, byte[] content);
}
