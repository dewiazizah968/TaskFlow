package com.taskflow.repository;

import com.taskflow.entity.Task;
import com.taskflow.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    List<TaskHistory> findByTask(Task task);
}