package com.filemanager.plusv2.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.filemanager.plusv2.type.ConditionType;

import java.io.File;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleCondition {
    public ConditionType type;
    public String value;

    public boolean test(File f) {
        if (f == null) return false;
        String name = f.getName();
        try {
            switch (type) {
                case CONTAINS: return name.contains(value);
                case NOT_CONTAINS: return !name.contains(value);
                case STARTS_WITH: return name.startsWith(value);
                case ENDS_WITH: return name.endsWith(value);
                case REGEX_MATCH: return name.matches(value);
                case FILE_SIZE_GT: return f.length() > Double.parseDouble(value) * 1024 * 1024;
                case FILE_SIZE_LT: return f.length() < Double.parseDouble(value) * 1024 * 1024;
                case PARENT_DIR_IS: return f.getParentFile() != null && f.getParentFile().getName().equals(value);
                case FILE_EXT_IS: return name.toLowerCase().endsWith("." + value.toLowerCase().replace(".", ""));
                default: return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override public String toString() { return type + " [" + value + "]"; }
}