package com.example.querybuilderapi.service.automation;

import com.example.querybuilderapi.model.Activity;
import com.example.querybuilderapi.model.ActivityType;
import com.example.querybuilderapi.model.EntityType;
import com.example.querybuilderapi.model.Opportunity;
import com.example.querybuilderapi.repository.ActivityRepository;
import com.example.querybuilderapi.repository.OpportunityRepository;
import com.example.querybuilderapi.service.FcmNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ScheduledCrmJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCrmJobs.class);

    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final FcmNotificationService fcmNotificationService;

    public ScheduledCrmJobs(OpportunityRepository opportunityRepository,
                            ActivityRepository activityRepository,
                            FcmNotificationService fcmNotificationService) {
        this.opportunityRepository = opportunityRepository;
        this.activityRepository = activityRepository;
        this.fcmNotificationService = fcmNotificationService;
    }

    /**
     * Runs every day at 1:00 AM.
     * Identifies active deals inactive for 14+ days, creates a follow-up task,
     * and sends an FCM notification to the deal owner (if they have a token)
     * and to the managers topic.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void flagStaleDeals() {
        log.info("Running job: flagStaleDeals");
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);

        List<Opportunity> staleOpportunities = opportunityRepository.findAllByIsDeletedFalse()
                .stream()
                .filter(opp -> !opp.getStage().equals("CLOSED_WON") && !opp.getStage().equals("CLOSED_LOST"))
                .filter(opp -> opp.getUpdatedAt() != null && opp.getUpdatedAt().isBefore(fourteenDaysAgo))
                .toList();

        int tasksCreated = 0;
        for (Opportunity opp : staleOpportunities) {
            boolean hasOpenStaleTask = activityRepository.findByEntityTypeAndEntityId(EntityType.OPPORTUNITY, opp.getId())
                    .stream()
                    .anyMatch(a -> a.getSubject() != null && a.getSubject().startsWith("[Stale Deal Alert]"));

            if (!hasOpenStaleTask) {
                Activity task = new Activity();
                task.setActivityType(ActivityType.TASK);
                task.setSubject("[Stale Deal Alert] Follow up with " + opp.getName());
                task.setBody("This deal has been inactive for over 14 days. Please review and update its status.");
                task.setEntityType(EntityType.OPPORTUNITY);
                task.setEntityId(opp.getId());
                task.setTaskDueDate(LocalDate.now().plusDays(1));
                activityRepository.save(task);
                tasksCreated++;

                // Notify the assigned owner via their device token
                if (opp.getAssignedTo() != null && opp.getAssignedTo().getFcmToken() != null) {
                    fcmNotificationService.sendToToken(
                            opp.getAssignedTo().getFcmToken(),
                            "Stale Deal Alert",
                            "Deal \"" + opp.getName() + "\" has had no activity for 14 days.",
                            Map.of("dealId", opp.getId().toString(), "type", "STALE_DEAL")
                    );
                }
            }
        }

        // Broadcast a summary to managers topic
        if (tasksCreated > 0) {
            fcmNotificationService.sendToTopic(
                    "managers",
                    "Stale Deals Report",
                    tasksCreated + " deal(s) have been flagged as stale. Review and reassign if needed.",
                    Map.of("type", "STALE_DEALS_SUMMARY", "count", String.valueOf(tasksCreated))
            );
        }

        log.info("Job completed: flagStaleDeals. {} stale deals found, {} tasks created.", staleOpportunities.size(), tasksCreated);
    }

    /**
     * Runs every day at 2:00 AM.
     * Identifies overdue tasks, creates an escalation task linked to the same entity,
     * and sends an FCM alert to the /topics/managers topic.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void flagOverdueTasks() {
        log.info("Running job: flagOverdueTasks");
        LocalDate today = LocalDate.now();

        List<Activity> overdueTasks = activityRepository.findAllByIsDeletedFalse()
                .stream()
                .filter(a -> a.getActivityType() == ActivityType.TASK)
                .filter(a -> a.getTaskDueDate() != null && a.getTaskDueDate().isBefore(today))
                .toList();

        int escalationsCreated = 0;
        for (Activity overdueTask : overdueTasks) {
            // Avoid duplicate escalation tasks
            boolean alreadyEscalated = activityRepository
                    .findByEntityTypeAndEntityId(overdueTask.getEntityType(), overdueTask.getEntityId())
                    .stream()
                    .anyMatch(a -> a.getSubject() != null
                            && a.getSubject().startsWith("[Overdue Escalation]")
                            && a.getSubject().contains(overdueTask.getSubject()));

            if (!alreadyEscalated) {
                Activity escalation = new Activity();
                escalation.setActivityType(ActivityType.TASK);
                escalation.setSubject("[Overdue Escalation] " + overdueTask.getSubject());
                escalation.setBody("Task \"" + overdueTask.getSubject() + "\" was due on "
                        + overdueTask.getTaskDueDate() + " and has not been completed. Manager review required.");
                escalation.setEntityType(overdueTask.getEntityType());
                escalation.setEntityId(overdueTask.getEntityId());
                escalation.setTaskDueDate(LocalDate.now().plusDays(1));
                activityRepository.save(escalation);
                escalationsCreated++;
            }

            // Notify the assigned team member via their token
            if (overdueTask.getAssignedTo() != null && overdueTask.getAssignedTo().getFcmToken() != null) {
                fcmNotificationService.sendToToken(
                        overdueTask.getAssignedTo().getFcmToken(),
                        "Task Overdue",
                        "Your task \"" + overdueTask.getSubject() + "\" is overdue.",
                        Map.of("taskId", overdueTask.getId().toString(), "type", "OVERDUE_TASK")
                );
            }
        }

        // Broadcast summary to managers
        if (escalationsCreated > 0) {
            fcmNotificationService.sendToTopic(
                    "managers",
                    "Overdue Tasks Report",
                    escalationsCreated + " overdue task(s) have been escalated for your review.",
                    Map.of("type", "OVERDUE_TASKS_SUMMARY", "count", String.valueOf(escalationsCreated))
            );
        }

        log.info("Job completed: flagOverdueTasks. {} overdue tasks found, {} escalations created.", overdueTasks.size(), escalationsCreated);
    }
}
