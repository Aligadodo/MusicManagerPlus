package com.filemanager.tool.unzip.engine;

import com.filemanager.tool.unzip.AbstractUnarchiveEngine;
import com.filemanager.tool.unzip.ProgressListener;
import com.filemanager.tool.unzip.UnarchiveTask;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import java.io.*;
import java.nio.file.Files;
import java.util.List;

/**
 * @author 28667
 */
public class JavaBuiltInEngine extends AbstractUnarchiveEngine {

    @Override
    public boolean extract(UnarchiveTask task) {
        File archive = new File(task.archivePath);
        File destDir = new File(task.targetDir);
        
        // 内置引擎无法处理有密码的情况（通常需要特定的加密库支持）
        String pwd = (task.password != null) ? task.password : autoDetectPassword(task.archivePath);

        try (InputStream fi = Files.newInputStream(archive.toPath());
             InputStream bi = new BufferedInputStream(fi);
             ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(bi)) {

            // 获取总条目数用于计算进度（可选，会略微增加耗时）
            // 注意：某些流式格式无法获取条目总数，这里采用估算或文件计数
            long totalFiles = 0; // 如果需要精确进度，建议预扫描或按字节计算

            ArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!in.canReadEntryData(entry)) {
                    if (pwd != null) throw new IOException("内置引擎不支持加密流，请用外部引擎。");
                    continue;
                }

                File target = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!target.isDirectory() && !target.mkdirs()) throw new IOException("无法创建目录: " + target);
                } else {
                    File parent = target.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("无法创建父目录: " + parent);
                    
                    // 覆盖策略判断
                    if (target.exists() && !task.overwrite) continue;

                    // [优化] 使用 64KB 缓冲区进行流拷贝
                    try (OutputStream o = Files.newOutputStream(target.toPath())) {
                        byte[] buffer = new byte[64 * 1024];
                        int n;
                        while (-1 != (n = in.read(buffer))) {
                            o.write(buffer, 0, n);
                        }
                    }
                }
                
                // 触发监听器（内置引擎可以实现更细粒度的回调）
                if (task.listener != null) {
                    // 这里简单示意，实际可根据 entry 计数推算百分比
                    task.listener.onProgress(-1); 
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected List<String> buildCommand(UnarchiveTask task) { return null; } // 无需命令行

    @Override
    protected void parseProgress(String line, ProgressListener listener) { } // 无需解析文本

    @Override
    public boolean isAvailable() {
        // 内置引擎始终可用
        return true;
    }
}