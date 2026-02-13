package com.filemanager.plugin.impl.advancedrename.model;

import com.filemanager.plugin.impl.advancedrename.enums.ConditionType;
import java.io.File;

public class RuleCondition {
    private ConditionType type;
    private String value;

    public RuleCondition() {
    }

    public RuleCondition(ConditionType type, String value) {
        this.type = type;
        this.value = value;
    }

    public boolean test(String fileName) {
        if (fileName == null || type == null) {
            return false;
        }

        File file = new File(fileName);
        String name = file.getName();
        String path = file.getAbsolutePath();
        String ext = getExtension(name);

        try {
            switch (type) {
                case CONTAINS:
                    return name.contains(value);
                case NOT_CONTAINS:
                    return !name.contains(value);
                case STARTS_WITH:
                    return name.startsWith(value);
                case ENDS_WITH:
                    return name.endsWith(value);
                case REGEX_MATCH:
                    return name.matches(value);
                case FILE_SIZE_GT:
                    return file.length() > parseSize(value);
                case FILE_SIZE_LT:
                    return file.length() < parseSize(value);
                case PARENT_DIR_IS:
                    return file.getParentFile() != null && file.getParentFile().getName().equals(value);
                case PATH_CONTAINS:
                    return path.contains(value);
                case PATH_NOT_CONTAINS:
                    return !path.contains(value);
                case EXT_IN:
                    return checkExtensionList(ext, value, true);
                case EXT_NOT_IN:
                    return checkExtensionList(ext, value, false);
                case IS_DIRECTORY:
                    return file.isDirectory();
                case IS_FILE:
                    return file.isFile();
                default:
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private long parseSize(String val) {
        try {
            return (long) (Double.parseDouble(val) * 1024 * 1024);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean checkExtensionList(String currentExt, String configStr, boolean matchIfIn) {
        if (configStr == null || configStr.isEmpty()) {
            return false;
        }

        String[] exts = configStr.split(",");
        for (String ext : exts) {
            if (ext.trim().equalsIgnoreCase(currentExt)) {
                return matchIfIn;
            }
        }

        return !matchIfIn;
    }

    public ConditionType getType() {
        return type;
    }

    public void setType(ConditionType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}