/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
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

    public List<RuleCondition> getConditions() {
        return conditions;
    }
    
    @Override
    public String toString() {
        if (conditions.isEmpty()) return "无限制 (总是通过)";
        return conditions.stream().map(RuleCondition::toString).collect(Collectors.joining(" 且 "));
    }
}