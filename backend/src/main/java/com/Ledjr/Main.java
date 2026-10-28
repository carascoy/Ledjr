package com.Ledjr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// the exclude: Spring Security makes a fake "user" account with a random password and prints it
// at startup, in case you have no logins of your own. we do (AuthController), so skip it
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
