package com.upemail.registration.service;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
@AllArgsConstructor
public class EmailSenderService {

    private final static Logger LOGGER =
            LoggerFactory.getLogger(EmailSenderService.class);

    JavaMailSender javaMailSender;

    public void send(String toEmail, String body) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "utf-8");
            mimeMessageHelper.setText(body, true);
            mimeMessageHelper.setTo(toEmail);
            mimeMessageHelper.setSubject("Activate your email");
            mimeMessageHelper.setFrom("yudhkra212@gmail.com");
            javaMailSender.send(mimeMessage);

        } catch (MessagingException e) {
            LOGGER.error("failed to send email", e);
            throw new IllegalStateException("failed to send email");
        }
    }

}
