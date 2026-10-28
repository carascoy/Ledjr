package com.Ledjr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * for local dev: doesn't send anything, prints the email in the backend log instead
 * so you can copy the reset link out of it.
 * NOT for the real server: reset links in logs are as good as passwords. EmailConfig only
 * picks this one when RESEND_API_KEY isn't set
 */
public class LogEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void send(String to, String subject, String text){
        log.info("\n---- email (not actually sent, RESEND_API_KEY isn't set) ----\nto: {}\nsubject: {}\n\n{}\n----------------------------------------------------------------",
            to, subject, text);
    }
}
