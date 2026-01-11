/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.rule;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Rule {
    public Set<String> keywords = new HashSet<>();
    public Set<String> filetypes= new HashSet<>();
    public Pattern regex;
    public long minFileSize;
    public long maxFileSize = Long.MAX_VALUE;
    public boolean reverse;

    public Rule() {
    }

    public Rule(String keywords, String filetypes) {
        this.keywords = getKeywords(keywords);
        this.filetypes = getKeywords(filetypes);
    }

    public Rule(String pattern) {
        this.regex = pattern==null?null: Pattern.compile(pattern);
    }

    private Set<String> getKeywords(String str){
        Set<String> originalKeywords = str==null?new HashSet<>(): Arrays.stream(str.split(",")).collect(Collectors.toSet());
        Set<String> keywords = new HashSet<>();
        originalKeywords.stream().forEach(i -> {
            keywords.add(ZhConverterUtil.toSimple(i.toUpperCase()));
        });
        return keywords;
    }


    public boolean isApply(File file){
        String fileName  = ZhConverterUtil.toSimple(file.getName().toUpperCase());
        if(fileName.indexOf('.') == -1){
            return false;
        }
        String nameOnly = fileName.substring(0, fileName.lastIndexOf('.'));
        String fileType = fileName.substring(fileName.lastIndexOf('.'));

        if(!keywords.isEmpty()){
            boolean match = keywords.stream().anyMatch(nameOnly::contains);
            if(reverse&&match){
                return false;
            }
            if(!reverse&&!match){
                return false;
            }
        }

        if(regex!=null){
            boolean match = regex.matcher(nameOnly).matches();
            if(reverse&&match){
                return false;
            }
            if(!reverse&&!match){
                return false;
            }
        }

        if(!filetypes.isEmpty()){
            if(filetypes.stream().noneMatch(fileType::contains)){
                return false;
            }
        }
        long fileSizeBytes = file.length();
        // 转换为KB
        float fileSizeKB = fileSizeBytes / 1024f;
        if(fileSizeKB < minFileSize){
            return false;
        }
        if(fileSizeKB > maxFileSize){
            return false;
        }
        return true;
    }

    public Rule keywords(Set<String> keywords) {
        this.keywords = keywords;
        return this;
    }

    public Rule keywords(String keywords) {
        this.keywords = getKeywords(keywords);
        return this;
    }

    public Rule filetypes(String filetypes) {
        this.filetypes = getKeywords(filetypes);
        return this;
    }


    public Rule filetypes(Set<String> filetypes) {
        this.filetypes = filetypes;
        return this;
    }

    public Rule regex(String pattern) {
        this.regex = Pattern.compile(pattern);
        return this;
    }

    public Rule regex(Pattern regex) {
        this.regex = regex;
        return this;
    }

    public Rule minFileSize(long minFileSize) {
        this.minFileSize = minFileSize;
        return this;
    }

    public Rule maxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
        return this;
    }

    public Rule reverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    public void execute(){

    }
}