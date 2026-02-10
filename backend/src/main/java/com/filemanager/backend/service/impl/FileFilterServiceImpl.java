package com.filemanager.backend.service.impl;

import com.filemanager.backend.service.FileFilterService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class FileFilterServiceImpl implements FileFilterService {
    
    private final List<String> scanFilterList = new CopyOnWriteArrayList<>();
    
    public FileFilterServiceImpl() {
        initDefaultFilters();
    }
    
    private void initDefaultFilters() {
        scanFilterList.addAll(Arrays.asList(
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
        ));
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
        }
    }

    @Override
    public void removeScanFilter(String filter) {
        scanFilterList.remove(filter);
    }

    @Override
    public void clearScanFilters() {
        scanFilterList.clear();
    }
    
    public void saveConfig(Properties props) {
        if (props == null) return;
        String filters = String.join("||", scanFilterList);
        props.setProperty("filter.scan.rules", filters);
    }
    
    public void loadConfig(Properties props) {
        if (props == null) return;
        String filters = props.getProperty("filter.scan.rules");
        if (filters != null && !filters.isEmpty()) {
            scanFilterList.clear();
            for (String filter : filters.split("\\|\\|")) {
                if (!filter.trim().isEmpty()) {
                    scanFilterList.add(filter);
                }
            }
        }
    }
}
