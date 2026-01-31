package com.filemanager.domain.dto;

import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;

/**
 * 变更记录查询响应DTO，封装分页查询结果
 */
public class ChangeRecordResponseDTO {
    private List<ChangeRecord> records;  // 变更记录列表
    private long total;                  // 总记录数
    private int page;                    // 当前页码
    private int size;                    // 每页大小
    private int pages;                   // 总页数

    public ChangeRecordResponseDTO() {
    }

    public ChangeRecordResponseDTO(List<ChangeRecord> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = (int) Math.ceil((double) total / size);
    }

    public List<ChangeRecord> getRecords() {
        return records;
    }

    public void setRecords(List<ChangeRecord> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
        this.pages = (int) Math.ceil((double) total / size);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
        this.pages = (int) Math.ceil((double) total / size);
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }
}
