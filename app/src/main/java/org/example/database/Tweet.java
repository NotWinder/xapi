package org.example.database;

public class Tweet {
    private String id;
    private String userId;
    private String text;
    private int wordCount;
    private String dateFetched;

    // Constructor, getters and setters
    public Tweet(String id, String userId, String text, int wordCount, String dateFetched) {
        this.id = id;
        this.userId = userId;
        this.text = text;
        this.wordCount = wordCount;
        this.dateFetched = dateFetched;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getText() {
        return text;
    }

    public int getWordCount() {
        return wordCount;
    }

    public String getDateFetched() {
        return dateFetched;
    }
}
