# WA Alarm 🔔

An Android app that triggers a loud alarm when a **specific person** messages you on WhatsApp — so you can sleep without missing important messages.

---

## How It Works

1. Uses Android's `NotificationListenerService` to read incoming notifications
2. Filters by **contact name** (as saved in your phonebook)
3. If the sender matches → plays your default alarm sound + vibrates
4. Works for both **individual chats** and **group chats**

---

## Setup

### 1. Clone & Open
```bash
git clone https://github.com/ronitx05/WhatsAppAlarm
```
Open in **Android Studio** (Electric Eel or newer).

### 2. Build & Install
Run on your physical Android device (minSdk 26 = Android 8.0+).

### 3. Grant Permission
- Open the app
- Tap **"Grant Permission"**
- Find **"WA Alarm"** in the Notification Listener list and enable it

### 4. Add a Contact
- Tap the **+** button
- Enter the name **exactly as it appears** in your WhatsApp notifications
  - e.g., if WhatsApp shows "John Doe" → type `John Doe`

That's it. Lock your phone and sleep. 💤

---

## Project Structure

```
app/src/main/java/com/example/whatsappalarm/
├── MainActivity.kt                  # UI — add/remove watched contacts
├── WhatsAppNotificationListener.kt  # Core logic — intercepts notifications
├── WatchedContactsPrefs.kt          # Stores contact list in SharedPreferences
└── ui/theme/Theme.kt                # Dark green theme
```

---

## Known Limitations

| Scenario | Works? |
|---|---|
| Individual WhatsApp chat | ✅ Yes |
| Group chat (name in text preview) | ✅ Yes |
| Notification previews disabled in WhatsApp | ❌ No — name not visible |
| WhatsApp Business | ✅ Yes (same package filter) |

> **Fix for previews:** WhatsApp Settings → Notifications → "Show Notifications" → enable message preview.

---

## Tech Stack

- **Kotlin** + **Jetpack Compose**
- `NotificationListenerService` — system API for notification access
- `MediaPlayer` with `USAGE_ALARM` audio focus — plays over DND/silent mode
- `SharedPreferences` + Kotlin `Flow` — reactive contact list

---

## Why Android Only?

iOS sandboxing does not allow any app to read another app's notifications. This is fundamentally an Android-only capability.

---

## License
MIT
