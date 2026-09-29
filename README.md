# Notify

## 1. Project Overview

Notify is a premium, highly configurable, privacy-first notification vault and manager for Android. Unlike standard utility applications that present a chaotic dashboard of cards, Notify is designed with a visual language centered on calm intelligence, modern productivity, and editorial spacing. It acts as an interceptor and intelligent archivist for device notifications.

The primary objective of Notify is to seamlessly intercept system notifications via an Android `NotificationListenerService`, categorize them based on a multi-tier resolution hierarchy, store them securely in a local SQLite database, and present them through an ultra-clean, minimalist interface. It allows users to retroactively analyze their communication flow while strictly preventing notification fatigue.

## 2. Architecture and Data Flow

The architecture follows a strict unidirectional data flow, utilizing MVVM (Model-View-ViewModel) paired with Kotlin Coroutines and StateFlow for reactive UI updates. Dependency injection is managed via Hilt, and local persistence is handled by Room Database.

### 2.1 The Interception Pipeline
1. **Capture:** The `NotificationProcessor` receives a raw `StatusBarNotification` (SBN) from the system service.
2. **Standardization:** The raw payload is passed through adapter layers (standard, messaging, email) to extract a normalized structure (Title, Text, Sender Name, Channel Importance, Conversation Flag).
3. **Resolution:** The `PriorityEngine` evaluates the normalized record against user-defined rules and system states to assign a `Tier` (HIGH, NORMAL, IGNORE).
4. **Persistence:** The notification is stored in the Room database, triggering a reactive update across all UI observers.
5. **Learning:** Background counters increment click/dismiss statistics, feeding the `LearningEngine`.

### 2.2 Tiering and Storage Retention
- **HIGH Priority:** Stored for 14 days. Reserved for VIP senders, critical apps, and high-importance system channels.
- **NORMAL Priority:** Stored for 24 hours. Represents general background noise and standard alerts.
- **IGNORE Priority:** Dropped instantly. Never hits the storage layer.

Retention is enforced via a background worker (`PurgeWorker`) that runs daily to securely scrub expired records.

## 3. Priority Resolution Hierarchy

The core intelligence of the application resides within the `PriorityEngine`. To guarantee that specific user intents always override general rules, the engine resolves priority in the following strict order (first match wins):

1. **User-Defined Sender Rules:** Surgical rules targeting a specific sender within a specific app (e.g., "John Doe" on WhatsApp).
2. **User-Defined App Priorities:** Broad rules defining the default behavior for an entire application (e.g., Ignore all of Instagram).
3. **Learned Sender Rules:** Rules accepted by the user through the Smart Suggestions system based on behavioral analytics for specific senders.
4. **Learned App Rules:** Rules accepted through the Smart Suggestions system based on behavioral analytics for entire applications.
5. **Default System Ignore List:** Hardcoded array of OS-level packages (e.g., `com.android.systemui`) that are inherently ignored.
6. **Channel Importance:** If the Android Notification Channel specifies an importance of HIGH or MAX, it is automatically tiered as HIGH.
7. **Conversation Flag:** If the notification is flagged natively as a conversation, it defaults to HIGH.
8. **Fallback:** If no conditions are met, the notification is tiered as NORMAL.

## 4. Feature Documentation

### 4.1 Home Screen (The Vault)
The Home Screen serves as the primary consumption interface, restricted strictly to HIGH priority notifications. 
- **Grouping Logic:** Notifications are aggregated first by Application, and subsequently grouped by Sender/Conversation.
- **Navigation:** Tapping an application group opens the `AppDetailScreen`, which breaks down the chronological flow of VIP messages for that specific app.

### 4.2 Activity Screen (The Feed)
A raw, chronological ledger of all retained notifications (HIGH and NORMAL). 
- **Filtering:** Users can filter the feed by specific Applications, specific Priority Tiers (High/Normal), and Date ranges (Today, Yesterday, Past Week).
- **Search:** A full-text search bar queries titles, text, and sender names across the entire database.

### 4.3 App Priorities (Global Classification)
A dedicated management interface replacing standard "Ignore" lists.
- **Functionality:** Lists every installed application on the device.
- **Grouping:** Applications with custom priority configurations (HIGH or IGNORE) are anchored to the top under "CONFIGURED APPS". Untouched applications are listed below under "OTHER APPS".
- **Modification:** Tapping an application triggers a bottom sheet to globally assign it to High, Normal, or Ignore. Modifying an app retroactively scans the database and instantly upgrades/downgrades all historical notifications belonging to that package.

### 4.4 Rules Engine (Surgical Classification)
A dedicated interface for creating exceptions (Sender Rules).
- **Sender Key Generation:** To prevent spelling mismatches, creating a rule requires selecting the parent Application from an `ExposedDropdownMenuBox` (populated dynamically from the database). The system extracts the underlying package name (e.g., `com.whatsapp`) and concatenates it with the manually typed sender name, generating a foolproof `senderKey`.
- **Retroactive Application:** Adding or deleting a Sender Rule triggers a background query that instantly realigns the tier of all existing notifications from that sender.

### 4.5 Smart Suggestions (Learning Engine)
An analytical module that monitors user interaction patterns.
- **Metrics Tracking:** Tracks total captures, clicks, and dismissals per entity (App or Sender).
- **Thresholds:** If an entity has a click rate > 60% (minimum 3 notifications), it flags for a HIGH priority upgrade. If an entity has a dismiss/swipe rate > 80% (minimum 5 notifications), it flags for an IGNORE downgrade.
- **Actionability:** Suggestions are presented in a dedicated UI. Users can accept, reject, or snooze ("Never Ask") the recommendation. Accepted suggestions are written to the rule repository.

### 4.6 Notification Inspection (Detail View)
Clicking any notification opens the `NotificationDetailScreen`.
- **Content Display:** Renders the full title, body text, and sender identity.
- **Metadata:** Displays critical debugging and contextual metadata:
  - Priority (High/Normal)
  - Type (Conversation/Message)
  - Sender ID (The exact string extracted by the engine for rule matching)
  - Captured Timestamp (Exact device time of interception)
- **Actions:** Provides immediate access to delete the notification from the local SQLite vault entirely, or securely bounce the user into the parent application via package intent.

### 4.7 Storage and Export Analytics
- **Storage Metrics:** The Settings screen provides real-time counts of HIGH and NORMAL notifications, along with rule counts.
- **Data Deletion:** A master kill-switch to wipe the local Room database, ensuring privacy is in the hands of the user.
- **JSON Export:** Users can query specific date ranges and tiers, exporting the raw notification payload to a local JSON file for external archival or analysis.

## 5. UI / UX Principles

The user interface was built iteratively adhering to strict aesthetic constraints:
- **Aesthetic Definition:** "Calm intelligence + private vault + modern productivity".
- **Color System:** Avoidance of default Material purple/blue. Usage of custom tokens (Ink950, PrimarySoft, SurfaceColor, BorderColor).
- **Component Architecture:** Flattened, spacious lists utilizing `HorizontalDivider` over elevated cards. Focus on typography hierarchy (Inter/Roboto) and generous padding to ensure an editorial, fintech-like presentation. Buttons (`PrimaryButton`, `SecondaryButton`) are pill-shaped with subtle interactions.
