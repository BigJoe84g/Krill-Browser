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

    // Custom UI Components
    private JPanel tabContainer;
    private JPanel browserContainer;

    private List<CefBrowser> browsers = new ArrayList<>();
    private List<JButton> tabButtons = new ArrayList<>();

    private JTextField urlBar;
    private JLabel statusBar;
    private BrowserProfile.ProfileType currentProfile = BrowserProfile.ProfileType.DEFAULT;
    private CefBrowser activeBrowser;

    public static void main(String[] args) {
        // Enforce settings BEFORE any Swing classes load
        // This forces the menu bar to be part of the JFrame, not the Mac system bar
        System.setProperty("apple.laf.useScreenMenuBar", "false");

        // Initialize CEF on the main thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Use System L&F for best compatibility, BUT with the screenMenuBar property
                // forced above
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                new KrillBrowserChromium().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public KrillBrowserChromium()
            throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
        super("Krill Browser 🦐 [Chromium Edition]");

        // Enable Lightweight popups:
        // Since we are using JCEF OSR (Off-Screen Rendering), the browser is a
        // lightweight Swing component.
        // Therefore, we should use lightweight popups to ensure they stay in the Java
        // layer and don't
        // fight for native window focus (which causes them to close immediately).
        JPopupMenu.setDefaultLightWeightPopupEnabled(true);

        // Initialize JCEF
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
        // Enable windowless rendering (OSR) to make it a lightweight Swing component.
        settings.windowless_rendering_enabled = true;

        settings.cache_path = System.getProperty("user.home") + "/.krillbrowser/cache";

        // Security: Disable remote debugging in production
        settings.remote_debugging_port = 0;

        // Set modern User Agent
        settings.user_agent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

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
                    if (browser == activeBrowser) {
                        urlBar.setText(url);
                        updateSecurityIndicator(url);
                    }
                });
            }

            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                SwingUtilities.invokeLater(() -> {
                    for (Component comp : tabContainer.getComponents()) {
                        if (comp instanceof JPanel) {
                            JPanel panel = (JPanel) comp;
                            if (panel.getClientProperty("browser") == browser) {
                                // Find JLabel safely
                                for (Component inner : panel.getComponents()) {
                                    if (inner instanceof JLabel) {
                                        String shortTitle = title.length() > 18 ? title.substring(0, 15) + "..."
                                                : title;
                                        ((JLabel) inner).setText(shortTitle);
                                        ((JLabel) inner).setToolTipText(title);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
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
                    if (browser == activeBrowser) {
                        statusBar.setText(isLoading ? "Loading..." : "Ready");
                    }
                });
            }

            // REMOVED Retro CSS Injection - Keeping the web modern!
        });

        // Setup LifeSpan handler to force popups into tabs
        cefClient.addLifeSpanHandler(new org.cef.handler.CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String targetUrl, String targetFrameName) {
                // Return true to cancel the popup creation
                // And manually open it in a new tab instead
                SwingUtilities.invokeLater(() -> createNewTab(targetUrl));
                return true;
            }
        });
    }

    private void setupUI() {
        // --- 🎨 MODERN KRILL THEME 🎨 ---
        // Clean, Flat, Minimalist
        Color bgLight = new Color(245, 246, 250); // Soft white/gray
        Color accentBlue = new Color(33, 150, 243); // Material Blue
        Color borderGray = new Color(220, 220, 220); // Subtle border

        // Modern Font
        Font modernFont = new Font("SansSerif", Font.PLAIN, 13);

        getContentPane().setBackground(bgLight);
        setLayout(new BorderLayout());

        // Create menu bar with theme
        JMenuBar mb = createMenuBar();
        // Flat styling for Menu Bar
        mb.setBackground(Color.WHITE);
        mb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderGray));
        setJMenuBar(mb);

        // Top Container (Toolbar + Tab Bar)
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBackground(bgLight);
        topContainer.setBorder(null); // No outer border for clean look

        // Navigation toolbar
        JToolBar toolbar = createToolbar();
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderGray));

        // Style ALL buttons in the toolbar to look Modern
        for (Component c : toolbar.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setBackground(Color.WHITE);
                b.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8)); // Padding only
                b.setFocusPainted(false);
                b.setFont(modernFont.deriveFont(Font.BOLD, 14));

                // Add simple hover effect
                b.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        b.setBackground(new Color(230, 240, 255));
                    }

                    public void mouseExited(MouseEvent e) {
                        b.setBackground(Color.WHITE);
                    }
                });
            }
        }

        topContainer.add(toolbar, BorderLayout.NORTH);

        // Custom Tab Bar
        tabContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabContainer.setBackground(bgLight);
        tabContainer.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        topContainer.add(tabContainer, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);

        // Browser Container (Center)
        browserContainer = new JPanel(new BorderLayout());
        browserContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, borderGray)); // Separator line
        add(browserContainer, BorderLayout.CENTER);

        // Status bar
        statusBar = new JLabel("Ready");
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusBar.setForeground(Color.GRAY);
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusBar, BorderLayout.SOUTH);
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(false);

        // Back button
        JButton backBtn = new JButton("◀");
        backBtn.setToolTipText("Back");
        backBtn.addActionListener(e -> {
            if (activeBrowser != null)
                activeBrowser.goBack();
        });
        toolbar.add(backBtn);

        // Forward button
        JButton fwdBtn = new JButton("▶");
        fwdBtn.setToolTipText("Forward");
        fwdBtn.addActionListener(e -> {
            if (activeBrowser != null)
                activeBrowser.goForward();
        });
        toolbar.add(fwdBtn);

        // Reload button
        JButton reloadBtn = new JButton("⟳");
        reloadBtn.setToolTipText("Reload");
        reloadBtn.addActionListener(e -> {
            if (activeBrowser != null)
                activeBrowser.reload();
        });
        toolbar.add(reloadBtn);

        toolbar.add(Box.createHorizontalStrut(10));

        // URL bar - Modern Flat
        urlBar = new JTextField();
        // Round border illusion using compound border
        urlBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        urlBar.setBackground(new Color(245, 245, 245));
        urlBar.setFont(new Font("SansSerif", Font.PLAIN, 13));
        urlBar.addActionListener(e -> loadUrl(urlBar.getText()));

        toolbar.add(urlBar);
        toolbar.add(Box.createHorizontalStrut(10));

        return toolbar;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Helper to style menus
        ActionListener styleMenu = e -> {
        };

        // File menu
        JMenu fileMenu = new JMenu("File");
        styleModernMenu(fileMenu);
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
        styleModernMenu(securityMenu);

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
        styleModernMenu(profilesMenu);
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
        styleModernMenu(helpMenu);
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> showAbout());
        helpMenu.add(about);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private void styleModernMenu(JMenu menu) {
        menu.setOpaque(true);
        menu.setBackground(Color.WHITE);
        menu.setFont(new Font("SansSerif", Font.PLAIN, 13));
        menu.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }

    private JButton newTabAddBtn;

    private void createNewTab(String url) {
        // Create browser instance
        CefBrowser browser = cefClient.createBrowser(url, false, false);
        browsers.add(browser);

        // Create tab component panel
        // Use BorderLayout to ensure Close Button is always visible on the right
        JPanel tabPanel = new JPanel(new BorderLayout(5, 0));
        tabPanel.setOpaque(true);
        tabPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 4));
        tabPanel.setPreferredSize(new Dimension(160, 26)); // Set fixed width for consistency

        // Title Label
        JLabel titleLabel = new JLabel("New Tab");
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tabPanel.add(titleLabel, BorderLayout.CENTER);

        // Close Button
        // Modern Style: Simple Clean 'x'
        JButton closeBtn = new JButton("x");
        closeBtn.setMargin(new Insets(0, 0, 0, 0));
        closeBtn.setPreferredSize(new Dimension(30, 30)); // Increased to 30x30 to almost guarantee fit
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setOpaque(false);
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 16)); // Slightly smaller bold font to fit better
        closeBtn.setForeground(Color.GRAY);

        closeBtn.putClientProperty("close", true);
        closeBtn.addActionListener(e -> closeTab(browser));

        // Hover effect for clearer interaction
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                e.consume();
            }

            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(new Color(255, 80, 80)); // Red hover
            }

            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(Color.GRAY);
            }
        });

        // Wrap in a panel to protect size in BorderLayout.EAST
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(3, 4, 1, 0));
        btnPanel.add(closeBtn);

        tabPanel.add(btnPanel, BorderLayout.EAST);

        // Make the whole panel clickable to switch tabs
        MouseAdapter selectTabListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    closeTab(browser);
                } else {
                    switchTab(browser);
                }
            }
        };
        tabPanel.addMouseListener(selectTabListener);
        titleLabel.addMouseListener(selectTabListener);

        // Add to main UI
        // If "New Tab" button exists, add before it. Otherwise add at end.
        if (newTabAddBtn != null && newTabAddBtn.getParent() == tabContainer) {
            tabContainer.remove(newTabAddBtn);
            tabContainer.add(tabPanel);
            tabContainer.add(newTabAddBtn);
        } else {
            tabContainer.add(tabPanel);
            // Initialize the add button if it doesn't exist (first run)
            if (newTabAddBtn == null) {
                createAddTabButton();
            }
            tabContainer.add(newTabAddBtn);
        }

        // Store reference for styling
        tabPanel.putClientProperty("browser", browser);

        // Switch to new tab
        switchTab(browser);
    }

    private void createAddTabButton() {
        newTabAddBtn = new JButton("+");
        newTabAddBtn.setMargin(new Insets(2, 6, 2, 6));
        newTabAddBtn.setFocusPainted(false);
        newTabAddBtn.setBackground(new Color(245, 246, 250));
        newTabAddBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        newTabAddBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

        newTabAddBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                newTabAddBtn.setOpaque(true);
                newTabAddBtn.setBackground(new Color(220, 230, 255));
            }

            public void mouseExited(MouseEvent e) {
                newTabAddBtn.setOpaque(false);
                newTabAddBtn.setBackground(new Color(245, 246, 250));
            }
        });

        newTabAddBtn.addActionListener(e -> createNewTab("https://duckduckgo.com"));
    }

    private void switchTab(CefBrowser browser) {
        this.activeBrowser = browser;

        // Update Browser View
        browserContainer.removeAll();
        browserContainer.add(browser.getUIComponent(), BorderLayout.CENTER);

        // Update UI State
        urlBar.setText(browser.getURL());
        updateSecurityIndicator(browser.getURL());
        statusBar.setText("Ready");

        // Update Tab Styles
        for (Component comp : tabContainer.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                CefBrowser panelBrowser = (CefBrowser) panel.getClientProperty("browser");

                JLabel label = null;
                for (Component inner : panel.getComponents()) {
                    if (inner instanceof JLabel) {
                        label = (JLabel) inner;
                        break;
                    }
                }

                if (panelBrowser == browser) {
                    // Active Tab: White with Top Blue Line
                    panel.setBackground(Color.WHITE);
                    panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(33, 150, 243)), // Blue top
                            BorderFactory.createEmptyBorder(0, 1, 0, 1) // Side spacing
                    ));

                    if (label != null) {
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                        label.setForeground(new Color(33, 150, 243)); // Blue text
                    }
                } else {
                    // Inactive Tab: Gray
                    panel.setBackground(new Color(230, 230, 230));
                    panel.setBorder(BorderFactory.createEmptyBorder(2, 1, 0, 1));

                    if (label != null) {
                        label.setFont(label.getFont().deriveFont(Font.PLAIN));
                        label.setForeground(Color.DARK_GRAY);
                    }
                }
            }
        }

        // Validate layout to apply changes
        browserContainer.revalidate();
        browserContainer.repaint();
        tabContainer.revalidate();
        tabContainer.repaint();
    }

    private void closeTab(CefBrowser browser) {
        int index = browsers.indexOf(browser);
        if (index >= 0) {
            browsers.remove(index);

            // Find and remove the correct panel
            for (Component comp : tabContainer.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    if (panel.getClientProperty("browser") == browser) {
                        tabContainer.remove(panel);
                        break;
                    }
                }
            }

            // Cleanup browser
            browser.close(false);

            // Switch to another tab if available
            if (!browsers.isEmpty()) {
                // Go to previous tab or first
                int newIndex = Math.max(0, index - 1);
                switchTab(browsers.get(newIndex));
            } else {
                // Ensure there's always one tab
                createNewTab("https://duckduckgo.com");
            }

            tabContainer.revalidate();
            tabContainer.repaint();
        }
    }

    private void loadUrl(String url) {
        if (activeBrowser == null)
            return;

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

        activeBrowser.loadURL(url);
        urlBar.setText(url);

        // Add to history
        if (!SecurityManager.getInstance().isPrivateMode()) {
            HistoryManager.getInstance().addToHistory(url);
        }
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
