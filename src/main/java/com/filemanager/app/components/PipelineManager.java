/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-12
 */
package com.filemanager.app.components;

import com.filemanager.app.base.IAppController;
import com.filemanager.app.base.IAppStrategy;
import com.filemanager.app.tools.MultiThreadTaskEstimator;
import com.filemanager.app.tools.display.FXDialogUtils;
import com.filemanager.app.tools.display.ProgressBarDisplay;
import com.filemanager.app.ui.PreviewView;
import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.AppStrategyFactory;
import com.filemanager.tool.RetryableThreadPool;
import com.filemanager.tool.ThreadPoolManager;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import com.filemanager.type.TaskStatus;
import com.filemanager.util.file.FileLockManagerUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class PipelineManager {
    private final IAppController app;
    private final ThreadPoolManager threadPoolManager;
    private final AtomicLong lastRefresh;
    private final AtomicBoolean isTaskRunning;
    private final Map<String, MultiThreadTaskEstimator> localEstimatorMap = new HashMap<>();
    private final AtomicLong fileSequenceGenerator = new AtomicLong(0);
    private List<ChangeRecord> fullChangeList;
    private Task<?> currentTask;
    private MultiThreadTaskEstimator threadTaskEstimator;

    public PipelineManager(IAppController app, ThreadPoolManager threadPoolManager) {
        this.app = app;
        this.threadPoolManager = threadPoolManager;
        this.lastRefresh = new AtomicLong(System.currentTimeMillis());
        this.isTaskRunning = app.getTaskRunningStatus();
        this.fullChangeList = new ArrayList<>();
    }

    public void resetPipeline() {
        // 清空策略流水线
        app.getPipelineStrategies().clear();

        // 重置状态
        isTaskRunning.set(false);

        // 清空变更列表
        if (fullChangeList != null) {
            fullChangeList.clear();
        }

        // 取消当前任务
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel(true);
        }
        currentTask = null;

        // 清空估算器
        localEstimatorMap.clear();
        threadTaskEstimator = null;

        // 重置文件序列号生成器
        fileSequenceGenerator.set(0);

        app.log("流水线已重置");
    }

    public void runPipelineAnalysis() {
        if (app.getSourceRoots().isEmpty()) {
            FXDialogUtils.showToast(app.getPrimaryStage(), "请先添加源目录！", FXDialogUtils.ToastType.INFO);
            return;
        }
        if (app.getPipelineStrategies().isEmpty()) {
            FXDialogUtils.showToast(app.getPrimaryStage(), "请先添加步骤！",
                    FXDialogUtils.ToastType.INFO);
            return;
        }
        if (isTaskRunning.get()) {
            FXDialogUtils.showToast(app.getPrimaryStage(), "任务执行中，请先停止前面的任务再执行预览！",
                    FXDialogUtils.ToastType.INFO);
            return;
        }
        if (app.getAutoRun().isSelected()) {
            if (!FXDialogUtils.showConfirm("确认执行", "预览完毕会立即执行，确认要执行?")) {
                app.getAutoRun().setSelected(false);
            }
        }

        // 重置任务时间戳
        if (app instanceof com.filemanager.app.FileManagerPlusApp) {
            ((com.filemanager.app.FileManagerPlusApp) app).resetTaskTimestamps();
        }

        isTaskRunning.set(true);
        fullChangeList.clear();
        app.switchView(app.getPreviewView().getViewNode());

        // 捕获所有策略参数
        for (IAppStrategy s : app.getPipelineStrategies()) {
            s.captureParams();
        }

        // 从GlobalSettingsView获取参数
        int minDepth, maxDepth;
        String recursionMode = app.getCbRecursionMode().getValue();
        if ("当前目录".equals(recursionMode)) {
            minDepth = 0;
            maxDepth = 1;
        } else if ("全部文件".equals(recursionMode)) {
            minDepth = 0;
            maxDepth = Integer.MAX_VALUE;
        } else if ("指定目录层级".equals(recursionMode)) {
            minDepth = app.getSpRecursionDepth().getValue();
            maxDepth = app.getSpRecursionDepth().getValue();
        } else if ("目录层级范围".equals(recursionMode)) {
            minDepth = app.getSpMinRecursionDepth().getValue();
            maxDepth = app.getSpMaxRecursionDepth().getValue();
        } else {
            minDepth = 0;
            maxDepth = Integer.MAX_VALUE;
        }
        // 应用预览数量限制
        PreviewView previewView = app.getPreviewView();
        int limit = previewView.getGlobalPreviewLimit();
        AtomicInteger globalLimitRemaining = new AtomicInteger(limit);
        Task<List<ChangeRecord>> task = new Task<List<ChangeRecord>>() {
            @Override
            protected List<ChangeRecord> call() throws Exception {
                // 同步根路径线程配置
                syncRootPathThreadConfig();
                updateMessage("▶ ▶ ▶ 扫描源文件...");
                List<File> initialFiles = new ArrayList<>();
                for (File r : app.getSourceRoots()) {
                    if (isCancelled()) break;
                    int rootLimit = previewView.getRootPathPreviewLimit(r.getAbsolutePath());
                    AtomicInteger dirLimitRemaining = new AtomicInteger(rootLimit);
                    initialFiles.addAll(app.scanFilesRobust(r, minDepth, maxDepth, globalLimitRemaining, dirLimitRemaining, msg -> app.setRunningUI("▶ ▶ ▶ " + msg)));
                }
                if (isCancelled()) return null;
                app.setRunningUI("▶ ▶ ▶ 扫描完成，共 " + initialFiles.size() + " 个文件。");
                List<ChangeRecord> currentRecords = initialFiles.stream()
                        .map(f -> {
                            ChangeRecord record = new ChangeRecord(f.getName(), f.getName(), f, false, f.getAbsolutePath(), OperationType.NONE);
                            // 为每个记录分配唯一id
                            record.setId(fileSequenceGenerator.incrementAndGet());
                            return record;
                        })
                        .collect(Collectors.toList());

                int total = currentRecords.size();
                AtomicInteger processed = new AtomicInteger(0);
                threadTaskEstimator = new MultiThreadTaskEstimator(total, Math.max(Math.min(50, total / 20), 1));
                threadTaskEstimator.start();
                ConcurrentLinkedDeque<ChangeRecord> newRecords = new ConcurrentLinkedDeque<>();
                // 过滤掉重复的记录（通过id）
                Set<Long> existingIds = new HashSet<>();

                // 先添加currentRecords中的id到existingIds，确保不与新记录重复
                for (ChangeRecord record : currentRecords) {
                    existingIds.add(record.getId());
                }

                currentRecords.parallelStream().forEach(rec -> {
                    try {
                        int curr = processed.incrementAndGet();
                        Platform.runLater(() -> updateProgress(curr, total));
                        if (isCancelled()) {
                            return;
                        }
                        for (int i = 0; i < app.getPipelineStrategies().size(); i++) {
                            IAppStrategy strategy = app.getPipelineStrategies().get(i);
                            List<ChangeRecord> newRecordAfter = strategy.analyzeWithPreCheck(rec, currentRecords, app.getSourceRoots());
                            // 为新记录分配唯一id
                            newRecordAfter.stream().filter(record -> {
                                long id = record.getId();
                                return id == 0 || !existingIds.contains(id);
                            }).forEach(newRecord -> {
                                if (newRecord.getId() == 0) {
                                    newRecord.setId(fileSequenceGenerator.incrementAndGet());
                                }
                                newRecords.add(newRecord);
                            });
                        }
                    } catch (Exception e) {
                        rec.setStatus(ExecStatus.ANALYZE_FAILED);
                        rec.setFailReason(ExceptionUtils.getStackTrace(e));
                        app.logError("❌ 分析失败: " + rec.getFileHandle().getAbsolutePath() + ",原因" + e.getMessage());
                        app.logError("❌ 失败详细原因:" + ExceptionUtils.getStackTrace(e));
                    } finally {
                        threadTaskEstimator.oneCompleted();
                        if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
                            app.setRunningUI("▶ ▶ ▶ 预览任务进度: " + threadTaskEstimator.getDisplayInfo());
                            lastRefresh.set(System.currentTimeMillis());
                        }
                    }
                });

                if (!newRecords.isEmpty()) {
                    currentRecords.addAll(newRecords);
                }
                return currentRecords;
            }
        };

        setStartTaskUI("▶ ▶ ▶ 预览中...", task);

        task.setOnSucceeded(e -> {
            fullChangeList = task.getValue();
            app.setFullChangeList(fullChangeList);
            setFinishTaskUI("➡ ➡ ➡ 预览完成 ⬅ ⬅ ⬅", TaskStatus.SUCCESS);
            boolean hasChanges = fullChangeList.stream().anyMatch(ChangeRecord::isChanged);
            app.changeExecuteButton(hasChanges);
            if (hasChanges && app.getAutoRun().isSelected()) {
                runPipelineExecution();
            }
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

        // 重置任务时间戳
        if (app instanceof com.filemanager.app.FileManagerPlusApp) {
            ((com.filemanager.app.FileManagerPlusApp) app).resetTaskTimestamps();
        }

        isTaskRunning.set(true);

        prepareExecutionUI();

        Task<Void> task = createExecutionTask();
        setStartTaskUI("▶ ▶ ▶ 执行中...", task);
        task.setOnSucceeded(e -> setFinishTaskUI("➡ ➡ ➡ 执行成功 ⬅ ⬅ ⬅", TaskStatus.SUCCESS));

        handleTaskLifecycle(task);
        new Thread(task).start();
    }

    /**
     * 执行指定的任务列表
     */
    public void runPipelineExecution(List<ChangeRecord> targetRecords) {
        if (targetRecords == null || targetRecords.isEmpty()) {
            return;
        }

        // 过滤出待执行的任务
        List<ChangeRecord> pendingRecords = targetRecords.stream()
                .filter(record -> record.isChanged() && record.getOpType() != OperationType.NONE && record.getStatus() == ExecStatus.PENDING)
                .collect(Collectors.toList());

        if (pendingRecords.isEmpty()) {
            return;
        }

        // 重置任务时间戳
        if (app instanceof com.filemanager.app.FileManagerPlusApp) {
            ((com.filemanager.app.FileManagerPlusApp) app).resetTaskTimestamps();
        }

        isTaskRunning.set(true);

        prepareExecutionUI();

        Task<Void> task = createExecutionTask(pendingRecords);
        setStartTaskUI("▶ ▶ ▶ 执行中...", task);
        task.setOnSucceeded(e -> setFinishTaskUI("➡ ➡ ➡ 执行成功 ⬅ ⬅ ⬅", TaskStatus.SUCCESS));

        handleTaskLifecycle(task);
        new Thread(task).start();
    }

    /**
     * 执行单个任务
     */
    public void runPipelineExecution(ChangeRecord record) {
        if (record == null) {
            return;
        }

        runPipelineExecution(Collections.singletonList(record));
    }

    private long countPendingTasks() {
        return fullChangeList.stream()
                .filter(record -> record.isChanged()
                        && record.getStatus() == ExecStatus.PENDING)
                .count();
    }

    private boolean confirmExecution(long count) {
        if (app.getAutoRun().isSelected()) {
            return true;
        }
        return FXDialogUtils.showConfirm("确认", "执行 " + count + " 个变更?");
    }

    private void prepareExecutionUI() {
        app.changePreviewButton(true);
        app.changeExecuteButton(false);
        app.switchView(app.getPreviewView().getViewNode());
    }

    private Task<Void> createExecutionTask() {
        // 同步根路径线程配置
        syncRootPathThreadConfig();
        List<ChangeRecord> todos = fullChangeList.stream()
                .filter(record -> record.isChanged()
                        && record.getOpType() != OperationType.NONE
                        && record.getStatus() == ExecStatus.PENDING)
                .collect(Collectors.toList());

        return createExecutionTask(todos);
    }

    private Task<Void> createExecutionTask(List<ChangeRecord> todos) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int total = todos.size();
                AtomicInteger curr = new AtomicInteger(0);

                // 线程池和估算器管理
                localEstimatorMap.clear();

                // 任务数量限制计数器
                final java.util.Map<String, AtomicInteger> executedCountByRootPath = new java.util.concurrent.ConcurrentHashMap<>();
                final AtomicInteger globalExecutedCount = new AtomicInteger(0);

                // 创建全局估算器
                threadTaskEstimator = new MultiThreadTaskEstimator(total, Math.max(Math.min(20, total / 20), 1));
                threadTaskEstimator.start();
                app.log("▶ ▶ ▶ 任务启动，并发线程: " + app.getSpExecutionThreads().getValue());
                app.log("▶ ▶ ▶ 当前线程池模式: " + threadPoolManager.getThreadPoolMode());
                app.log("▶ ▶ ▶ 注意：部分任务依赖同一个原始文件，会因为加锁导致串行执行，任务会一直轮询！");
                app.log("▶ ▶ ▶ 开始任务执行，总待执行任务数：" + todos.size());


                while (!todos.isEmpty() && !isCancelled() && todos.stream().anyMatch(rec -> rec.getStatus() == ExecStatus.PENDING)) {
                    AtomicBoolean anyChange = new AtomicBoolean(false);
                    for (ChangeRecord rec : todos) {
                        if (isCancelled()) {
                            break;
                        }
                        if (threadTaskEstimator.getRunningTaskCount() > app.getSpExecutionThreads().getValue()) {
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
                        String rootPath = app.findRootPathForFile(sourcePath);

                        // 检查任务数量限制
                        boolean exceedLimit = checkExecutionLimits(rootPath, globalExecutedCount, executedCountByRootPath);
                        if (exceedLimit) {
                            rec.setFailReason("已超出执行限制，忽略接下来的操作！！！");
                            rec.setStatus(ExecStatus.SKIPPED);
                            threadTaskEstimator.oneCompleted();
                            continue;
                        }

                        // 预增加计数器，防止并发问题
                        globalExecutedCount.incrementAndGet();
                        executedCountByRootPath.computeIfAbsent(rootPath, k -> new AtomicInteger(0)).incrementAndGet();

                        // 获取执行线程池
                        RetryableThreadPool sourceExecutor = threadPoolManager.getExecutionThreadPool(rootPath);

                        // 获取或创建该根路径的任务估算器
                        createRootPathEstimatorIfNeeded(localEstimatorMap, rootPath, todos);

                        final String finalRootPath = rootPath;
                        sourceExecutor.execute(() -> executeSingleTask(rec, curr, total, localEstimatorMap, anyChange,
                                finalRootPath, globalExecutedCount, executedCountByRootPath));
                    }

                    // 更新任务进度（每次循环都更新）
                    long completed = threadTaskEstimator.getCompletedTasks();
                    updateProgress(completed, total);

                    // 适当Sleep，避免反复刷数据
                    // 定期更新根路径进度UI和文本信息
                    if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
                        lastRefresh.set(System.currentTimeMillis());
                        app.setRunningUI("▶ ▶ ▶ 执行任务进度: " + threadTaskEstimator.getDisplayInfo());
                        app.refreshPreviewTableFilter();
                        // 更新根路径进度UI
                        app.getPreviewView().updateRootPathProgress();
                    }
                    Thread.sleep(100);
                }

                // 确保进度条显示100%
                updateProgress(total, total);

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
        PreviewView previewView = app.getPreviewView();
        boolean exceedLimit = globalExecutedCount.get() >= previewView.getGlobalExecutionLimit();
        // 检查全局执行数量限制
        // 检查根路径执行数量限制
        AtomicInteger rootExecutedCount = executedCountByRootPath.computeIfAbsent(rootPath, k -> new AtomicInteger(0));
        if (rootExecutedCount.get() >= previewView.getRootPathExecutionLimit(rootPath)) {
            exceedLimit = true;
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
                        return app.findRootPathForFile(filePath).equals(k);
                    })
                    .count();
            MultiThreadTaskEstimator estimator = new MultiThreadTaskEstimator(rootTaskCount, Math.max(Math.min(20, (int) rootTaskCount / 20), 1));
            estimator.start();
            app.log("▶ ▶ ▶ 为根路径创建任务估算器: " + k + "，总任务数: " + rootTaskCount);
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
                // 计数器已在任务提交前增加，这里不再重复增加
            } else {
                return;
            }
        }

        try {
            // 执行策略
            IAppStrategy s = AppStrategyFactory.findStrategyForOp(rec.getOpType(), app.getPipelineStrategies());
            app.log("▶ 开始处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName());
            if (s != null) {
                s.execute(rec);
                rec.setStatus(ExecStatus.SUCCESS);
                app.log("✅️ 成功处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName());
            } else {
                rec.setFailReason("没找到对应的执行节点，请检查代码实现！！！");
                rec.setStatus(ExecStatus.SKIPPED);
            }
        } catch (TimeoutException e) {
            rec.setStatus(ExecStatus.FAILED);
            rec.setFailReason("Execution timeout: " + e.getMessage());
            app.logError("⏰ 执行超时: " + rec.getFileHandle().getAbsolutePath() + ",原因" + e.getMessage());
        } catch (Exception e) {
            rec.setStatus(ExecStatus.FAILED);
            rec.setFailReason(ExceptionUtils.getStackTrace(e));
            app.logError("❌ 失败处理: " + rec.getFileHandle().getAbsolutePath() + "，操作类型：" + rec.getOpType().getName() + ",目标路径：" + rec.getNewName() + ",原因" + e.getMessage());
            app.logError("❌ 失败详细原因:" + ExceptionUtils.getStackTrace(e));
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

        if (System.currentTimeMillis() - lastRefresh.get() > 1000) {
            lastRefresh.set(System.currentTimeMillis());
            app.setRunningUI("▶ ▶ ▶ 执行任务进度: " + threadTaskEstimator.getDisplayInfo());
            app.refreshPreviewTableFilter();
            // 更新根路径进度UI
            app.getPreviewView().updateRootPathProgress();
        }
    }

    private void setStartTaskUI(String msg, Task<?> task) {
        app.changeStopButton(true);
        isTaskRunning.set(true);
        lastRefresh.set(System.currentTimeMillis());
        app.updateProgressStatus(TaskStatus.RUNNING);
        app.bindProgress(task);
        app.updateRunningProgress(msg);
        app.refreshPreviewTableFilter();
        app.updateStats();

        ProgressBarDisplay.updateProgressStatus(app.getPreviewView().getMainProgressBar(), TaskStatus.RUNNING);
        app.getPreviewView().getMainProgressBar().progressProperty().unbind();
        app.getPreviewView().getMainProgressBar().progressProperty().set(0);
        if (task != null) {
            app.getPreviewView().getMainProgressBar().progressProperty().bind(task.progressProperty());
        }

        // 禁用线程池模式下拉框
        Platform.runLater(() -> {
            app.getPreviewView().getCbThreadPoolMode().setDisable(true);
        });
    }

    /**
     * 状态,建议颜色,Hex 代码,视觉感受
     * 执行中 (Running),天蓝色,#BDE0FE,清爽、宁静，表示正在进行
     * 成功 (Success),薄荷绿,#B9FBC0,健康、完成，给予正面反馈
     * 失败 (Failure),珊瑚粉,#FFADAD,柔和的警告，不刺眼但明确
     * 取消 (Canceled),奶油黄/淡灰,#FDFFB6,中性色，表示任务已停止
     *
     * @param msg
     * @param status
     */
    private void setFinishTaskUI(String msg, TaskStatus status) {
        app.changePreviewButton(true);
        app.changeStopButton(false);
        isTaskRunning.set(false);
        app.updateProgressStatus(status);
        app.updateRunningProgress(msg);
        // 固化任务结束时间
        if (app instanceof com.filemanager.app.FileManagerPlusApp) {
            ((com.filemanager.app.FileManagerPlusApp) app).setTaskEndTimestamp(System.currentTimeMillis());
        }

        app.updateStats();
        if (TaskStatus.CANCELED == status) {
            app.getPreviewView().getMainProgressBar().progressProperty().unbind();
            app.getPreviewView().getMainProgressBar().progressProperty().set(0);
        }
        if (TaskStatus.SUCCESS == status) {
            app.getPreviewView().getMainProgressBar().progressProperty().unbind();
            app.getPreviewView().getMainProgressBar().progressProperty().set(1.0);

            // 强制更新所有根路径进度条到100%
            Platform.runLater(() -> {
                app.getPreviewView().updateRootPathProgress();
            });
        }
        // 设置进度条为颜色
        ProgressBarDisplay.updateProgressStatus(app.getPreviewView().getMainProgressBar(), status);
        currentTask = null;

        // 强制刷新预览列表，确保显示最新状态
        Platform.runLater(() -> {
            app.refreshPreviewTableFilter();
        });

        // 启用线程池模式下拉框
        Platform.runLater(() -> {
            app.getPreviewView().getCbThreadPoolMode().setDisable(false);
        });
    }

    private void handleTaskLifecycle(Task<?> t) {
        currentTask = t;
        t.setOnFailed(e -> {
            setFinishTaskUI("❌ ❌ ❌ 出错 ❌ ❌ ❌", TaskStatus.FAILURE);
            app.logError("❌ 失败: " + e.getSource().getException().getMessage());
        });
        t.setOnCancelled(e -> {
            setFinishTaskUI("🛑 🛑 🛑 已取消 🛑 🛑 🛑", TaskStatus.CANCELED);
        });
    }

    /**
     * 同步根路径线程配置到线程池管理器
     */
    private void syncRootPathThreadConfig() {
        for (File root : app.getSourceRoots()) {
            String rootPath = root.getAbsolutePath();
            int previewThreads = app.getRootPathThreadConfig().getOrDefault(rootPath + "_preview", app.getSpPreviewThreads().getValue());
            int executionThreads = app.getRootPathThreadConfig().getOrDefault(rootPath, app.getSpExecutionThreads().getValue());
            threadPoolManager.setRootPathPreviewThreads(rootPath, previewThreads);
            threadPoolManager.setRootPathExecutionThreads(rootPath, executionThreads);
        }
    }

    public void forceStop() {
        if (isTaskRunning.get()) {
            isTaskRunning.set(false);
            if (currentTask != null) {
                currentTask.cancel();
            }
            threadPoolManager.shutdownAll();
            app.log("🛑 强制停止");
            setFinishTaskUI("🛑 🛑 🛑 已停止 🛑 🛑 🛑", TaskStatus.CANCELED);
        }
    }

    public MultiThreadTaskEstimator getRootPathEstimator(String rootPath) {
        return this.localEstimatorMap.get(rootPath);
    }
}