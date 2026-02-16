package com.filemanager.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filemanager.backend.entity.ConfigSnapshotPO;
import com.filemanager.backend.mapper.ConfigSnapshotMapper;
import com.filemanager.backend.model.TaskConfigSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ConfigSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigSnapshotService.class);

    @Autowired
    private ConfigSnapshotMapper configSnapshotMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取或创建配置快照
     * 如果当前配置与最新快照相同，则复用现有快照
     * 否则创建新的快照
     * 
     * @param configSnapshot 配置快照对象
     * @param snapshotType 快照类型（TASK_CONFIG, PIPELINE_CONFIG）
     * @return 快照ID
     */
    public String getOrCreateSnapshot(TaskConfigSnapshot configSnapshot, String snapshotType) {
        try {
            String configHash = calculateConfigHash(configSnapshot);
            
            ConfigSnapshotPO latestSnapshot = configSnapshotMapper.getLatestSnapshotByType(snapshotType);
            
            if (latestSnapshot != null) {
                String latestHash = latestSnapshot.getSnapshotId();
                if (latestHash.equals(configHash)) {
                    logger.info("[ConfigSnapshot] 配置未变更，复用现有快照: {}", latestSnapshot.getSnapshotId());
                    return latestSnapshot.getSnapshotId();
                }
            }
            
            ConfigSnapshotPO newSnapshot = new ConfigSnapshotPO();
            newSnapshot.setSnapshotId(configHash);
            newSnapshot.setSnapshotName(generateSnapshotName(snapshotType));
            newSnapshot.setSnapshotType(snapshotType);
            newSnapshot.setConfigData(objectMapper.writeValueAsString(configSnapshot));
            newSnapshot.setDescription("自动生成的配置快照");
            newSnapshot.setIsTemplate(false);
            newSnapshot.setCreatedAt(new Date());
            newSnapshot.setUpdatedAt(new Date());
            newSnapshot.setCreatedBy("SYSTEM");
            
            configSnapshotMapper.insert(newSnapshot);
            
            logger.info("[ConfigSnapshot] 创建新配置快照: {}", configHash);
            return configHash;
            
        } catch (Exception e) {
            logger.error("[ConfigSnapshot] 创建配置快照失败", e);
            throw new RuntimeException("创建配置快照失败", e);
        }
    }

    /**
     * 计算配置的哈希值
     * 用于比较配置是否发生变更
     * 
     * @param configSnapshot 配置快照对象
     * @return 配置的MD5哈希值
     */
    private String calculateConfigHash(TaskConfigSnapshot configSnapshot) {
        try {
            String configJson = objectMapper.writeValueAsString(configSnapshot);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(configJson.getBytes("UTF-8"));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            logger.error("[ConfigSnapshot] 计算配置哈希失败", e);
            return String.valueOf(System.currentTimeMillis());
        }
    }

    /**
     * 生成快照名称
     * 
     * @param snapshotType 快照类型
     * @return 快照名称
     */
    private String generateSnapshotName(String snapshotType) {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return snapshotType + "_" + timestamp;
    }

    /**
     * 根据快照ID获取配置快照
     * 
     * @param snapshotId 快照ID
     * @return 配置快照对象
     */
    public TaskConfigSnapshot getSnapshot(String snapshotId) {
        try {
            ConfigSnapshotPO snapshotPO = configSnapshotMapper.selectById(snapshotId);
            if (snapshotPO == null) {
                logger.warn("[ConfigSnapshot] 快照不存在: {}", snapshotId);
                return null;
            }
            
            return objectMapper.readValue(snapshotPO.getConfigData(), TaskConfigSnapshot.class);
            
        } catch (Exception e) {
            logger.error("[ConfigSnapshot] 加载配置快照失败: {}", snapshotId, e);
            return null;
        }
    }

    /**
     * 获取指定类型的所有快照
     * 
     * @param snapshotType 快照类型
     * @return 快照列表
     */
    public List<ConfigSnapshotPO> getSnapshotsByType(String snapshotType) {
        return configSnapshotMapper.selectByType(snapshotType);
    }

    /**
     * 删除配置快照
     * 
     * @param snapshotId 快照ID
     * @return 是否删除成功
     */
    public boolean deleteSnapshot(String snapshotId) {
        try {
            int result = configSnapshotMapper.deleteById(snapshotId);
            logger.info("[ConfigSnapshot] 删除配置快照: {}", snapshotId);
            return result > 0;
        } catch (Exception e) {
            logger.error("[ConfigSnapshot] 删除配置快照失败: {}", snapshotId, e);
            return false;
        }
    }

    /**
     * 清理过期的快照
     * 保留最近N天的快照
     * 
     * @param daysToKeep 保留天数
     * @return 清理的快照数量
     */
    public int cleanupOldSnapshots(int daysToKeep) {
        try {
            Date cutoffDate = new Date(System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L));
            int result = configSnapshotMapper.deleteOldSnapshots(cutoffDate);
            logger.info("[ConfigSnapshot] 清理过期快照: {} 天前，删除 {} 个快照", daysToKeep, result);
            return result;
        } catch (Exception e) {
            logger.error("[ConfigSnapshot] 清理过期快照失败", e);
            return 0;
        }
    }

    /**
     * 分页获取快照列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 快照列表
     */
    public List<ConfigSnapshotPO> getSnapshotsByPage(int page, int size) {
        try {
            int offset = (page - 1) * size;
            return configSnapshotMapper.selectByPage(null, null, null, null, null, null, offset, size);
        } catch (Exception e) {
            logger.error("[ConfigSnapshot] 分页获取快照失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取快照总数
     * 
     * @return 快照总数
     */
    public long getTotalSnapshotCount() {
        try {
            return configSnapshotMapper.countByPage(null, null, null, null);
        } catch (Exception e) {
            logger.error("[ConfigSnapshot] 获取快照总数失败", e);
            return 0;
        }
    }

    /**
     * 根据快照ID获取快照PO对象
     * 
     * @param snapshotId 快照ID
     * @return 快照PO对象
     */
    public ConfigSnapshotPO getSnapshotById(String snapshotId) {
        try {
            return configSnapshotMapper.selectById(snapshotId);
        } catch (Exception e) {
            logger.error("[ConfigSnapshot] 获取快照PO失败: {}", snapshotId, e);
            return null;
        }
    }

    /**
     * 创建快照
     * 
     * @param snapshot 快照PO对象
     * @return 创建的快照PO对象
     */
    public ConfigSnapshotPO createSnapshot(ConfigSnapshotPO snapshot) {
        try {
            Date now = new Date();
            snapshot.setCreatedAt(now);
            snapshot.setUpdatedAt(now);
            configSnapshotMapper.insert(snapshot);
            logger.info("[ConfigSnapshot] 创建快照: {}", snapshot.getSnapshotId());
            return snapshot;
        } catch (Exception e) {
            logger.error("[ConfigSnapshot] 创建快照失败", e);
            throw new RuntimeException("创建快照失败", e);
        }
    }

    /**
     * 更新快照
     * 
     * @param snapshot 快照PO对象
     * @return 更新的快照PO对象
     */
    public ConfigSnapshotPO updateSnapshot(ConfigSnapshotPO snapshot) {
        try {
            snapshot.setUpdatedAt(new Date());
            configSnapshotMapper.update(snapshot);
            logger.info("[ConfigSnapshot] 更新快照: {}", snapshot.getSnapshotId());
            return snapshot;
        } catch (Exception e) {
            logger.error("[ConfigSnapshot] 更新快照失败", e);
            throw new RuntimeException("更新快照失败", e);
        }
    }
}