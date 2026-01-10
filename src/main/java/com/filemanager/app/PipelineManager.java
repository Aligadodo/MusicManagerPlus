package com.filemanager.app;

import com.filemanager.base.IAppController;
import com.filemanager.base.IAppStrategy;
import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.AppStrategyFactory;
import com.filemanager.tool.MultiThreadTaskEstimator;
import com.filemanager.tool.RetryableThreadPool;
import com.filemanager.tool.ThreadPoolManager;
import com.filemanager.tool.display.FXDialogUtils;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.TaskStatus;
import com.filemanager.util.file.FileLockManagerUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class PipelineManager {
    private final IAppController appController;
    private final ThreadPoolManager threadPoolManager;
    private final AtomicLong lastRefresh;
    private final AtomicBoolean isTaskRunning;
    private List<ChangeRecord> fullChangeList;
    private Task<?> currentTask;
    private MultiThreadTaskEstimator threadTaskEstimator;
    private String threadPoolMode = ThreadPoolManager.MODE_GLOBAL;
    
    public PipelineManager(IAppController appController, ThreadPoolManager threadPoolManager) {
        this.appController = appController;
        this.threadPoolManager = threadPoolManager;
        this.lastRefresh = new AtomicLong(System.currentTimeMillis());
        this.isTaskRunning = new AtomicBoolean(false);
        this.fullChangeList = new ArrayList<>();
    }
    
    public void runPipelineAnalysis() {
        if (appController.getSourceRoots().isEmpty()) {
            FXDialogUtils.showToast(appController.getPrimaryStage(), "请先添加源目录！", FXDialogUtils.ToastType.INFO);
            return;
        }
        if (appController.getPipelineStrategies().isEmpty()) {
            FXDialogUtils.showToast(appController.getPrimaryStage(), "请先添加步骤！",
                    FXDialogUtils.ToastType.INFO);
            return;
        }
        if (isTaskRunning.get()) {
            FXDialogUtils.showToast(appController.getPrimaryStage(), "任务执行中，请先停止前面的任务再执行预览！",
                    FXDialogUtils.ToastType.INFO);
            return;
        }
        
        fullChangeList.clear();
        
        // 捕获所有策略参数
        for (IAppStrategy s : appController.getPipelineStrategies()) {
            s.captureParams();
        }
        
        // 从GlobalSettingsView获取参数
        int maxDepth = "当前目录".equals(appController.getCbRecursionMode().getValue()) ? 1 :
                ("全部文件".equals(appController.getCbRecursionMode().getValue()) ? Integer.MAX_VALUE : appController.getSpRecursionDepth().getValue());
        
        Task<List<ChangeRecord>> task = new Task<List<ChangeRecord>>() {
            @Override
            protected List<ChangeRecord> call() throws Exception {
                updateMessage("▶ ▶ ▶ 扫描源文件...");
                List<File> initialFiles = new ArrayList<>();
                for (File r : appController.getSourceRoots()) {
                    if (isCancelled()) break;
                    initialFiles.addAll(appController.scanFilesRobust(r, maxDepth, msg -> appController.setRunningUI("▶ ▶ ▶ " + msg)));
                }
                if (isCancelled()) return null;
                appController.setRunningUI("▶ ▶ ▶ 扫描完成，共 " + initialFiles.size() + " 个文件。");
                
                // 应用预览数量限制
                PreviewView previewView = (PreviewView) appController.getPreviewView();
                List<File> limitedFiles = initialFiles;
                
                // 检查全局预览数量限制
                if (!previewView.isUnlimitedPreview()) {
                    int limit = previewView.getGlobalPreviewLimit();
                    if (initialFiles.size() > limit) {
                        limitedFiles = initialFiles.stream().limit(limit).collect(Collectors.toList());
                        appController.log("▶ ▶ ▶ 已应用全局预览数量限制，仅处理 " + limit + " 个文件");
                    }
                }
                
                // 检查根路径预览数量限制
                List<File> finalLimitedFiles = new ArrayList<>();
                java.util.Map<String, Integer> processedCountByRoot = new java.util.concurrent.ConcurrentHashMap<>();
                
                for (File file : limitedFiles) {
                    String filePath = file.isDirectory() ? file.getAbsolutePath() : file.getParent();
                    String rootPath = appController.findRootPathForFile(filePath);
                    
                    // 检查根路径预览数量限制
                    if (!previewView.isRootPathUnlimitedPreview(rootPath)) {
                        int rootLimit = previewView.getRootPathPreviewLimit(rootPath);
                        int processed = processedCountByRoot.computeIfAbsent(rootPath, k -> 0);
                        
                        if (processed >= rootLimit) {
                            continue; // 达到根路径预览数量限制，跳过该文件
                        }
                        
                        processedCountByRoot.put(rootPath, processed + 1);
                    }
                    
                    finalLimitedFiles.add(file);
                }
                
                if (finalLimitedFiles.size() < limitedFiles.size()) {
                    appController.log("▶ ▶ ▶ 已应用根路径预览数量限制，共处理 " + finalLimitedFiles.size() + " 个文件");
                }
                
                List<ChangeRecord> currentRecords = finalLimitedFiles.stream()
                        .map(f -> new ChangeRecord(f.getName(), f.getName(), f, false, f.getAbsolutePath(), OperationType.NONE))
                        .collect(Collectors.toList());
                
                int total = currentRecords.size();
                AtomicInteger processed = new AtomicInteger(0);
                threadTaskEstimator = new MultiThreadTaskEstimator(total, Math.max(Math.min(50, total / 20), 1));
                threadTaskEstimator.start();
                ConcurrentLinkedDeque<ChangeRecord> newRecords = new ConcurrentLinkedDeque<>();
                
                currentRecords.parallelStream().forEach(rec -> {
                    try {
                        int curr = processed.incrementAndGet();
                        Platform.runLater(() -> updateProgress(curr, total));
                        if (isCancelled()) {
                            return;
                        }
                        for (int i = 0; i < appController.getPipelineStrategies().size(); i++) {
                            IAppStrategy strategy = appController.getPipelineStrategies().get(i);
                            List<ChangeRecord> newRecordAfter = strategy.analyzeWithPreCheck(rec, currentRecords, appController.getSourceRoots());
                            newRecords.addAll(newRecordAfter);
                        }
                    } catch (Exception e) {
                        rec.setStatus(ExecStatus.ANALYZE_FAILED);
                        rec.setFailReason(e.getMessage());
                        appController.logError("❌ 分析失败: " + rec.getFileHandle().getAbsolutePath() + ",原因" + e.getMessage());
                        appController.logError("❌ 失败详细原因:" + e.getMessage());
                    } finally {
                        threadTaskEstimator.oneCompleted();
                        if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
                            appController.setRunningUI("▶ ▶ ▶ 预览任务进度: " + threadTaskEstimator.getDisplayInfo());
                            lastRefresh.set(System.currentTimeMillis());
                        }
                    }
                });
                
                if (!newRecords.isEmpty()) {
                    List<ChangeRecord> union = new ArrayList<>(newRecords);
                    union.addAll(currentRecords);
                    return union;
                }
                return currentRecords;
            }
        };
        
        setStartTaskUI("▶ ▶ ▶ 预览中...", task);
        
        task.setOnSucceeded(e -> {
            fullChangeList = task.getValue();
            appController.setFullChangeList(fullChangeList);
            setFinishTaskUI("➡ ➡ ➡ 预览完成 ⬅ ⬅ ⬅", TaskStatus.SUCCESS);
            boolean hasChanges = fullChangeList.stream().anyMatch(ChangeRecord::isChanged);
            appController.enableExecuteButton(!hasChanges);
        });
        
        handleTaskLifecycle(task);
        new Thread(task).start();
    }
    
    public void runPipelineExecution() {
        long count = countPendingTasks();
        if (count == 0) {
            return;
        }
        
        if (!confirmExecution(count)) {
            return;
        }
        
        prepareExecutionUI();
        
        Task<Void> task = createExecutionTask();
        setStartTaskUI("▶ ▶ ▶ 执行中...", task);
        task.setOnSucceeded(e -> setFinishTaskUI("➡ ➡ ➡ 执行成功 ⬅ ⬅ ⬅", TaskStatus.SUCCESS));
        
        handleTaskLifecycle(task);
        new Thread(task).start();
    }
    
    private long countPendingTasks() {
        return fullChangeList.stream()
                .filter(record -> record.isChanged()
                        && record.getStatus() == ExecStatus.PENDING)
                .count();
    }
    
    private boolean confirmExecution(long count) {
        return FXDialogUtils.showConfirm("确认", "执行 " + count + " 个变更?");
    }
    
    private void prepareExecutionUI() {
        appController.disableGoButton(true);
        appController.disableExecuteButton(true);
    }
    
    private Task<Void> createExecutionTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                List<ChangeRecord> todos = fullChangeList.stream()
                        .filter(record -> record.isChanged()
                                && record.getOpType() != OperationType.NONE
                                && record.getStatus() == ExecStatus.PENDING)
                        .collect(Collectors.toList());
                int total = todos.size();
                AtomicInteger curr = new AtomicInteger(0);
                
                // 线程池和估算器管理
                final java.util.Map<String, MultiThreadTaskEstimator> localEstimatorMap = new java.util.concurrent.ConcurrentHashMap<>();
                
                // 设置线程池模式
                threadPoolManager.setThreadPoolMode(threadPoolMode);
                
                // 任务数量限制计数器
                final java.util.Map<String, AtomicInteger> executedCountByRootPath = new java.util.concurrent.ConcurrentHashMap<>();
                final AtomicInteger globalExecutedCount = new AtomicInteger(0);
                
                // 创建全局估算器
                threadTaskEstimator = new MultiThreadTaskEstimator(total, Math.max(Math.min(20, total / 20), 1));
                threadTaskEstimator.start();
                appController.log("▶ ▶ ▶ 任务启动，并发线程: " + appController.getSpExecutionThreads().getValue());
                appController.log("▶ ▶ ▶ 当前线程池模式: " + threadPoolMode);
                appController.log("▶ ▶ ▶ 注意：部分任务依赖同一个原始文件，会因为加锁导致串行执行，任务会一直轮询！");
                appController.log("▶ ▶ ▶ 第[" + 1 + "]轮任务扫描，总待执行任务数：" + todos.size());
                AtomicInteger round = new AtomicInteger(1);
                
                while (!todos.isEmpty() && !isCancelled() && todos.stream().anyMatch(rec -> rec.getStatus() == ExecStatus.PENDING)) {
                    AtomicBoolean anyChange = new AtomicBoolean(false);
                    for (ChangeRecord rec : todos) {
                        if (isCancelled()) {
                            break;
                        }
                        if (threadTaskEstimator.getRunningTaskCount() > appController.getSpExecutionThreads().getValue()) {
                            Thread.sleep(1);
                            continue;
                        }
                        if (rec.getStatus() != ExecStatus.PENDING) {
                            continue;
                        }
                        // 检查文件锁
                        if (FileLockManagerUtil.isLocked(rec.getFileHandle())) {
                            continue;
                        }
                        
                        // 获取来源文件的绝对路径
                        File sourceFile = rec.getFileHandle();
                        String sourcePath = sourceFile.getAbsolutePath();
                        if (!sourceFile.isDirectory()) {
                            sourcePath = sourceFile.getParent();
                        }
                        
                        // 找到该文件所在的根路径
                        String rootPath = appController.findRootPathForFile(sourcePath);
                        
                        // 检查任务数量限制
                        boolean exceedLimit = checkExecutionLimits(rootPath, globalExecutedCount, executedCountByRootPath);
                        if (exceedLimit) {
                            continue;
                        }
                        
                        // 获取执行线程池
                        RetryableThreadPool sourceExecutor = threadPoolManager.getExecutionThreadPool(rootPath);
                        
                        // 获取或创建该根路径的任务估算器
                        createRootPathEstimatorIfNeeded(localEstimatorMap, rootPath, todos);
                        
                        final String finalRootPath = rootPath;
                        sourceExecutor.execute(() -> executeSingleTask(rec, curr, total, localEstimatorMap, anyChange,
                                finalRootPath, globalExecutedCount, executedCountByRootPath));
                    }
                    
                    // 适当Sleep，避免反复刷数据
                    // 定期更新根路径进度UI
                    if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
                        lastRefresh.set(System.currentTimeMillis());
                        appController.getPreviewView().updateRootPathProgress();
                    }
                    Thread.sleep(100);
                }
                
                // 关闭所有线程池
                threadPoolManager.shutdownAll();
                    
                // 等待所有线程池终止
                threadPoolManager.awaitTermination();
                
                return null;
            }
        };
    }
    
    private boolean checkExecutionLimits(String rootPath, AtomicInteger globalExecutedCount,
            java.util.Map<String, AtomicInteger> executedCountByRootPath) {
        PreviewView previewView = (PreviewView) appController.getPreviewView();
        boolean exceedLimit = false;
        
        // 检查全局执行数量限制
        if (!previewView.isUnlimitedExecution()) {
            if (globalExecutedCount.get() >= previewView.getGlobalExecutionLimit()) {
                exceedLimit = true;
            }
        }
        
        // 检查根路径执行数量限制
        if (!exceedLimit && !previewView.isRootPathUnlimitedExecution(rootPath)) {
            AtomicInteger rootExecutedCount = executedCountByRootPath.computeIfAbsent(rootPath, k -> new AtomicInteger(0));
            if (rootExecutedCount.get() >= previewView.getRootPathExecutionLimit(rootPath)) {
                exceedLimit = true;
            }
        }
        
        return exceedLimit;
    }
    
    private void createRootPathEstimatorIfNeeded(java.util.Map<String, MultiThreadTaskEstimator> localEstimatorMap,
            String rootPath, List<ChangeRecord> todos) {
        localEstimatorMap.computeIfAbsent(rootPath, k -> {
            // 计算该根路径下的待执行任务数
            long rootTaskCount = todos.stream()
                    .filter(record -> {
                        File file = record.getFileHandle();
                        String filePath = file.isDirectory() ? file.getAbsolutePath() : file.getParent();
                        return appController.findRootPathForFile(filePath).equals(k);
                    })
                    .count();
            MultiThreadTaskEstimator estimator = new MultiThreadTaskEstimator(rootTaskCount, Math.max(Math.min(20, (int)rootTaskCount / 20), 1));
            estimator.start();
            appController.log("▶ ▶ ▶ 为根路径创建任务估算器: " + k + "，总任务数: " + rootTaskCount);
            return estimator;
        });
    }
    
    private void executeSingleTask(ChangeRecord rec, AtomicInteger curr, int total,
            java.util.Map<String, MultiThreadTaskEstimator> localEstimatorMap, AtomicBoolean anyChange,
            String finalRootPath, AtomicInteger globalExecutedCount,
            java.util.Map<String, AtomicInteger> executedCountByRootPath) {
        synchronized (rec) {
            if (rec.getStatus() == ExecStatus.PENDING &&
                    !FileLockManagerUtil.isLocked(rec.getFileHandle())) {
                if (!FileLockManagerUtil.lock(rec.getFileHandle())) {
                    return;
                }
                // 对原始文件加逻辑锁，避免并发操作同一个文件
                rec.setStatus(ExecStatus.RUNNING);
                anyChange.set(true);
                threadTaskEstimator.oneStarted();
                // 更新根路径估算器
                MultiThreadTaskEstimator rootEstimator = localEstimatorMap.get(finalRootPath);
                if (rootEstimator != null) {
                    rootEstimator.oneStarted();
                }
                // 增加任务数量限制计数器
                globalExecutedCount.incrementAndGet();
                executedCountByRootPath.computeIfAbsent(finalRootPath, k -> new AtomicInteger(0)).incrementAndGet();
            } else {
                return;
            }
        }
        
        try {
            // 执行策略
            IAppStrategy s = AppStrategyFactory.findStrategyForOp(rec.getOpType(), appController.getPipelineStrategies());
            appController.log("▶ 开始处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName());
            if (s != null) {
                s.execute(rec);
                rec.setStatus(ExecStatus.SUCCESS);
                appController.log("✅️ 成功处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName());
            } else {
                rec.setStatus(ExecStatus.SKIPPED);
            }
        } catch (Exception e) {
            rec.setStatus(ExecStatus.FAILED);
            rec.setFailReason(e.getMessage());
            appController.logError("❌ 失败处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName() + ",原因" + e.getMessage());
            appController.logError("❌ 失败详细原因:" + e.getMessage());
        } finally {
            completeSingleTask(rec, curr, total, localEstimatorMap, finalRootPath);
        }
    }
    
    private void completeSingleTask(ChangeRecord rec, AtomicInteger curr, int total,
            java.util.Map<String, MultiThreadTaskEstimator> localEstimatorMap, String finalRootPath) {
        threadTaskEstimator.oneCompleted();
        // 更新根路径估算器
        MultiThreadTaskEstimator rootEstimator = localEstimatorMap.get(finalRootPath);
        if (rootEstimator != null) {
            rootEstimator.oneCompleted();
        }
        // 文件解锁
        FileLockManagerUtil.unlock(rec.getFileHandle());
        int c = curr.incrementAndGet();
        if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
            lastRefresh.set(System.currentTimeMillis());
            appController.setRunningUI("▶ ▶ ▶ 执行任务进度: " + threadTaskEstimator.getDisplayInfo());
            appController.refreshPreviewTableFilter();
        }
    }
    
    private void setStartTaskUI(String msg, Task task) {
        appController.enableStopButton(true);
        isTaskRunning.set(true);
        lastRefresh.set(System.currentTimeMillis());
        appController.updateProgressStatus(TaskStatus.RUNNING);
        appController.bindProgress(task);
        appController.updateRunningProgress(msg);
        appController.refreshPreviewTableFilter();
        appController.updateStats();
    }
    
    private void setFinishTaskUI(String msg, TaskStatus status) {
        appController.enableExecuteButton(false);
        appController.enableStopButton(false);
        isTaskRunning.set(false);
        appController.updateProgressStatus(status);
        appController.updateRunningProgress(msg);
        appController.refreshPreviewTableFilter();
        appController.updateStats();
        currentTask = null;
    }
    
    private void handleTaskLifecycle(Task<?> t) {
        currentTask = t;
        t.setOnFailed(e -> {
            appController.enableExecuteButton(false);
            setFinishTaskUI("❌ ❌ ❌ 出错 ❌ ❌ ❌", TaskStatus.FAILURE);
            appController.logError("❌ 失败: " + e.getSource().getException().getMessage());
        });
        t.setOnCancelled(e -> {
            setFinishTaskUI("🛑 🛑 🛑 已取消 🛑 🛑 🛑", TaskStatus.CANCELED);
        });
    }
    
    public void forceStop() {
        if (isTaskRunning.get()) {
            isTaskRunning.set(false);
            if (currentTask != null) {
                currentTask.cancel();
            }
            threadPoolManager.shutdownAll();
            appController.log("🛑 强制停止");
            setFinishTaskUI("🛑 🛑 🛑 已停止 🛑 🛑 🛑", TaskStatus.CANCELED);
        }
    }
    
    public void setThreadPoolMode(String threadPoolMode) {
        this.threadPoolMode = threadPoolMode;
    }
    
    public String getThreadPoolMode() {
        return threadPoolMode;
    }
    
    public boolean isTaskRunning() {
        return isTaskRunning.get();
    }
    
    public List<ChangeRecord> getFullChangeList() {
        return fullChangeList;
    }
}