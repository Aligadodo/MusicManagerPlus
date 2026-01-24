package com.filemanager.tool.unzip.engine;

import com.filemanager.tool.unzip.AbstractUnarchiveEngine;
import com.filemanager.tool.unzip.ProgressListener;
import com.filemanager.tool.unzip.UnarchiveTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BandizipEngine extends AbstractUnarchiveEngine {
    public BandizipEngine(String path) {
        this.executablePath = path;
    }

    @Override
    protected List<String> buildCommand(UnarchiveTask task) {
        List<String> cmd = new ArrayList<>();
        cmd.add(executablePath);
        cmd.add("x");
        cmd.add("-o:" + task.targetDir);
        cmd.add(task.overwrite ? "-aoa" : "-aos");
        cmd.add("-y");

        String pwd = (task.password != null) ? task.password : autoDetectPassword(task.archivePath);
        if (pwd != null) cmd.add("-p:" + pwd);

        cmd.add(task.archivePath);
        return cmd;
    }

    @Override
    protected void parseProgress(String line, ProgressListener listener) {
        // bc.exe 进度解析需特定版本，此处通常留空或记录简要状态
    }

    @Override
    public boolean isAvailable() {
        return new File(executablePath).exists();
    }
}