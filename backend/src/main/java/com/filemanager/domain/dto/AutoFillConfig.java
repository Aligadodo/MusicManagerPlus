package com.filemanager.domain.dto;

/**
 * 自动填充配置
 * 用于定义参数的自动填充逻辑
 * 当依赖参数值变化时，自动填充当前参数的值
 */
public class AutoFillConfig {
    
    private String triggerParam; // 触发参数名，当该参数值变化时触发自动填充
    private String triggerValue; // 触发值，当触发参数等于该值时执行自动填充
    private String fillType; // 填充类型：auto_detect（自动检测）、fixed_value（固定值）、expression（表达式）
    private String fillValue; // 填充值，当fillType为fixed_value时使用
    private String detectPattern; // 检测模式，当fillType为auto_detect时使用
    
    public AutoFillConfig() {
    }
    
    public AutoFillConfig(String triggerParam, String triggerValue, String fillType) {
        this.triggerParam = triggerParam;
        this.triggerValue = triggerValue;
        this.fillType = fillType;
    }
    
    public String getTriggerParam() {
        return triggerParam;
    }
    
    public void setTriggerParam(String triggerParam) {
        this.triggerParam = triggerParam;
    }
    
    public String getTriggerValue() {
        return triggerValue;
    }
    
    public void setTriggerValue(String triggerValue) {
        this.triggerValue = triggerValue;
    }
    
    public String getFillType() {
        return fillType;
    }
    
    public void setFillType(String fillType) {
        this.fillType = fillType;
    }
    
    public String getFillValue() {
        return fillValue;
    }
    
    public void setFillValue(String fillValue) {
        this.fillValue = fillValue;
    }
    
    public String getDetectPattern() {
        return detectPattern;
    }
    
    public void setDetectPattern(String detectPattern) {
        this.detectPattern = detectPattern;
    }
}