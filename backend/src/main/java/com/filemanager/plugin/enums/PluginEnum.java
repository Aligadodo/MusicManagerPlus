package com.filemanager.plugin.enums;

import com.filemanager.domain.dto.EnumOptionDTO;

import java.util.List;

/**
 * 插件枚举接口
 * 所有用于插件参数的枚举类都应该实现此接口
 * 提供统一的枚举选项获取方式
 */
public interface PluginEnum {
    
    /**
     * 获取枚举的代码值
     * @return 代码值
     */
    String getCode();
    
    /**
     * 获取中文名称
     * @return 中文名称
     */
    String getNameZh();
    
    /**
     * 获取英文名称
     * @return 英文名称
     */
    String getNameEn();
    
    /**
     * 获取中文描述
     * @return 中文描述
     */
    String getDescriptionZh();
    
    /**
     * 获取英文描述
     * @return 英文描述
     */
    String getDescriptionEn();
    
    /**
     * 转换为 EnumOptionDTO
     * @return EnumOptionDTO 对象
     */
    default EnumOptionDTO toEnumOptionDTO() {
        EnumOptionDTO dto = new EnumOptionDTO();
        dto.setValue(getCode());
        dto.setLabel(getNameZh());
        dto.setNameEn(getNameEn());
        dto.setDescriptionZh(getDescriptionZh());
        dto.setDescriptionEn(getDescriptionEn());
        return dto;
    }
    
    /**
     * 转换为 EnumOptionDTO（兼容前端格式）
     * 前端期望的字段名是 code 和 nameZh
     * @return EnumOptionDTO 对象
     */
    default EnumOptionDTO toEnumOptionDTOForFrontend() {
        EnumOptionDTO dto = new EnumOptionDTO();
        dto.setValue(getCode());
        dto.setLabel(getNameZh());
        dto.setNameEn(getNameEn());
        dto.setDescriptionZh(getDescriptionZh());
        dto.setDescriptionEn(getDescriptionEn());
        return dto;
    }
    
    /**
     * 获取所有枚举选项
     * @return 枚举选项列表
     */
    static List<EnumOptionDTO> getEnumOptions(Class<? extends PluginEnum> enumClass) {
        if (!enumClass.isEnum()) {
            return java.util.Collections.emptyList();
        }
        
        try {
            PluginEnum[] enumConstants = (PluginEnum[]) enumClass.getEnumConstants();
            java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
            for (PluginEnum enumConstant : enumConstants) {
                options.add(enumConstant.toEnumOptionDTO());
            }
            return options;
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * 根据代码值获取枚举实例
     * @param code 代码值
     * @param enumClass 枚举类
     * @param defaultValue 默认值
     * @param <T> 枚举类型
     * @return 枚举实例
     */
    static <T extends PluginEnum> T fromCode(String code, Class<T> enumClass, T defaultValue) {
        if (code == null || !enumClass.isEnum()) {
            return defaultValue;
        }
        
        try {
            T[] enumConstants = enumClass.getEnumConstants();
            for (T enumConstant : enumConstants) {
                if (enumConstant.getCode().equals(code)) {
                    return enumConstant;
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return defaultValue;
    }
}
