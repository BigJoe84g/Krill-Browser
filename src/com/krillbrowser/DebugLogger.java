package com.krillbrowser;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugLogger {
    private static final String LOG_FILE = "krill_debug.log";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static void log(String category, String message) {
        String timestamp = dtf.format(LocalDateTime.now());
        String logLine = String.format("[%s] [%s] %s", timestamp, category, message);

        // Print to console
        System.out.println(logLine);

        // Write to file
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(logLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logError(String category, String message, Throwable t) {
        log(category, "ERROR: " + message);
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            t.printStackTrace(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
