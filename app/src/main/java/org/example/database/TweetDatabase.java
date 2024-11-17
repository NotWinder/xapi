package org.example.database;

import java.sql.*;

public class TweetDatabase {

    private static final String DB_URL = "jdbc:sqlite:twitter_data.db"; // Path to your SQLite database

    static {
        try {
            // Register SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found.");
            e.printStackTrace();
        }
    }

    // Method to get a connection to the SQLite database
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // Method to initialize the database (create table)
    public static void initialize() {
        // Initialize the database (e.g., create the table if it doesn't exist)
        createTable();
    }

    // Method to insert a tweet into the database
    public static void insertTweet(String id, String userId, String text, int wordCount, String dateFetched) {
        String sql = "INSERT INTO tweets (id, user_id, text, word_count, date_fetched) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, userId);
            pstmt.setString(3, text);
            pstmt.setInt(4, wordCount);
            pstmt.setString(5, dateFetched);
            pstmt.executeUpdate();
            System.out.println("Tweet inserted into database.");

        } catch (SQLException e) {
            System.err.println("Error inserting tweet: " + e.getMessage());
        }
    }

    // Method to display all tweets in the database
    public static void displayAllTweets() {
        String sql = "SELECT * FROM tweets";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("Displaying all tweets:");
            while (rs.next()) {
                String id = rs.getString("id");
                String userId = rs.getString("user_id");
                String tweetText = rs.getString("tweet_text");
                int wordCount = rs.getInt("word_count");
                String dateFetched = rs.getString("date_fetched");

                System.out.println("ID: " + id);
                System.out.println("User ID: " + userId);
                System.out.println("Text: " + tweetText);
                System.out.println("Word Count: " + wordCount);
                System.out.println("Date Fetched: " + dateFetched);
                System.out.println("------------------------------");
            }
        } catch (SQLException e) {
            System.err.println("Error displaying tweets: " + e.getMessage());
        }
    }

    // Method to create the tweets table if it doesn't exist
    private static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS tweets (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT, " +
                "text TEXT, " +
                "word_count INTEGER, " +
                "date_fetched TEXT)";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tweets table created or already exists.");
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
        }
    }
}
