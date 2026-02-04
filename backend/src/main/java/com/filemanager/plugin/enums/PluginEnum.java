package com.filemanager.plugin.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface PluginEnum {
    
    String getCode();
    
    String getNameZh();
    
    String getNameEn();
    
    String getDescriptionZh();
    
    String getDescriptionEn();
    
    @JsonValue
    default String toJson() {
        return getCode();
    }
    
    default String getDisplayName() {
        return getNameZh();
    }
    
    default String getDisplayDescription() {
        return getDescriptionZh();
    }
    
    static <T extends PluginEnum> T fromCode(String code, Class<T> enumClass, T defaultValue) {
        if (code == null) {
            return defaultValue;
        }
        
        try {
            List<T> values = Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toList());
            
            for (T value : values) {
                if (value.getCode().equals(code)) {
                    return value;
                }
            }
        } catch (Exception e) {
            return defaultValue;
        }
        
        return defaultValue;
    }
    
    static <T extends PluginEnum> T fromCode(String code, Class<T> enumClass) {
        return fromCode(code, enumClass, null);
    }
}