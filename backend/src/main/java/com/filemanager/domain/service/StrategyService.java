package com.filemanager.domain.service;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;

public interface StrategyService {

    List<StrategyInfoDTO> getAvailableStrategies();

    StrategyInfoDTO getStrategyInfo(String strategyId);

    StrategyConfigDTO getStrategyConfig(String strategyId);

    boolean updateStrategyConfig(String strategyId, StrategyConfigDTO config);

    List<ChangeRecord> analyzeFiles(String strategyId, List<String> filePaths, StrategyConfigDTO config);

    List<ChangeRecord> executeStrategy(String strategyId, List<String> filePaths, StrategyConfigDTO config);
}
