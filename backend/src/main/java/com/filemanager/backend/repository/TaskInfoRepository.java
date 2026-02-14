package com.filemanager.backend.repository;

import com.filemanager.backend.entity.TaskInfoPO;
import com.filemanager.backend.mapper.TaskInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class TaskInfoRepository {
    
    @Autowired
    private TaskInfoMapper taskInfoMapper;
    
    public int create(TaskInfoPO taskInfo) {
        return taskInfoMapper.insert(taskInfo);
    }
    
    public int update(TaskInfoPO taskInfo) {
        return taskInfoMapper.update(taskInfo);
    }
    
    public int delete(String taskId) {
        return taskInfoMapper.deleteByTaskId(taskId);
    }
    
    public TaskInfoPO findById(String taskId) {
        return taskInfoMapper.selectByTaskId(taskId);
    }
    
    public List<TaskInfoPO> findAll() {
        return taskInfoMapper.selectAll();
    }
    
    public List<TaskInfoPO> findByStatus(String status) {
        return taskInfoMapper.selectByStatus(status);
    }
    
    public List<TaskInfoPO> findByDateRange(Date startDate, Date endDate) {
        return taskInfoMapper.selectByDateRange(startDate, endDate);
    }
    
    public List<TaskInfoPO> findByKeyword(String keyword) {
        return taskInfoMapper.selectByKeyword(keyword);
    }
    
    public List<TaskInfoPO> findByPage(String status, Date startDate, Date endDate, 
                                         String keyword, String sortBy, String sortOrder, 
                                         int page, int size) {
        int offset = (page - 1) * size;
        return taskInfoMapper.selectByPage(status, startDate, endDate, keyword, 
                                         sortBy, sortOrder, offset, size);
    }
    
    public int countByPage(String status, Date startDate, Date endDate, String keyword) {
        return taskInfoMapper.countByPage(status, startDate, endDate, keyword);
    }
    
    public int updateStatus(String taskId, String status) {
        return taskInfoMapper.updateStatus(taskId, status);
    }
    
    public int updateProgress(String taskId, Double progress) {
        return taskInfoMapper.updateProgress(taskId, progress);
    }
    
    public int updateMessage(String taskId, String message) {
        return taskInfoMapper.updateMessage(taskId, message);
    }
}
