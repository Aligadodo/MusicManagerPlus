package com.filemanager.model;

import com.jfoenix.controls.JFXComboBox;
import com.filemanager.strategy.FileCleanupStrategy;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import java.util.Properties;

public class CleanupParams {
    // --- Runtime Params ---
    private FileCleanupStrategy.CleanupMode pMode;
    private FileCleanupStrategy.DeleteMethod pMethod;
    private String pTrashPath;
    private boolean pKeepLargest;
    private boolean pKeepEarliest;
    private String pKeepExt;
    // 预处理参数
    private boolean pPreprocessLower;
    private boolean pPreprocessUpper;
    private boolean pPreprocessSimplified;
    // 文件大小范围参数
    private FileCleanupStrategy.FileSizeRange pSizeRange;
    // 音频特殊处理参数
    private boolean pAudioSpecial;

    public CleanupParams() {
        // 默认参数
        this.pMode = FileCleanupStrategy.CleanupMode.DEDUP_FILES;
        this.pMethod = FileCleanupStrategy.DeleteMethod.PSEUDO_DELETE;
        this.pTrashPath = ".EchoTrash";
        this.pKeepLargest = true;
        this.pKeepEarliest = true;
        this.pKeepExt = "wav";
        this.pPreprocessLower = true;
        this.pPreprocessUpper = false;
        this.pPreprocessSimplified = false;
        this.pSizeRange = FileCleanupStrategy.FileSizeRange.ALL;
        this.pAudioSpecial = true;
    }

    public void captureParams(com.filemanager.app.components.CleanupUIConfig uiConfig) {
        JFXComboBox<FileCleanupStrategy.CleanupMode> cbMode = uiConfig.getCbMode();
        JFXComboBox<FileCleanupStrategy.DeleteMethod> cbMethod = uiConfig.getCbMethod();
        TextField txtTrashPath = uiConfig.getTxtTrashPath();
        CheckBox chkKeepLargest = uiConfig.getChkKeepLargest();
        CheckBox chkKeepEarliest = uiConfig.getChkKeepEarliest();
        TextField txtKeepExt = uiConfig.getTxtKeepExt();
        CheckBox chkPreprocessLower = uiConfig.getChkPreprocessLower();
        CheckBox chkPreprocessUpper = uiConfig.getChkPreprocessUpper();
        CheckBox chkPreprocessSimplified = uiConfig.getChkPreprocessSimplified();
        JFXComboBox<FileCleanupStrategy.FileSizeRange> cbSizeRange = uiConfig.getCbSizeRange();
        CheckBox chkAudioSpecial = uiConfig.getChkAudioSpecial();

        pMode = cbMode.getValue();
        pMethod = cbMethod.getValue();
        pTrashPath = txtTrashPath.getText();
        if (pTrashPath == null || pTrashPath.trim().isEmpty()) pTrashPath = ".EchoTrash";
        pKeepLargest = chkKeepLargest.isSelected();
        pKeepEarliest = chkKeepEarliest.isSelected();
        pKeepExt = txtKeepExt.getText();
        
        // 捕获预处理参数
        pPreprocessLower = chkPreprocessLower.isSelected();
        pPreprocessUpper = chkPreprocessUpper.isSelected();
        pPreprocessSimplified = chkPreprocessSimplified.isSelected();
        
        // 捕获文件大小范围参数
        pSizeRange = cbSizeRange.getValue();
        
        // 捕获音频特殊处理参数
        pAudioSpecial = chkAudioSpecial.isSelected();
    }

    public void saveConfig(Properties props) {
        props.setProperty("clean_mode", pMode.name());
        props.setProperty("clean_method", pMethod.name());
        props.setProperty("clean_trash", pTrashPath);
        props.setProperty("clean_keepLarge", String.valueOf(pKeepLargest));
        props.setProperty("clean_keepEarly", String.valueOf(pKeepEarliest));
        props.setProperty("clean_keepExt", pKeepExt);
        
        // 保存预处理参数
        props.setProperty("clean_preprocessLower", String.valueOf(pPreprocessLower));
        props.setProperty("clean_preprocessUpper", String.valueOf(pPreprocessUpper));
        props.setProperty("clean_preprocessSimplified", String.valueOf(pPreprocessSimplified));
        
        // 保存文件大小范围参数
        props.setProperty("clean_sizeRange", pSizeRange.name());
        
        // 保存音频特殊处理参数
        props.setProperty("clean_audioSpecial", String.valueOf(pAudioSpecial));
    }

    public void loadConfig(Properties props, com.filemanager.app.components.CleanupUIConfig uiConfig) {
        if (props.containsKey("clean_mode"))
            uiConfig.getCbMode().getSelectionModel().select(FileCleanupStrategy.CleanupMode.valueOf(props.getProperty("clean_mode")));
        if (props.containsKey("clean_method"))
            uiConfig.getCbMethod().getSelectionModel().select(FileCleanupStrategy.DeleteMethod.valueOf(props.getProperty("clean_method")));
        if (props.containsKey("clean_trash")) uiConfig.getTxtTrashPath().setText(props.getProperty("clean_trash"));
        if (props.containsKey("clean_keepLarge"))
            uiConfig.getChkKeepLargest().setSelected(Boolean.parseBoolean(props.getProperty("clean_keepLarge")));
        if (props.containsKey("clean_keepEarly"))
            uiConfig.getChkKeepEarliest().setSelected(Boolean.parseBoolean(props.getProperty("clean_keepEarly")));
        if (props.containsKey("clean_keepExt")) uiConfig.getTxtKeepExt().setText(props.getProperty("clean_keepExt"));
        
        // 加载预处理参数
        if (props.containsKey("clean_preprocessLower"))
            uiConfig.getChkPreprocessLower().setSelected(Boolean.parseBoolean(props.getProperty("clean_preprocessLower")));
        if (props.containsKey("clean_preprocessUpper"))
            uiConfig.getChkPreprocessUpper().setSelected(Boolean.parseBoolean(props.getProperty("clean_preprocessUpper")));
        if (props.containsKey("clean_preprocessSimplified"))
            uiConfig.getChkPreprocessSimplified().setSelected(Boolean.parseBoolean(props.getProperty("clean_preprocessSimplified")));
        
        // 加载文件大小范围参数
        if (props.containsKey("clean_sizeRange"))
            uiConfig.getCbSizeRange().getSelectionModel().select(FileCleanupStrategy.FileSizeRange.valueOf(props.getProperty("clean_sizeRange")));
        
        // 加载音频特殊处理参数
        if (props.containsKey("clean_audioSpecial"))
            uiConfig.getChkAudioSpecial().setSelected(Boolean.parseBoolean(props.getProperty("clean_audioSpecial")));

        // 重新捕获参数以确保内部状态同步
        captureParams(uiConfig);
    }

    // Getters for all parameters
    public FileCleanupStrategy.CleanupMode getMode() {
        return pMode;
    }

    public FileCleanupStrategy.DeleteMethod getMethod() {
        return pMethod;
    }

    public String getTrashPath() {
        return pTrashPath;
    }

    public boolean isKeepLargest() {
        return pKeepLargest;
    }

    public boolean isKeepEarliest() {
        return pKeepEarliest;
    }

    public String getKeepExt() {
        return pKeepExt;
    }

    public boolean isPreprocessLower() {
        return pPreprocessLower;
    }

    public boolean isPreprocessUpper() {
        return pPreprocessUpper;
    }

    public boolean isPreprocessSimplified() {
        return pPreprocessSimplified;
    }

    public FileCleanupStrategy.FileSizeRange getSizeRange() {
        return pSizeRange;
    }

    public boolean isAudioSpecial() {
        return pAudioSpecial;
    }

    // Setters for all parameters (if needed)
    public void setMode(FileCleanupStrategy.CleanupMode pMode) {
        this.pMode = pMode;
    }

    public void setMethod(FileCleanupStrategy.DeleteMethod pMethod) {
        this.pMethod = pMethod;
    }

    public void setTrashPath(String pTrashPath) {
        this.pTrashPath = pTrashPath;
    }

    public void setKeepLargest(boolean pKeepLargest) {
        this.pKeepLargest = pKeepLargest;
    }

    public void setKeepEarliest(boolean pKeepEarliest) {
        this.pKeepEarliest = pKeepEarliest;
    }

    public void setKeepExt(String pKeepExt) {
        this.pKeepExt = pKeepExt;
    }

    public void setPreprocessLower(boolean pPreprocessLower) {
        this.pPreprocessLower = pPreprocessLower;
    }

    public void setPreprocessUpper(boolean pPreprocessUpper) {
        this.pPreprocessUpper = pPreprocessUpper;
    }

    public void setPreprocessSimplified(boolean pPreprocessSimplified) {
        this.pPreprocessSimplified = pPreprocessSimplified;
    }

    public void setSizeRange(FileCleanupStrategy.FileSizeRange pSizeRange) {
        this.pSizeRange = pSizeRange;
    }

    public void setAudioSpecial(boolean pAudioSpecial) {
        this.pAudioSpecial = pAudioSpecial;
    }
}