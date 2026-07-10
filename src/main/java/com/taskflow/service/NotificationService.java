package com.taskflow.service;

import com.taskflow.entity.Notification;
import com.taskflow.entity.Project;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.repository.NotificationRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    
    @Scheduled(cron = "0 0 7 * * *")
    // @Scheduled(fixedRate = 60000)

    public void generateDeadlineNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);

        List<Task> tasks = taskRepository.findByDeadlineBetween(now, tomorrow);

        for (Task task : tasks) {
            if (task.getAssignedUser() != null) {
                Notification notification = Notification.builder()
                        .user(task.getAssignedUser())
                        .task(task)
                        .message("Deadline task '" + task.getTitle() + "' akan segera berakhir.")
                        .build();

                notificationRepository.save(notification);
            }
        }
    }

    /** Sends an "invitation" style notification when a user is added to a project. */
    public void createProjectInviteNotification(User invitedUser, Project project) {
        Notification notification = Notification.builder()
                .user(invitedUser)
                .project(project)
                .type("PROJECT_INVITE")
                .message("You are invited to join the project '" + project.getName() + "' by "
                        + project.getOwner().getName() + ".")
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Notifies a project participant that a new chat message arrived.
     */
    public void createChatMessageNotification(User recipient, Project project, User sender) {
        boolean alreadyPending = notificationRepository
                .findFirstByUserAndProjectAndTypeAndIsReadFalse(recipient, project, "PROJECT_CHAT")
                .isPresent();

        if (alreadyPending) {
            return;
        }

        Notification notification = Notification.builder()
                .user(recipient)
                .project(project)
                .type("PROJECT_CHAT")
                .message(sender.getName() + " send a new message in the project chat group '" + project.getName() + "'.")
                .build();

        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByUser(user);
    }
}