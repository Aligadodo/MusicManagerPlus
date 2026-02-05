package com.filemanager.plugin.util;

import com.filemanager.domain.dto.PreconditionDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;

import java.io.File;
import java.util.List;

public class PreconditionEvaluator {

    public static boolean evaluate(File file, List<PreconditionGroupDTO> preconditionGroups) {
        if (preconditionGroups == null || preconditionGroups.isEmpty()) {
            return true;
        }

        for (PreconditionGroupDTO group : preconditionGroups) {
            if (evaluateGroup(file, group)) {
                return true;
            }
        }

        return false;
    }

    private static boolean evaluateGroup(File file, PreconditionGroupDTO group) {
        if (group == null || group.getPreconditions() == null || group.getPreconditions().isEmpty()) {
            return true;
        }

        String logicType = group.getLogicType();
        if ("AND".equalsIgnoreCase(logicType)) {
            return evaluateAll(file, group.getPreconditions());
        } else if ("OR".equalsIgnoreCase(logicType)) {
            return evaluateAny(file, group.getPreconditions());
        }

        return evaluateAll(file, group.getPreconditions());
    }

    private static boolean evaluateAll(File file, List<PreconditionDTO> preconditions) {
        for (PreconditionDTO precondition : preconditions) {
            if (!evaluateCondition(file, precondition)) {
                return false;
            }
        }
        return true;
    }

    private static boolean evaluateAny(File file, List<PreconditionDTO> preconditions) {
        for (PreconditionDTO precondition : preconditions) {
            if (evaluateCondition(file, precondition)) {
                return true;
            }
        }
        return false;
    }

    private static boolean evaluateCondition(File file, PreconditionDTO precondition) {
        if (precondition == null || precondition.getField() == null) {
            return false;
        }

        String field = precondition.getField();
        PreconditionDTO.OperatorType operator = precondition.getOperator();
        Object value = precondition.getValue();

        if (operator == null) {
            return false;
        }

        Object fieldValue = getFieldValue(file, field);
        if (fieldValue == null) {
            return false;
        }

        return compareValues(fieldValue, operator, value);
    }

    private static Object getFieldValue(File file, String field) {
        switch (field.toLowerCase()) {
            case "file":
                return file.isFile();
            case "directory":
                return file.isDirectory();
            case "extension":
                String name = file.getName();
                int dotIndex = name.lastIndexOf('.');
                return dotIndex > 0 ? name.substring(dotIndex) : "";
            case "size":
                return file.length();
            case "modified":
                return file.lastModified();
            case "name":
                return file.getName();
            case "path":
                return file.getAbsolutePath();
            default:
                return null;
        }
    }

    private static boolean compareValues(Object fieldValue, PreconditionDTO.OperatorType operator, Object conditionValue) {
        try {
            switch (operator) {
                case EQUALS:
                    return fieldValue.equals(conditionValue);
                case NOT_EQUALS:
                    return !fieldValue.equals(conditionValue);
                case GREATER_THAN:
                    return compareNumbers(fieldValue, conditionValue) > 0;
                case LESS_THAN:
                    return compareNumbers(fieldValue, conditionValue) < 0;
                case GREATER_THAN_EQUALS:
                    return compareNumbers(fieldValue, conditionValue) >= 0;
                case LESS_THAN_EQUALS:
                    return compareNumbers(fieldValue, conditionValue) <= 0;
                case CONTAINS:
                    return fieldValue.toString().contains(conditionValue.toString());
                case NOT_CONTAINS:
                    return !fieldValue.toString().contains(conditionValue.toString());
                case STARTS_WITH:
                    return fieldValue.toString().startsWith(conditionValue.toString());
                case ENDS_WITH:
                    return fieldValue.toString().endsWith(conditionValue.toString());
                case REGEX_MATCH:
                    return fieldValue.toString().matches(conditionValue.toString());
                case IN:
                    return conditionValue.toString().contains(fieldValue.toString());
                case IS:
                    if (fieldValue instanceof Boolean) {
                        return fieldValue.equals(Boolean.parseBoolean(conditionValue.toString()));
                    }
                    return fieldValue.equals(conditionValue);
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static int compareNumbers(Object value1, Object value2) {
        double num1 = Double.parseDouble(value1.toString());
        double num2 = Double.parseDouble(value2.toString());
        return Double.compare(num1, num2);
    }
}
