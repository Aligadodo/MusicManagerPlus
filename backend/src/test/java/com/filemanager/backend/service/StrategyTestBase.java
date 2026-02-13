package com.filemanager.backend.service;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.StrategyConfigurable;
import com.filemanager.plugin.StrategyRegistry;
import com.filemanager.backend.service.impl.StrategyServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public abstract class StrategyTestBase {

    @Autowired
    protected StrategyServiceImpl strategyService;

    protected StrategyRegistry strategyRegistry;

    protected Path tempDir;

    @BeforeEach
    public void setUp() throws IOException {
        strategyRegistry = StrategyRegistry.getInstance();
        tempDir = Files.createTempDirectory("strategy_test_");
    }

    protected File createTestFile(String name, String content) throws IOException {
        File file = new File(tempDir.toFile(), name);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }

    protected File createTestDirectory(String name) throws IOException {
        File dir = new File(tempDir.toFile(), name);
        dir.mkdirs();
        return dir;
    }

    protected ExecutionContext createTestExecutionContext() {
        return new ExecutionContext() {
            private final List<String> logs = new ArrayList<>();

            @Override
            public void logInfo(String message) {
                logs.add("[INFO] " + message);
                System.out.println("[INFO] " + message);
            }

            @Override
            public void logWarn(String message) {
                logs.add("[WARN] " + message);
                System.out.println("[WARN] " + message);
            }

            @Override
            public void logError(String message) {
                logs.add("[ERROR] " + message);
                System.err.println("[ERROR] " + message);
            }

            @Override
            public void logDebug(String message) {
                logs.add("[DEBUG] " + message);
                System.out.println("[DEBUG] " + message);
            }
        };
    }

    protected void assertChangeRecord(ChangeRecord record, boolean expectedChanged, String expectedStatus) {
        assertNotNull(record, "ChangeRecord should not be null");
        assertEquals(expectedChanged, record.isChanged(), "Changed status mismatch");
        assertEquals(expectedStatus, record.getStatus(), "Status mismatch");
    }

    protected void assertFileExists(File file, boolean shouldExist) {
        assertEquals(shouldExist, file.exists(), 
            "File " + file.getAbsolutePath() + " should " + (shouldExist ? "exist" : "not exist"));
    }

    protected void cleanup() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            deleteDirectory(tempDir.toFile());
        }
    }

    private void deleteDirectory(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    Files.delete(file.toPath());
                }
            }
        }
        Files.delete(dir.toPath());
    }
}
