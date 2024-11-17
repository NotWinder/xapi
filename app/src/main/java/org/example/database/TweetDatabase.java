package org.example.database;

import java.sql.*;

public class TweetDatabase {
    private static final String DB_URL = "jdbc:sqlite:twitter_data.db"; // Path to the SQLite database file

    // Method to initialize the database and create the table if it doesn't exist
    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Create the table if it doesn't exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS tweets (" +
                    "id TEXT PRIMARY KEY, " +
                    "user_id TEXT, " +
                    "tweet_text TEXT, " +
                    "word_count INTEGER, " +
                    "date_fetched TEXT)";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSQL);
                System.out.println("Database and table created (if not already exist).");
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    // Method to insert tweet data into the database
    public static void insertTweet(String id, String userId, String tweetText, int wordCount, String dateFetched) {
        String insertSQL = "INSERT INTO tweets (id, user_id, tweet_text, word_count, date_fetched) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, id);
            pstmt.setString(2, userId);
            pstmt.setString(3, tweetText);
            pstmt.setInt(4, wordCount);
            pstmt.setString(5, dateFetched);
            pstmt.executeUpdate();
            System.out.println("Tweet inserted into database.");
        } catch (SQLException e) {
            System.err.println("Error inserting tweet: " + e.getMessage());
        }
    }
}
