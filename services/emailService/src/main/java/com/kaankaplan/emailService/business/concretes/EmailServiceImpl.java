package com.kaankaplan.emailService.business.concretes;

import com.kaankaplan.emailService.business.abstracts.EmailService;
import com.kaankaplan.emailService.storage.EmailArchiveStorageService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final FreeMarkerConfigurer configuration;
    private final EmailArchiveStorageService emailArchiveStorageService;

    @Override
    public void sendEmail(String sender, String recipient, String subject, Map<String, String> model, String ticketReference) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            Template template = configuration.getConfiguration().getTemplate("emailTemplate.ftlh");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

            helper.setFrom(sender);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);

            javaMailSender.send(mimeMessage);

            try {
                emailArchiveStorageService.archiveTicketEmail(ticketReference, recipient, subject, html);
            } catch (RuntimeException archiveException) {
                log.warn("Ticket email was sent but could not be archived to object storage for {}.", recipient, archiveException);
            }
        } catch (MessagingException | TemplateException | IOException exception) {
            throw new RuntimeException("Failed to send ticket email.", exception);
        }
    }
}
