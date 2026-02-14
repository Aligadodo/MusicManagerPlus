package com.filemanager.backend.service.impl;

import com.filemanager.backend.entity.ChangeRecordPO;
import com.filemanager.backend.mapper.ChangeRecordMapper;
import com.filemanager.backend.service.ChangeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChangeRecordServiceImpl implements ChangeRecordService {

    @Autowired
    private ChangeRecordMapper changeRecordMapper;

    @Override
    public ChangeRecordPO createRecord(ChangeRecordPO changeRecord) {
        changeRecordMapper.insert(changeRecord);
        return changeRecord;
    }

    @Override
    public ChangeRecordPO getRecordById(Long id) {
        return changeRecordMapper.selectById(id);
    }

    @Override
    public List<ChangeRecordPO> getRecordsByTaskId(String taskId) {
        return changeRecordMapper.selectByTaskId(taskId);
    }

    @Override
    public List<ChangeRecordPO> getRecordsByStatus(String status) {
        return changeRecordMapper.selectByPage(null, status, null, null, null, null, null, null, 0, 1000);
    }

    @Override
    public List<ChangeRecordPO> getRecordsByOperationType(String operationType) {
        return changeRecordMapper.selectByPage(null, null, operationType, null, null, null, null, null, 0, 1000);
    }

    @Override
    public List<ChangeRecordPO> searchRecords(String keyword, String searchFields, int page, int size) {
        int offset = (page - 1) * size;
        return changeRecordMapper.selectByPage(null, null, null, null, keyword, searchFields, null, null, offset, size);
    }

    @Override
    public List<ChangeRecordPO> getRecordsByPage(String taskId, String status, String operationType, Boolean changed, String keyword, String searchFields, String sortBy, String sortOrder, int page, int size) {
        int offset = (page - 1) * size;
        return changeRecordMapper.selectByPage(taskId, status, operationType, changed, keyword, searchFields, sortBy, sortOrder, offset, size);
    }

    @Override
    public ChangeRecordPO updateRecord(ChangeRecordPO changeRecord) {
        changeRecordMapper.update(changeRecord);
        return changeRecord;
    }

    @Override
    public boolean deleteRecord(Long id) {
        return changeRecordMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteRecordsByTaskId(String taskId) {
        return changeRecordMapper.deleteByTaskId(taskId) > 0;
    }

    @Override
    public long getTotalRecordCount() {
        return changeRecordMapper.countByPage(null, null, null, null, null, null);
    }

    @Override
    public long getRecordCountByTaskId(String taskId) {
        return changeRecordMapper.countByPage(taskId, null, null, null, null, null);
    }

    @Override
    public long getRecordCountByStatus(String status) {
        return changeRecordMapper.countByPage(null, status, null, null, null, null);
    }

    @Override
    public long countByPage(String taskId, String status, String operationType, Boolean changed, String keyword) {
        return changeRecordMapper.countByPage(taskId, status, operationType, changed, null, keyword);
    }
}
