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
        Dotenv env = Dotenv.load();

        String url = env.get("DB_URL");
        String user = env.get("DB_USER");
        String password = env.get("DB_PASSWORD");

        try(Connection conn = DriverManager.getConnection(url, user, password)){
            log.info("Connected to Database: " + !conn.isClosed());
        }catch(SQLException e){
            log.error("Connection to Database failed: " + e.getMessage());
        }
    }

}