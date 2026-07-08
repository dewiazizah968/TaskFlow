package com.taskflow.repository;

import com.taskflow.entity.Notification;
import com.taskflow.entity.Project;
import com.taskflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser(User user);

    List<Notification> findByUserAndIsReadFalse(User user);

    long countByUserAndIsReadFalse(User user);

    long countByUser(User user);

    /** Used to dedupe chat notifications: one unread "new messages" ping per project is enough. */
    Optional<Notification> findFirstByUserAndProjectAndTypeAndIsReadFalse(User user, Project project, String type);
}