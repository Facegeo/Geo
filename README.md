# Geo 🌌

**Geo** is a futuristic, multimodal AI voice assistant for Android powered by Google Gemini AI, Jetpack Compose, and modern Android services. Geo combines real-time speech interaction, camera vision analysis, system overlay controls, automated tasks, and smart notification listening into a unified AI experience.

---

## ✨ Features

- **🎙️ Voice & Speech Interface**: Natural voice interaction powered by speech-to-text recognition, text-to-speech voice synthesis, and real-time animated audio waveforms.
- **👁️ Multimodal Camera Vision**: Capture and analyze images instantly using CameraX and Gemini AI for visual reasoning, object identification, and scene description.
- **🔮 Floating Overlay Orb**: System overlay floating bubble service (`GeoOverlayService`) enabling quick AI assistance from any screen on your device.
- **⚡ System Automation & Accessibility**: Android Accessibility Service integration (`GeoAccessibilityService`) supporting app launching, contact search, phone calls, SMS sending, and automated WhatsApp replies.
- **⏰ Smart Task Reminders**: AlarmManager-backed exact scheduling (`ReminderManager`, `ReminderReceiver`) with snooze options, task completion, and management via the Geo Dashboard.
- **🔔 Notification Listener**: Integrated Notification Listener Service (`NotificationService`) capturing and organizing incoming notifications for smart summaries.
- **📊 Geo Dashboard & Command Center**: Overview dashboard showing device battery level, active notification count, pending tasks, and completed task history with clear options.
- **🔒 Privacy-First Controls**: Comprehensive privacy settings for toggling camera vision, accessibility actions, notification listening, and local memory storage.

---

## 🛠️ Tech Stack & Libraries

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3 Design
- **Architecture**: MVVM (Model-View-ViewModel), Repositories, Services & Broadcast Receivers
- **AI Engine**: Google Generative AI SDK (`com.google.ai.client.generativeai`) — Gemini Models
- **Camera**: Android CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`)
- **Asynchronous Processing**: Kotlin Coroutines & Flow
- **Compatibility**: Minimum SDK 29 (Android 10) | Target SDK 36 (Android 15)

---

## 📁 Project Structure

```
Geo/
├── app/
│   ├── src/main/
│   │   ├── java/com/ernest/geo/
│   │   │   ├── ai/            # Gemini AI Integration & Command Processors
│   │   │   ├── data/          # Memory, Reminders, and Notification Repositories
│   │   │   ├── model/         # Message & Data Models
│   │   │   ├── reminder/      # AlarmManager Schedulers & Receivers
│   │   │   ├── service/       # Overlay, Accessibility, & Notification Services
│   │   │   ├── settings/      # Privacy & Preference Settings
│   │   │   ├── ui/            # Jetpack Compose Screens, Theme, & Waveform Views
│   │   │   ├── util/          # Contact & System Helpers
│   │   │   ├── viewmodel/     # Chat & AI State ViewModels
│   │   │   └── voice/         # Speech Recognition & TTS Managers
│   │   └── res/               # Layouts, Drawables, Mipmaps, and XML configs
│   └── build.gradle.kts
└── build.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Ladybug / Hedgehog or newer recommended)
- **JDK 17** or higher
- **Google Gemini API Key** (from [Google AI Studio](https://aistudio.google.com/))

### Installation & Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/Geo.git
   cd Geo
   ```

2. **Configure Gemini API Key**:
   Add your Gemini API key to your `local.properties` file in the project root directory:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Build the Project**:
   Open the project in Android Studio and trigger a Gradle Sync, or run from terminal:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on Device**:
   Deploy the application to an Android device or emulator running API 29+.

---

## 🛡️ Permissions & Setup on Device

To unlock all of Geo's capabilities, grant the following permissions on your device:

- **Microphone**: For voice commands and speech recognition.
- **Camera**: For multimodal vision analysis.
- **Notifications**: For task reminders and notification listening.
- **Display Over Other Apps**: Enables the floating overlay orb.
- **Accessibility Service**: Enables automated device actions and WhatsApp helper features.
- **Notification Listener Service**: Allows Geo to read and summarize incoming notifications.

---
