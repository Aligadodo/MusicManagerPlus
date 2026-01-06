package com.filemanager.tool.file;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    /**
     * 将原始路径映射到新的根路径下
     * * @param originalPathStr 原始路径，例如 "D:\\catch\\data"
     * @param newRootStr      新的根路径，例如 "C:\\mock"
     * @return 转换后的路径，例如 "C:\\mock\\D\\catch\\data"
     */
    public static String mapToNewRoot(String originalPathStr, String newRootStr) {
        // 1. 转换为 Path 对象并确保是绝对路径
        Path originalPath = Paths.get(originalPathStr).toAbsolutePath();
        Path newRoot = Paths.get(newRootStr);

        // 2. 提取根部（Windows下通常是盘符 D:\）
        Path root = originalPath.getRoot();
        
        // 3. 处理盘符：将 "D:\" 转换为 "D"
        // 这里的 replace 是为了兼容 Windows 的冒号和路径分隔符
        String driveName = root.toString()
                .replace(":", "")
                .replace("\\", "")
                .replace("/", "");

        // 4. 获取除根部以外的剩余路径 (catch\data)
        // relativize 方法会计算从根部到原始路径的相对距离
        Path relativePart = root.relativize(originalPath);

        // 5. 拼接：newRoot + 盘符名 + 相对路径
        Path result = newRoot.resolve(driveName).resolve(relativePart);

        return result.toString();
    }

    public static String fixFolderName(String name) {
        if (name == null) return null;
        // 1. 去除首尾空格
        // 2. 移除 Windows 不允许的末尾点号
        return name.trim().replaceAll("[\\.\\s]+$", "");
    }

    public static void main(String[] args) {
        String oldPath = "D:\\catch\\data";
        String newRoot = "C:\\mock";

        String result = mapToNewRoot(oldPath, newRoot);
        
        System.out.println("原始路径: " + oldPath);
        System.out.println("转换后路径: " + result);
    }
}