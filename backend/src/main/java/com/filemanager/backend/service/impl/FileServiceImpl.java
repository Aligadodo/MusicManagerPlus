package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.FileInfoDTO;
import com.filemanager.domain.service.FileService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public List<FileInfoDTO> scanDirectory(String path, int minDepth, int maxDepth, String pattern) {
        List<FileInfoDTO> fileInfos = new ArrayList<>();
        Path rootPath = Paths.get(path);

        if (!Files.exists(rootPath)) {
            return fileInfos;
        }

        try {
            Files.walk(rootPath, maxDepth)
                    .filter(p -> Files.isRegularFile(p) || Files.isDirectory(p))
                    .filter(p -> {
                        int depth = (int) rootPath.relativize(p).getNameCount();
                        return depth >= minDepth;
                    })
                    .filter(p -> {
                        if (pattern == null || pattern.isEmpty()) {
                            return true;
                        }
                        return p.getFileName().toString().matches(pattern);
                    })
                    .forEach(p -> {
                        FileInfoDTO info = convertToFileInfo(p);
                        fileInfos.add(info);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileInfos;
    }

    @Override
    public FileInfoDTO getFileInfo(String path) {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            return null;
        }
        return convertToFileInfo(filePath);
    }

    @Override
    public Map<String, Boolean> checkExists(List<String> paths) {
        Map<String, Boolean> result = new HashMap<>();
        for (String path : paths) {
            result.put(path, Files.exists(Paths.get(path)));
        }
        return result;
    }

    @Override
    public boolean copy(String source, String target) {
        try {
            Files.copy(Paths.get(source), Paths.get(target));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean move(String source, String target) {
        try {
            Files.move(Paths.get(source), Paths.get(target));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(String path) {
        try {
            Files.delete(Paths.get(path));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean rename(String source, String target) {
        try {
            Files.move(Paths.get(source), Paths.get(target));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public byte[] getFileContent(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean writeFileContent(String path, byte[] content) {
        try {
            Files.write(Paths.get(path), content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private FileInfoDTO convertToFileInfo(Path path) {
        FileInfoDTO info = new FileInfoDTO();
        info.setPath(path.toString());
        info.setName(path.getFileName().toString());
        info.setDirectory(Files.isDirectory(path));

        try {
            if (Files.isRegularFile(path)) {
                info.setSize(Files.size(path));
                info.setLastModified(Files.getLastModifiedTime(path).toMillis());
                
                String fileName = path.getFileName().toString();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    info.setExtension(fileName.substring(dotIndex + 1).toLowerCase());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return info;
    }
}
