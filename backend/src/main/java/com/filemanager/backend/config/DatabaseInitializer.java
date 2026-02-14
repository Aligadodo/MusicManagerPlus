package com.filemanager.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class DatabaseInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    
    @Value("${app.database.auto-init:true}")
    private boolean autoInit;
    
    @Value("${app.database.path:data/music_manager.db}")
    private String databasePath;
    
    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        if (!autoInit) {
            logger.info("数据库自动初始化已禁用");
            return null;
        }
        
        logger.info("开始初始化数据库: {}", databasePath);
        
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        try {
            ClassPathResource resource = new ClassPathResource("db/schema.sql");
            if (resource.exists()) {
                populator.addScript(resource);
                initializer.setDatabasePopulator(populator);
                logger.info("数据库初始化脚本加载成功");
            } else {
                logger.warn("数据库初始化脚本不存在: db/schema.sql");
            }
        } catch (Exception e) {
            logger.error("加载数据库初始化脚本失败", e);
        }
        
        return initializer;
    }
    
    public static void initializeDatabase(DataSource dataSource, String databasePath) {
        Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            logger.info("检查数据库文件是否存在: {}", databasePath);
            
            statement.execute("SELECT name FROM sqlite_master WHERE type='table' LIMIT 1");
            logger.info("数据库初始化完成");
            
        } catch (SQLException e) {
            logger.error("数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }
}
