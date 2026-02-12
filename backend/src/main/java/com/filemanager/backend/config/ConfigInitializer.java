package com.filemanager.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 配置初始化器
 * 在应用启动时初始化配置
 */
@Component
public class ConfigInitializer implements ApplicationRunner {

    private final ConfigManager configManager;

    public ConfigInitializer(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        configManager.init();
        System.out.println("配置初始化完成");
    }
}