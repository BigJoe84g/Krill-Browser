package com.krillbrowser;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * BrowserProfile - Manages different browsing modes/profiles
 * 
 * Each profile has optimized settings for different use cases:
 * - Gaming: Minimal distractions, performance focused
 * - Work: Productivity focused, balanced security
 * - Coding: Developer-friendly, relaxed security for localhost
 * - Secure: Maximum privacy and security
 * - Default: Balanced settings
 */
public class BrowserProfile {

    public enum ProfileType {
        DEFAULT("🦐 Default", "Balanced browsing experience"),
        GAMING("🎮 Gaming", "Performance mode - minimal distractions"),
        WORK("💼 Work", "Productivity focused - blocks social media"),
        CODING("💻 Coding", "Developer mode - allows localhost, relaxed security"),
        SECURE("🔒 Secure", "Maximum privacy - blocks everything");

        private final String displayName;
        private final String description;

        ProfileType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    private static BrowserProfile instance;
    private ProfileType currentProfile = ProfileType.DEFAULT;
    private Map<ProfileType, ProfileSettings> profileSettings;
    private Path settingsFile;

    // Sites to block per profile
    private static final String[] GAMING_BLOCKED = {
            "facebook.com", "twitter.com", "instagram.com", "tiktok.com",
            "reddit.com", "news.ycombinator.com", "linkedin.com"
    };

    private static final String[] WORK_BLOCKED = {
            "youtube.com", "twitch.tv", "netflix.com", "reddit.com",
            "tiktok.com", "instagram.com", "twitter.com", "discord.com",
            "steampowered.com", "epicgames.com"
    };

    private static final String[] CODING_ALLOWED = {
            "localhost", "127.0.0.1", "github.com", "stackoverflow.com",
            "developer.mozilla.org", "docs.oracle.com"
    };

    private BrowserProfile() {
        profileSettings = new HashMap<>();
        initializeProfiles();
        settingsFile = getDataDirectory().resolve("profile.txt");
        loadCurrentProfile();
    }

    public static synchronized BrowserProfile getInstance() {
        if (instance == null) {
            instance = new BrowserProfile();
        }
        return instance;
    }

    private void initializeProfiles() {
        // Default - balanced
        ProfileSettings defaultSettings = new ProfileSettings();
        defaultSettings.blockTrackers = true;
        defaultSettings.blockAds = true;
        defaultSettings.forceHttps = true;
        defaultSettings.javascriptEnabled = true;
        defaultSettings.blockedSites = new HashSet<>();
        profileSettings.put(ProfileType.DEFAULT, defaultSettings);

        // Gaming - performance, no distractions
        ProfileSettings gamingSettings = new ProfileSettings();
        gamingSettings.blockTrackers = true;
        gamingSettings.blockAds = true;
        gamingSettings.forceHttps = true;
        gamingSettings.javascriptEnabled = true;
        gamingSettings.blockedSites = new HashSet<>(Arrays.asList(GAMING_BLOCKED));
        gamingSettings.performanceMode = true;
        profileSettings.put(ProfileType.GAMING, gamingSettings);

        // Work - productivity
        ProfileSettings workSettings = new ProfileSettings();
        workSettings.blockTrackers = true;
        workSettings.blockAds = true;
        workSettings.forceHttps = true;
        workSettings.javascriptEnabled = true;
        workSettings.blockedSites = new HashSet<>(Arrays.asList(WORK_BLOCKED));
        profileSettings.put(ProfileType.WORK, workSettings);

        // Coding - developer friendly
        ProfileSettings codingSettings = new ProfileSettings();
        codingSettings.blockTrackers = false; // May break dev tools
        codingSettings.blockAds = true;
        codingSettings.forceHttps = false; // Allow localhost HTTP
        codingSettings.javascriptEnabled = true;
        codingSettings.blockedSites = new HashSet<>();
        codingSettings.allowedSites = new HashSet<>(Arrays.asList(CODING_ALLOWED));
        codingSettings.developerMode = true;
        profileSettings.put(ProfileType.CODING, codingSettings);

        // Secure - maximum privacy
        ProfileSettings secureSettings = new ProfileSettings();
        secureSettings.blockTrackers = true;
        secureSettings.blockAds = true;
        secureSettings.forceHttps = true;
        secureSettings.httpsOnly = true;
        secureSettings.javascriptEnabled = false; // Disable JS for security
        secureSettings.blockReferrer = true;
        secureSettings.blockedSites = new HashSet<>();
        profileSettings.put(ProfileType.SECURE, secureSettings);
    }

    public void switchProfile(ProfileType profile) {
        this.currentProfile = profile;
        applyProfileSettings();
        saveCurrentProfile();
        System.out.println("🦐 Switched to profile: " + profile.getDisplayName());
    }

    private void applyProfileSettings() {
        ProfileSettings settings = profileSettings.get(currentProfile);
        AdvancedSecurityManager security = AdvancedSecurityManager.getInstance();

        security.setBlockTrackers(settings.blockTrackers);
        security.setBlockAds(settings.blockAds);
        security.setJavascriptEnabled(settings.javascriptEnabled);
        security.setHttpsOnly(settings.httpsOnly);
        security.setBlockReferrer(settings.blockReferrer);

        SecurityManager.getInstance().setForceHttps(settings.forceHttps);
    }

    public ProfileType getCurrentProfile() {
        return currentProfile;
    }

    public ProfileSettings getCurrentSettings() {
        return profileSettings.get(currentProfile);
    }

    public boolean shouldBlockSite(String url) {
        if (url == null)
            return false;
        String lowerUrl = url.toLowerCase();

        ProfileSettings settings = getCurrentSettings();

        // Check if explicitly allowed (coding mode)
        if (settings.allowedSites != null) {
            for (String allowed : settings.allowedSites) {
                if (lowerUrl.contains(allowed)) {
                    return false;
                }
            }
        }

        // Check blocked sites for this profile
        if (settings.blockedSites != null) {
            for (String blocked : settings.blockedSites) {
                if (lowerUrl.contains(blocked)) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getBlockMessage() {
        switch (currentProfile) {
            case GAMING:
                return "🎮 Gaming Mode: This site is blocked to minimize distractions.\nFocus on your game!";
            case WORK:
                return "💼 Work Mode: This site is blocked for productivity.\nGet back to work!";
            case SECURE:
                return "🔒 Secure Mode: This site is blocked for security reasons.";
            default:
                return "This site is blocked by your current profile.";
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

    private void loadCurrentProfile() {
        try {
            if (Files.exists(settingsFile)) {
                String profileName = Files.readString(settingsFile).trim();
                currentProfile = ProfileType.valueOf(profileName);
                applyProfileSettings();
            }
        } catch (Exception e) {
            currentProfile = ProfileType.DEFAULT;
        }
    }

    private void saveCurrentProfile() {
        try {
            Files.writeString(settingsFile, currentProfile.name());
        } catch (IOException e) {
            System.err.println("Failed to save profile: " + e.getMessage());
        }
    }

    /**
     * Profile-specific settings
     */
    public static class ProfileSettings {
        public boolean blockTrackers = true;
        public boolean blockAds = true;
        public boolean forceHttps = true;
        public boolean httpsOnly = false;
        public boolean javascriptEnabled = true;
        public boolean blockReferrer = false;
        public boolean performanceMode = false;
        public boolean developerMode = false;
        public Set<String> blockedSites;
        public Set<String> allowedSites;
    }
}
