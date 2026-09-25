package com.Ledjr;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;

public class Main{
    //creates a log for main class.
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args){
    //Loads DB Sensitive info from an ENV file so you dont need to add it to your shell.
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();

        String url = getEnv("DB_URL", env);
        String user = getEnv("DB_USER", env);
        String password = getEnv("DB_PASSWORD", env);

        try(Connection conn = DriverManager.getConnection(url, user, password)){
            log.info("Connected to Database: " + !conn.isClosed());
        }catch(SQLException e){ 
            log.error("Connection to Database failed: " + e.getMessage());
        }
    }

    private static String getEnv(String key, Dotenv dotenv) {
    String value = System.getenv(key);
    return value != null ? value : dotenv.get(key);
    }

}
