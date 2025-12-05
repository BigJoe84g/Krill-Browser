package com.krillbrowser;

import java.util.*;

/**
 * DownloadSecurity - Scans downloads for potentially dangerous files
 * 
 * Features:
 * - Warns about executable file types
 * - Checks file extensions
 * - Alerts for double extensions (document.pdf.exe)
 */
public class DownloadSecurity {

    private static DownloadSecurity instance;

    // Dangerous file extensions
    private static final Set<String> DANGEROUS_EXTENSIONS = new HashSet<>(Arrays.asList(
            // Executables
            "exe", "msi", "bat", "cmd", "com", "scr", "pif",
            // Scripts
            "vbs", "js", "jse", "ws", "wsf", "wsh", "ps1", "psm1",
            // macOS specific
            "app", "dmg", "pkg",
            // Linux specific
            "sh", "run", "bin",
            // Archives with potential threats
            "jar", "apk",
            // Office macros
            "docm", "xlsm", "pptm"));

    // Warning extensions (not blocked, just warned)
    private static final Set<String> WARNING_EXTENSIONS = new HashSet<>(Arrays.asList(
            "zip", "rar", "7z", "tar", "gz",
            "iso", "img",
            "torrent"));

    private DownloadSecurity() {
    }

    public static synchronized DownloadSecurity getInstance() {
        if (instance == null) {
            instance = new DownloadSecurity();
        }
        return instance;
    }

    /**
     * Check if a download is potentially dangerous
     */
    public DownloadResult checkDownload(String filename, String url) {
        if (filename == null)
            return new DownloadResult(false, false, null);

        String lowerFilename = filename.toLowerCase();
        String extension = getExtension(lowerFilename);

        // Check for double extensions (document.pdf.exe)
        if (hasDoubleExtension(lowerFilename)) {
            return new DownloadResult(true, true,
                    "⚠️ DANGEROUS: File has hidden extension!\n" +
                            "This file appears to be '" + lowerFilename + "'\n" +
                            "but may actually be an executable.");
        }

        // Check dangerous extensions
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            return new DownloadResult(true, true,
                    "⚠️ DANGEROUS: Executable file detected!\n\n" +
                            "File: " + filename + "\n" +
                            "Type: ." + extension + "\n\n" +
                            "This file type can harm your computer.\n" +
                            "Only download if you trust the source.");
        }

        // Check warning extensions
        if (WARNING_EXTENSIONS.contains(extension)) {
            return new DownloadResult(false, true,
                    "⚠️ Caution: Archive file\n\n" +
                            "File: " + filename + "\n\n" +
                            "Archives can contain harmful files.\n" +
                            "Scan with antivirus before opening.");
        }

        return new DownloadResult(false, false, null);
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    private boolean hasDoubleExtension(String filename) {
        // Check for patterns like document.pdf.exe
        String[] dangerousDoubles = {
                ".pdf.exe", ".doc.exe", ".jpg.exe", ".png.exe",
                ".txt.exe", ".xls.exe", ".mp3.exe", ".mp4.exe",
                ".pdf.scr", ".doc.scr", ".jpg.js", ".png.vbs"
        };

        for (String pattern : dangerousDoubles) {
            if (filename.endsWith(pattern)) {
                return true;
            }
        }

        // Check for any document extension followed by executable
        if (filename.matches(".*\\.(pdf|doc|docx|xls|xlsx|jpg|png|gif|mp3|mp4)\\.(exe|scr|bat|cmd|com|vbs|js)$")) {
            return true;
        }

        return false;
    }

    /**
     * Result of download security check
     */
    public static class DownloadResult {
        public final boolean isDangerous;
        public final boolean showWarning;
        public final String warningMessage;

        public DownloadResult(boolean isDangerous, boolean showWarning, String warningMessage) {
            this.isDangerous = isDangerous;
            this.showWarning = showWarning;
            this.warningMessage = warningMessage;
        }
    }
}
