package com.filemanager.backend.controller;

import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
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
@RequestMapping("/api/strategies")
public class StrategyController {

    private static final Logger logger = LoggerFactory.getLogger(StrategyController.class);

    @Autowired
    private StrategyService strategyService;

    @GetMapping
    public ResponseEntity<List<StrategyInfoDTO>> getStrategies() {
        logger.info("[API] GET /api/strategies - 获取所有策略列表");
        try {
            List<StrategyInfoDTO> strategies = strategyService.getAvailableStrategies();
            logger.info("[API] GET /api/strategies - 成功返回 {} 个策略", strategies.size());
            return ResponseEntity.ok(strategies);
        } catch (Exception e) {
            logger.error("[API] GET /api/strategies - 获取策略列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<StrategyInfoDTO> getStrategyInfo(@PathVariable String id) {
        logger.info("[API] GET /api/strategies/{} - 获取策略信息", id);
        try {
            StrategyInfoDTO strategy = strategyService.getStrategyInfo(id);
            if (strategy != null) {
                logger.info("[API] GET /api/strategies/{} - 成功返回策略: {}", id, strategy.getName());
            } else {
                logger.warn("[API] GET /api/strategies/{} - 策略不存在", id);
            }
            return ResponseEntity.ok(strategy);
        } catch (Exception e) {
            logger.error("[API] GET /api/strategies/{} - 获取策略信息失败", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{id}/config")
    public ResponseEntity<StrategyConfigDTO> getStrategyConfig(@PathVariable String id) {
        logger.info("[API] GET /api/strategies/{}/config - 获取策略配置", id);
        try {
            StrategyConfigDTO config = strategyService.getStrategyConfig(id);
            if (config != null && config.getConfigValues() != null) {
                logger.info("[API] GET /api/strategies/{}/config - 成功返回配置，包含 {} 个参数", id, config.getConfigValues().size());
            } else {
                logger.warn("[API] GET /api/strategies/{}/config - 配置为空或不存在", id);
            }
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            logger.error("[API] GET /api/strategies/{}/config - 获取策略配置失败", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/{id}/config")
    public ResponseEntity<Map<String, Object>> updateStrategyConfig(@PathVariable String id, @RequestBody StrategyConfigDTO config) {
        logger.info("[API] POST /api/strategies/{}/config - 更新策略配置", id);
        try {
            boolean success = strategyService.updateStrategyConfig(id, config);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", "配置更新成功");
            logger.info("[API] POST /api/strategies/{}/config - 配置更新{}", id, success ? "成功" : "失败");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] POST /api/strategies/{}/config - 更新策略配置失败", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<List<ChangeRecord>> analyzeFiles(@PathVariable String id, @RequestBody Map<String, Object> request) {
        logger.info("[API] POST /api/strategies/{}/analyze - 分析文件", id);
        try {
            List<String> files = (List<String>) request.get("files");
            StrategyConfigDTO config = (StrategyConfigDTO) request.get("config");
            logger.info("[API] POST /api/strategies/{}/analyze - 文件数量: {}", id, files != null ? files.size() : 0);

            List<ChangeRecord> changes = strategyService.analyzeFiles(id, files, config);
            logger.info("[API] POST /api/strategies/{}/analyze - 分析完成，生成 {} 条变更记录", id, changes.size());
            return ResponseEntity.ok(changes);
        } catch (Exception e) {
            logger.error("[API] POST /api/strategies/{}/analyze - 分析文件失败", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<List<ChangeRecord>> executeStrategy(@PathVariable String id, @RequestBody Map<String, Object> request) {
        logger.info("[API] POST /api/strategies/{}/execute - 执行策略", id);
        try {
            List<String> files = (List<String>) request.get("files");
            StrategyConfigDTO config = (StrategyConfigDTO) request.get("config");
            logger.info("[API] POST /api/strategies/{}/execute - 文件数量: {}", id, files != null ? files.size() : 0);

            List<ChangeRecord> changes = strategyService.executeStrategy(id, files, config);
            logger.info("[API] POST /api/strategies/{}/execute - 执行完成，生成 {} 条变更记录", id, changes.size());
            return ResponseEntity.ok(changes);
        } catch (Exception e) {
            logger.error("[API] POST /api/strategies/{}/execute - 执行策略失败", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
