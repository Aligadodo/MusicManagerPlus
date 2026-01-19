package com.filemanager.tool.unzip.engine;

import com.filemanager.tool.unzip.AbstractUnarchiveEngine;
import com.filemanager.tool.unzip.ProgressListener;
import com.filemanager.tool.unzip.UnarchiveTask;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 28667
 */
public class SevenZipEngine extends AbstractUnarchiveEngine {
    public SevenZipEngine(String path) { this.executablePath = path; }

    @Override
    protected List<String> buildCommand(UnarchiveTask task) {
        List<String> cmd = new ArrayList<>();
        cmd.add(executablePath);
        cmd.add("x");
        cmd.add(task.archivePath);
        cmd.add("-o" + task.targetDir);
        cmd.add("-y");
        cmd.add(task.overwrite ? "-aoa" : "-aos");

        // 线程控制
        cmd.add("-mmt=" + (task.threadCount > 0 ? task.threadCount : "on"));

        // 核心性能参数切换
        if (task.listener != null) {
            cmd.add("-bsp1"); // 开启进度信息输出
        } else {
            cmd.add("-bso0"); // 完全静默输出，进一步减少 7z 进程开销
            cmd.add("-bsp0");
        }

        String pwd = (task.password != null) ? task.password : autoDetectPassword(task.archivePath);
        if (pwd != null) cmd.add("-p" + pwd);

        return cmd;
    }

    @Override
    protected void parseProgress(String line, ProgressListener listener) {
        if (line.contains("%")) {
            try {
                String trimmed = line.trim();
                int percentIdx = trimmed.lastIndexOf("%");
                int startIdx = trimmed.lastIndexOf(" ", percentIdx);
                if (startIdx != -1) {
                    String num = trimmed.substring(startIdx + 1, percentIdx);
                    listener.onProgress(Integer.parseInt(num));
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public boolean isAvailable() {
        try { return new ProcessBuilder(executablePath, "-h").start().waitFor() == 0; }
        catch (Exception e) { return false; }
    }
}