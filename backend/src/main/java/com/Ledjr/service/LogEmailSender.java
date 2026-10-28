package com.Ledjr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * for local dev: doesn't send anything, prints the email in the backend log instead
 * so you can copy the reset link out of it.
 * NOT for the real server: reset links in logs are as good as passwords. swap in a real sender at deploy
 */
@Component
public class LogEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void send(String to, String subject, String text){
        log.info("\n---- email (not actually sent, no email service set up yet) ----\nto: {}\nsubject: {}\n\n{}\n----------------------------------------------------------------",
            to, subject, text);
    }
}
