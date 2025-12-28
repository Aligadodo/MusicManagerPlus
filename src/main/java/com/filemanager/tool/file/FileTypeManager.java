package com.filemanager.tool.file;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.controlsfx.control.CheckComboBox;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 文件类型管理工具类
 * 用于管理文件分类、生成选择器UI以及执行文件过滤判断
 */
public class FileTypeManager {

    // 存储类别名称到匹配逻辑的映射
    private final Map<String, Predicate<File>> categoryRules = new LinkedHashMap<>();
    
    // 存储类别名称的顺序（用于UI显示）
    private final List<String> categoryOrder = new ArrayList<>();

    // 预定义的常量key，方便引用
    public static final String CAT_ALL_FILES = "全部文件";
    public static final String CAT_DIRECTORY = "文件夹";
    public static final String CAT_FILE_ONLY = "仅文件(非文件夹)";

    public FileTypeManager() {
        // 构造时可以初始化一些基础规则，或者留空由外部配置
    }

    /**
     * 注册一个基于后缀名的类别
     * @param categoryName 类别显示名称 (如 "音频文件")
     * @param extensions 该类别包含的后缀名 (不带点，如 "mp3", "wav")
     */
    public void registerExtensionCategory(String categoryName, String... extensions) {
        Set<String> extSet = new HashSet<>();
        for (String ext : extensions) {
            extSet.add(ext.toLowerCase());
        }

        // 定义匹配逻辑：是文件 且 后缀名匹配
        Predicate<File> rule = file -> {
            if (file.isDirectory()) return false;
            String name = file.getName().toLowerCase();
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                String ext = name.substring(lastDot + 1);
                return extSet.contains(ext);
            }
            return false;
        };

        addRule(categoryName, rule);
    }

    /**
     * 注册一个基于自定义逻辑的类别
     * @param categoryName 类别名称
     * @param rule 匹配逻辑
     */
    public void registerCustomCategory(String categoryName, Predicate<File> rule) {
        addRule(categoryName, rule);
    }

    /**
     * 注册一个“聚合”类别（包含其他已有的类别）
     * 比如：定义了 "MP3" 和 "WAV"，可以定义一个 "所有音频" 包含它们
     * @param categoryName 新的大类名称
     * @param targetCategories 要包含的已有类别名称
     */
    public void registerCompositeCategory(String categoryName, String... targetCategories) {
        Predicate<File> compositeRule = file -> {
            for (String targetCat : targetCategories) {
                Predicate<File> rule = categoryRules.get(targetCat);
                if (rule != null && rule.test(file)) {
                    return true;
                }
            }
            return false;
        };
        addRule(categoryName, compositeRule);
    }

    private void addRule(String name, Predicate<File> rule) {
        categoryRules.put(name, rule);
        if (!categoryOrder.contains(name)) {
            categoryOrder.add(name);
        }
    }

    /**
     * 创建配置好的 CheckComboBox
     */
    public CheckComboBox<String> createCheckComboBox() {
        ObservableList<String> items = FXCollections.observableArrayList(categoryOrder);
        CheckComboBox<String> ccb = new CheckComboBox<>(items);
        
        // 默认全选，或者根据需求修改
        ccb.getCheckModel().checkAll();
        return ccb;
    }

    /**
     * 核心判断方法：判断文件是否符合当前选中的类别
     * 逻辑：只要文件符合“被勾选的任意一个类别”的规则，即视为通过 (OR 逻辑)
     * 
     * @param file 待判断文件
     * @param selectedCategories 当前 CheckComboBox 选中的类别列表
     * @return 是否匹配
     */
    public boolean accept(File file, List<String> selectedCategories) {
        if (file == null || selectedCategories == null || selectedCategories.isEmpty()) {
            return false;
        }

        // 性能优化：如果选了“全部文件”，直接返回true (假设有这个类别且逻辑正确)
        if (selectedCategories.contains(CAT_ALL_FILES)) {
            return true;
        }

        for (String catName : selectedCategories) {
            Predicate<File> rule = categoryRules.get(catName);
            if (rule != null) {
                if (rule.test(file)) {
                    return true;
                }
            }
        }
        return false;
    }
}
