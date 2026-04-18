package com.example.querybuilderapi.service.automation;

import com.example.querybuilderapi.model.Opportunity;
import com.example.querybuilderapi.service.FcmNotificationService;
import com.example.querybuilderapi.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SendFcmActionExecutor implements ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(SendFcmActionExecutor.class);

    private final FcmNotificationService fcmNotificationService;
    private final NotificationService notificationService;

    public SendFcmActionExecutor(FcmNotificationService fcmNotificationService,
                                  NotificationService notificationService) {
        this.fcmNotificationService = fcmNotificationService;
        this.notificationService = notificationService;
    }

    @Override
    public void execute(Map<String, Object> actionConfig, Object targetEntity) {
        String title = (String) actionConfig.getOrDefault("title", "Automated Notification");
        String body  = (String) actionConfig.getOrDefault("body", "");

        // Enrich title/body with deal context when triggered by an Opportunity
        if (targetEntity instanceof Opportunity opp) {
            String dealName = opp.getName() != null ? opp.getName() : "Unknown Deal";
            String amount   = opp.getAmount() != null ? "$" + opp.getAmount().toPlainString() : "";
            String stage    = opp.getStage() != null ? opp.getStage().replace("_", " ") : "";

            // Replace any placeholders the user may have used
            title = title.replace("{{dealName}}", dealName)
                         .replace("{{amount}}", amount)
                         .replace("{{stage}}", stage);
            body  = body.replace("{{dealName}}", dealName)
                        .replace("{{amount}}", amount)
                        .replace("{{stage}}", stage);

            // Always append deal details so the notification is specific
            String dealSummary = dealName + (amount.isBlank() ? "" : " (" + amount + ")");
            body = body.isBlank() ? dealSummary : body + "\n" + dealSummary;
        }

        // Always create an in-app notification so it appears in the bell dropdown
        notificationService.createNotification(title, body, "success");

        // Attempt FCM push only when a target is configured
        String targetType  = (String) actionConfig.getOrDefault("targetType", "TOPIC");
        String targetValue = (String) actionConfig.get("targetValue");

        if (targetValue == null || targetValue.trim().isEmpty()) {
            log.info("SEND_FCM action: no targetValue configured, skipping FCM push.");
            return;
        }

        if ("TOKEN".equalsIgnoreCase(targetType)) {
            fcmNotificationService.sendToToken(targetValue, title, body, null);
        } else if ("TOPIC".equalsIgnoreCase(targetType)) {
            fcmNotificationService.sendToTopic(targetValue, title, body, null);
        }
    }

    @Override
    public String getActionType() {
        return "SEND_FCM";
    }
}
