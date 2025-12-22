package com.filemanager.tool.display;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Pane;
import lombok.var;

import java.util.Collections;
import java.util.List;

public class NodeUtils {

    /**
     * 交换 Pane 中两个指定组件的位置
     */
    public static void swap(Pane p, Node node1, Node node2) {
        var children = p.getChildren();
        if (children.contains(node1) && children.contains(node2)) {
            int index1 = children.indexOf(node1);
            int index2 = children.indexOf(node2);
            Collections.swap(children, index1, index2);
        }
    }

    /**
     * 将指定组件向左移动一位
     */
    public static void moveLeft(Pane p, Node node) {
        var children = p.getChildren();
        int currentIndex = children.indexOf(node);
        
        if (currentIndex > 0) {
            // 与前一个元素交换位置
            Collections.swap(children, currentIndex, currentIndex - 1);
        }
    }

    // [新增] 通用：列表项移动辅助方法
    public static <T> void moveListItem(List<T> list, int index, int direction) {
        int newIndex = index + direction;
        if (newIndex >= 0 && newIndex < list.size()) {
            Collections.swap(list, index, newIndex);

        }
    }

    /**
     * 将指定组件向右移动一位
     */
    public static void moveRight(Pane p, Node node) {
        var children = p.getChildren();
        int currentIndex = children.indexOf(node);
        
        if (currentIndex >= 0 && currentIndex < children.size() - 1) {
            // 与后一个元素交换位置
            Collections.swap(children, currentIndex, currentIndex + 1);
        }
    }

    /**
     * 移至最左侧或最右侧
     */
    public static void moveToEdge(Pane p, Node node, boolean toRight) {
        var children = p.getChildren();
        if (children.remove(node)) {
            if (toRight) {
                children.add(node); // 添加到末尾
            } else {
                children.add(0, node); // 插入到开头
            }
        }
    }
}