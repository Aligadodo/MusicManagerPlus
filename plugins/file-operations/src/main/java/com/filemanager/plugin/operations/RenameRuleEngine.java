package com.filemanager.plugin.operations;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameRuleEngine {
    
    public enum RuleType {
        REPLACE,
        PREFIX,
        SUFFIX,
        REMOVE,
        CASE_CHANGE,
        NUMBERING,
        METADATA,
        REGEX_REPLACE
    }
    
    public enum CaseType {
        LOWER,
        UPPER,
        TITLE,
        SENTENCE
    }
    
    public static class RenameRule {
        private RuleType type;
        private String pattern;
        private String replacement;
        private CaseType caseType;
        private boolean enabled;
        private int startNumber;
        private int numberStep;
        private int numberDigits;
        private String metadataField;
        private String metadataFormat;
        private boolean caseSensitive;
        
        public RenameRule() {
            this.enabled = true;
            this.startNumber = 1;
            this.numberStep = 1;
            this.numberDigits = 2;
            this.caseSensitive = false;
        }
        
        public RenameRule(RuleType type, String pattern, String replacement) {
            this();
            this.type = type;
            this.pattern = pattern;
            this.replacement = replacement;
        }
        
        public RuleType getType() {
            return type;
        }
        
        public void setType(RuleType type) {
            this.type = type;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        
        public String getReplacement() {
            return replacement;
        }
        
        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }
        
        public CaseType getCaseType() {
            return caseType;
        }
        
        public void setCaseType(CaseType caseType) {
            this.caseType = caseType;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getStartNumber() {
            return startNumber;
        }
        
        public void setStartNumber(int startNumber) {
            this.startNumber = startNumber;
        }
        
        public int getNumberStep() {
            return numberStep;
        }
        
        public void setNumberStep(int numberStep) {
            this.numberStep = numberStep;
        }
        
        public int getNumberDigits() {
            return numberDigits;
        }
        
        public void setNumberDigits(int numberDigits) {
            this.numberDigits = numberDigits;
        }
        
        public String getMetadataField() {
            return metadataField;
        }
        
        public void setMetadataField(String metadataField) {
            this.metadataField = metadataField;
        }
        
        public String getMetadataFormat() {
            return metadataFormat;
        }
        
        public void setMetadataFormat(String metadataFormat) {
            this.metadataFormat = metadataFormat;
        }
        
        public boolean isCaseSensitive() {
            return caseSensitive;
        }
        
        public void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }
    }
    
    public static class RuleResult {
        private String originalName;
        private String newName;
        private boolean changed;
        private List<String> appliedRules;
        
        public RuleResult(String originalName, String newName) {
            this.originalName = originalName;
            this.newName = newName;
            this.changed = !originalName.equals(newName);
            this.appliedRules = new ArrayList<>();
        }
        
        public String getOriginalName() {
            return originalName;
        }
        
        public String getNewName() {
            return newName;
        }
        
        public void setNewName(String newName) {
            this.newName = newName;
            this.changed = !originalName.equals(newName);
        }
        
        public boolean isChanged() {
            return changed;
        }
        
        public List<String> getAppliedRules() {
            return appliedRules;
        }
        
        public void addAppliedRule(String rule) {
            this.appliedRules.add(rule);
        }
    }
    
    private List<RenameRule> rules;
    private Map<String, Object> metadata;
    private boolean stopOnFirstMatch;
    private boolean preserveExtension;
    
    public RenameRuleEngine() {
        this.rules = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.stopOnFirstMatch = false;
        this.preserveExtension = true;
    }
    
    public RenameRuleEngine(List<RenameRule> rules) {
        this();
        if (rules != null) {
            this.rules.addAll(rules);
        }
    }
    
    public void addRule(RenameRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }
    
    public void addRules(List<RenameRule> rules) {
        if (rules != null) {
            this.rules.addAll(rules);
        }
    }
    
    public void clearRules() {
        rules.clear();
    }
    
    public List<RenameRule> getRules() {
        return new ArrayList<>(rules);
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public boolean isStopOnFirstMatch() {
        return stopOnFirstMatch;
    }
    
    public void setStopOnFirstMatch(boolean stopOnFirstMatch) {
        this.stopOnFirstMatch = stopOnFirstMatch;
    }
    
    public boolean isPreserveExtension() {
        return preserveExtension;
    }
    
    public void setPreserveExtension(boolean preserveExtension) {
        this.preserveExtension = preserveExtension;
    }
    
    public RuleResult apply(String fileName) {
        String originalName = fileName;
        String currentName = fileName;
        
        String extension = "";
        if (preserveExtension && fileName.contains(".")) {
            int dotIndex = fileName.lastIndexOf('.');
            extension = fileName.substring(dotIndex);
            currentName = fileName.substring(0, dotIndex);
        }
        
        RuleResult result = new RuleResult(originalName, currentName + extension);
        
        for (RenameRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            
            String processedName = applyRule(currentName, rule);
            
            if (!processedName.equals(currentName)) {
                result.setNewName(processedName + extension);
                result.addAppliedRule(rule.getType().toString());
                currentName = processedName;
                
                if (stopOnFirstMatch) {
                    break;
                }
            }
        }
        
        return result;
    }
    
    public RuleResult apply(File file) {
        return apply(file.getName());
    }
    
    public List<RuleResult> applyBatch(List<String> fileNames) {
        List<RuleResult> results = new ArrayList<>();
        
        for (String fileName : fileNames) {
            results.add(apply(fileName));
        }
        
        return results;
    }
    
    private String applyRule(String name, RenameRule rule) {
        switch (rule.getType()) {
            case REPLACE:
                return applyReplace(name, rule);
            case PREFIX:
                return applyPrefix(name, rule);
            case SUFFIX:
                return applySuffix(name, rule);
            case REMOVE:
                return applyRemove(name, rule);
            case CASE_CHANGE:
                return applyCaseChange(name, rule);
            case NUMBERING:
                return applyNumbering(name, rule);
            case METADATA:
                return applyMetadata(name, rule);
            case REGEX_REPLACE:
                return applyRegexReplace(name, rule);
            default:
                return name;
        }
    }
    
    private String applyReplace(String name, RenameRule rule) {
        String pattern = rule.getPattern();
        String replacement = rule.getReplacement();
        
        if (pattern == null || pattern.isEmpty()) {
            return name;
        }
        
        if (rule.isCaseSensitive()) {
            return name.replace(pattern, replacement != null ? replacement : "");
        } else {
            return name.replaceAll("(?i)" + Pattern.quote(pattern), replacement != null ? replacement : "");
        }
    }
    
    private String applyPrefix(String name, RenameRule rule) {
        String prefix = rule.getReplacement();
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + name;
        }
        return name;
    }
    
    private String applySuffix(String name, RenameRule rule) {
        String suffix = rule.getReplacement();
        if (suffix != null && !suffix.isEmpty()) {
            return name + suffix;
        }
        return name;
    }
    
    private String applyRemove(String name, RenameRule rule) {
        String pattern = rule.getPattern();
        if (pattern != null && !pattern.isEmpty()) {
            if (rule.isCaseSensitive()) {
                return name.replace(pattern, "");
            } else {
                return name.replaceAll("(?i)" + Pattern.quote(pattern), "");
            }
        }
        return name;
    }
    
    private String applyCaseChange(String name, RenameRule rule) {
        CaseType caseType = rule.getCaseType();
        
        switch (caseType) {
            case LOWER:
                return name.toLowerCase();
            case UPPER:
                return name.toUpperCase();
            case TITLE:
                return toTitleCase(name);
            case SENTENCE:
                return toSentenceCase(name);
            default:
                return name;
        }
    }
    
    private String applyNumbering(String name, RenameRule rule) {
        int number = rule.getStartNumber();
        int digits = rule.getNumberDigits();
        String format = "%0" + digits + "d";
        String numbered = String.format(format, number);
        
        rule.setStartNumber(number + rule.getNumberStep());
        
        return name + numbered;
    }
    
    private String applyMetadata(String name, RenameRule rule) {
        String field = rule.getMetadataField();
        if (field == null || field.isEmpty()) {
            return name;
        }
        
        Object value = metadata.get(field);
        if (value == null) {
            return name;
        }
        
        String format = rule.getMetadataFormat();
        if (format != null && !format.isEmpty()) {
            return format.replace("{" + field + "}", value.toString());
        }
        
        return name + value.toString();
    }
    
    private String applyRegexReplace(String name, RenameRule rule) {
        String pattern = rule.getPattern();
        String replacement = rule.getReplacement();
        
        if (pattern == null || pattern.isEmpty()) {
            return name;
        }
        
        try {
            int flags = rule.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
            Pattern regex = Pattern.compile(pattern, flags);
            Matcher matcher = regex.matcher(name);
            
            return matcher.replaceAll(replacement != null ? replacement : "");
        } catch (Exception e) {
            return name;
        }
    }
    
    private String toTitleCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        
        return result.toString();
    }
    
    private String toSentenceCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        StringBuilder result = new StringBuilder(str.toLowerCase());
        
        if (result.length() > 0) {
            result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
        }
        
        return result.toString();
    }
    
    public static RenameRule createReplaceRule(String pattern, String replacement) {
        return new RenameRule(RuleType.REPLACE, pattern, replacement);
    }
    
    public static RenameRule createPrefixRule(String prefix) {
        RenameRule rule = new RenameRule();
        rule.setType(RuleType.PREFIX);
        rule.setReplacement(prefix);
        return rule;
    }
    
    public static RenameRule createSuffixRule(String suffix) {
        RenameRule rule = new RenameRule();
        rule.setType(RuleType.SUFFIX);
        rule.setReplacement(suffix);
        return rule;
    }
    
    public static RenameRule createRemoveRule(String pattern) {
        RenameRule rule = new RenameRule();
        rule.setType(RuleType.REMOVE);
        rule.setPattern(pattern);
        return rule;
    }
    
    public static RenameRule createCaseChangeRule(CaseType caseType) {
        RenameRule rule = new RenameRule();
        rule.setType(RuleType.CASE_CHANGE);
        rule.setCaseType(caseType);
        return rule;
    }
    
    public static RenameRule createNumberingRule(int startNumber, int step, int digits) {
        RenameRule rule = new RenameRule();
        rule.setType(RuleType.NUMBERING);
        rule.setStartNumber(startNumber);
        rule.setNumberStep(step);
        rule.setNumberDigits(digits);
        return rule;
    }
    
    public static RenameRule createRegexReplaceRule(String pattern, String replacement) {
        RenameRule rule = new RenameRule();
        rule.setType(RuleType.REGEX_REPLACE);
        rule.setPattern(pattern);
        rule.setReplacement(replacement);
        return rule;
    }
}
