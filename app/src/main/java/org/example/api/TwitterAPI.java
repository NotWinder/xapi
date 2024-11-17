package org.example.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public class TwitterAPI {
    private static final String SEARCH_URL = "https://api.twitter.com/2/tweets/search/recent";
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF = TimeUnit.SECONDS.toMillis(10);
    private static final int CONNECT_TIMEOUT = 30000; // 30 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds

    public static JSONObject fetchTweetsByHashtag(String bearerToken, String hashtag, int maxResults)
            throws IOException {
        if (bearerToken == null || bearerToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Bearer token cannot be null or empty.");
        }
        if (hashtag == null || hashtag.trim().isEmpty()) {
            throw new IllegalArgumentException("Hashtag cannot be null or empty.");
        }

        // Twitter API enforces a maxResults value between 10 and 100
        if (maxResults < 10) {
            maxResults = 10; // Set minimum to 10 to satisfy API restrictions
        }

        String encodedQuery = URLEncoder.encode("#" + hashtag.trim(), StandardCharsets.UTF_8.toString());
        String apiUrl = String.format("%s?query=%s&max_results=%d", SEARCH_URL, encodedQuery, maxResults);
        System.out.println("Requesting API URL: " + apiUrl);

        return makeRequest(apiUrl, bearerToken);
    }

    private static JSONObject makeRequest(String apiUrl, String bearerToken) throws IOException {
        long backoff = INITIAL_BACKOFF;
        IOException lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                // Add additional headers that might help
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "Java/TwitterAPI");

                System.out.printf("Attempt %d of %d...%n", attempt + 1, MAX_RETRIES);

                int status = connection.getResponseCode();

                if (status == HttpURLConnection.HTTP_OK) {
                    return readResponse(connection.getInputStream());
                }

                if (status == 429) {
                    handleRateLimit(connection, backoff);
                    backoff *= 2; // Exponential backoff
                    continue;
                }

                // Handle error response
                JSONObject errorResponse = readResponse(connection.getErrorStream());
                System.err.println("API Error (Status " + status + "): " + errorResponse.toString(2));

                // If it's a client error (4xx), don't retry
                if (status >= 400 && status < 500) {
                    return errorResponse;
                }
            } catch (IOException e) {
                lastException = e;
                System.err.printf("Request failed (attempt %d/%d): %s%n",
                        attempt + 1, MAX_RETRIES, e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            // Wait before retrying
            try {
                long waitTime = backoff * (attempt + 1);
                System.out.printf("Waiting %d ms before retry...%n", waitTime);
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
        }

        throw new IOException("Failed after " + MAX_RETRIES + " attempts. Last error: " +
                (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    private static void handleRateLimit(HttpURLConnection connection, long defaultBackoff) throws IOException {
        String resetTime = connection.getHeaderField("x-rate-limit-reset");
        if (resetTime != null) {
            try {
                long resetUnix = Long.parseLong(resetTime);
                long currentTime = System.currentTimeMillis() / 1000;
                long waitTime = Math.max(resetUnix - currentTime, 0) * 1000;
                System.out.println("Rate limit exceeded. Waiting until reset: " + waitTime + " ms");
                Thread.sleep(waitTime);
            } catch (NumberFormatException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Error handling rate limit", e);
            }
        } else {
            try {
                System.out.println("Rate limit exceeded. Retrying in " + defaultBackoff + " ms...");
                Thread.sleep(defaultBackoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Error handling rate limit", e);
            }
        }
    }

    private static JSONObject readResponse(java.io.InputStream stream) throws IOException {
        if (stream == null) {
            throw new IOException("Response stream is null");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return new JSONObject(response.toString());
        }
    }

    public static List<JSONObject> findTopLongestTweets(JSONArray tweets) {
        if (tweets == null || tweets.length() == 0) {
            return Collections.emptyList();
        }

        // Create main and auxiliary stacks
        Stack<JSONObject> mainStack = new Stack<>();
        Stack<JSONObject> auxStack = new Stack<>();

        // First, push all tweets to the main stack
        for (int i = 0; i < tweets.length(); i++) {
            JSONObject currentTweet = tweets.getJSONObject(i);

            // If stack is empty or current tweet has more words than top of stack
            if (mainStack.isEmpty() ||
                    wordCount(currentTweet.getString("text")) >= wordCount(mainStack.peek().getString("text"))) {
                mainStack.push(currentTweet);
            } else {
                // Move elements to auxiliary stack until we find the right position
                while (!mainStack.isEmpty() &&
                        wordCount(currentTweet.getString("text")) < wordCount(mainStack.peek().getString("text"))) {
                    auxStack.push(mainStack.pop());
                }

                // Push current tweet
                mainStack.push(currentTweet);

                // Move back all elements from auxiliary stack
                while (!auxStack.isEmpty()) {
                    mainStack.push(auxStack.pop());
                }
            }
        }

        // Convert stack to list (will be in reverse order - longest to shortest)
        List<JSONObject> sortedTweets = new ArrayList<>();
        int count = 0;
        while (!mainStack.isEmpty() && count < 5) {
            sortedTweets.add(mainStack.pop());
            count++;
        }

        return sortedTweets;
    }

    public static int wordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    public static void printTweets(JSONArray tweets) {
        for (int i = 0; i < tweets.length(); i++) {
            JSONObject tweet = tweets.getJSONObject(i);
            System.out.printf("- ID: %s%n  Text: %s%n  Word Count: %d%n%n",
                    tweet.getString("id"),
                    tweet.getString("text"),
                    wordCount(tweet.getString("text")));
        }
    }
}
