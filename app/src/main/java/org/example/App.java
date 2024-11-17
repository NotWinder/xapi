package org.example;

import org.example.cli.*;
import org.example.database.*;

public class App {
    public static void main(String[] args) {

        // Initialize the database and create the table
        TweetDatabase.initialize();

        String bearerToken = "";

        TwitterCLI cli = new TwitterCLI(bearerToken);
        cli.start();
    }
}
