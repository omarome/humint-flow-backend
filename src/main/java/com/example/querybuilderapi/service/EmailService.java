package com.example.querybuilderapi.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.mail.from:noreply@humintflow.com}")
    private String fromAddress;

    public void sendInvitation(String toEmail, String displayName, String role,
                               String invitedByName, String setPasswordLink) {
        if (mailSender == null) {
            log.warn("Mail sender not configured — skipping invitation email to {}. Set MAIL_HOST to enable.", toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("You've been invited to HumintFlow");
            helper.setText(buildHtml(displayName, formatRole(role), invitedByName, setPasswordLink, frontendUrl), true);
            mailSender.send(message);
            log.info("Invitation email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send invitation email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String formatRole(String role) {
        return switch (role) {
            case "SUPER_ADMIN"     -> "Super Admin";
            case "WORKSPACE_OWNER" -> "Owner";
            case "ADMIN"           -> "Admin";
            case "MANAGER"        -> "Manager";
            case "SALES_REP"       -> "Sales Rep";
            case "VIEWER"          -> "Viewer";
            case "GUEST"           -> "Guest";
            default                -> role;
        };
    }

    private String buildHtml(String name, String role, String invitedBy,
                             String setPasswordLink, String appUrl) {
        String ctaHref  = setPasswordLink != null ? setPasswordLink : appUrl;
        String ctaLabel = setPasswordLink != null ? "Set Your Password" : "Sign In to HumintFlow";
        String footer   = setPasswordLink != null
            ? "This link expires in 1 hour and can only be used once. After setting your password you can sign in at <a href=\"" + appUrl + "\" style=\"color:#7c69ef;\">" + appUrl + "</a>."
            : "Visit the app and use the <em>Forgot password</em> link to set your password before signing in.";

        return """
            <!DOCTYPE html>
            <html lang="en">
            <body style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#f8fafc;margin:0;padding:40px 20px;">
              <div style="max-width:480px;margin:0 auto;background:#fff;border-radius:12px;padding:40px;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                <h1 style="margin:0 0 8px;font-size:1.5rem;color:#0f172a;">You've been invited!</h1>
                <p style="color:#64748b;margin:0 0 24px;">
                  <strong>%s</strong> has invited you to join <strong>HumintFlow</strong> as a <strong>%s</strong>.
                  Click the button below to set your password and get started.
                </p>
                <a href="%s"
                   style="display:inline-block;padding:12px 28px;background:linear-gradient(135deg,#7c69ef,#6c5ce7);color:#fff;text-decoration:none;border-radius:8px;font-weight:600;font-size:0.95rem;">
                  %s
                </a>
                <p style="margin-top:28px;font-size:0.82rem;color:#94a3b8;line-height:1.6;">%s</p>
              </div>
            </body>
            </html>
            """.formatted(invitedBy, role, ctaHref, ctaLabel, footer);
    }
}
