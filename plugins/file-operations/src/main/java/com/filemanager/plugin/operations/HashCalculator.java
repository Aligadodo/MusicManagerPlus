package com.filemanager.plugin.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class HashCalculator {
    
    private static final int BUFFER_SIZE = 8192;
    private static final Map<String, String> cache = new HashMap<>();
    
    public static String calculateSHA1(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
        
        String cacheKey = "SHA1_" + file.getAbsolutePath() + "_" + file.lastModified() + "_" + file.length();
        
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] sha1Bytes = digest.digest();
        String sha1Hex = bytesToHex(sha1Bytes);
        
        cache.put(cacheKey, sha1Hex);
        
        return sha1Hex;
    }
    
    public static String calculateSHA256(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
        
        String cacheKey = "SHA256_" + file.getAbsolutePath() + "_" + file.lastModified() + "_" + file.length();
        
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] sha256Bytes = digest.digest();
        String sha256Hex = bytesToHex(sha256Bytes);
        
        cache.put(cacheKey, sha256Hex);
        
        return sha256Hex;
    }
    
    public static String calculateSHA1(String filePath) throws IOException {
        return calculateSHA1(new File(filePath));
    }
    
    public static String calculateSHA256(String filePath) throws IOException {
        return calculateSHA256(new File(filePath));
    }
    
    public static String calculateSHA1(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
        
        byte[] sha1Bytes = digest.digest(data);
        return bytesToHex(sha1Bytes);
    }
    
    public static String calculateSHA256(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
        
        byte[] sha256Bytes = digest.digest(data);
        return bytesToHex(sha256Bytes);
    }
    
    public static boolean compareFilesBySHA1(File file1, File file2) throws IOException {
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
        
        String sha1_1 = calculateSHA1(file1);
        String sha1_2 = calculateSHA1(file2);
        
        return sha1_1.equals(sha1_2);
    }
    
    public static boolean compareFilesBySHA256(File file1, File file2) throws IOException {
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
        
        String sha256_1 = calculateSHA256(file1);
        String sha256_2 = calculateSHA256(file2);
        
        return sha256_1.equals(sha256_2);
    }
    
    public static boolean compareFilesBySHA1(String filePath1, String filePath2) throws IOException {
        return compareFilesBySHA1(new File(filePath1), new File(filePath2));
    }
    
    public static boolean compareFilesBySHA256(String filePath1, String filePath2) throws IOException {
        return compareFilesBySHA256(new File(filePath1), new File(filePath2));
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
