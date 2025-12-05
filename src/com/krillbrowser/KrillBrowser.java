package com.krillbrowser;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Krill Browser - A simple web browser built with JavaFX
 * 
 * Main entry point for the browser application.
 * This class sets up the primary window and initializes all browser components.
 */
public class KrillBrowser extends Application {

    private TabPane tabPane;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Create the main layout
        BorderPane root = new BorderPane();

        // Create tab pane for multiple tabs
        tabPane = new TabPane();

        // Create initial tab
        createNewTab("https://www.google.com");

        // Create menu bar
        MenuBar menuBar = createMenuBar();

        // Set up the layout
        root.setTop(menuBar);
        root.setCenter(tabPane);

        // Create and configure the scene
        Scene scene = new Scene(root, 1200, 800);

        // Apply CSS styling (optional - browser works without it)
        try {
            java.net.URL cssUrl = getClass().getResource("/styles/browser.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            System.out.println("CSS not found, using default styling");
        }

        primaryStage.setTitle("Krill Browser 🦐");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Creates a new browser tab with the specified URL
     */
    public BrowserTab createNewTab(String url) {
        BrowserTab browserTab = new BrowserTab(url, this);
        tabPane.getTabs().add(browserTab.getTab());
        tabPane.getSelectionModel().select(browserTab.getTab());
        return browserTab;
    }

    /**
     * Creates the application menu bar
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem newTabItem = new MenuItem("New Tab");
        newTabItem.setOnAction(e -> createNewTab("https://www.google.com"));
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> primaryStage.close());
        fileMenu.getItems().addAll(newTabItem, new SeparatorMenuItem(), exitItem);

        // Bookmarks Menu
        Menu bookmarksMenu = new Menu("Bookmarks");
        MenuItem viewBookmarksItem = new MenuItem("View Bookmarks");
        viewBookmarksItem.setOnAction(e -> showBookmarksDialog());
        bookmarksMenu.getItems().add(viewBookmarksItem);

        // History Menu
        Menu historyMenu = new Menu("History");
        MenuItem viewHistoryItem = new MenuItem("View History");
        viewHistoryItem.setOnAction(e -> showHistoryDialog());
        MenuItem clearHistoryItem = new MenuItem("Clear History");
        clearHistoryItem.setOnAction(e -> HistoryManager.getInstance().clearHistory());
        historyMenu.getItems().addAll(viewHistoryItem, clearHistoryItem);

        // Tools Menu
        Menu toolsMenu = new Menu("Tools");
        MenuItem cookiesItem = new MenuItem("Manage Cookies");
        cookiesItem.setOnAction(e -> showCookiesDialog());
        toolsMenu.getItems().add(cookiesItem);

        // Security Menu - COMPREHENSIVE
        Menu securityMenu = new Menu("🛡️ Security");
        AdvancedSecurityManager advSecurity = AdvancedSecurityManager.getInstance();

        // Tracker/Ad Blocking
        CheckMenuItem blockTrackersItem = new CheckMenuItem("Block Trackers");
        blockTrackersItem.setSelected(advSecurity.isBlockTrackers());
        blockTrackersItem.setOnAction(e -> advSecurity.setBlockTrackers(blockTrackersItem.isSelected()));

        CheckMenuItem blockAdsItem = new CheckMenuItem("Block Ads");
        blockAdsItem.setSelected(advSecurity.isBlockAds());
        blockAdsItem.setOnAction(e -> advSecurity.setBlockAds(blockAdsItem.isSelected()));

        // HTTPS Options
        CheckMenuItem forceHttpsItem = new CheckMenuItem("Upgrade to HTTPS");
        forceHttpsItem.setSelected(SecurityManager.getInstance().isForceHttps());
        forceHttpsItem.setOnAction(e -> SecurityManager.getInstance().setForceHttps(forceHttpsItem.isSelected()));

        CheckMenuItem httpsOnlyItem = new CheckMenuItem("HTTPS-Only Mode (Block HTTP)");
        httpsOnlyItem.setSelected(advSecurity.isHttpsOnly());
        httpsOnlyItem.setOnAction(e -> advSecurity.setHttpsOnly(httpsOnlyItem.isSelected()));

        // Privacy Options
        CheckMenuItem privateModeItem = new CheckMenuItem("🕵️ Private Mode");
        privateModeItem.setSelected(SecurityManager.getInstance().isPrivateMode());
        privateModeItem.setOnAction(e -> {
            SecurityManager.getInstance().setPrivateMode(privateModeItem.isSelected());
            if (privateModeItem.isSelected()) {
                primaryStage.setTitle("Krill Browser 🦐 [Private Mode]");
            } else {
                primaryStage.setTitle("Krill Browser 🦐");
            }
        });

        CheckMenuItem blockReferrerItem = new CheckMenuItem("Block Referrer Headers");
        blockReferrerItem.setSelected(advSecurity.isBlockReferrer());
        blockReferrerItem.setOnAction(e -> advSecurity.setBlockReferrer(blockReferrerItem.isSelected()));

        CheckMenuItem doNotTrackItem = new CheckMenuItem("Send Do Not Track");
        doNotTrackItem.setSelected(advSecurity.isSendDoNotTrack());
        doNotTrackItem.setOnAction(e -> advSecurity.setSendDoNotTrack(doNotTrackItem.isSelected()));

        // JavaScript Toggle
        CheckMenuItem javascriptItem = new CheckMenuItem("Enable JavaScript");
        javascriptItem.setSelected(advSecurity.isJavascriptEnabled());
        javascriptItem.setOnAction(e -> advSecurity.setJavascriptEnabled(javascriptItem.isSelected()));

        // Auto-clear
        CheckMenuItem clearOnExitItem = new CheckMenuItem("Clear Data on Exit");
        clearOnExitItem.setSelected(advSecurity.isClearOnExit());
        clearOnExitItem.setOnAction(e -> advSecurity.setClearOnExit(clearOnExitItem.isSelected()));

        // View blocked sites
        MenuItem blockedSitesItem = new MenuItem("View Blocked Sites (" + advSecurity.getBlockedDomainsCount() + ")");
        blockedSitesItem.setOnAction(e -> showBlockedSitesDialog());

        // Clear data
        MenuItem clearAllDataItem = new MenuItem("Clear All Browsing Data");
        clearAllDataItem.setOnAction(e -> clearAllBrowsingData());

        // PANIC BUTTON
        MenuItem panicButton = new MenuItem("🚨 PANIC - Clear Everything NOW!");
        panicButton.setOnAction(e -> {
            advSecurity.panicClear();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Data Cleared");
            alert.setHeaderText("🚨 Panic Clear Complete");
            alert.setContentText("All cookies, history, and bookmarks have been deleted.");
            alert.showAndWait();
        });

        securityMenu.getItems().addAll(
                blockTrackersItem, blockAdsItem,
                new SeparatorMenuItem(),
                forceHttpsItem, httpsOnlyItem,
                new SeparatorMenuItem(),
                privateModeItem, blockReferrerItem, doNotTrackItem,
                new SeparatorMenuItem(),
                javascriptItem, clearOnExitItem,
                new SeparatorMenuItem(),
                blockedSitesItem, clearAllDataItem,
                new SeparatorMenuItem(),
                panicButton);
        // Profiles Menu - UNIQUE FEATURE
        Menu profilesMenu = new Menu("👤 Profiles");
        BrowserProfile profiles = BrowserProfile.getInstance();

        ToggleGroup profileGroup = new ToggleGroup();

        for (BrowserProfile.ProfileType profileType : BrowserProfile.ProfileType.values()) {
            RadioMenuItem profileItem = new RadioMenuItem(profileType.getDisplayName());
            profileItem.setToggleGroup(profileGroup);
            profileItem.setSelected(profiles.getCurrentProfile() == profileType);

            final BrowserProfile.ProfileType selectedProfile = profileType;
            profileItem.setOnAction(e -> {
                profiles.switchProfile(selectedProfile);
                updateTitleForProfile(selectedProfile);
                showProfileSwitchNotification(selectedProfile);
            });

            profilesMenu.getItems().add(profileItem);
        }

        profilesMenu.getItems().add(new SeparatorMenuItem());
        MenuItem profileInfoItem = new MenuItem("About Profiles...");
        profileInfoItem.setOnAction(e -> showProfileInfoDialog());
        profilesMenu.getItems().add(profileInfoItem);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About Krill Browser");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, bookmarksMenu, historyMenu, toolsMenu, securityMenu, profilesMenu,
                helpMenu);
        return menuBar;
    }

    private void showBookmarksDialog() {
        BookmarkManager bookmarkManager = BookmarkManager.getInstance();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Bookmarks");
        dialog.setHeaderText("Your Bookmarks");

        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(bookmarkManager.getBookmarks());
        listView.setPrefSize(400, 300);

        // Double-click to open bookmark
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedUrl = listView.getSelectionModel().getSelectedItem();
                if (selectedUrl != null) {
                    createNewTab(selectedUrl);
                    dialog.close();
                }
            }
        });

        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showHistoryDialog() {
        HistoryManager historyManager = HistoryManager.getInstance();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("History");
        dialog.setHeaderText("Browsing History");

        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(historyManager.getHistory());
        listView.setPrefSize(500, 400);

        // Double-click to open history item
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedEntry = listView.getSelectionModel().getSelectedItem();
                if (selectedEntry != null) {
                    // Extract URL from history entry (format: "timestamp - url")
                    String url = selectedEntry.substring(selectedEntry.indexOf(" - ") + 3);
                    createNewTab(url);
                    dialog.close();
                }
            }
        });

        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showCookiesDialog() {
        CookieManager cookieManager = CookieManager.getInstance();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Cookies");
        dialog.setHeaderText("Stored Cookies");

        VBox content = new VBox(10);

        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(cookieManager.getAllCookieInfo());
        listView.setPrefSize(500, 300);

        Button clearButton = new Button("Clear All Cookies");
        clearButton.setOnAction(e -> {
            cookieManager.clearAllCookies();
            listView.getItems().clear();
        });

        content.getChildren().addAll(listView, clearButton);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Krill Browser");
        alert.setHeaderText("Krill Browser 🦐");
        alert.setContentText(
                "Version 1.0\n\n" +
                        "A simple web browser built with Java and JavaFX.\n\n" +
                        "Created as a learning project for Java and AP CSA preparation.\n\n" +
                        "Features:\n" +
                        "• Tabbed browsing\n" +
                        "• Bookmarks\n" +
                        "• History tracking\n" +
                        "• Cookie management\n" +
                        "• Security features\n" +
                        "• HTTPS enforcement");
        alert.showAndWait();
    }

    private void showBlockedSitesDialog() {
        SecurityManager security = SecurityManager.getInstance();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Blocked Sites");
        dialog.setHeaderText("Sites blocked for your safety");

        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(security.getBlockedDomains());
        listView.setPrefSize(400, 300);

        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void clearAllBrowsingData() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Browsing Data");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText(
                "This will delete:\n• All cookies\n• All history\n• All bookmarks\n\nThis cannot be undone!");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                HistoryManager.getInstance().clearHistory();
                BookmarkManager.getInstance().clearBookmarks();
                CookieManager.getInstance().clearAllCookies();

                Alert done = new Alert(Alert.AlertType.INFORMATION);
                done.setTitle("Data Cleared");
                done.setHeaderText("All browsing data has been cleared");
                done.showAndWait();
            }
        });
    }

    private void updateTitleForProfile(BrowserProfile.ProfileType profile) {
        String title = "Krill Browser 🦐";
        switch (profile) {
            case GAMING:
                title += " [🎮 Gaming Mode]";
                break;
            case WORK:
                title += " [💼 Work Mode]";
                break;
            case CODING:
                title += " [💻 Coding Mode]";
                break;
            case SECURE:
                title += " [🔒 Secure Mode]";
                break;
            default:
                break;
        }
        primaryStage.setTitle(title);
    }

    private void showProfileSwitchNotification(BrowserProfile.ProfileType profile) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Profile Switched");
        alert.setHeaderText(profile.getDisplayName());
        alert.setContentText(profile.getDescription());
        alert.showAndWait();
    }

    private void showProfileInfoDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Profiles");
        alert.setHeaderText("Krill Browser Profiles 🦐");
        alert.setContentText(
                "🦐 Default - Balanced browsing experience\n\n" +
                        "🎮 Gaming - Blocks social media & distractions\n\n" +
                        "💼 Work - Blocks entertainment sites\n\n" +
                        "💻 Coding - Developer mode, allows localhost\n\n" +
                        "🔒 Secure - Maximum privacy, disables JavaScript");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
