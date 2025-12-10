package com.filemanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleConditionGroup {
    private List<RuleCondition> conditions = new ArrayList<>();
    
    // 组内逻辑：所有条件都满足 (AND)
    public boolean test(File f) {
        if (conditions.isEmpty()) return true; // 空组默认通过
        for (RuleCondition c : conditions) {
            if (!c.test(f)) return false;
        }
        return true;
    }
    
    public void add(RuleCondition c) { conditions.add(c); }
    public void remove(RuleCondition c) { conditions.remove(c); }
    public void clear() { conditions.clear(); }
    
    @Override
    public String toString() {
        if (conditions.isEmpty()) return "无限制 (总是通过)";
        return conditions.stream().map(RuleCondition::toString).collect(Collectors.joining(" 且 "));
    }
}