package com.filemanager.backend.controller;

import com.filemanager.domain.dto.FileInfoDTO;
import com.filemanager.domain.service.FileService;
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

    @Autowired
    private FileService fileService;

    @GetMapping("/scan")
    public ResponseEntity<List<FileInfoDTO>> scanDirectory(
            @RequestParam String path,
            @RequestParam(required = false, defaultValue = "0") int minDepth,
            @RequestParam(required = false, defaultValue = "3") int maxDepth,
            @RequestParam(required = false) String pattern) {
        try {
            List<FileInfoDTO> fileInfos = fileService.scanDirectory(path, minDepth, maxDepth, pattern);
            return ResponseEntity.ok(fileInfos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/info")
    public ResponseEntity<FileInfoDTO> getFileInfo(@RequestParam String path) {
        try {
            FileInfoDTO fileInfo = fileService.getFileInfo(path);
            return ResponseEntity.ok(fileInfo);
        } catch (Exception e) {
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
        try {
            String operation = (String) request.get("operation");
            String source = (String) request.get("source");
            String target = (String) request.get("target");

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
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
