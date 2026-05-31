package com.jullyscraft.dto;

import com.jullyscraft.entity.NotificationLog;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class NotificationEvent {

    private Long                              userId;
    private String                            recipientEmail;
    private String                            recipientPhone;
    private String                            fcmToken;

    private NotificationLog.NotificationType  type;
    private NotificationLog.NotificationChannel channel;

    private String                            subject;      // email only
    private String                            templateName; // email template
    private String                            smsBody;      // SMS / WhatsApp body
    private String                            pushTitle;    // push title
    private String                            pushBody;     // push body

    private Map<String, Object>               templateVars; // Thymeleaf variables
}