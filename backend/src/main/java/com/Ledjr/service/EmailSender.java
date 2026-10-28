package com.Ledjr.service;

/**
 * sends an email. split out as an interface so the code that needs to send one
 * (password reset) doesn't care how it actually goes out.
 * two of them: ResendEmailSender (really sends it) and LogEmailSender (prints it, for local dev).
 * EmailConfig picks which one
 */
public interface EmailSender {

    void send(String to, String subject, String text);

}
