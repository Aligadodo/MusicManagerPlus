package com.filemanager.backend.dto;

/**
 * 分页查询参数
 * 用于任务数据列表的分页查询
 */
public class PaginationParams {
    
    private int page = 1;                    // 当前页码，默认1
    private int pageSize = 20;               // 每页数量，默认20
    private String search;                   // 搜索关键词
    private String sortField;                // 排序字段
    private String sortOrder = "asc";        // 排序方向：asc/desc
    
    // 扫描阶段筛选参数
    private String fileType;                 // 文件类型筛选
    private Long minSize;                    // 最小文件大小（字节）
    private Long maxSize;                    // 最大文件大小（字节）
    private Long startTime;                  // 开始时间戳
    private Long endTime;                    // 结束时间戳
    
    // 预览/执行阶段筛选参数
    private String operationType;            // 操作类型筛选
    private String status;                   // 状态筛选
    private Boolean changed;                 // 是否变更筛选
    
    public PaginationParams() {
    }
    
    // Getters and Setters
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = Math.max(1, page);
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        // 限制每页数量范围
        this.pageSize = Math.max(1, Math.min(1000, pageSize));
    }
    
    public String getSearch() {
        return search;
    }
    
    public void setSearch(String search) {
        this.search = search;
    }
    
    public String getSortField() {
        return sortField;
    }
    
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }
    
    public String getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(String sortOrder) {
        this.sortOrder = "desc".equalsIgnoreCase(sortOrder) ? "desc" : "asc";
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public Long getMinSize() {
        return minSize;
    }
    
    public void setMinSize(Long minSize) {
        this.minSize = minSize;
    }
    
    public Long getMaxSize() {
        return maxSize;
    }
    
    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }
    
    public Long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }
    
    public Long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Boolean getChanged() {
        return changed;
    }
    
    public void setChanged(Boolean changed) {
        this.changed = changed;
    }
    
    /**
     * 获取计算后的偏移量
     */
    public int getOffset() {
        return (page - 1) * pageSize;
    }
    
    @Override
    public String toString() {
        return "PaginationParams{" +
                "page=" + page +
                ", pageSize=" + pageSize +
                ", search='" + search + '\'' +
                ", sortField='" + sortField + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                ", fileType='" + fileType + '\'' +
                ", minSize=" + minSize +
                ", maxSize=" + maxSize +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", operationType='" + operationType + '\'' +
                ", status='" + status + '\'' +
                ", changed=" + changed +
                '}';
    }
}
