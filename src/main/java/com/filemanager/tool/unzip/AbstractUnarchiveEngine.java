package com.filemanager.tool.unzip;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class AbstractUnarchiveEngine implements UnarchiveEngine {
    protected String executablePath;

    // 自动识别系统编码
    protected final Charset sysCharset = System.getProperty("os.name").toLowerCase().contains("win")
            ? Charset.forName("GBK") : StandardCharsets.UTF_8;

    @Override
    public boolean extract(UnarchiveTask task) {
        try {
            List<String> command = buildCommand(task);
            ProcessBuilder pb = new ProcessBuilder(command);

            // 性能优化逻辑
            if (task.listener == null) {
                // 【模式 A：高性能静默模式】
                // 直接让 OS 将输出丢向黑洞，不占用 Java 堆内存，不产生 IO 阻塞
                String nullDevice = System.getProperty("os.name").toLowerCase().contains("win") ? "NUL" : "/dev/null";
                pb.redirectOutput(ProcessBuilder.Redirect.to(new File(nullDevice)));
                pb.redirectError(ProcessBuilder.Redirect.to(new File(nullDevice)));

                Process process = pb.start();
                return process.waitFor() == 0;
            } else {
                // 【模式 B：进度监听模式】
                // 必须通过管道读取流
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), sysCharset))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        parseProgress(line, task.listener);
                    }
                }
                return process.waitFor() == 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected String autoDetectPassword(String archivePath) {
        File file = new File(archivePath);
        File pwdFile = new File(file.getParent(), "password.txt");
        if (pwdFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(pwdFile))) {
                return br.readLine().trim();
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    protected abstract List<String> buildCommand(UnarchiveTask task);

    protected abstract void parseProgress(String line, ProgressListener listener);
}