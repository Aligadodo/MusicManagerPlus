/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.rule.rename;

import com.filemanager.app.tools.display.NodeUtils;
import com.filemanager.app.tools.display.StyleFactory;
import com.filemanager.model.RuleCondition;
import com.filemanager.strategy.AdvancedRenameStrategy;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

import java.util.stream.Collectors;

public class RenameRuleListCell extends ListCell<RenameRule> {
    private final AdvancedRenameStrategy strategy;
    
    public RenameRuleListCell(AdvancedRenameStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    protected void updateItem(RenameRule item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            String cond = item.conditions.isEmpty() ? "无条件"
                    : item.conditions.stream().map(RuleCondition::toString).collect(Collectors.joining(" & "));
            Label l1 = StyleFactory.createDescLabel("若: " + cond);
            l1.setStyle("-fx-font-size: 11px;");
            Label l2 = StyleFactory.createDescLabel(item.getActionDesc());
            l2.setStyle("-fx-font-weight: bold;");
            HBox actions = StyleFactory.createTreeItemMenu(
                    e -> strategy.showRuleEditDialog(item),
                    e -> NodeUtils.moveListItem(getListView().getItems(), getIndex(), -1),
                    e -> NodeUtils.moveListItem(getListView().getItems(), getIndex(), 1),
                    e -> getListView().getItems().remove(item));
            setGraphic(StyleFactory.createHBox(StyleFactory.createVBox(l1, l2), StyleFactory.createSpacer(),
                    actions));
        }
    }

    @Override
    public void updateSelected(boolean selected) {
        super.updateSelected(selected);
        if (!isEmpty() && getItem() != null) {
            StyleFactory.updateTreeItemStyle(this, selected);
        }
    }
}