package com.filemanager.backend.service.impl;

import com.filemanager.backend.config.ConfigManager;
import com.filemanager.backend.service.FileFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class FileFilterServiceImpl implements FileFilterService {
    
    private final List<String> scanFilterList = new CopyOnWriteArrayList<>();
    private final ConfigManager configManager;
    
    @Autowired
    public FileFilterServiceImpl(ConfigManager configManager) {
        this.configManager = configManager;
        initDefaultFilters();
        loadFiltersFromConfig();
    }
    
    private void initDefaultFilters() {
        List<String> defaultFilters = Arrays.asList(
            "*Convert*",
            "*Split*",
            "*System*",
            "*trash*",
            "*Temp*",
            "*Cache*",
            "*Log*",
            "*\\Windows\\*",
            "*\\Program Files\\*",
            "*\\Program Files (x86)\\*",
            "*\\ProgramData\\*",
            "*\\AppData\\*",
            "*\\Local Settings\\*",
            "*\\Application Data\\*",
            "*\\Recycle Bin\\*",
            "*\\System Volume Information\\*",
            "*\\.*",
            "*\\~*",
            "*\\Thumbs.db",
            "*\\Temp\\*",
            "*\\TMP\\*"
        );
        
        // 只有当配置中没有过滤规则时才使用默认规则
        Object filtersFromConfig = configManager.getConfig(ConfigManager.KEY_SCAN_FILTER_LIST, Object.class);
        if (filtersFromConfig == null) {
            scanFilterList.addAll(defaultFilters);
            saveFiltersToConfig();
        }
    }

    private void loadFiltersFromConfig() {
        Object filtersObj = configManager.getConfig(ConfigManager.KEY_SCAN_FILTER_LIST, Object.class);
        if (filtersObj instanceof List) {
            List<?> filtersList = (List<?>) filtersObj;
            scanFilterList.clear();
            for (Object filter : filtersList) {
                if (filter instanceof String) {
                    scanFilterList.add((String) filter);
                }
            }
        }
    }

    private void saveFiltersToConfig() {
        configManager.setConfig(ConfigManager.KEY_SCAN_FILTER_LIST, new ArrayList<>(scanFilterList));
    }

    @Override
    public boolean isFileIncluded(File file) {
        return file != null && file.exists() && !isFileFiltered(file);
    }

    @Override
    public boolean isFileFiltered(File file) {
        if (file == null) {
            return false;
        }
        String fullPath = file.getAbsolutePath();
        for (String filter : scanFilterList) {
            if (matchesFilter(fullPath, filter)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesFilter(String fullPath, String filter) {
        String regex = filter
                .replaceAll("([\\\\/:\\[\\]{}()+.^$|])", "\\\\$1")
                .replace("*", ".*")
                .replace("?", ".");
        return fullPath.matches(regex);
    }

    @Override
    public List<String> getScanFilterList() {
        return new ArrayList<>(scanFilterList);
    }

    @Override
    public void addScanFilter(String filter) {
        if (filter != null && !filter.trim().isEmpty() && !scanFilterList.contains(filter)) {
            scanFilterList.add(filter);
            saveFiltersToConfig();
        }
    }

    @Override
    public void removeScanFilter(String filter) {
        scanFilterList.remove(filter);
        saveFiltersToConfig();
    }

    @Override
    public void clearScanFilters() {
        scanFilterList.clear();
        saveFiltersToConfig();
    }
}
