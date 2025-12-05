package com.krillbrowser;

import java.util.*;
import java.util.regex.*;

/**
 * PhishingDetector - Detects phishing attempts and suspicious URLs
 * 
 * Checks for:
 * - Known phishing domains
 * - Lookalike domain attacks (paypa1.com instead of paypal.com)
 * - Suspicious URL patterns
 * - Homoglyph attacks (using similar-looking characters)
 */
public class PhishingDetector {

    private static PhishingDetector instance;

    // Known legitimate domains to protect
    private static final Map<String, String[]> PROTECTED_BRANDS = new HashMap<>();
    static {
        PROTECTED_BRANDS.put("paypal", new String[] { "paypal.com", "paypal.me" });
        PROTECTED_BRANDS.put("google", new String[] { "google.com", "gmail.com", "accounts.google.com" });
        PROTECTED_BRANDS.put("apple", new String[] { "apple.com", "icloud.com", "appleid.apple.com" });
        PROTECTED_BRANDS.put("amazon", new String[] { "amazon.com", "aws.amazon.com" });
        PROTECTED_BRANDS.put("microsoft", new String[] { "microsoft.com", "live.com", "outlook.com" });
        PROTECTED_BRANDS.put("facebook", new String[] { "facebook.com", "fb.com", "meta.com" });
        PROTECTED_BRANDS.put("netflix", new String[] { "netflix.com" });
        PROTECTED_BRANDS.put("bank", new String[] { "chase.com", "bankofamerica.com", "wellsfargo.com", "citi.com" });
    }

    // Known phishing domains (sample - would be updated regularly in production)
    private Set<String> knownPhishingDomains;

    // Suspicious patterns
    private static final String[] SUSPICIOUS_PATTERNS = {
            "login.*verify",
            "account.*suspended",
            "update.*payment",
            "secure.*login",
            "verify.*identity",
            "confirm.*account"
    };

    // Lookalike character substitutions
    private static final Map<Character, String[]> LOOKALIKES = new HashMap<>();
    static {
        LOOKALIKES.put('a', new String[] { "4", "@", "α" });
        LOOKALIKES.put('e', new String[] { "3", "€" });
        LOOKALIKES.put('i', new String[] { "1", "!", "l", "|" });
        LOOKALIKES.put('o', new String[] { "0" });
        LOOKALIKES.put('s', new String[] { "5", "$" });
        LOOKALIKES.put('l', new String[] { "1", "|", "i" });
    }

    private PhishingDetector() {
        knownPhishingDomains = new HashSet<>();
        loadPhishingDatabase();
    }

    public static synchronized PhishingDetector getInstance() {
        if (instance == null) {
            instance = new PhishingDetector();
        }
        return instance;
    }

    private void loadPhishingDatabase() {
        // Common phishing domain patterns
        String[] phishingDomains = {
                "paypa1.com", "paypal-verify.com", "paypal-secure.net",
                "g00gle.com", "google-login.net", "accounts-google.com",
                "app1e.com", "apple-id-verify.com", "icloud-secure.net",
                "amaz0n.com", "amazon-order.net", "amazon-secure.com",
                "faceb00k.com", "facebook-login.net", "fb-verify.com",
                "netf1ix.com", "netflix-update.com",
                "micros0ft.com", "microsoft-verify.net",
                "chasebank-verify.com", "bankofamerica-secure.net",
                "secure-login-verify.com", "account-update-required.net",
                "verify-your-account.com", "payment-update.net"
        };
        Collections.addAll(knownPhishingDomains, phishingDomains);
    }

    /**
     * Check if a URL is potentially phishing
     */
    public PhishingResult checkUrl(String url) {
        if (url == null)
            return new PhishingResult(false, null, 0);

        String domain = extractDomain(url);
        String lowerDomain = domain.toLowerCase();
        String lowerUrl = url.toLowerCase();

        // Check 1: Known phishing domains
        if (knownPhishingDomains.contains(lowerDomain)) {
            return new PhishingResult(true, "Known phishing domain", 100);
        }

        // Check 2: Lookalike domain detection
        for (Map.Entry<String, String[]> brand : PROTECTED_BRANDS.entrySet()) {
            String brandName = brand.getKey();
            String[] legitimateDomains = brand.getValue();

            // Check if domain contains brand name but isn't the real domain
            if (lowerDomain.contains(brandName)) {
                boolean isLegitimate = false;
                for (String legit : legitimateDomains) {
                    if (lowerDomain.equals(legit) || lowerDomain.endsWith("." + legit)) {
                        isLegitimate = true;
                        break;
                    }
                }
                if (!isLegitimate) {
                    return new PhishingResult(true,
                            "Suspicious " + brandName + " lookalike domain", 85);
                }
            }

            // Check for number substitutions (paypa1 instead of paypal)
            if (containsLookalike(lowerDomain, brandName)) {
                return new PhishingResult(true,
                        "Possible " + brandName + " impersonation (character substitution)", 90);
            }
        }

        // Check 3: Suspicious URL patterns
        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (Pattern.compile(pattern).matcher(lowerUrl).find()) {
                return new PhishingResult(true,
                        "Suspicious URL pattern detected", 70);
            }
        }

        // Check 4: Too many subdomains (common phishing tactic)
        long subdomainCount = lowerDomain.chars().filter(ch -> ch == '.').count();
        if (subdomainCount > 3) {
            return new PhishingResult(true,
                    "Unusually complex domain structure", 60);
        }

        // Check 5: IP address in URL (often phishing)
        if (Pattern.matches(".*\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*", url)) {
            return new PhishingResult(true,
                    "URL contains IP address (suspicious)", 75);
        }

        return new PhishingResult(false, null, 0);
    }

    private boolean containsLookalike(String domain, String brand) {
        // Check if domain looks like brand with character substitutions
        for (int i = 0; i < brand.length(); i++) {
            char c = brand.charAt(i);
            if (LOOKALIKES.containsKey(c)) {
                for (String substitute : LOOKALIKES.get(c)) {
                    String fakeVersion = brand.substring(0, i) + substitute + brand.substring(i + 1);
                    if (domain.contains(fakeVersion)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String extractDomain(String url) {
        try {
            String domain = url.replaceFirst("^(https?://)?", "");
            domain = domain.split("/")[0];
            domain = domain.split("\\?")[0];
            return domain;
        } catch (Exception e) {
            return url;
        }
    }

    public void addPhishingDomain(String domain) {
        knownPhishingDomains.add(domain.toLowerCase());
    }

    /**
     * Result of phishing check
     */
    public static class PhishingResult {
        public final boolean isPhishing;
        public final String reason;
        public final int confidence; // 0-100

        public PhishingResult(boolean isPhishing, String reason, int confidence) {
            this.isPhishing = isPhishing;
            this.reason = reason;
            this.confidence = confidence;
        }
    }
}
