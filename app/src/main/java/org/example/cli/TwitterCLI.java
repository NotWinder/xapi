package org.example.cli;

import java.io.*;
import java.util.*;
import org.example.api.*;
import org.example.database.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class TwitterCLI {
    private String currentHashtag;
    private String bearerToken;
    private final BufferedReader reader;
    private static final String CONFIG_FILE = "twittercli.properties";

    public TwitterCLI(String bearerToken) {
        this.currentHashtag = "Golang"; // Default hashtag
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.bearerToken = bearerToken;
        loadConfig();
    }

    public void start() {
        boolean running = true;

        while (running) {
            try {
                displayMenu();
                String choice = reader.readLine();
                if (choice == null) {
                    System.out.println("Input stream closed. Exiting...");
                    break;
                }

                choice = choice.trim();
                switch (choice) {
                    case "1":
                        fetchAndDisplayTweets();
                        break;
                    case "2":
                        changeHashtag();
                        break;
                    case "3":
                        setBearerToken();
                        break;
                    case "4":
                        running = false;
                        System.out.println("Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }

                if (running) {
                    System.out.println("\nPress Enter to continue...");
                    reader.readLine();
                }
            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
                running = false;
            }
        }

        try {
            reader.close();
        } catch (IOException e) {
            System.err.println("Error closing reader: " + e.getMessage());
        }
    }

    private void displayMenu() {
        clearScreen();
        System.out.println("=================================");
        System.out.println("Twitter Hashtag Monitor");
        System.out.println("=================================");
        System.out.println("Current Hashtag: #" + currentHashtag);
        System.out.println("Bearer Token: " + (bearerToken == null ? "Not Set" : "Set"));
        System.out.println("---------------------------------");
        System.out.println("1. Fetch Latest Tweets");
        System.out.println("2. Change Hashtag");
        System.out.println("3. Set/Change Bearer Token");
        System.out.println("4. Exit");
        System.out.println("=================================");
        System.out.print("Enter your choice (1-4): ");
    }

    private void fetchAndDisplayTweets() {
        if (bearerToken == null || bearerToken.trim().isEmpty()) {
            System.out.println("Error: Bearer token not set. Please set it first.");
            return;
        }

        clearScreen();
        System.out.println("Fetching tweets for #" + currentHashtag + "...\n");
        try {
            JSONObject response = TwitterAPI.fetchTweetsByHashtag(bearerToken, currentHashtag, 5); // Fetch only 5
                                                                                                   // tweets

            // Print the entire response for debugging
            System.out.println(response.toString(2)); // Pretty print the JSON response

            if (response.has("data")) {
                System.out.println("\nResults:");
                System.out.println("=========");
                JSONArray tweets = response.getJSONArray("data");

                // Sort tweets by length and print the top 5
                List<JSONObject> topTweets = TwitterAPI.findTopLongestTweets(tweets);

                // Select the top 3 longest tweets
                List<JSONObject> top3Tweets = topTweets.size() > 3 ? topTweets.subList(0, 3) : topTweets;

                // Print the top 3 longest tweets
                TwitterAPI.printTweets(new JSONArray(top3Tweets));

                // Insert the top 3 tweets into the database
                for (JSONObject tweet : top3Tweets) {
                    String id = tweet.getString("id");
                    String userId = tweet.has("author_id") ? tweet.getString("author_id") : "Unknown"; // Handle missing
                                                                                                       // "author_id"
                    String text = tweet.getString("text");
                    int wordCount = TwitterAPI.wordCount(text);
                    String dateFetched = java.time.LocalDate.now().toString(); // Use today's date
                    TweetDatabase.insertTweet(id, userId, text, wordCount, dateFetched);
                }
            } else {
                System.out.println("No tweets found for #" + currentHashtag);
                if (response.has("error")) {
                    System.out.println("Error: " + response.getJSONObject("error").getString("message"));
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching tweets: " + e.getMessage());
        }
    }

    private void changeHashtag() throws IOException {
        clearScreen();
        System.out.println("Change Hashtag");
        System.out.println("=============");
        System.out.println("Current hashtag: #" + currentHashtag);
        System.out.print("Enter new hashtag (without #): ");

        String newHashtag = reader.readLine();
        if (newHashtag == null) {
            System.out.println("Input stream closed. Keeping current hashtag.");
            return;
        }

        newHashtag = newHashtag.trim();
        if (newHashtag.isEmpty()) {
            System.out.println("Hashtag cannot be empty. Keeping current hashtag.");
        } else if (newHashtag.contains(" ")) {
            System.out.println("Hashtag cannot contain spaces. Keeping current hashtag.");
        } else {
            currentHashtag = newHashtag;
            System.out.println("Hashtag updated successfully to #" + currentHashtag);
        }
    }

    private void setBearerToken() throws IOException {
        clearScreen();
        System.out.println("Set Bearer Token");
        System.out.println("================");
        System.out.print("Enter your Bearer Token: ");

        String newBearerToken = reader.readLine();
        if (newBearerToken == null || newBearerToken.trim().isEmpty()) {
            System.out.println("Bearer token cannot be empty.");
        } else {
            bearerToken = newBearerToken.trim();
            saveConfig();
            System.out.println("Bearer token set successfully.");
        }
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            bearerToken = properties.getProperty("bearerToken");
            System.out.println("Configuration loaded.");
        } catch (IOException e) {
            System.out.println("No configuration file found. Starting with default settings.");
        }
    }

    private void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty("bearerToken", bearerToken);

        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "TwitterCLI Configuration");
            System.out.println("Configuration saved.");
        } catch (IOException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
        }
    }

    private void clearScreen() {
        // This will work on most terminals
        System.out.print("\033[H\033[2J");
        System.out.flush();
        // For systems where ANSI escape codes don't work, print some newlines
        System.out.println("\n\n\n\n\n");
    }
}
