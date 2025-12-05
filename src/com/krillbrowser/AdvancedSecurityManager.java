package com.krillbrowser;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * AdvancedSecurityManager - Comprehensive security features for daily browser
 * use
 * 
 * Features:
 * - Tracker/Ad blocking using blocklists
 * - Do Not Track headers
 * - Referrer blocking
 * - JavaScript control
 * - Cookie policies
 * - Auto-clear on exit
 * - Panic button support
 */
public class AdvancedSecurityManager {

    private static AdvancedSecurityManager instance;

    // Security settings
    private boolean blockTrackers = true;
    private boolean blockAds = true;
    private boolean sendDoNotTrack = true;
    private boolean blockReferrer = true;
    private boolean blockThirdPartyCookies = true;
    private boolean clearOnExit = false;
    private boolean javascriptEnabled = true;
    private boolean httpsOnly = false; // Strict mode - block all HTTP

    // Tracker/Ad blocklist (commonly blocked domains)
    private Set<String> blockedDomains;

    // Tracking keywords in URLs
    private static final String[] TRACKING_PARAMS = {
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid", "msclkid", "twclid", "igshid",
            "mc_eid", "mc_cid", "_ga", "_gl", "ref", "source"
    };

    // Common tracker/ad domains
    private static final String[] BLOCKED_TRACKER_DOMAINS = {
            // Analytics
            "google-analytics.com", "googletagmanager.com", "googleadservices.com",
            "doubleclick.net", "googlesyndication.com", "googletagservices.com",
            // Facebook
            "facebook.net", "fbcdn.net", "facebook.com/tr", "connect.facebook.net",
            // Twitter
            "ads-twitter.com", "analytics.twitter.com",
            // Other trackers
            "hotjar.com", "mixpanel.com", "segment.io", "amplitude.com",
            "newrelic.com", "nr-data.net", "fullstory.com",
            "mouseflow.com", "crazyegg.com", "luckyorange.com",
            // Ad networks
            "adnxs.com", "adsrvr.org", "advertising.com", "adform.net",
            "criteo.com", "criteo.net", "outbrain.com", "taboola.com",
            "amazon-adsystem.com", "media.net", "pubmatic.com",
            // Malware/Scam (expanded list)
            "malware.com", "phishing-site.com", "fake-bank.com",
            "virus-download.net", "steal-passwords.com", "crypto-scam.com"
    };

    private AdvancedSecurityManager() {
        blockedDomains = new HashSet<>();
        Collections.addAll(blockedDomains, BLOCKED_TRACKER_DOMAINS);
        loadCustomBlocklist();
    }

    public static synchronized AdvancedSecurityManager getInstance() {
        if (instance == null) {
            instance = new AdvancedSecurityManager();
        }
        return instance;
    }

    /**
     * Check if a URL should be blocked (trackers, ads, malware)
     */
    public boolean shouldBlockUrl(String url) {
        if (url == null)
            return false;

        String lowerUrl = url.toLowerCase();

        // Check blocked domains
        if (blockTrackers || blockAds) {
            // WHITELIST: Allow Google Video (YouTube CDN)
            if (lowerUrl.contains("googlevideo.com") || lowerUrl.contains("youtube.com")) {
                DebugLogger.log("SecurityManager", "Whitelisted YouTube: " + url);
                return false;
            }

            for (String domain : blockedDomains) {
                if (lowerUrl.contains(domain)) {
                    DebugLogger.log("SecurityManager", "BLOCKED domain (" + domain + "): " + url);
                    System.out.println("🛡️ Blocked: " + domain);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Clean tracking parameters from URL
     */
    public String cleanUrl(String url) {
        if (url == null || !url.contains("?"))
            return url;

        // CRITICAL FOR YOUTUBE: Do not strip parameters from video chunks
        if (url.contains("googlevideo.com")) {
            return url;
        }

        try {
            URI uri = new URI(url);
            String query = uri.getQuery();

            if (query == null)
                return url;

            // Parse and filter query parameters
            StringBuilder cleanQuery = new StringBuilder();
            String[] params = query.split("&");

            for (String param : params) {
                String paramName = param.split("=")[0].toLowerCase();
                boolean isTracker = false;

                for (String trackParam : TRACKING_PARAMS) {
                    if (paramName.equals(trackParam) || paramName.startsWith(trackParam)) {
                        isTracker = true;
                        System.out.println("🧹 Removed tracking param: " + paramName);
                        break;
                    }
                }

                if (!isTracker) {
                    if (cleanQuery.length() > 0)
                        cleanQuery.append("&");
                    cleanQuery.append(param);
                }
            }

            // Rebuild URL
            String baseUrl = url.split("\\?")[0];
            if (cleanQuery.length() > 0) {
                return baseUrl + "?" + cleanQuery.toString();
            }
            return baseUrl;

        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Check if JavaScript should be enabled for this domain
     */
    public boolean isJavaScriptAllowed(String url) {
        return javascriptEnabled;
    }

    /**
     * Get referrer policy
     */
    public String getReferrerPolicy() {
        return blockReferrer ? "no-referrer" : "strict-origin-when-cross-origin";
    }

    /**
     * Check if this is an HTTPS-only violation
     */
    public boolean isHttpsViolation(String url) {
        return httpsOnly && url != null && url.startsWith("http://");
    }

    /**
     * Upgrade HTTP to HTTPS
     */
    public String upgradeToHttps(String url) {
        if (url != null && url.startsWith("http://")) {
            return url.replace("http://", "https://");
        }
        return url;
    }

    /**
     * Clear all browsing data - PANIC BUTTON
     */
    public void panicClear() {
        System.out.println("🚨 PANIC CLEAR - Wiping all data!");
        HistoryManager.getInstance().clearHistory();
        BookmarkManager.getInstance().clearBookmarks();
        CookieManager.getInstance().clearAllCookies();
    }

    /**
     * Load custom blocklist from file
     */
    private void loadCustomBlocklist() {
        try {
            Path blocklistPath = getDataDirectory().resolve("blocklist.txt");
            if (Files.exists(blocklistPath)) {
                List<String> customDomains = Files.readAllLines(blocklistPath);
                blockedDomains.addAll(customDomains);
            }
        } catch (IOException e) {
            System.err.println("Could not load custom blocklist: " + e.getMessage());
        }
    }

    /**
     * Add domain to blocklist
     */
    public void addToBlocklist(String domain) {
        blockedDomains.add(domain.toLowerCase());
        saveCustomBlocklist();
    }

    /**
     * Save custom blocklist
     */
    private void saveCustomBlocklist() {
        try {
            Path blocklistPath = getDataDirectory().resolve("blocklist.txt");
            Files.write(blocklistPath, blockedDomains);
        } catch (IOException e) {
            System.err.println("Could not save blocklist: " + e.getMessage());
        }
    }

    private Path getDataDirectory() {
        String userHome = System.getProperty("user.home");
        Path dataDir = Paths.get(userHome, ".krillbrowser");
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            System.err.println("Failed to create data directory");
        }
        return dataDir;
    }

    /**
     * Get security statistics
     */
    public Map<String, Integer> getSecurityStats() {
        // In a real implementation, these would be tracked during browsing
        Map<String, Integer> stats = new HashMap<>();
        stats.put("trackersBlocked", 0);
        stats.put("adsBlocked", 0);
        stats.put("httpsUpgrades", 0);
        return stats;
    }

    /**
     * Get all blocked domains
     */
    public Set<String> getBlockedDomains() {
        return new HashSet<>(blockedDomains);
    }

    public int getBlockedDomainsCount() {
        return blockedDomains.size();
    }

    // Getters and setters
    public boolean isBlockTrackers() {
        return blockTrackers;
    }

    public void setBlockTrackers(boolean block) {
        this.blockTrackers = block;
    }

    public boolean isBlockAds() {
        return blockAds;
    }

    public void setBlockAds(boolean block) {
        this.blockAds = block;
    }

    public boolean isSendDoNotTrack() {
        return sendDoNotTrack;
    }

    public void setSendDoNotTrack(boolean send) {
        this.sendDoNotTrack = send;
    }

    public boolean isBlockReferrer() {
        return blockReferrer;
    }

    public void setBlockReferrer(boolean block) {
        this.blockReferrer = block;
    }

    public boolean isBlockThirdPartyCookies() {
        return blockThirdPartyCookies;
    }

    public void setBlockThirdPartyCookies(boolean block) {
        this.blockThirdPartyCookies = block;
    }

    public boolean isClearOnExit() {
        return clearOnExit;
    }

    public void setClearOnExit(boolean clear) {
        this.clearOnExit = clear;
    }

    public boolean isJavascriptEnabled() {
        return javascriptEnabled;
    }

    public void setJavascriptEnabled(boolean enabled) {
        this.javascriptEnabled = enabled;
    }

    public boolean isHttpsOnly() {
        return httpsOnly;
    }

    public void setHttpsOnly(boolean httpsOnly) {
        this.httpsOnly = httpsOnly;
    }
}
