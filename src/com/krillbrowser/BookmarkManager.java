package com.krillbrowser;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manages bookmarks with persistence to disk.
 */
public class BookmarkManager {

    private static BookmarkManager instance;
    private Set<String> bookmarks;
    private Path bookmarksFile;

    private BookmarkManager() {
        bookmarks = new LinkedHashSet<>();
        bookmarksFile = getDataDirectory().resolve("bookmarks.txt");
        loadBookmarks();
    }

    public static synchronized BookmarkManager getInstance() {
        if (instance == null) {
            instance = new BookmarkManager();
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

    public void addBookmark(String url) {
        if (url != null && !url.isEmpty()) {
            bookmarks.add(url);
            saveBookmarks();
        }
    }

    public void removeBookmark(String url) {
        bookmarks.remove(url);
        saveBookmarks();
    }

    public boolean isBookmarked(String url) {
        return bookmarks.contains(url);
    }

    public List<String> getBookmarks() {
        return new ArrayList<>(bookmarks);
    }

    public void clearBookmarks() {
        bookmarks.clear();
        saveBookmarks();
    }

    private void loadBookmarks() {
        try {
            if (Files.exists(bookmarksFile)) {
                List<String> lines = Files.readAllLines(bookmarksFile);
                bookmarks.addAll(lines);
            }
        } catch (IOException e) {
            System.err.println("Failed to load bookmarks: " + e.getMessage());
        }
    }

    private void saveBookmarks() {
        try {
            Files.write(bookmarksFile, bookmarks);
        } catch (IOException e) {
            System.err.println("Failed to save bookmarks: " + e.getMessage());
        }
    }
}
