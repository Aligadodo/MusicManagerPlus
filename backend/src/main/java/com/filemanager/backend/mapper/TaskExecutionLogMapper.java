package com.filemanager.backend.mapper;

import com.filemanager.backend.entity.TaskExecutionLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TaskExecutionLogMapper {

    @Insert("INSERT INTO task_execution_log (task_id, timestamp, log_level, log_type, message, details, created_at) " +
            "VALUES (#{taskId}, #{timestamp}, #{logLevel}, #{logType}, #{message}, #{details}, #{createdAt})")
    int insert(TaskExecutionLog log);

    @Select("SELECT * FROM task_execution_log WHERE task_id = #{taskId} ORDER BY timestamp DESC LIMIT #{limit} OFFSET #{offset}")
    List<TaskExecutionLog> selectByTaskId(@Param("taskId") String taskId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM task_execution_log WHERE task_id = #{taskId}")
    int countByTaskId(@Param("taskId") String taskId);

    @Select("SELECT * FROM task_execution_log WHERE task_id = #{taskId} AND log_level = #{logLevel} ORDER BY timestamp DESC LIMIT #{limit} OFFSET #{offset}")
    List<TaskExecutionLog> selectByTaskIdAndLevel(@Param("taskId") String taskId, @Param("logLevel") String logLevel, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM task_execution_log WHERE task_id = #{taskId} AND log_type = #{logType} ORDER BY timestamp DESC LIMIT #{limit} OFFSET #{offset}")
    List<TaskExecutionLog> selectByTaskIdAndType(@Param("taskId") String taskId, @Param("logType") String logType, @Param("limit") int limit, @Param("offset") int offset);

    @Delete("DELETE FROM task_execution_log WHERE task_id = #{taskId}")
    int deleteByTaskId(@Param("taskId") String taskId);

    @Select("SELECT * FROM task_execution_log WHERE task_id = #{taskId} AND timestamp > #{since} ORDER BY timestamp ASC")
    List<TaskExecutionLog> selectNewLogs(@Param("taskId") String taskId, @Param("since") Long since);
}
