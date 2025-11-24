package com.fieldforcepro.service;

import com.fieldforcepro.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAgentCredentials(User user, String rawPassword) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Your FieldForcePro Login Details");

        StringBuilder body = new StringBuilder();
        body.append("Dear ")
                .append(user.getName() != null ? user.getName() : "Field Staff")
                .append(",\n\n")
                .append("Your FieldForcePro login has been created.\n\n")
                .append("Login URL: http://localhost:3000/\n")
                .append("Username: ").append(user.getEmail()).append("\n")
                .append("Password: ").append(rawPassword).append("\n\n")
                .append("You can change your password after login from the Settings page.\n\n")
                .append("Regards,\nFieldForcePro Admin");

        message.setText(body.toString());
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            // Do not fail the request if email cannot be sent; just log it.
            log.warn("Failed to send agent credentials email to {}: {}", user.getEmail(), ex.getMessage());
        }
    }
}
