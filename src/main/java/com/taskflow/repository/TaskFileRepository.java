package com.taskflow.repository;

import com.taskflow.entity.Task;
import com.taskflow.entity.TaskFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskFileRepository
        extends JpaRepository<TaskFile, Long> {

    List<TaskFile> findByTask(Task task);
}