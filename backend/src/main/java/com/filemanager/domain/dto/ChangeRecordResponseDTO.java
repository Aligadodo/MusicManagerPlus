package com.filemanager.domain.dto;

import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;

/**
 * 变更记录查询响应DTO，封装分页查询结果
 */
public class ChangeRecordResponseDTO {
    private List<ChangeRecord> records;  // 变更记录列表
    private List<ChangeRecord> changes;  // 兼容前端，与records相同
    private long total;                  // 总记录数
    private int page;                    // 当前页码
    private int size;                    // 每页大小
    private int pages;                   // 总页数
    private boolean success;             // 是否成功
    private String message;              // 消息

    public ChangeRecordResponseDTO() {
    }

    public ChangeRecordResponseDTO(List<ChangeRecord> records, long total, int page, int size) {
        this.records = records;
        this.changes = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = (int) Math.ceil((double) total / size);
        this.success = true;
        this.message = "查询成功";
    }

    public List<ChangeRecord> getRecords() {
        return records;
    }

    public void setRecords(List<ChangeRecord> records) {
        this.records = records;
        this.changes = records;
    }

    public List<ChangeRecord> getChanges() {
        return changes;
    }

    public void setChanges(List<ChangeRecord> changes) {
        this.changes = changes;
        this.records = changes;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
