package com.filemanager.backend.controller;

import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    @Autowired
    private StrategyService strategyService;

    @GetMapping
    public ResponseEntity<List<StrategyInfoDTO>> getStrategies() {
        try {
            List<StrategyInfoDTO> strategies = strategyService.getAvailableStrategies();
            return ResponseEntity.ok(strategies);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<StrategyInfoDTO> getStrategyInfo(@PathVariable String id) {
        try {
            StrategyInfoDTO strategy = strategyService.getStrategyInfo(id);
            return ResponseEntity.ok(strategy);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{id}/config")
    public ResponseEntity<StrategyConfigDTO> getStrategyConfig(@PathVariable String id) {
        try {
            StrategyConfigDTO config = strategyService.getStrategyConfig(id);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/{id}/config")
    public ResponseEntity<Map<String, Object>> updateStrategyConfig(@PathVariable String id, @RequestBody StrategyConfigDTO config) {
        try {
            boolean success = strategyService.updateStrategyConfig(id, config);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", "配置更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<List<ChangeRecord>> analyzeFiles(@PathVariable String id, @RequestBody Map<String, Object> request) {
        try {
            List<String> files = (List<String>) request.get("files");
            StrategyConfigDTO config = (StrategyConfigDTO) request.get("config");

            List<ChangeRecord> changes = strategyService.analyzeFiles(id, files, config);
            return ResponseEntity.ok(changes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<List<ChangeRecord>> executeStrategy(@PathVariable String id, @RequestBody Map<String, Object> request) {
        try {
            List<String> files = (List<String>) request.get("files");
            StrategyConfigDTO config = (StrategyConfigDTO) request.get("config");

            List<ChangeRecord> changes = strategyService.executeStrategy(id, files, config);
            return ResponseEntity.ok(changes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
