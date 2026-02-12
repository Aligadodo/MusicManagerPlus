package com.filemanager.backend.controller;

import com.filemanager.backend.domain.dto.ThemeDTO;
import com.filemanager.backend.service.ThemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/themes")
public class ThemeController {

    @Autowired
    private ThemeService themeService;

    @GetMapping
    public ResponseEntity<List<ThemeDTO>> getAllThemes() {
        try {
            List<ThemeDTO> themes = themeService.getAllThemes();
            return ResponseEntity.ok(themes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThemeDTO> getThemeById(@PathVariable String id) {
        try {
            ThemeDTO theme = themeService.getThemeById(id);
            if (theme != null) {
                return ResponseEntity.ok(theme);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<ThemeDTO> createTheme(@RequestBody ThemeDTO theme) {
        try {
            ThemeDTO createdTheme = themeService.createTheme(theme);
            if (createdTheme != null) {
                return ResponseEntity.status(HttpStatus.CREATED).body(createdTheme);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ThemeDTO> updateTheme(@PathVariable String id, @RequestBody ThemeDTO theme) {
        try {
            ThemeDTO updatedTheme = themeService.updateTheme(id, theme);
            if (updatedTheme != null) {
                return ResponseEntity.ok(updatedTheme);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTheme(@PathVariable String id) {
        try {
            boolean deleted = themeService.deleteTheme(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", deleted);
            result.put("message", deleted ? "主题删除成功" : "主题不存在");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "删除主题失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/default")
    public ResponseEntity<ThemeDTO> getDefaultTheme() {
        try {
            ThemeDTO theme = themeService.getDefaultTheme();
            return ResponseEntity.ok(theme);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/default")
    public ResponseEntity<Map<String, Object>> setDefaultTheme(@RequestBody Map<String, String> request) {
        try {
            String themeId = request.get("themeId");
            if (themeId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "缺少主题ID");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            themeService.setDefaultTheme(themeId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "默认主题设置成功");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "设置默认主题失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}