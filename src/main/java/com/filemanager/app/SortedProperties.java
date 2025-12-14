package com.filemanager.app;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * SortedProperties 类继承自 Properties，
 * 旨在覆盖其迭代行为，确保在调用 store() 方法时，
 * 属性键按字母顺序（自然顺序）进行排序输出。
 */
public class SortedProperties extends Properties {

    private static final long serialVersionUID = 1L;

    /**
     * 覆盖 keySet() 方法。
     * Properties 的 keySet() 方法通常返回一个无序的 Set。
     * 这里我们返回一个由 TreeSet 包装的不可修改 Set，
     * 从而保证键是按字母顺序排列的。
     *
     * @return 按字母顺序排序的键集合。
     */
    @Override
    public Set<Object> keySet() {
        // 使用 TreeSet 保证键的自然排序（字母顺序）
        return Collections.unmodifiableSet(new TreeSet<>(super.keySet()));
    }

    /**
     * 覆盖 keys() 方法。
     * keys() 方法在老版本的 JDK 中是 store() 依赖的关键方法。
     * 它返回一个按字母顺序排序的键的 Enumeration。
     *
     * @return 按字母顺序排序的键枚举。
     */
    @Override
    public Enumeration<Object> keys() {
        // 将排序后的 keySet 转换为一个 Enumeration
        Set<Object> sortedKeys = new TreeSet<>(super.keySet());
        return new Vector<>(sortedKeys).elements();
    }

    /**
     * 重写 store(Writer writer, String comments) 方法，以保持行为一致性
     * （实际上它依赖于重写的 keySet 或 keys）。
     */
    @Override
    public void store(Writer writer, String comments) throws IOException {
        super.store(writer, comments);
    }

    /**
     * 重写 store(OutputStream out, String comments) 方法，以保持行为一致性
     * （实际上它依赖于重写的 keySet 或 keys）。
     */
    @Override
    public void store(OutputStream out, String comments) throws IOException {
        super.store(out, comments);
    }
}