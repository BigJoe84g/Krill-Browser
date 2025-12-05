package com.krillbrowser;

import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Represents a single browser tab with its own WebView and navigation controls.
 */
public class BrowserTab {

    private Tab tab;
    private WebView webView;
    private WebEngine webEngine;
    private TextField urlField;
    private Button backButton;
    private Button forwardButton;
    private Button reloadButton;
    private Button bookmarkButton;
    private Label securityIndicator;
    private KrillBrowser browser;

    public BrowserTab(String url, KrillBrowser browser) {
        this.browser = browser;

        // Create the tab
        tab = new Tab("New Tab");
        tab.setClosable(true);

        // Create WebView with performance optimizations
        webView = new WebView();
        webEngine = webView.getEngine();

        // === PERFORMANCE OPTIMIZATIONS ===

        // Enable caching for faster page loads
        webEngine.setUserAgent("Krill/1.0 (compatible; MSIE 11.0; Windows NT 10.0)");

        // Disable context menu for faster response
        webView.setContextMenuEnabled(false);

        // Reduce memory by limiting font smoothing
        webView.setFontSmoothingType(javafx.scene.text.FontSmoothingType.LCD);

        // Set cache to be more aggressive
        webView.setCache(true);

        // Optimize zoom for performance
        webView.setZoom(1.0);

        // Enable JavaScript (needed for most sites)
        webEngine.setJavaScriptEnabled(true);

        // Create navigation bar
        HBox navigationBar = createNavigationBar();

        // Create layout
        BorderPane tabContent = new BorderPane();
        tabContent.setTop(navigationBar);
        tabContent.setCenter(webView);

        tab.setContent(tabContent);

        // Set up listeners
        setupListeners();

        // Load initial URL
        loadUrl(url);
    }

    private HBox createNavigationBar() {
        HBox navBar = new HBox(5);
        navBar.setPadding(new Insets(5));
        navBar.getStyleClass().add("navigation-bar");

        // Back button
        backButton = new Button("◀");
        backButton.setTooltip(new Tooltip("Go Back"));
        backButton.setOnAction(e -> goBack());
        backButton.setDisable(true);

        // Forward button
        forwardButton = new Button("▶");
        forwardButton.setTooltip(new Tooltip("Go Forward"));
        forwardButton.setOnAction(e -> goForward());
        forwardButton.setDisable(true);

        // Reload button
        reloadButton = new Button("⟳");
        reloadButton.setTooltip(new Tooltip("Reload"));
        reloadButton.setOnAction(e -> reload());

        // Security indicator
        securityIndicator = new Label("🔓");
        securityIndicator.setTooltip(new Tooltip("Connection not secure"));
        securityIndicator.getStyleClass().add("security-indicator");

        // URL field
        urlField = new TextField();
        urlField.setPromptText("Enter URL...");
        urlField.getStyleClass().add("url-field");
        HBox.setHgrow(urlField, Priority.ALWAYS);

        // Handle Enter key to navigate
        urlField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String url = urlField.getText().trim();
                if (!url.isEmpty()) {
                    loadUrl(url);
                }
            }
        });

        // Bookmark button
        bookmarkButton = new Button("☆");
        bookmarkButton.setTooltip(new Tooltip("Add Bookmark"));
        bookmarkButton.setOnAction(e -> toggleBookmark());

        // New tab button
        Button newTabButton = new Button("+");
        newTabButton.setTooltip(new Tooltip("New Tab"));
        newTabButton.setOnAction(e -> browser.createNewTab("https://www.google.com"));

        navBar.getChildren().addAll(
                backButton, forwardButton, reloadButton,
                securityIndicator, urlField,
                bookmarkButton, newTabButton);

        return navBar;
    }

    private void setupListeners() {
        // Listen for page load state changes
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                reloadButton.setText("✕");
                reloadButton.setTooltip(new Tooltip("Stop"));
            } else {
                reloadButton.setText("⟳");
                reloadButton.setTooltip(new Tooltip("Reload"));
            }

            if (newState == Worker.State.SUCCEEDED) {
                // Update URL field
                String currentUrl = webEngine.getLocation();
                urlField.setText(currentUrl);

                // Update tab title
                String title = webEngine.getTitle();
                if (title != null && !title.isEmpty()) {
                    // Truncate long titles
                    if (title.length() > 20) {
                        title = title.substring(0, 17) + "...";
                    }
                    tab.setText(title);
                }

                // Update security indicator
                updateSecurityIndicator(currentUrl);

                // Update navigation buttons
                updateNavigationButtons();

                // Add to history
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    HistoryManager.getInstance().addToHistory(currentUrl);
                }

                // Update bookmark button
                updateBookmarkButton(currentUrl);
            }
        });

        // Handle errors
        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldEx, newEx) -> {
            if (newEx != null) {
                showError("Failed to load page: " + newEx.getMessage());
            }
        });
    }

    private void updateSecurityIndicator(String url) {
        if (url != null && url.startsWith("https://")) {
            securityIndicator.setText("🔒");
            securityIndicator.setTooltip(new Tooltip("Secure connection (HTTPS)"));
            securityIndicator.getStyleClass().removeAll("insecure");
            securityIndicator.getStyleClass().add("secure");
        } else {
            securityIndicator.setText("🔓");
            securityIndicator.setTooltip(new Tooltip("Connection not secure"));
            securityIndicator.getStyleClass().removeAll("secure");
            securityIndicator.getStyleClass().add("insecure");
        }
    }

    private void updateNavigationButtons() {
        backButton.setDisable(webEngine.getHistory().getCurrentIndex() <= 0);
        forwardButton.setDisable(
                webEngine.getHistory().getCurrentIndex() >= webEngine.getHistory().getEntries().size() - 1);
    }

    private void updateBookmarkButton(String url) {
        if (BookmarkManager.getInstance().isBookmarked(url)) {
            bookmarkButton.setText("★");
            bookmarkButton.setTooltip(new Tooltip("Remove Bookmark"));
        } else {
            bookmarkButton.setText("☆");
            bookmarkButton.setTooltip(new Tooltip("Add Bookmark"));
        }
    }

    private void toggleBookmark() {
        String currentUrl = webEngine.getLocation();
        if (currentUrl != null && !currentUrl.isEmpty()) {
            BookmarkManager bookmarkManager = BookmarkManager.getInstance();
            if (bookmarkManager.isBookmarked(currentUrl)) {
                bookmarkManager.removeBookmark(currentUrl);
                bookmarkButton.setText("☆");
                bookmarkButton.setTooltip(new Tooltip("Add Bookmark"));
            } else {
                bookmarkManager.addBookmark(currentUrl);
                bookmarkButton.setText("★");
                bookmarkButton.setTooltip(new Tooltip("Remove Bookmark"));
            }
        }
    }

    /**
     * Loads the given URL in the WebView with comprehensive security checks
     */
    public void loadUrl(String url) {
        AdvancedSecurityManager advSecurity = AdvancedSecurityManager.getInstance();
        SecurityManager security = SecurityManager.getInstance();

        // Add protocol if missing
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // Check if it looks like a URL or a search query
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                // Use DuckDuckGo for privacy-focused search
                url = "https://duckduckgo.com/?q=" + url.replace(" ", "+");
            }
        }

        // HTTPS-only mode check
        if (advSecurity.isHttpsOnly() && url.startsWith("http://")) {
            showSecurityWarning(
                    "🔒 HTTPS-Only Mode\n\nThis site uses insecure HTTP. Connection blocked.\n\nURL: " + url);
            return;
        }

        // Upgrade HTTP to HTTPS
        url = advSecurity.upgradeToHttps(url);

        // Clean tracking parameters from URL
        url = advSecurity.cleanUrl(url);

        // Check profile-based site blocking (Gaming blocks social, Work blocks
        // entertainment)
        BrowserProfile profile = BrowserProfile.getInstance();
        if (profile.shouldBlockSite(url)) {
            showProfileWarning(profile.getBlockMessage());
            return;
        }

        // Block trackers/malware
        if (advSecurity.shouldBlockUrl(url) || security.shouldBlockUrl(url)) {
            showSecurityWarning("🛡️ Blocked!\n\nThis site contains trackers or malware.\n\nURL: " + url);
            return;
        }

        // Apply JavaScript setting
        webEngine.setJavaScriptEnabled(advSecurity.isJavascriptEnabled());

        // Update UI
        if (!security.isPrivateMode()) {
            urlField.setText(url);
            // Add to history only if not in private mode
            HistoryManager.getInstance().addToHistory(url);
        } else {
            urlField.setText("🕵️ " + url);
        }

        webEngine.load(url);
    }

    private void showSecurityWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Security Warning");
        alert.setHeaderText("Krill Browser Security");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showProfileWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Site Blocked by Profile");
        alert.setHeaderText("Blocked by Current Profile");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void goBack() {
        if (webEngine.getHistory().getCurrentIndex() > 0) {
            webEngine.getHistory().go(-1);
        }
    }

    public void goForward() {
        if (webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1) {
            webEngine.getHistory().go(1);
        }
    }

    public void reload() {
        if (webEngine.getLoadWorker().isRunning()) {
            webEngine.getLoadWorker().cancel();
        } else {
            webEngine.reload();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Navigation Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Tab getTab() {
        return tab;
    }

    public WebEngine getWebEngine() {
        return webEngine;
    }
}
