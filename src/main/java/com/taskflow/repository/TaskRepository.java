package com.taskflow.repository;

import com.taskflow.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import com.taskflow.entity.Project;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.Priority;
import com.taskflow.entity.User;

import java.util.List;
import java.time.LocalDateTime;

public interface TaskRepository
        extends JpaRepository<Task, Long> {
                List<Task> findByProject(Project project);

                long countByProject(Project project);

                long countByProjectAndStatus(
                        Project project,
                        TaskStatus status
                );

                List<Task> findByStatus(TaskStatus status);

                List<Task> findByPriority(Priority priority);

                List<Task> findByAssignedUser(User user);

                List<Task> findByDeadlineBetween(LocalDateTime start, LocalDateTime end);

                long countByStatus(TaskStatus status);

                List<Task> findByTitleContainingIgnoreCase(String keyword);
}