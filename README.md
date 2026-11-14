# CampusPulse Attendance & Dismissal Logs (Android App)

A native Android digital check-in and campus dismissal system built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room (SQLite)** that lets teachers mark attendance in seconds and automatically alerts parents via **Direct Android Cellular SMS (`SmsManager`)**, **Twilio SMS / WhatsApp**, or **Resend Email** when their child arrives at or leaves campus.

---

## Key Features

1. **Mark Attendance in Seconds (`RollCallScreen.kt`)**
   - **1-Tap Student Roster Grid**: Tap `Present`, `Late`, `Absent`, or `Dismiss` on any student card to record an exact timestamp and immediately trigger a parent alert.
   - **⚡ 1-Tap "Mark Remaining Present" Speed Action**: Teachers only need to tap the 1–2 absent or late students, then tap **"1-Tap Mark Remaining Present & Alert Parents"** to complete the entire class roll call and notify all parents in under 3 seconds.
   - **Pre-Seeded Offline-First Room Database**: Pre-loaded with 3 demo classrooms (*Grade 3A • Homeroom*, *Grade 5B • STEM Lab*, *Kindergarten • Sunshine*) and 15 students with guardian contact details, preferred alert channels, and pickup authorizations.
   - **Add Student Modal**: Add new students, badge IDs, guardian phone/email, preferred alert channel, and carline/bus notes on the fly.

2. **Afternoon Campus Dismissal & Authorized Pickup Verification (`DismissalScreen.kt`)**
   - Filter students by **Parent Carline**, **School Bus**, **Authorized Walker**, or **Campus Aftercare**.
   - Displays each student's **Authorized Pickup Adults** list (e.g., *Elena Martinez (Mother), Carlos Martinez (Father), Rosa Gomez (Grandmother)*) and Carline Tag / Bus Route notes.
   - **1-Tap Individual Release** or **Batch Bus/Carline Release** (release an entire bus route or carline wave and notify all parents simultaneously).

3. **Real-Time Parent Alert Dispatcher (`ParentAlertDispatcher.kt`)**
   - **Direct Android Cellular SMS (`SmsManager`)**: Sends SMS messages directly through the phone/tablet's SIM card with zero third-party API fees when `SEND_SMS` permission is granted.
   - **Twilio SMS & WhatsApp API**: Native OkHttp integration with Twilio's `/Messages.json` endpoint for SMS and `whatsapp:` numbers.
   - **Resend Transactional Email API**: Native OkHttp integration with `https://api.resend.com/emails`.
   - **Safe Sandbox Mode (Default) + Offline Queue & Retry**: Logs and previews every generated alert in Sandbox Mode so you can test freely, and queues alerts for 1-tap retry if offline.

4. **Audit Logs & CSV Export (`LogsScreen.kt`)**
   - **Parent Alerts Tab**: Inspect every sent/simulated/queued message, recipient contact, channel, and timestamp with a 1-tap **Resend** button.
   - **Attendance & Dismissal Log Tab**: View arrival and dismissal timestamps and export a daily CSV report via the Android Share Sheet (`Intent.ACTION_SEND`).

---

## Opening & Running in Android Studio

1. Open **Android Studio** (Iguana / Jellyfish / Koala or newer).
2. Select **Open** and choose the `c:\Users\User\Desktop\Attendance app\Android app` folder.
3. Let Android Studio sync Gradle and download the Android SDK / Jetpack Compose dependencies.
4. Click **Run ('app')** on an Android emulator or physical Android device (API 26+).
