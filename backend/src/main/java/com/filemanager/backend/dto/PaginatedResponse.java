package com.filemanager.backend.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页响应数据
 * 用于任务数据列表的分页查询响应
 */
public class PaginatedResponse<T> {
    
    private List<T> list;        // 数据列表
    private long total;          // 总记录数
    private int page;            // 当前页码
    private int pageSize;        // 每页数量
    private int totalPages;      // 总页数
    private boolean hasNext;     // 是否有下一页
    private boolean hasPrevious; // 是否有上一页
    
    public PaginatedResponse() {
    }
    
    public PaginatedResponse(List<T> list, long total, int page, int pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
        this.hasNext = page < totalPages;
        this.hasPrevious = page > 1;
    }
    
    // Getters and Setters
    public List<T> getList() {
        return list;
    }
    
    public void setList(List<T> list) {
        this.list = list;
    }
    
    public long getTotal() {
        return total;
    }
    
    public void setTotal(long total) {
        this.total = total;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public boolean isHasNext() {
        return hasNext;
    }
    
    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
    
    public boolean isHasPrevious() {
        return hasPrevious;
    }
    
    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
    
    /**
     * 创建空响应
     */
    public static <T> PaginatedResponse<T> empty(int page, int pageSize) {
        return new PaginatedResponse<>(new ArrayList<>(), 0, page, pageSize);
    }
    
    /**
     * 创建成功响应
     */
    public static <T> PaginatedResponse<T> of(List<T> list, long total, int page, int pageSize) {
        return new PaginatedResponse<>(list, total, page, pageSize);
    }
    
    @Override
    public String toString() {
        return "PaginatedResponse{" +
                "listSize=" + (list != null ? list.size() : 0) +
                ", total=" + total +
                ", page=" + page +
                ", pageSize=" + pageSize +
                ", totalPages=" + totalPages +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                '}';
    }
}
