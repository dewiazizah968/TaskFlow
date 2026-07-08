package com.taskflow.repository;

import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMessageRepository extends JpaRepository<ProjectMessage, Long> {

    /** Full history, oldest first — used when the chat is first opened. */
    List<ProjectMessage> findByProjectOrderByCreatedAtAsc(Project project);

    /** Only messages newer than a given id — used for lightweight polling. */
    List<ProjectMessage> findByProjectAndIdGreaterThanOrderByCreatedAtAsc(Project project, Long afterId);
}
