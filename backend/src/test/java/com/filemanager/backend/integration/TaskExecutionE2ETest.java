package com.filemanager.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filemanager.backend.entity.ConfigSnapshotPO;
import com.filemanager.backend.entity.TaskInfoPO;
import com.filemanager.backend.entity.ChangeRecordPO;
import com.filemanager.backend.mapper.ConfigSnapshotMapper;
import com.filemanager.backend.mapper.TaskInfoMapper;
import com.filemanager.backend.mapper.ChangeRecordMapper;
import com.filemanager.backend.service.ConfigSnapshotService;
import com.filemanager.backend.service.TaskStorageService;
import com.filemanager.backend.service.TaskExecutionService;
import com.filemanager.backend.service.TaskRegistry;
import com.filemanager.backend.model.TaskConfigSnapshot;
import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.enums.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 任务执行全链路端到端测试
 * 测试从任务创建到完成的完整流程，包括前后端交互
 */
@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TaskExecutionE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Autowired
    private ChangeRecordMapper changeRecordMapper;

    @Autowired
    private ConfigSnapshotMapper configSnapshotMapper;

    @Autowired
    private ConfigSnapshotService configSnapshotService;

    @Autowired
    private TaskStorageService taskStorageService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskRegistry taskRegistry;

    private Path testDirectory;
    private String testTaskId;
    private List<String> createdTaskIds = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        testDirectory = Files.createTempDirectory("music-manager-e2e-test");
        
        for (int i = 1; i <= 10; i++) {
            String fileName = "test-song-" + i + ".mp3";
            Path filePath = testDirectory.resolve(fileName);
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write("Test music file " + i);
            }
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        for (String taskId : createdTaskIds) {
            try {
                taskStorageService.deleteTask(taskId);
            } catch (Exception e) {
                System.err.println("清理任务失败: " + taskId + ", " + e.getMessage());
            }
        }
        
        if (testDirectory != null && Files.exists(testDirectory)) {
            Files.walk(testDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        System.err.println("删除文件失败: " + path + ", " + e.getMessage());
                    }
                });
        }
    }

    @Test
    void testCompleteTaskExecutionFlow() throws Exception {
        System.out.println("=== 开始测试完整任务执行流程 ===");

        TaskRequestDTO request = createTestTaskRequest();
        
        System.out.println("1. 创建任务");
        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").exists())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
        testTaskId = (String) dataMap.get("taskId");
        createdTaskIds.add(testTaskId);
        System.out.println("   任务ID: " + testTaskId);

        System.out.println("2. 验证任务信息已保存到数据库");
        TaskInfoPO taskInfo = taskInfoMapper.selectByTaskId(testTaskId);
        assertNotNull(taskInfo, "任务信息应该保存到数据库");
        assertEquals("CREATED", taskInfo.getStatus(), "任务状态应该是CREATED");
        assertNotNull(taskInfo.getConfigSnapshotId(), "应该有配置快照ID");
        System.out.println("   数据库任务状态: " + taskInfo.getStatus());
        System.out.println("   配置快照ID: " + taskInfo.getConfigSnapshotId());

        System.out.println("3. 验证配置快照已创建");
        ConfigSnapshotPO snapshot = configSnapshotMapper.selectById(taskInfo.getConfigSnapshotId());
        assertNotNull(snapshot, "配置快照应该存在");
        assertNotNull(snapshot.getConfigData(), "配置数据应该存在");
        System.out.println("   快照类型: " + snapshot.getSnapshotType());
        System.out.println("   配置数据长度: " + snapshot.getConfigData().length());

        System.out.println("4. 执行文件扫描");
        mockMvc.perform(post("/api/tasks/" + testTaskId + "/scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("扫描已开始"));

        waitForTaskStatus(testTaskId, "SCANNED", 30);
        System.out.println("   扫描完成");

        System.out.println("5. 验证扫描结果");
        taskInfo = taskInfoMapper.selectByTaskId(testTaskId);
        assertEquals("SCANNED", taskInfo.getStatus(), "任务状态应该是SCANNED");
        System.out.println("   扫描状态: " + taskInfo.getStatus());

        System.out.println("6. 执行预览分析");
        mockMvc.perform(post("/api/tasks/" + testTaskId + "/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("预览已开始"));

        waitForTaskStatus(testTaskId, "PREVIEWED", 30);
        System.out.println("   预览完成");

        System.out.println("7. 验证变更记录");
        List<ChangeRecordPO> changeRecords = changeRecordMapper.selectByTaskId(testTaskId);
        assertTrue(changeRecords.size() > 0, "应该有变更记录");
        System.out.println("   变更记录数量: " + changeRecords.size());

        System.out.println("8. 执行任务");
        mockMvc.perform(post("/api/tasks/" + testTaskId + "/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("执行已开始"));

        waitForTaskStatus(testTaskId, "COMPLETED", 60);
        System.out.println("   执行完成");

        System.out.println("9. 验证最终状态");
        taskInfo = taskInfoMapper.selectByTaskId(testTaskId);
        assertEquals("COMPLETED", taskInfo.getStatus(), "任务状态应该是COMPLETED");
        assertNotNull(taskInfo.getCompletedAt(), "完成时间应该设置");
        System.out.println("   最终状态: " + taskInfo.getStatus());
        System.out.println("   完成时间: " + taskInfo.getCompletedAt());

        System.out.println("10. 获取任务详情");
        mockMvc.perform(get("/api/tasks/" + testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(testTaskId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.configSnapshotId").exists())
                .andExpect(jsonPath("$.data.configSnapshot").exists());

        System.out.println("=== 完整任务执行流程测试通过 ===");
    }

    @Test
    void testConfigSnapshotReuse() throws Exception {
        System.out.println("=== 测试配置快照复用 ===");

        TaskRequestDTO request = createTestTaskRequest();
        
        System.out.println("1. 创建第一个任务");
        MvcResult result1 = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response1 = objectMapper.readValue(result1.getResponse().getContentAsString(), Map.class);
        Map<String, Object> dataMap1 = (Map<String, Object>) response1.get("data");
        String taskId1 = (String) dataMap1.get("taskId");
        createdTaskIds.add(taskId1);

        TaskInfoPO taskInfo1 = taskInfoMapper.selectByTaskId(taskId1);
        String snapshotId1 = taskInfo1.getConfigSnapshotId();
        System.out.println("   第一个任务快照ID: " + snapshotId1);

        System.out.println("2. 创建第二个任务（相同配置）");
        MvcResult result2 = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response2 = objectMapper.readValue(result2.getResponse().getContentAsString(), Map.class);
        Map<String, Object> dataMap2 = (Map<String, Object>) response2.get("data");
        String taskId2 = (String) dataMap2.get("taskId");
        createdTaskIds.add(taskId2);

        TaskInfoPO taskInfo2 = taskInfoMapper.selectByTaskId(taskId2);
        String snapshotId2 = taskInfo2.getConfigSnapshotId();
        System.out.println("   第二个任务快照ID: " + snapshotId2);

        System.out.println("3. 验证配置快照复用");
        assertEquals(snapshotId1, snapshotId2, "相同配置应该复用快照");

        System.out.println("4. 验证快照存在");
        ConfigSnapshotPO snapshot = configSnapshotMapper.selectBySnapshotId(snapshotId1);
        assertNotNull(snapshot, "快照应该存在");

        System.out.println("=== 配置快照复用测试通过 ===");
    }

    @Test
    void testTaskRestartFromScanStage() throws Exception {
        System.out.println("=== 测试从扫描阶段重启任务 ===");

        TaskRequestDTO request = createTestTaskRequest();
        
        System.out.println("1. 创建并执行任务到扫描完成");
        MvcResult result = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
        testTaskId = (String) dataMap.get("taskId");
        createdTaskIds.add(testTaskId);

        mockMvc.perform(post("/api/tasks/" + testTaskId + "/scan"))
                .andExpect(status().isOk());
        waitForTaskStatus(testTaskId, "SCANNED", 30);

        System.out.println("2. 重新扫描");
        mockMvc.perform(post("/api/tasks/" + testTaskId + "/restart/scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("重新扫描已开始"));

        waitForTaskStatus(testTaskId, "SCANNED", 30);
        System.out.println("   重新扫描完成");

        TaskInfoPO taskInfo = taskInfoMapper.selectByTaskId(testTaskId);
        assertEquals("SCANNED", taskInfo.getStatus());
        System.out.println("=== 从扫描阶段重启测试通过 ===");
    }

    @Test
    void testTaskRestartFromPreviewStage() throws Exception {
        System.out.println("=== 测试从预览阶段重启任务 ===");

        TaskRequestDTO request = createTestTaskRequest();
        
        System.out.println("1. 创建并执行任务到预览完成");
        MvcResult result = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
        testTaskId = (String) dataMap.get("taskId");
        createdTaskIds.add(testTaskId);

        mockMvc.perform(post("/api/tasks/" + testTaskId + "/scan"))
                .andExpect(status().isOk());
        waitForTaskStatus(testTaskId, "SCANNED", 30);

        mockMvc.perform(post("/api/tasks/" + testTaskId + "/preview"))
                .andExpect(status().isOk());
        waitForTaskStatus(testTaskId, "PREVIEWED", 30);

        System.out.println("2. 重新预览");
        mockMvc.perform(post("/api/tasks/" + testTaskId + "/restart/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("重新预览已开始"));

        waitForTaskStatus(testTaskId, "PREVIEWED", 30);
        System.out.println("   重新预览完成");

        TaskInfoPO taskInfo = taskInfoMapper.selectByTaskId(testTaskId);
        assertEquals("PREVIEWED", taskInfo.getStatus());
        System.out.println("=== 从预览阶段重启测试通过 ===");
    }

    @Test
    void testTaskRestartFromExecutionStage() throws Exception {
        System.out.println("=== 测试从执行阶段重启任务 ===");

        TaskRequestDTO request = createTestTaskRequest();
        
        System.out.println("1. 创建并执行完整任务");
        MvcResult result = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
        testTaskId = (String) dataMap.get("taskId");
        createdTaskIds.add(testTaskId);

        mockMvc.perform(post("/api/tasks/" + testTaskId + "/scan"))
                .andExpect(status().isOk());
        waitForTaskStatus(testTaskId, "SCANNED", 30);

        mockMvc.perform(post("/api/tasks/" + testTaskId + "/preview"))
                .andExpect(status().isOk());
        waitForTaskStatus(testTaskId, "PREVIEWED", 30);

        mockMvc.perform(post("/api/tasks/" + testTaskId + "/execute"))
                .andExpect(status().isOk());
        waitForTaskStatus(testTaskId, "COMPLETED", 60);

        System.out.println("2. 重新执行");
        mockMvc.perform(post("/api/tasks/" + testTaskId + "/restart/execution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("重新执行已开始"));

        waitForTaskStatus(testTaskId, "COMPLETED", 60);
        System.out.println("   重新执行完成");

        TaskInfoPO taskInfo = taskInfoMapper.selectByTaskId(testTaskId);
        assertEquals("COMPLETED", taskInfo.getStatus());
        System.out.println("=== 从执行阶段重启测试通过 ===");
    }

    @Test
    void testTaskDeletion() throws Exception {
        System.out.println("=== 测试任务删除 ===");

        TaskRequestDTO request = createTestTaskRequest();
        
        System.out.println("1. 创建任务");
        MvcResult result = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
        testTaskId = (String) dataMap.get("taskId");

        System.out.println("2. 执行扫描和预览");
        mockMvc.perform(post("/api/tasks/" + testTaskId + "/scan"))
                .andExpect(status().isOk());
        waitForTaskStatus(testTaskId, "SCANNED", 30);

        mockMvc.perform(post("/api/tasks/" + testTaskId + "/preview"))
                .andExpect(status().isOk());
        waitForTaskStatus(testTaskId, "PREVIEWED", 30);

        System.out.println("3. 创建变更记录");
        List<ChangeRecordPO> changeRecords = changeRecordMapper.selectByTaskId(testTaskId);
        assertTrue(changeRecords.size() > 0, "应该有变更记录");
        int recordCount = changeRecords.size();

        System.out.println("4. 删除任务");
        mockMvc.perform(delete("/api/tasks/" + testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("任务已删除"));

        System.out.println("5. 验证任务已删除");
        TaskInfoPO taskInfo = taskInfoMapper.selectByTaskId(testTaskId);
        assertNull(taskInfo, "任务应该从数据库删除");

        System.out.println("6. 验证变更记录已删除");
        List<ChangeRecordPO> remainingRecords = changeRecordMapper.selectByTaskId(testTaskId);
        assertEquals(0, remainingRecords.size(), "变更记录应该被删除");

        System.out.println("=== 任务删除测试通过 ===");
    }

    @Test
    void testTaskListAndPagination() throws Exception {
        System.out.println("=== 测试任务列表和分页 ===");

        System.out.println("1. 创建多个任务");
        for (int i = 1; i <= 5; i++) {
            TaskRequestDTO request = createTestTaskRequest();
            request.setTaskName("测试任务 " + i);
            
            MvcResult result = mockMvc.perform(post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
            Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
            String taskId = (String) dataMap.get("taskId");
            createdTaskIds.add(taskId);
        }

        System.out.println("2. 查询任务列表");
        // 不硬编码期望的任务数量，只验证列表是数组且有数据
        mockMvc.perform(get("/api/tasks")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").exists())
                .andExpect(jsonPath("$.data.list").isArray());
        
        // 验证至少创建了5个任务
        MvcResult result = mockMvc.perform(get("/api/tasks")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
        List<?> taskList = (List<?>) dataMap.get("list");
        assertTrue(taskList.size() >= 5, "至少应该有5个任务");

        System.out.println("3. 分页查询");
        mockMvc.perform(get("/api/tasks")
                .param("page", "1")
                .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(2));

        System.out.println("=== 任务列表和分页测试通过 ===");
    }

    @Test
    void testTaskDetailWithConfigSnapshot() throws Exception {
        System.out.println("=== 测试任务详情和配置快照 ===");

        TaskRequestDTO request = createTestTaskRequest();
        
        System.out.println("1. 创建任务");
        MvcResult result = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
        testTaskId = (String) dataMap.get("taskId");
        createdTaskIds.add(testTaskId);

        System.out.println("2. 获取任务详情");
        mockMvc.perform(get("/api/tasks/" + testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(testTaskId))
                .andExpect(jsonPath("$.data.configSnapshotId").exists())
                .andExpect(jsonPath("$.data.configSnapshot").exists())
                .andExpect(jsonPath("$.data.configSnapshot.sourceDirectories").isArray())
                .andExpect(jsonPath("$.data.configSnapshot.sourceDirectories[0].path").value(testDirectory.toString()));

        System.out.println("3. 验证配置快照数据");
        TaskInfoPO taskInfo = taskInfoMapper.selectByTaskId(testTaskId);
        ConfigSnapshotPO snapshot = configSnapshotMapper.selectById(taskInfo.getConfigSnapshotId());
        assertNotNull(snapshot);
        
        TaskConfigSnapshot configSnapshot = objectMapper.readValue(snapshot.getConfigData(), TaskConfigSnapshot.class);
        assertNotNull(configSnapshot.getSourceDirectories());
        assertEquals(1, configSnapshot.getSourceDirectories().size());
        assertEquals(testDirectory.toString(), configSnapshot.getSourceDirectories().get(0).getPath());

        System.out.println("=== 任务详情和配置快照测试通过 ===");
    }

    private TaskRequestDTO createTestTaskRequest() {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setTaskName("E2E测试任务");
        
        List<TaskRequestDTO.SourceDirectoryDTO> sourceDirs = new ArrayList<>();
        TaskRequestDTO.SourceDirectoryDTO sourceDir = new TaskRequestDTO.SourceDirectoryDTO();
        sourceDir.setPath(testDirectory.toString());
        sourceDir.setDepth(1);
        sourceDir.setRecursive(true);
        sourceDirs.add(sourceDir);
        request.setSourceDirectories(sourceDirs);
        
        request.setPipelineId("default-pipeline");
        
        TaskRequestDTO.GlobalSettingsDTO globalSettings = new TaskRequestDTO.GlobalSettingsDTO();
        globalSettings.setDryRun(false);
        globalSettings.setMaxThreads(4);
        request.setGlobalSettings(globalSettings);
        
        return request;
    }

    private void waitForTaskStatus(String taskId, String expectedStatus, int timeoutSeconds) throws Exception {
        long startTime = System.currentTimeMillis();
        long timeout = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeout) {
            TaskInfoPO taskInfo = taskInfoMapper.selectByTaskId(taskId);
            if (taskInfo != null && expectedStatus.equals(taskInfo.getStatus())) {
                return;
            }
            Thread.sleep(1000);
        }
        
        TaskInfoPO finalTaskInfo = taskInfoMapper.selectByTaskId(taskId);
        if (finalTaskInfo == null || !expectedStatus.equals(finalTaskInfo.getStatus())) {
            throw new RuntimeException("等待任务状态超时: 期望 " + expectedStatus + ", 实际 " + 
                (finalTaskInfo != null ? finalTaskInfo.getStatus() : "null"));
        }
    }
}
