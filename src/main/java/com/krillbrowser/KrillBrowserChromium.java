package com.krillbrowser;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Krill Browser v2.0 - Chromium-Powered Edition
 * 
 * Uses JCEF (Java Chromium Embedded Framework) for Chrome-level
 * performance and compatibility.
 */
public class KrillBrowserChromium extends JFrame {

    private static CefApp cefApp;
    private CefClient cefClient;
    private JTabbedPane tabbedPane;
    private List<CefBrowser> browsers = new ArrayList<>();
    private JTextField urlBar;
    private JLabel statusBar;
    private BrowserProfile.ProfileType currentProfile = BrowserProfile.ProfileType.DEFAULT;

    public static void main(String[] args) {
        // Initialize CEF on the main thread
        SwingUtilities.invokeLater(() -> {
            try {
                new KrillBrowserChromium().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to start Krill Browser:\n" + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public KrillBrowserChromium()
            throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
        super("Krill Browser 🦐 [Chromium Edition]");

        // Initialize JCEF
        initializeCef();

        // Setup UI
        setupUI();

        // Create initial tab
        createNewTab("https://duckduckgo.com");

        // Window settings
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // macOS window visibility fix
        setAlwaysOnTop(true);
        toFront();
        requestFocus();
        setAlwaysOnTop(false); // Reset after bringing to front

        // Cleanup on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Clear data on exit if enabled
                if (AdvancedSecurityManager.getInstance().isClearOnExit()) {
                    AdvancedSecurityManager.getInstance().panicClear();
                }

                // Dispose CEF
                for (CefBrowser browser : browsers) {
                    browser.close(true);
                }
                cefClient.dispose();
                cefApp.dispose();
            }
        });
    }

    private void initializeCef()
            throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
        // Build JCEF app (downloads native libs on first run)
        CefAppBuilder builder = new CefAppBuilder();

        // Configure settings
        CefSettings settings = builder.getCefSettings();
        settings.windowless_rendering_enabled = false;
        settings.cache_path = System.getProperty("user.home") + "/.krillbrowser/cache";

        // Security: Disable remote debugging in production
        settings.remote_debugging_port = 0;

        // Build and get app
        builder.setInstallDir(new java.io.File(System.getProperty("user.home") + "/.krillbrowser/jcef"));

        // Show progress during first-time download
        builder.setProgressHandler((stage, percent) -> {
            System.out.println("JCEF: " + stage + " - " + percent + "%");
        });

        cefApp = builder.build();
        cefClient = cefApp.createClient();

        // Setup display handler for URL updates
        cefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                SwingUtilities.invokeLater(() -> {
                    urlBar.setText(url);
                    updateSecurityIndicator(url);
                });
            }

            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                SwingUtilities.invokeLater(() -> {
                    int index = getBrowserTabIndex(browser);
                    if (index >= 0) {
                        String shortTitle = title.length() > 25 ? title.substring(0, 22) + "..." : title;
                        tabbedPane.setTitleAt(index, shortTitle);
                    }
                });
            }
        });

        // Setup load handler
        cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack,
                    boolean canGoForward) {
                SwingUtilities.invokeLater(() -> {
                    statusBar.setText(isLoading ? "Loading..." : "Ready");
                });
            }
        });
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Create menu bar
        setJMenuBar(createMenuBar());

        // Navigation toolbar
        JToolBar toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);

        // Tabbed pane for browser tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index >= 0 && index < browsers.size()) {
                urlBar.setText(browsers.get(index).getURL());
            }
        });
        add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        statusBar = new JLabel("Ready");
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        add(statusBar, BorderLayout.SOUTH);
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // Back button
        JButton backBtn = new JButton("◀");
        backBtn.setToolTipText("Back");
        backBtn.addActionListener(e -> getCurrentBrowser().goBack());
        toolbar.add(backBtn);

        // Forward button
        JButton fwdBtn = new JButton("▶");
        fwdBtn.setToolTipText("Forward");
        fwdBtn.addActionListener(e -> getCurrentBrowser().goForward());
        toolbar.add(fwdBtn);

        // Reload button
        JButton reloadBtn = new JButton("⟳");
        reloadBtn.setToolTipText("Reload");
        reloadBtn.addActionListener(e -> getCurrentBrowser().reload());
        toolbar.add(reloadBtn);

        toolbar.addSeparator();

        // URL bar
        urlBar = new JTextField();
        urlBar.addActionListener(e -> loadUrl(urlBar.getText()));
        toolbar.add(urlBar);

        toolbar.addSeparator();

        // New tab button
        JButton newTabBtn = new JButton("+");
        newTabBtn.setToolTipText("New Tab");
        newTabBtn.addActionListener(e -> createNewTab("https://duckduckgo.com"));
        toolbar.add(newTabBtn);

        return toolbar;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newTab = new JMenuItem("New Tab");
        newTab.addActionListener(e -> createNewTab("https://duckduckgo.com"));
        fileMenu.add(newTab);
        fileMenu.addSeparator();
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());
        fileMenu.add(exit);
        menuBar.add(fileMenu);

        // Security menu
        JMenu securityMenu = new JMenu("🛡️ Security");

        JCheckBoxMenuItem blockTrackers = new JCheckBoxMenuItem("Block Trackers", true);
        blockTrackers.addActionListener(
                e -> AdvancedSecurityManager.getInstance().setBlockTrackers(blockTrackers.isSelected()));
        securityMenu.add(blockTrackers);

        JCheckBoxMenuItem blockAds = new JCheckBoxMenuItem("Block Ads", true);
        blockAds.addActionListener(e -> AdvancedSecurityManager.getInstance().setBlockAds(blockAds.isSelected()));
        securityMenu.add(blockAds);

        securityMenu.addSeparator();

        JMenuItem panicBtn = new JMenuItem("🚨 PANIC - Clear Everything!");
        panicBtn.addActionListener(e -> {
            AdvancedSecurityManager.getInstance().panicClear();
            JOptionPane.showMessageDialog(this, "All data cleared!", "Panic Clear", JOptionPane.INFORMATION_MESSAGE);
        });
        securityMenu.add(panicBtn);

        menuBar.add(securityMenu);

        // Profiles menu
        JMenu profilesMenu = new JMenu("👤 Profiles");
        ButtonGroup profileGroup = new ButtonGroup();

        for (BrowserProfile.ProfileType profile : BrowserProfile.ProfileType.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(profile.getDisplayName());
            item.setSelected(profile == currentProfile);
            item.addActionListener(e -> switchProfile(profile));
            profileGroup.add(item);
            profilesMenu.add(item);
        }

        menuBar.add(profilesMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> showAbout());
        helpMenu.add(about);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private void createNewTab(String url) {
        CefBrowser browser = cefClient.createBrowser(url, false, false);
        browsers.add(browser);

        Component component = browser.getUIComponent();
        tabbedPane.addTab("New Tab", component);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

        // Add close button to tab
        int index = tabbedPane.getTabCount() - 1;
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("New Tab  ");
        JButton closeBtn = new JButton("×");
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.addActionListener(e -> closeTab(browser));
        tabPanel.add(titleLabel);
        tabPanel.add(closeBtn);
        tabbedPane.setTabComponentAt(index, tabPanel);
    }

    private void closeTab(CefBrowser browser) {
        int index = browsers.indexOf(browser);
        if (index >= 0) {
            browsers.remove(index);
            tabbedPane.remove(index);
            browser.close(false);
        }

        // Don't close last tab, create new one
        if (browsers.isEmpty()) {
            createNewTab("https://duckduckgo.com");
        }
    }

    private void loadUrl(String url) {
        // Add protocol if missing
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                // Search with DuckDuckGo
                url = "https://duckduckgo.com/?q=" + url.replace(" ", "+");
            }
        }

        // Security checks
        AdvancedSecurityManager security = AdvancedSecurityManager.getInstance();
        url = security.cleanUrl(url);

        if (security.shouldBlockUrl(url)) {
            JOptionPane.showMessageDialog(this,
                    "🛡️ Blocked!\n\nThis site contains trackers or malware.",
                    "Security Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Profile-based blocking
        BrowserProfile profile = BrowserProfile.getInstance();
        if (profile.shouldBlockSite(url)) {
            JOptionPane.showMessageDialog(this,
                    profile.getBlockMessage(),
                    "Blocked by Profile", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        getCurrentBrowser().loadURL(url);
        urlBar.setText(url);

        // Add to history
        if (!SecurityManager.getInstance().isPrivateMode()) {
            HistoryManager.getInstance().addToHistory(url);
        }
    }

    private CefBrowser getCurrentBrowser() {
        int index = tabbedPane.getSelectedIndex();
        if (index >= 0 && index < browsers.size()) {
            return browsers.get(index);
        }
        return browsers.get(0);
    }

    private int getBrowserTabIndex(CefBrowser browser) {
        return browsers.indexOf(browser);
    }

    private void updateSecurityIndicator(String url) {
        if (url.startsWith("https://")) {
            statusBar.setText("🔒 Secure | Ready");
        } else if (url.startsWith("http://")) {
            statusBar.setText("🔓 Not Secure | Ready");
        }
    }

    private void switchProfile(BrowserProfile.ProfileType profile) {
        currentProfile = profile;
        BrowserProfile.getInstance().switchProfile(profile);
        setTitle("Krill Browser 🦐 [" + profile.getDisplayName() + "]");
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "Krill Browser v2.0 🦐\n\n" +
                        "Chromium-Powered Edition\n\n" +
                        "Features:\n" +
                        "• Chrome-level speed & compatibility\n" +
                        "• Tracker & ad blocking\n" +
                        "• 5 browsing profiles\n" +
                        "• DuckDuckGo search\n" +
                        "• Privacy-first design\n\n" +
                        "Built for AP CSA learning",
                "About Krill Browser", JOptionPane.INFORMATION_MESSAGE);
    }
}
