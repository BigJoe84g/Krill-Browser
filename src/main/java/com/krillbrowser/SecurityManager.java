package com.krillbrowser;

import java.util.*;

/**
 * SecurityManager - Handles browser security features
 * 
 * Features:
 * - Block known malicious/phishing domains
 * - Force HTTPS upgrades
 * - Track and warn about insecure connections
 */
public class SecurityManager {

    private static SecurityManager instance;
    private Set<String> blockedDomains;
    private boolean forceHttps = true;
    private boolean blockPopups = true;
    private boolean privateMode = false;

    // Known malicious/phishing domains (sample list - in production this would be
    // much larger)
    private static final String[] BLOCKED_DOMAINS = {
            "malware.com",
            "phishing-site.com",
            "fake-bank.com",
            "virus-download.net",
            "steal-passwords.com",
            "tracking-ads.com",
            "crypto-scam.com",
            "free-iphone-winner.com",
            "your-computer-infected.com",
            "click-here-now.xyz"
    };

    private SecurityManager() {
        blockedDomains = new HashSet<>();
        Collections.addAll(blockedDomains, BLOCKED_DOMAINS);
    }

    public static synchronized SecurityManager getInstance() {
        if (instance == null) {
            instance = new SecurityManager();
        }
        return instance;
    }

    /**
     * Check if a URL should be blocked
     */
    public boolean shouldBlockUrl(String url) {
        if (url == null)
            return false;

        String lowerUrl = url.toLowerCase();

        // Check against blocked domains
        for (String domain : blockedDomains) {
            if (lowerUrl.contains(domain)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Upgrade HTTP to HTTPS if force HTTPS is enabled
     */
    public String upgradeToHttps(String url) {
        if (url == null)
            return url;

        if (forceHttps && url.startsWith("http://")) {
            return url.replace("http://", "https://");
        }
        return url;
    }

    /**
     * Check if a URL is secure (HTTPS)
     */
    public boolean isSecureUrl(String url) {
        return url != null && url.startsWith("https://");
    }

    /**
     * Get security level for a URL (for display purposes)
     */
    public SecurityLevel getSecurityLevel(String url) {
        if (url == null)
            return SecurityLevel.UNKNOWN;

        if (shouldBlockUrl(url)) {
            return SecurityLevel.DANGEROUS;
        }

        if (url.startsWith("https://")) {
            return SecurityLevel.SECURE;
        }

        if (url.startsWith("http://")) {
            return SecurityLevel.INSECURE;
        }

        return SecurityLevel.UNKNOWN;
    }

    /**
     * Add a domain to the block list
     */
    public void blockDomain(String domain) {
        blockedDomains.add(domain.toLowerCase());
    }

    /**
     * Remove a domain from the block list
     */
    public void unblockDomain(String domain) {
        blockedDomains.remove(domain.toLowerCase());
    }

    /**
     * Get all blocked domains
     */
    public Set<String> getBlockedDomains() {
        return new HashSet<>(blockedDomains);
    }

    // Getters and setters for settings
    public boolean isForceHttps() {
        return forceHttps;
    }

    public void setForceHttps(boolean forceHttps) {
        this.forceHttps = forceHttps;
    }

    public boolean isBlockPopups() {
        return blockPopups;
    }

    public void setBlockPopups(boolean blockPopups) {
        this.blockPopups = blockPopups;
    }

    public boolean isPrivateMode() {
        return privateMode;
    }

    public void setPrivateMode(boolean privateMode) {
        this.privateMode = privateMode;
    }

    /**
     * Security levels for URLs
     */
    public enum SecurityLevel {
        SECURE, // HTTPS connection
        INSECURE, // HTTP connection
        DANGEROUS, // Known malicious site
        UNKNOWN // Can't determine
    }
}
