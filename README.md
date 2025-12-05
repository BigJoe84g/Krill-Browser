# 🦐 Krill Browser

A privacy-focused, feature-rich web browser built with Java and JavaFX.

![Java](https://img.shields.io/badge/Java-21+-blue?logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21-orange)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)

## ✨ Features

### 🛡️ Security First
- **50+ Trackers Blocked** - Google Analytics, Facebook, ad networks
- **URL Cleaning** - Removes tracking parameters (utm, fbclid, etc.)
- **HTTPS Upgrade** - Automatically upgrades HTTP to HTTPS
- **HTTPS-Only Mode** - Block all insecure connections
- **Private Mode** - Browse without saving history
- **Panic Button** - Instantly clear all data

### 👤 Unique Profiles
| Profile | Purpose |
|---------|---------|
| 🦐 Default | Balanced browsing |
| 🎮 Gaming | Blocks social media distractions |
| 💼 Work | Blocks entertainment sites |
| 💻 Coding | Developer-friendly, allows localhost |
| 🔒 Secure | Maximum privacy, disables JavaScript |

### 🔍 DuckDuckGo Search
Privacy-respecting search by default. No Google tracking.

## 🚀 Quick Start

### Requirements
- Java 21+ (JDK)
- JavaFX SDK 21+

### Run
```bash
./run.sh
```

Or manually:
```bash
java --module-path javafx-sdk-21.0.5/lib:out \
     --add-modules javafx.controls,javafx.web \
     -m KrillBrowser/com.krillbrowser.KrillBrowser
```

## 📁 Project Structure
```
Krill Browser/
├── src/
│   ├── com/krillbrowser/
│   │   ├── KrillBrowser.java      # Main application
│   │   ├── BrowserTab.java        # Tab management
│   │   ├── BrowserProfile.java    # Profile system
│   │   ├── SecurityManager.java   # Basic security
│   │   ├── AdvancedSecurityManager.java  # Tracker blocking
│   │   ├── HistoryManager.java    # Browsing history
│   │   ├── BookmarkManager.java   # Bookmarks
│   │   └── CookieManager.java     # Cookie management
│   ├── styles/
│   │   └── browser.css            # UI styling
│   └── module-info.java
├── run.sh                         # Launch script
└── README.md
```

## 🎯 Keyboard Shortcuts
| Shortcut | Action |
|----------|--------|
| Enter (in URL bar) | Navigate |
| Back button | Go back |
| Forward button | Go forward |

## ⚠️ Limitations
This is a learning project. For maximum security, use Firefox or Brave for:
- Banking
- Email
- Shopping
- Any sensitive accounts

## 📄 License
MIT License - Free to use, modify, and distribute.

## 🤝 Contributing
Contributions welcome! Feel free to:
- Report bugs
- Suggest features
- Submit pull requests

---
Built with ❤️ for learning Java and AP CSA preparation 🦐
