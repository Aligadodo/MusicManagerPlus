package com.filemanager.plugin.operations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RenameConditionEvaluator {
    
    public enum ConditionType {
        FILE_NAME_CONTAINS,
        FILE_NAME_STARTS_WITH,
        FILE_NAME_ENDS_WITH,
        FILE_NAME_MATCHES_REGEX,
        FILE_SIZE_GREATER_THAN,
        FILE_SIZE_LESS_THAN,
        FILE_SIZE_EQUALS,
        FILE_EXTENSION_EQUALS,
        FILE_EXTENSION_IN,
        FILE_EXTENSION_NOT_IN,
        FILE_EXISTS,
        FILE_IS_DIRECTORY,
        FILE_IS_READONLY,
        METADATA_FIELD_EXISTS,
        METADATA_FIELD_EQUALS,
        METADATA_FIELD_CONTAINS,
        METADATA_FIELD_GREATER_THAN,
        METADATA_FIELD_LESS_THAN,
        CUSTOM_CONDITION
    }
    
    public enum LogicalOperator {
        AND,
        OR,
        NOT
    }
    
    public static class Condition {
        private ConditionType type;
        private String field;
        private String value;
        private String pattern;
        private boolean caseSensitive;
        private boolean negate;
        private LogicalOperator operator;
        private List<Condition> subConditions;
        
        public Condition() {
            this.caseSensitive = false;
            this.negate = false;
            this.operator = LogicalOperator.AND;
            this.subConditions = new ArrayList<>();
        }
        
        public Condition(ConditionType type, String value) {
            this();
            this.type = type;
            this.value = value;
        }
        
        public ConditionType getType() {
            return type;
        }
        
        public void setType(ConditionType type) {
            this.type = type;
        }
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        
        public boolean isCaseSensitive() {
            return caseSensitive;
        }
        
        public void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }
        
        public boolean isNegate() {
            return negate;
        }
        
        public void setNegate(boolean negate) {
            this.negate = negate;
        }
        
        public LogicalOperator getOperator() {
            return operator;
        }
        
        public void setOperator(LogicalOperator operator) {
            this.operator = operator;
        }
        
        public List<Condition> getSubConditions() {
            return subConditions;
        }
        
        public void addSubCondition(Condition condition) {
            if (condition != null) {
                subConditions.add(condition);
            }
        }
        
        public void addSubConditions(List<Condition> conditions) {
            if (conditions != null) {
                subConditions.addAll(conditions);
            }
        }
    }
    
    public static class EvaluationResult {
        private boolean matched;
        private String reason;
        private List<String> matchedConditions;
        private List<String> failedConditions;
        
        public EvaluationResult(boolean matched) {
            this.matched = matched;
            this.reason = matched ? "Condition matched" : "Condition not matched";
            this.matchedConditions = new ArrayList<>();
            this.failedConditions = new ArrayList<>();
        }
        
        public boolean isMatched() {
            return matched;
        }
        
        public void setMatched(boolean matched) {
            this.matched = matched;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public List<String> getMatchedConditions() {
            return matchedConditions;
        }
        
        public void addMatchedCondition(String condition) {
            this.matchedConditions.add(condition);
        }
        
        public List<String> getFailedConditions() {
            return failedConditions;
        }
        
        public void addFailedCondition(String condition) {
            this.failedConditions.add(condition);
        }
    }
    
    private Map<String, Object> metadata;
    
    public RenameConditionEvaluator() {
        this.metadata = null;
    }
    
    public RenameConditionEvaluator(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public EvaluationResult evaluate(File file, Condition condition) {
        EvaluationResult result = new EvaluationResult(false);
        
        if (condition == null) {
            result.setMatched(true);
            result.setReason("No condition specified");
            return result;
        }
        
        boolean matched = evaluateCondition(file, condition);
        
        if (condition.isNegate()) {
            matched = !matched;
        }
        
        result.setMatched(matched);
        result.setReason(matched ? "Condition matched" : "Condition not matched");
        
        return result;
    }
    
    public EvaluationResult evaluate(String filePath, Condition condition) {
        return evaluate(new File(filePath), condition);
    }
    
    public boolean evaluateCondition(File file, Condition condition) {
        if (condition == null || !file.exists()) {
            return false;
        }
        
        switch (condition.getType()) {
            case FILE_NAME_CONTAINS:
                return evaluateFileNameContains(file, condition);
            case FILE_NAME_STARTS_WITH:
                return evaluateFileNameStartsWith(file, condition);
            case FILE_NAME_ENDS_WITH:
                return evaluateFileNameEndsWith(file, condition);
            case FILE_NAME_MATCHES_REGEX:
                return evaluateFileNameMatchesRegex(file, condition);
            case FILE_SIZE_GREATER_THAN:
                return evaluateFileSizeGreaterThan(file, condition);
            case FILE_SIZE_LESS_THAN:
                return evaluateFileSizeLessThan(file, condition);
            case FILE_SIZE_EQUALS:
                return evaluateFileSizeEquals(file, condition);
            case FILE_EXTENSION_EQUALS:
                return evaluateFileExtensionEquals(file, condition);
            case FILE_EXTENSION_IN:
                return evaluateFileExtensionIn(file, condition);
            case FILE_EXTENSION_NOT_IN:
                return evaluateFileExtensionNotIn(file, condition);
            case FILE_EXISTS:
                return file.exists();
            case FILE_IS_DIRECTORY:
                return file.isDirectory();
            case FILE_IS_READONLY:
                return !file.canWrite();
            case METADATA_FIELD_EXISTS:
                return evaluateMetadataFieldExists(condition);
            case METADATA_FIELD_EQUALS:
                return evaluateMetadataFieldEquals(condition);
            case METADATA_FIELD_CONTAINS:
                return evaluateMetadataFieldContains(condition);
            case METADATA_FIELD_GREATER_THAN:
                return evaluateMetadataFieldGreaterThan(condition);
            case METADATA_FIELD_LESS_THAN:
                return evaluateMetadataFieldLessThan(condition);
            case CUSTOM_CONDITION:
                return evaluateCustomCondition(file, condition);
            default:
                return false;
        }
    }
    
    private boolean evaluateFileNameContains(File file, Condition condition) {
        String fileName = file.getName();
        String value = condition.getValue();
        
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        if (condition.isCaseSensitive()) {
            return fileName.contains(value);
        } else {
            return fileName.toLowerCase().contains(value.toLowerCase());
        }
    }
    
    private boolean evaluateFileNameStartsWith(File file, Condition condition) {
        String fileName = file.getName();
        String value = condition.getValue();
        
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        if (condition.isCaseSensitive()) {
            return fileName.startsWith(value);
        } else {
            return fileName.toLowerCase().startsWith(value.toLowerCase());
        }
    }
    
    private boolean evaluateFileNameEndsWith(File file, Condition condition) {
        String fileName = file.getName();
        String value = condition.getValue();
        
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        if (condition.isCaseSensitive()) {
            return fileName.endsWith(value);
        } else {
            return fileName.toLowerCase().endsWith(value.toLowerCase());
        }
    }
    
    private boolean evaluateFileNameMatchesRegex(File file, Condition condition) {
        String fileName = file.getName();
        String pattern = condition.getPattern();
        
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        
        try {
            int flags = condition.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
            return Pattern.compile(pattern, flags).matcher(fileName).matches();
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean evaluateFileSizeGreaterThan(File file, Condition condition) {
        String value = condition.getValue();
        
        try {
            long size = Long.parseLong(value);
            return file.length() > size;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean evaluateFileSizeLessThan(File file, Condition condition) {
        String value = condition.getValue();
        
        try {
            long size = Long.parseLong(value);
            return file.length() < size;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean evaluateFileSizeEquals(File file, Condition condition) {
        String value = condition.getValue();
        
        try {
            long size = Long.parseLong(value);
            return file.length() == size;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean evaluateFileExtensionEquals(File file, Condition condition) {
        String fileName = file.getName();
        String value = condition.getValue();
        
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(fileName);
        
        if (condition.isCaseSensitive()) {
            return extension.equals(value);
        } else {
            return extension.equalsIgnoreCase(value);
        }
    }
    
    private boolean evaluateFileExtensionIn(File file, Condition condition) {
        String fileName = file.getName();
        String value = condition.getValue();
        
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(fileName);
        String[] extensions = value.split(",");
        
        for (String ext : extensions) {
            ext = ext.trim();
            
            if (condition.isCaseSensitive()) {
                if (extension.equals(ext)) {
                    return true;
                }
            } else {
                if (extension.equalsIgnoreCase(ext)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean evaluateFileExtensionNotIn(File file, Condition condition) {
        return !evaluateFileExtensionIn(file, condition);
    }
    
    private boolean evaluateMetadataFieldExists(Condition condition) {
        if (metadata == null) {
            return false;
        }
        
        String field = condition.getField();
        return field != null && !field.isEmpty() && metadata.containsKey(field);
    }
    
    private boolean evaluateMetadataFieldEquals(Condition condition) {
        if (metadata == null) {
            return false;
        }
        
        String field = condition.getField();
        String value = condition.getValue();
        
        if (field == null || field.isEmpty() || value == null) {
            return false;
        }
        
        Object fieldValue = metadata.get(field);
        
        if (fieldValue == null) {
            return false;
        }
        
        if (condition.isCaseSensitive()) {
            return fieldValue.toString().equals(value);
        } else {
            return fieldValue.toString().equalsIgnoreCase(value);
        }
    }
    
    private boolean evaluateMetadataFieldContains(Condition condition) {
        if (metadata == null) {
            return false;
        }
        
        String field = condition.getField();
        String value = condition.getValue();
        
        if (field == null || field.isEmpty() || value == null) {
            return false;
        }
        
        Object fieldValue = metadata.get(field);
        
        if (fieldValue == null) {
            return false;
        }
        
        if (condition.isCaseSensitive()) {
            return fieldValue.toString().contains(value);
        } else {
            return fieldValue.toString().toLowerCase().contains(value.toLowerCase());
        }
    }
    
    private boolean evaluateMetadataFieldGreaterThan(Condition condition) {
        if (metadata == null) {
            return false;
        }
        
        String field = condition.getField();
        String value = condition.getValue();
        
        if (field == null || field.isEmpty() || value == null) {
            return false;
        }
        
        Object fieldValue = metadata.get(field);
        
        if (fieldValue == null) {
            return false;
        }
        
        try {
            double fieldValueNum = Double.parseDouble(fieldValue.toString());
            double valueNum = Double.parseDouble(value);
            return fieldValueNum > valueNum;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean evaluateMetadataFieldLessThan(Condition condition) {
        if (metadata == null) {
            return false;
        }
        
        String field = condition.getField();
        String value = condition.getValue();
        
        if (field == null || field.isEmpty() || value == null) {
            return false;
        }
        
        Object fieldValue = metadata.get(field);
        
        if (fieldValue == null) {
            return false;
        }
        
        try {
            double fieldValueNum = Double.parseDouble(fieldValue.toString());
            double valueNum = Double.parseDouble(value);
            return fieldValueNum < valueNum;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean evaluateCustomCondition(File file, Condition condition) {
        return false;
    }
    
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
    
    public EvaluationResult evaluateWithLogic(File file, Condition condition) {
        if (condition == null) {
            EvaluationResult result = new EvaluationResult(true);
            result.setReason("No condition specified");
            return result;
        }
        
        if (condition.getSubConditions().isEmpty()) {
            return evaluate(file, condition);
        }
        
        EvaluationResult result = new EvaluationResult(false);
        
        switch (condition.getOperator()) {
            case AND:
                result = evaluateAnd(file, condition);
                break;
            case OR:
                result = evaluateOr(file, condition);
                break;
            case NOT:
                result = evaluateNot(file, condition);
                break;
        }
        
        return result;
    }
    
    private EvaluationResult evaluateAnd(File file, Condition condition) {
        EvaluationResult result = new EvaluationResult(true);
        
        for (Condition subCondition : condition.getSubConditions()) {
            EvaluationResult subResult = evaluateWithLogic(file, subCondition);
            
            if (!subResult.isMatched()) {
                result.setMatched(false);
                result.setReason("AND condition failed: " + subResult.getReason());
                return result;
            }
        }
        
        result.setReason("All AND conditions matched");
        return result;
    }
    
    private EvaluationResult evaluateOr(File file, Condition condition) {
        EvaluationResult result = new EvaluationResult(false);
        
        for (Condition subCondition : condition.getSubConditions()) {
            EvaluationResult subResult = evaluateWithLogic(file, subCondition);
            
            if (subResult.isMatched()) {
                result.setMatched(true);
                result.setReason("OR condition matched: " + subResult.getReason());
                return result;
            }
        }
        
        result.setReason("No OR conditions matched");
        return result;
    }
    
    private EvaluationResult evaluateNot(File file, Condition condition) {
        if (condition.getSubConditions().isEmpty()) {
            return evaluate(file, condition);
        }
        
        EvaluationResult subResult = evaluateWithLogic(file, condition.getSubConditions().get(0));
        EvaluationResult result = new EvaluationResult(!subResult.isMatched());
        result.setReason("NOT condition: " + subResult.getReason());
        
        return result;
    }
}
