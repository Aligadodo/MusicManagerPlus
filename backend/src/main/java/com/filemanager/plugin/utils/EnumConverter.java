package com.filemanager.plugin.utils;

import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.plugin.enums.PluginEnum;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class EnumConverter {
    
    public static <T extends PluginEnum> List<EnumOptionDTO> convertEnumToDTOs(Class<T> enumClass) {
        List<EnumOptionDTO> dtos = new ArrayList<>();
        
        if (!enumClass.isEnum()) {
            return dtos;
        }
        
        T[] enumConstants = enumClass.getEnumConstants();
        for (T enumConstant : enumConstants) {
            EnumOptionDTO dto = new EnumOptionDTO(
                enumConstant.getCode(),
                enumConstant.getNameZh(),
                enumConstant.getNameEn(),
                enumConstant.getDescriptionZh(),
                enumConstant.getDescriptionEn()
            );
            dtos.add(dto);
        }
        
        return dtos;
    }
    
    public static <T extends PluginEnum> EnumOptionDTO convertEnumToDTO(T enumConstant) {
        if (enumConstant == null) {
            return null;
        }
        
        return new EnumOptionDTO(
            enumConstant.getCode(),
            enumConstant.getNameZh(),
            enumConstant.getNameEn(),
            enumConstant.getDescriptionZh(),
            enumConstant.getDescriptionEn()
        );
    }
}