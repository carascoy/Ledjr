package com.Ledjr.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.Ledjr.service.EmailSender;
import com.Ledjr.service.LogEmailSender;
import com.Ledjr.service.ResendEmailSender;

/**
 * picks how emails go out: a Resend api key set = really send them, no key = print them to the log.
 * so your laptop needs no email account, and the real server sends real ones
 */
@Configuration
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    public EmailSender emailSender(@Value("${app.resend-api-key}") String apiKey,
                                   @Value("${app.mail-from}") String from){
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY isn't set, so emails get printed to this log instead of sent. Fine locally, not on the real server.");
            return new LogEmailSender();
        }
        log.info("Sending emails through Resend as {}", from);
        return new ResendEmailSender(apiKey.trim(), from);
    }
}
