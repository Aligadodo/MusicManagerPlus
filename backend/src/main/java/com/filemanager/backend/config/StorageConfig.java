package com.filemanager.backend.config;

import com.filemanager.backend.storage.ITaskStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StorageConfig {

    private static final Logger logger = LoggerFactory.getLogger(StorageConfig.class);

    @Value("${app.storage.mode:database}")
    private String storageMode;

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "taskStorage")
    public ITaskStorage taskStorage(
            com.filemanager.backend.storage.FileSystemTaskStorage fileSystemTaskStorage,
            com.filemanager.backend.storage.DatabaseTaskStorage databaseTaskStorage) {
        
        logger.info("[StorageConfig] 存储模式配置: {}", storageMode);
        
        if ("filesystem".equalsIgnoreCase(storageMode)) {
            logger.info("[StorageConfig] 使用文件系统存储模式");
            return fileSystemTaskStorage;
        } else if ("database".equalsIgnoreCase(storageMode)) {
            logger.info("[StorageConfig] 使用数据库存储模式");
            return databaseTaskStorage;
        } else {
            logger.warn("[StorageConfig] 未知的存储模式: {}, 默认使用数据库存储模式", storageMode);
            return databaseTaskStorage;
        }
    }
}