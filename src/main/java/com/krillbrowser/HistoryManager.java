package com.krillbrowser;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Manages browsing history with persistence to disk.
 */
public class HistoryManager {

    private static HistoryManager instance;
    private List<HistoryEntry> history;
    private Path historyFile;
    private static final int MAX_HISTORY_SIZE = 1000;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private HistoryManager() {
        history = new ArrayList<>();
        historyFile = getDataDirectory().resolve("history.txt");
        loadHistory();
    }

    public static synchronized HistoryManager getInstance() {
        if (instance == null) {
            instance = new HistoryManager();
        }
        return instance;
    }

    private Path getDataDirectory() {
        String userHome = System.getProperty("user.home");
        Path dataDir = Paths.get(userHome, ".krillbrowser");
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            System.err.println("Failed to create data directory: " + e.getMessage());
        }
        return dataDir;
    }

    public void addToHistory(String url) {
        if (url == null || url.isEmpty())
            return;

        // Don't add duplicate consecutive entries
        if (!history.isEmpty() && history.get(0).url.equals(url)) {
            return;
        }

        HistoryEntry entry = new HistoryEntry(url, LocalDateTime.now());
        history.add(0, entry);

        // Limit history size
        if (history.size() > MAX_HISTORY_SIZE) {
            history = new ArrayList<>(history.subList(0, MAX_HISTORY_SIZE));
        }

        saveHistory();
    }

    public List<String> getHistory() {
        List<String> formattedHistory = new ArrayList<>();
        for (HistoryEntry entry : history) {
            formattedHistory.add(entry.timestamp.format(FORMATTER) + " - " + entry.url);
        }
        return formattedHistory;
    }

    public void clearHistory() {
        history.clear();
        saveHistory();
    }

    private void loadHistory() {
        try {
            if (Files.exists(historyFile)) {
                List<String> lines = Files.readAllLines(historyFile);
                for (String line : lines) {
                    try {
                        int separatorIndex = line.indexOf("|");
                        if (separatorIndex > 0) {
                            String timestamp = line.substring(0, separatorIndex);
                            String url = line.substring(separatorIndex + 1);
                            LocalDateTime dateTime = LocalDateTime.parse(timestamp, FORMATTER);
                            history.add(new HistoryEntry(url, dateTime));
                        }
                    } catch (Exception e) {
                        // Skip malformed entries
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load history: " + e.getMessage());
        }
    }

    private void saveHistory() {
        try {
            List<String> lines = new ArrayList<>();
            for (HistoryEntry entry : history) {
                lines.add(entry.timestamp.format(FORMATTER) + "|" + entry.url);
            }
            Files.write(historyFile, lines);
        } catch (IOException e) {
            System.err.println("Failed to save history: " + e.getMessage());
        }
    }

    private static class HistoryEntry {
        String url;
        LocalDateTime timestamp;

        HistoryEntry(String url, LocalDateTime timestamp) {
            this.url = url;
            this.timestamp = timestamp;
        }
    }
}
