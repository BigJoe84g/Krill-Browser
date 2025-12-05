package com.krillbrowser;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manages cookies with persistence to disk.
 * Works with JavaFX's WebEngine cookie handler.
 */
public class CookieManager {

    private static CookieManager instance;
    private Map<String, Map<String, String>> cookies; // domain -> (name -> value)
    private Path cookiesFile;

    private CookieManager() {
        cookies = new HashMap<>();
        cookiesFile = getDataDirectory().resolve("cookies.txt");
        loadCookies();

        // Set up the default cookie manager for HTTP connections
        java.net.CookieManager cookieManager = new java.net.CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
    }

    public static synchronized CookieManager getInstance() {
        if (instance == null) {
            instance = new CookieManager();
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

    public void setCookie(String domain, String name, String value) {
        cookies.computeIfAbsent(domain, k -> new HashMap<>()).put(name, value);
        saveCookies();
    }

    public String getCookie(String domain, String name) {
        Map<String, String> domainCookies = cookies.get(domain);
        return domainCookies != null ? domainCookies.get(name) : null;
    }

    public Map<String, String> getCookiesForDomain(String domain) {
        return cookies.getOrDefault(domain, new HashMap<>());
    }

    public void removeCookie(String domain, String name) {
        Map<String, String> domainCookies = cookies.get(domain);
        if (domainCookies != null) {
            domainCookies.remove(name);
            if (domainCookies.isEmpty()) {
                cookies.remove(domain);
            }
        }
        saveCookies();
    }

    public void clearCookiesForDomain(String domain) {
        cookies.remove(domain);
        saveCookies();
    }

    public void clearAllCookies() {
        cookies.clear();
        saveCookies();

        // Also clear the system cookie manager
        try {
            java.net.CookieManager cookieManager = (java.net.CookieManager) CookieHandler.getDefault();
            if (cookieManager != null) {
                cookieManager.getCookieStore().removeAll();
            }
        } catch (Exception e) {
            System.err.println("Failed to clear system cookies: " + e.getMessage());
        }
    }

    public List<String> getAllCookieInfo() {
        List<String> cookieInfo = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> domainEntry : cookies.entrySet()) {
            String domain = domainEntry.getKey();
            for (Map.Entry<String, String> cookie : domainEntry.getValue().entrySet()) {
                cookieInfo.add(domain + " | " + cookie.getKey() + " = " + cookie.getValue());
            }
        }
        return cookieInfo;
    }

    public Set<String> getDomains() {
        return cookies.keySet();
    }

    private void loadCookies() {
        try {
            if (Files.exists(cookiesFile)) {
                List<String> lines = Files.readAllLines(cookiesFile);
                for (String line : lines) {
                    try {
                        String[] parts = line.split("\\|", 3);
                        if (parts.length == 3) {
                            String domain = parts[0].trim();
                            String name = parts[1].trim();
                            String value = parts[2].trim();
                            cookies.computeIfAbsent(domain, k -> new HashMap<>()).put(name, value);
                        }
                    } catch (Exception e) {
                        // Skip malformed entries
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load cookies: " + e.getMessage());
        }
    }

    private void saveCookies() {
        try {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, Map<String, String>> domainEntry : cookies.entrySet()) {
                String domain = domainEntry.getKey();
                for (Map.Entry<String, String> cookie : domainEntry.getValue().entrySet()) {
                    lines.add(domain + "|" + cookie.getKey() + "|" + cookie.getValue());
                }
            }
            Files.write(cookiesFile, lines);
        } catch (IOException e) {
            System.err.println("Failed to save cookies: " + e.getMessage());
        }
    }
}
