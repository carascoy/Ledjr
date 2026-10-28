package com.Ledjr.service;

/**
 * sends an email. split out as an interface so the code that needs to send one
 * (password reset) doesn't care how it actually goes out.
 * right now the only one is LogEmailSender (prints it to the log), a real one (Resend) comes with deploy
 */
public interface EmailSender {

    void send(String to, String subject, String text);

}
