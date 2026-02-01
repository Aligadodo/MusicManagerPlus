package com.filemanager.backend.controller;

import com.filemanager.domain.dto.FileInfoDTO;
import com.filemanager.domain.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    @GetMapping("/scan")
    public ResponseEntity<List<FileInfoDTO>> scanDirectory(
            @RequestParam String path,
            @RequestParam(required = false, defaultValue = "0") int minDepth,
            @RequestParam(required = false, defaultValue = "3") int maxDepth,
            @RequestParam(required = false) String pattern) {
        logger.info("[API] GET /api/files/scan - 扫描目录, path: {}, minDepth: {}, maxDepth: {}, pattern: {}", path, minDepth, maxDepth, pattern);
        try {
            List<FileInfoDTO> fileInfos = fileService.scanDirectory(path, minDepth, maxDepth, pattern);
            logger.info("[API] GET /api/files/scan - 扫描完成，找到 {} 个文件/目录", fileInfos.size());
            return ResponseEntity.ok(fileInfos);
        } catch (Exception e) {
            logger.error("[API] GET /api/files/scan - 扫描目录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/info")
    public ResponseEntity<FileInfoDTO> getFileInfo(@RequestParam String path) {
        logger.info("[API] GET /api/files/info - 获取文件信息, path: {}", path);
        try {
            FileInfoDTO fileInfo = fileService.getFileInfo(path);
            if (fileInfo != null) {
                logger.info("[API] GET /api/files/info - 文件信息获取成功, name: {}, size: {}", fileInfo.getName(), fileInfo.getSize());
            } else {
                logger.warn("[API] GET /api/files/info - 文件不存在, path: {}", path);
            }
            return ResponseEntity.ok(fileInfo);
        } catch (Exception e) {
            logger.error("[API] GET /api/files/info - 获取文件信息失败", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/exists")
    public ResponseEntity<Map<String, Boolean>> checkExists(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> paths = request.get("paths");
            if (paths == null) {
                return ResponseEntity.badRequest().body(null);
            }
            Map<String, Boolean> result = fileService.checkExists(paths);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/operation")
    public ResponseEntity<Map<String, Object>> fileOperation(@RequestBody Map<String, Object> request) {
        String operation = (String) request.get("operation");
        String source = (String) request.get("source");
        String target = (String) request.get("target");
        logger.info("[API] POST /api/files/operation - 文件操作, operation: {}, source: {}, target: {}", operation, source, target);
        
        try {
            boolean success = false;
            String message = "操作失败";

            switch (operation) {
                case "copy":
                    success = fileService.copy(source, target);
                    message = success ? "复制成功" : "复制失败";
                    break;
                case "move":
                    success = fileService.move(source, target);
                    message = success ? "移动成功" : "移动失败";
                    break;
                case "delete":
                    success = fileService.delete(source);
                    message = success ? "删除成功" : "删除失败";
                    break;
                case "rename":
                    success = fileService.rename(source, target);
                    message = success ? "重命名成功" : "重命名失败";
                    break;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", message);
            logger.info("[API] POST /api/files/operation - 操作完成, success: {}, message: {}", success, message);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] POST /api/files/operation - 文件操作失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
