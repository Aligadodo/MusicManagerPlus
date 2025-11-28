package com.filemanager.rule;

import java.io.File;

public class FileClassifyRule extends Rule{
    public File destPath;

    public FileClassifyRule(String keywords, String filetypes, long minFileSize, String destPath) {
        super(keywords, filetypes);
        this.minFileSize(minFileSize);
        this.destPath = new File(destPath);
    }

    public FileClassifyRule(String keywords, String filetypes, long minFileSize, long maxFileSize, String destPath) {
        super(keywords, filetypes);
        this.minFileSize(minFileSize);
        this.maxFileSize(maxFileSize);
        this.destPath = new File(destPath);
    }

    public FileClassifyRule(String pattern, String keywords, String filetypes, long minFileSize, long maxFileSize, String destPath) {
        super(keywords, filetypes);
        this.regex(pattern);
        this.minFileSize(minFileSize);
        this.maxFileSize(maxFileSize);
        this.destPath = new File(destPath);
    }

    public File getDestPath(File file) {
        String fileName = file.getName();
        String fileType = fileName.substring(fileName.lastIndexOf('.')+1);
        return new File(destPath.getPath()+"\\"+fileType.toLowerCase());
    }
}