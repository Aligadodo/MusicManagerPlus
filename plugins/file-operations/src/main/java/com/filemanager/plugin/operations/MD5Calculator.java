package com.filemanager.plugin.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class MD5Calculator {
    
    private static final int BUFFER_SIZE = 8192;
    private static final Map<String, String> cache = new HashMap<>();
    
    public static String calculateMD5(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
        
        String cacheKey = file.getAbsolutePath() + "_" + file.lastModified() + "_" + file.length();
        
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] md5Bytes = digest.digest();
        String md5Hex = bytesToHex(md5Bytes);
        
        cache.put(cacheKey, md5Hex);
        
        return md5Hex;
    }
    
    public static String calculateMD5(String filePath) throws IOException {
        return calculateMD5(new File(filePath));
    }
    
    public static String calculateMD5(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
        
        byte[] md5Bytes = digest.digest(data);
        return bytesToHex(md5Bytes);
    }
    
    public static boolean compareFilesByMD5(File file1, File file2) throws IOException {
        if (file1 == null || file2 == null) {
            return false;
        }
        
        if (file1.equals(file2)) {
            return true;
        }
        
        if (!file1.exists() || !file2.exists()) {
            return false;
        }
        
        if (file1.length() != file2.length()) {
            return false;
        }
        
        String md5_1 = calculateMD5(file1);
        String md5_2 = calculateMD5(file2);
        
        return md5_1.equals(md5_2);
    }
    
    public static boolean compareFilesByMD5(String filePath1, String filePath2) throws IOException {
        return compareFilesByMD5(new File(filePath1), new File(filePath2));
    }
    
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    public static void clearCache() {
        cache.clear();
    }
    
    public static int getCacheSize() {
        return cache.size();
    }
}
