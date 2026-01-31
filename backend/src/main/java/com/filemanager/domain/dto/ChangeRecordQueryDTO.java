package com.filemanager.domain.dto;

/**
 * 变更记录查询DTO，支持过滤条件和分页
 */
public class ChangeRecordQueryDTO {
    private String searchFilter;      // 搜索关键词
    private String statusFilter;       // 状态过滤
    private String operationTypeFilter; // 操作类型过滤
    private boolean hideUnchanged;     // 是否隐藏未变更的记录
    private int page;                  // 页码，从1开始
    private int size;                  // 每页大小
    private String sortBy;             // 排序字段
    private String sortDirection;      // 排序方向：ASC或DESC

    public ChangeRecordQueryDTO() {
        this.page = 1;
        this.size = 20;
        this.sortBy = "id";
        this.sortDirection = "ASC";
        this.hideUnchanged = true;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public String getOperationTypeFilter() {
        return operationTypeFilter;
    }

    public void setOperationTypeFilter(String operationTypeFilter) {
        this.operationTypeFilter = operationTypeFilter;
    }

    public boolean isHideUnchanged() {
        return hideUnchanged;
    }

    public void setHideUnchanged(boolean hideUnchanged) {
        this.hideUnchanged = hideUnchanged;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(1, page);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = Math.max(1, Math.min(100, size));
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
