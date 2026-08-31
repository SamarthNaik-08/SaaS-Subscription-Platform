package com.saasplatform.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailNotificationService {

    public void sendEmail(String toEmail, String subject, String body) {
        // Safe development fallback: Log email content rather than crashing when SMTP is absent
        log.info("[EMAIL NOTIFICATION] To: {}, Subject: {}, Body: {}", toEmail, subject, body);
    }
}
